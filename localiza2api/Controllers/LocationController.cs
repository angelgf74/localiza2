using System.Security.Claims;
using localiza2api.Data;
using localiza2api.DTOs;
using localiza2api.Models;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace localiza2api.Controllers;

[ApiController]
[Route("api/location")]
[Authorize]
public class LocationController(AppDbContext db) : ControllerBase
{
    private int CurrentUserId => int.Parse(User.FindFirstValue(ClaimTypes.NameIdentifier)!);

    [HttpPost]
    public async Task<IActionResult> UpdateLocation([FromBody] UpdateLocationDto dto)
    {
        db.UserLocations.Add(new UserLocation
        {
            UserId       = CurrentUserId,
            Latitude     = dto.Latitude,
            Longitude    = dto.Longitude,
            Accuracy     = dto.Accuracy,
            BatteryLevel = dto.BatteryLevel,
            Timestamp    = DateTime.UtcNow
        });
        await db.SaveChangesAsync();
        await PruneLocationsAsync(CurrentUserId);
        return NoContent();
    }

    // Política de retención por ventana temporal:
    //   Últimas 3 h     → 1 registro / minuto   (máx. 180)
    //   3 h – 24 h      → 1 registro / 5 min    (máx. 252)
    //   1 día – 30 días → 1 registro / 30 min   (máx. 1440)
    //   > 30 días       → eliminar todo
    private async Task PruneLocationsAsync(int userId)
    {
        var now      = DateTime.UtcNow;
        var cut30d   = now.AddDays(-30);
        var cut24h   = now.AddHours(-24);
        var cut3h    = now.AddHours(-3);

        // > 30 días: purga total
        await db.Database.ExecuteSqlAsync($"""
            DELETE FROM "UserLocations"
            WHERE "UserId" = {userId} AND "Timestamp" < {cut30d}
            """);

        // 1 día–30 días: conservar 1 por bucket de 30 min (el más reciente)
        await db.Database.ExecuteSqlAsync($"""
            DELETE FROM "UserLocations"
            WHERE "UserId" = {userId}
              AND "Timestamp" >= {cut30d}
              AND "Timestamp" < {cut24h}
              AND "Id" NOT IN (
                SELECT MAX("Id") FROM "UserLocations"
                 WHERE "UserId" = {userId}
                   AND "Timestamp" >= {cut30d}
                   AND "Timestamp" < {cut24h}
                 GROUP BY to_timestamp(floor(extract(epoch FROM "Timestamp") / 1800) * 1800)
              )
            """);

        // 3 h–24 h: conservar 1 por bucket de 5 min
        await db.Database.ExecuteSqlAsync($"""
            DELETE FROM "UserLocations"
            WHERE "UserId" = {userId}
              AND "Timestamp" >= {cut24h}
              AND "Timestamp" < {cut3h}
              AND "Id" NOT IN (
                SELECT MAX("Id") FROM "UserLocations"
                 WHERE "UserId" = {userId}
                   AND "Timestamp" >= {cut24h}
                   AND "Timestamp" < {cut3h}
                 GROUP BY to_timestamp(floor(extract(epoch FROM "Timestamp") / 300) * 300)
              )
            """);

        // Últimas 3 h: conservar 1 por bucket de 1 min
        await db.Database.ExecuteSqlAsync($"""
            DELETE FROM "UserLocations"
            WHERE "UserId" = {userId}
              AND "Timestamp" >= {cut3h}
              AND "Id" NOT IN (
                SELECT MAX("Id") FROM "UserLocations"
                 WHERE "UserId" = {userId}
                   AND "Timestamp" >= {cut3h}
                 GROUP BY to_timestamp(floor(extract(epoch FROM "Timestamp") / 60) * 60)
              )
            """);
    }

    [HttpGet("me")]
    public async Task<IActionResult> GetMyLocation()
    {
        var location = await db.UserLocations
            .Where(l => l.UserId == CurrentUserId)
            .OrderByDescending(l => l.Timestamp)
            .FirstOrDefaultAsync();

        if (location is null) return NotFound(new { message = "Sin ubicación disponible." });

        return Ok(new
        {
            location.Latitude,
            location.Longitude,
            location.Accuracy,
            location.Timestamp
        });
    }

    [HttpGet("me/history")]
    public async Task<IActionResult> GetMyLocationHistory([FromQuery] int limit = 50)
    {
        var locations = await db.UserLocations
            .Where(l => l.UserId == CurrentUserId)
            .OrderByDescending(l => l.Timestamp)
            .Take(limit)
            .OrderBy(l => l.Timestamp)
            .Select(l => new LocationPointDto(l.Latitude, l.Longitude, l.Accuracy, l.Timestamp))
            .ToListAsync();

        return Ok(locations);
    }

    [HttpGet("contacts/{contactId}/history")]
    public async Task<IActionResult> GetContactLocationHistory(int contactId, [FromQuery] int limit = 50)
    {
        var contact = await db.Contacts.FirstOrDefaultAsync(c =>
            c.Id == contactId
            && c.UserId == CurrentUserId
            && c.Status == ContactStatus.Accepted
            && c.LocationPermissionGranted
            && c.ContactUserId != null);

        if (contact is null) return NotFound();

        var locations = await db.UserLocations
            .Where(l => l.UserId == contact.ContactUserId)
            .OrderByDescending(l => l.Timestamp)
            .Take(limit)
            .OrderBy(l => l.Timestamp)
            .Select(l => new LocationPointDto(l.Latitude, l.Longitude, l.Accuracy, l.Timestamp))
            .ToListAsync();

        return Ok(locations);
    }

    [HttpGet("contacts")]
    public async Task<IActionResult> GetContactsLocations()
    {
        var acceptedContacts = await db.Contacts
            .Where(c => c.UserId == CurrentUserId
                     && c.Status == ContactStatus.Accepted
                     && c.LocationPermissionGranted
                     && c.ContactUserId != null)
            .ToListAsync();

        var contactUserIds = acceptedContacts.Select(c => c.ContactUserId!.Value).ToList();

        var latestLocations = await db.UserLocations
            .Where(l => contactUserIds.Contains(l.UserId))
            .GroupBy(l => l.UserId)
            .Select(g => g.OrderByDescending(l => l.Timestamp).First())
            .ToListAsync();

        var result = latestLocations.Select(loc =>
        {
            var contact = acceptedContacts.First(c => c.ContactUserId == loc.UserId);
            return new ContactLocationDto(
                contact.Id,
                contact.Alias,
                contact.PhotoUrl,
                loc.Latitude,
                loc.Longitude,
                loc.Accuracy,
                loc.Timestamp,
                loc.BatteryLevel);
        }).ToList();

        // Incluir la ubicación del propio usuario con id=0
        var myLocation = await db.UserLocations
            .Where(l => l.UserId == CurrentUserId)
            .OrderByDescending(l => l.Timestamp)
            .FirstOrDefaultAsync();

        if (myLocation != null)
        {
            var me = await db.Users.FindAsync(CurrentUserId);
            result.Insert(0, new ContactLocationDto(
                0,
                me?.Name ?? "Yo",
                null,
                myLocation.Latitude,
                myLocation.Longitude,
                myLocation.Accuracy,
                myLocation.Timestamp,
                myLocation.BatteryLevel));
        }

        return Ok(result);
    }

    [HttpGet("contacts/{contactId}")]
    public async Task<IActionResult> GetContactLocation(int contactId)
    {
        var contact = await db.Contacts.FirstOrDefaultAsync(c =>
            c.Id == contactId
            && c.UserId == CurrentUserId
            && c.Status == ContactStatus.Accepted
            && c.LocationPermissionGranted
            && c.ContactUserId != null);

        if (contact is null) return NotFound();

        var location = await db.UserLocations
            .Where(l => l.UserId == contact.ContactUserId)
            .OrderByDescending(l => l.Timestamp)
            .FirstOrDefaultAsync();

        if (location is null) return NotFound(new { message = "Sin ubicación disponible." });

        return Ok(new ContactLocationDto(
            contact.Id,
            contact.Alias,
            contact.PhotoUrl,
            location.Latitude,
            location.Longitude,
            location.Accuracy,
            location.Timestamp,
            location.BatteryLevel));
    }
}
