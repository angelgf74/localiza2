using localiza2api.Data;
using localiza2api.DTOs;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace localiza2api.Controllers;

[ApiController]
[Route("api/admin")]
[Authorize(Roles = "SuperAdmin")]
public class AdminController(AppDbContext db) : ControllerBase
{
    [HttpGet("users")]
    public async Task<IActionResult> GetUsers()
    {
        var users = await db.Users
            .OrderBy(u => u.Name)
            .Select(u => new AdminUserDto(
                u.Id,
                u.Email,
                u.Name,
                u.CreatedAt,
                u.SharingEnabled,
                db.UserLocations
                    .Where(l => l.UserId == u.Id)
                    .OrderByDescending(l => l.Timestamp)
                    .Select(l => (DateTime?)l.Timestamp)
                    .FirstOrDefault()))
            .ToListAsync();

        return Ok(users);
    }

    [HttpGet("users/{userId}/history")]
    public async Task<IActionResult> GetUserHistory(int userId, [FromQuery] int limit = 100, [FromQuery] DateTime? before = null)
    {
        if (!await db.Users.AnyAsync(u => u.Id == userId))
            return NotFound();

        var query = db.UserLocations.Where(l => l.UserId == userId);
        if (before.HasValue)
            query = query.Where(l => l.Timestamp < before.Value.ToUniversalTime());

        var locations = await query
            .OrderByDescending(l => l.Timestamp)
            .Take(limit)
            .OrderBy(l => l.Timestamp)
            .Select(l => new AdminLocationPointDto(l.Latitude, l.Longitude, l.Accuracy, l.BatteryLevel, l.Timestamp))
            .ToListAsync();

        return Ok(locations);
    }
}
