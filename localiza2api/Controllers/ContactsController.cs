using System.Security.Claims;
using localiza2api.Data;
using localiza2api.DTOs;
using localiza2api.Models;
using localiza2api.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace localiza2api.Controllers;

[ApiController]
[Route("api/contacts")]
[Authorize]
public class ContactsController(AppDbContext db, EmailService emailService) : ControllerBase
{
    private int CurrentUserId => int.Parse(User.FindFirstValue(ClaimTypes.NameIdentifier)!);

    [HttpGet]
    public async Task<IActionResult> GetContacts()
    {
        var contacts = await db.Contacts
            .Where(c => c.UserId == CurrentUserId)
            .Select(c => new ContactDto(
                c.Id,
                c.Email,
                c.Alias,
                c.PhotoUrl,
                c.Status.ToString(),
                c.LocationPermissionGranted,
                c.ContactUserId))
            .ToListAsync();
        return Ok(contacts);
    }

    [HttpPost("invite")]
    public async Task<IActionResult> Invite([FromBody] InviteContactDto dto)
    {
        var email = dto.Email.ToLower();
        if (await db.Contacts.AnyAsync(c => c.UserId == CurrentUserId && c.Email == email))
            return Conflict(new { message = "Ya tienes este contacto." });

        var currentUser = await db.Users.FindAsync(CurrentUserId);
        var contactUser = await db.Users.FirstOrDefaultAsync(u => u.Email == email);

        var contact = new Contact
        {
            UserId = CurrentUserId,
            Email = email,
            Alias = dto.Alias,
            ContactUserId = contactUser?.Id,
            Status = ContactStatus.Pending
        };
        db.Contacts.Add(contact);

        var invitation = new ContactInvitation
        {
            InviterUserId = CurrentUserId,
            InvitedEmail = email,
            Token = TokenService.GenerateSecureToken(),
            ExpiresAt = DateTime.UtcNow.AddDays(7)
        };
        db.ContactInvitations.Add(invitation);
        await db.SaveChangesAsync();

        await emailService.SendContactInvitationEmailAsync(
            email,
            currentUser!.Name,
            invitation.Token,
            contactUser is not null);

        return Ok(new { message = "Invitación enviada." });
    }

    [HttpGet("accept/{token}")]
    [AllowAnonymous]
    public async Task<IActionResult> AcceptInvitation(string token)
    {
        var invitation = await db.ContactInvitations
            .Include(i => i.InviterUser)
            .FirstOrDefaultAsync(i => i.Token == token);

        if (invitation is null)
            return NotFound(new { message = "Invitación inválida o expirada." });

        if (invitation.ExpiresAt < DateTime.UtcNow)
        {
            db.ContactInvitations.Remove(invitation);
            await db.SaveChangesAsync();
            return BadRequest(new { message = "La invitación ha expirado." });
        }

        var invitedUser = await db.Users.FirstOrDefaultAsync(u => u.Email == invitation.InvitedEmail);
        if (invitedUser is null)
            return BadRequest(new
            {
                message = "Descarga localiza2 y regístrate con este mismo correo. La invitación se aceptará automáticamente al confirmar tu cuenta.",
                inviterName = invitation.InviterUser.Name
            });

        var contact = await db.Contacts.FirstOrDefaultAsync(c =>
            c.UserId == invitation.InviterUserId && c.Email == invitation.InvitedEmail);

        if (contact is not null)
        {
            contact.Status = ContactStatus.Accepted;
            contact.LocationPermissionGranted = true;
            contact.ContactUserId = invitedUser.Id;
        }

        // Crear el contacto recíproco si no existe
        var reciprocal = await db.Contacts.FirstOrDefaultAsync(c =>
            c.UserId == invitedUser.Id && c.Email == invitation.InviterUser.Email);
        if (reciprocal is null)
        {
            db.Contacts.Add(new Contact
            {
                UserId = invitedUser.Id,
                Email = invitation.InviterUser.Email,
                Alias = invitation.InviterUser.Name,
                ContactUserId = invitation.InviterUserId,
                Status = ContactStatus.Accepted,
                LocationPermissionGranted = true
            });
        }
        else
        {
            reciprocal.Status = ContactStatus.Accepted;
            reciprocal.LocationPermissionGranted = true;
        }

        db.ContactInvitations.Remove(invitation);
        await db.SaveChangesAsync();

        return Ok(new { message = "Invitación aceptada. Ahora podéis compartir ubicación." });
    }

    [HttpPut("{id}")]
    public async Task<IActionResult> UpdateContact(int id, [FromBody] UpdateContactDto dto)
    {
        var contact = await db.Contacts.FirstOrDefaultAsync(c => c.Id == id && c.UserId == CurrentUserId);
        if (contact is null) return NotFound();

        contact.Alias = dto.Alias;
        contact.PhotoUrl = dto.PhotoUrl;
        await db.SaveChangesAsync();
        return NoContent();
    }

    [HttpDelete("{id}")]
    public async Task<IActionResult> DeleteContact(int id)
    {
        var contact = await db.Contacts.FirstOrDefaultAsync(c => c.Id == id && c.UserId == CurrentUserId);
        if (contact is null) return NotFound();

        db.Contacts.Remove(contact);
        await db.SaveChangesAsync();
        return NoContent();
    }

    // ── QR / Pairing ─────────────────────────────────────────────────────────

    [HttpPost("pair/qr")]
    public async Task<IActionResult> GenerateQrCode()
    {
        var user = await db.Users.FindAsync(CurrentUserId);
        if (user is null) return NotFound();

        user.PairingCode = TokenService.GenerateSecureToken()[..20];
        user.PairingCodeExpiry = DateTime.UtcNow.AddHours(24);
        await db.SaveChangesAsync();

        return Ok(new
        {
            token      = user.PairingCode,
            inviterName = user.Name,
            expiresAt  = user.PairingCodeExpiry
        });
    }

    [HttpDelete("pair/qr")]
    public async Task<IActionResult> RevokeQrCode()
    {
        var user = await db.Users.FindAsync(CurrentUserId);
        if (user is null) return NotFound();
        user.PairingCode = null;
        user.PairingCodeExpiry = null;
        await db.SaveChangesAsync();
        return NoContent();
    }

    [HttpGet("pair/info/{token}")]
    [AllowAnonymous]
    public async Task<IActionResult> GetPairingInfo(string token)
    {
        // Buscar por QR code
        var inviter = await db.Users.FirstOrDefaultAsync(u =>
            u.PairingCode == token && u.PairingCodeExpiry > DateTime.UtcNow);
        if (inviter is not null)
            return Ok(new { inviterName = inviter.Name, type = "qr" });

        // Buscar en invitaciones por email
        var invitation = await db.ContactInvitations
            .Include(i => i.InviterUser)
            .FirstOrDefaultAsync(i => i.Token == token && i.ExpiresAt > DateTime.UtcNow);
        if (invitation is not null)
            return Ok(new { inviterName = invitation.InviterUser.Name, type = "email", invitedEmail = invitation.InvitedEmail });

        return NotFound(new { message = "Código de emparejamiento inválido o expirado." });
    }

    [HttpPost("pair/accept")]
    public async Task<IActionResult> AcceptPairing([FromBody] AcceptPairingDto dto)
    {
        var me = await db.Users.FindAsync(CurrentUserId);
        if (me is null) return NotFound();

        // Intentar con QR token
        var inviter = await db.Users.FirstOrDefaultAsync(u =>
            u.PairingCode == dto.Token && u.PairingCodeExpiry > DateTime.UtcNow);
        if (inviter is not null)
        {
            if (inviter.Id == CurrentUserId)
                return BadRequest(new { message = "No puedes emparejarte contigo mismo." });

            await CreateBilateralContactAsync(inviter.Id, inviter.Name, inviter.Email,
                                              CurrentUserId, me.Name, me.Email);
            inviter.PairingCode = null;
            inviter.PairingCodeExpiry = null;
            await db.SaveChangesAsync();
            return Ok(new { message = $"Ahora estás conectado con {inviter.Name}.", contactName = inviter.Name });
        }

        // Intentar con invitación por email
        var invitation = await db.ContactInvitations
            .Include(i => i.InviterUser)
            .FirstOrDefaultAsync(i => i.Token == dto.Token && i.ExpiresAt > DateTime.UtcNow);
        if (invitation is not null)
        {
            if (!string.Equals(invitation.InvitedEmail, me.Email, StringComparison.OrdinalIgnoreCase))
                return Forbid();

            await CreateBilateralContactAsync(invitation.InviterUserId, invitation.InviterUser.Name, invitation.InviterUser.Email,
                                              CurrentUserId, me.Name, me.Email);
            db.ContactInvitations.Remove(invitation);
            await db.SaveChangesAsync();
            return Ok(new { message = $"Ahora estás conectado con {invitation.InviterUser.Name}.", contactName = invitation.InviterUser.Name });
        }

        return NotFound(new { message = "Código de emparejamiento inválido o expirado." });
    }

    private async Task CreateBilateralContactAsync(
        int userAId, string userAName, string userAEmail,
        int userBId, string userBName, string userBEmail)
    {
        var ab = await db.Contacts.FirstOrDefaultAsync(c => c.UserId == userAId && c.Email == userBEmail);
        if (ab is null)
            db.Contacts.Add(new Contact { UserId = userAId, Email = userBEmail, Alias = userBName,
                ContactUserId = userBId, Status = ContactStatus.Accepted, LocationPermissionGranted = true });
        else
        { ab.Status = ContactStatus.Accepted; ab.LocationPermissionGranted = true; ab.ContactUserId = userBId; }

        var ba = await db.Contacts.FirstOrDefaultAsync(c => c.UserId == userBId && c.Email == userAEmail);
        if (ba is null)
            db.Contacts.Add(new Contact { UserId = userBId, Email = userAEmail, Alias = userAName,
                ContactUserId = userAId, Status = ContactStatus.Accepted, LocationPermissionGranted = true });
        else
        { ba.Status = ContactStatus.Accepted; ba.LocationPermissionGranted = true; ba.ContactUserId = userAId; }

        await db.SaveChangesAsync();
    }
}
