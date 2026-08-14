using System.Security.Claims;
using localiza2api.Data;
using localiza2api.DTOs;
using localiza2api.Models;
using localiza2api.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.RateLimiting;
using Microsoft.EntityFrameworkCore;

namespace localiza2api.Controllers;

[ApiController]
[Route("api/auth")]
[EnableRateLimiting("auth")]
public class AuthController(AppDbContext db, EmailService emailService, TokenService tokenService, IConfiguration config) : ControllerBase
{
    private int CurrentUserId => int.Parse(User.FindFirstValue(ClaimTypes.NameIdentifier)!);
    private string WebUrl => config["App:WebUrl"]
        ?? (config["App:BaseUrl"] ?? "").Replace("-api.", "-app.").TrimEnd('/');
    [HttpPost("register")]
    public async Task<IActionResult> Register([FromBody] RegisterDto dto)
    {
        if (string.IsNullOrWhiteSpace(dto.Password) || dto.Password.Length < 8)
            return BadRequest(new { message = "La contraseña debe tener al menos 8 caracteres." });

        if (await db.Users.AnyAsync(u => u.Email == dto.Email.ToLower()))
            return Conflict(new { message = "El correo ya está registrado." });

        await db.PendingRegistrations
            .Where(p => p.Email == dto.Email.ToLower() && p.ExpiresAt < DateTime.UtcNow)
            .ExecuteDeleteAsync();

        if (await db.PendingRegistrations.AnyAsync(p => p.Email == dto.Email.ToLower()))
            return Conflict(new { message = "Ya existe una confirmación pendiente para este correo." });

        var pending = new PendingRegistration
        {
            Email = dto.Email.ToLower(),
            PasswordHash = BCrypt.Net.BCrypt.HashPassword(dto.Password),
            Name = dto.Name,
            Token = TokenService.GenerateSecureToken(),
            ExpiresAt = DateTime.UtcNow.AddHours(24)
        };
        db.PendingRegistrations.Add(pending);
        await db.SaveChangesAsync();

        try
        {
            await emailService.SendConfirmationEmailAsync(pending.Email, pending.Name, pending.Token);
        }
        catch (Exception ex)
        {
            db.PendingRegistrations.Remove(pending);
            await db.SaveChangesAsync();
            return StatusCode(500, new { message = "No se pudo enviar el correo de confirmación. Inténtalo de nuevo más tarde." });
        }

        return Ok(new { message = "Se ha enviado un correo de confirmación. Revisa tu bandeja de entrada." });
    }

    [HttpGet("confirm/{token}")]
    public async Task<IActionResult> Confirm(string token)
    {
        var pending = await db.PendingRegistrations.FirstOrDefaultAsync(p => p.Token == token);
        if (pending is null)
            return Redirect($"{WebUrl}/confirm.html?status=invalid");

        if (pending.ExpiresAt < DateTime.UtcNow)
        {
            db.PendingRegistrations.Remove(pending);
            await db.SaveChangesAsync();
            return Redirect($"{WebUrl}/confirm.html?status=expired");
        }

        var user = new User
        {
            Email = pending.Email,
            PasswordHash = pending.PasswordHash,
            Name = pending.Name
        };
        db.Users.Add(user);
        db.PendingRegistrations.Remove(pending);
        await db.SaveChangesAsync();

        // Actualizar los contactos que ya existían con este email
        var existingContacts = await db.Contacts
            .Where(c => c.Email == user.Email && c.ContactUserId == null)
            .ToListAsync();
        foreach (var contact in existingContacts)
            contact.ContactUserId = user.Id;

        // Aceptar invitaciones pendientes dirigidas a este email
        var pendingInvitations = await db.ContactInvitations
            .Include(i => i.InviterUser)
            .Where(i => i.InvitedEmail == user.Email && i.ExpiresAt >= DateTime.UtcNow)
            .ToListAsync();

        foreach (var invitation in pendingInvitations)
        {
            var contact = await db.Contacts.FirstOrDefaultAsync(c =>
                c.UserId == invitation.InviterUserId && c.Email == user.Email);
            if (contact is not null)
            {
                contact.Status = ContactStatus.Accepted;
                contact.LocationPermissionGranted = true;
                contact.ContactUserId = user.Id;
            }

            var reciprocal = await db.Contacts.FirstOrDefaultAsync(c =>
                c.UserId == user.Id && c.Email == invitation.InviterUser.Email);
            if (reciprocal is null)
            {
                db.Contacts.Add(new Contact
                {
                    UserId = user.Id,
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
        }

        await db.SaveChangesAsync();

        return Redirect($"{WebUrl}/confirm.html?status=ok");
    }

    [HttpPost("login")]
    public async Task<IActionResult> Login([FromBody] LoginDto dto)
    {
        var user = await db.Users.FirstOrDefaultAsync(u => u.Email == dto.Email.ToLower());
        if (user is null || !BCrypt.Net.BCrypt.Verify(dto.Password, user.PasswordHash))
            return Unauthorized(new { message = "Credenciales incorrectas." });

        var token = tokenService.GenerateJwt(user.Id, user.Email, user.Role);
        return Ok(new LoginResponseDto(token, user.Id, user.Name, user.Email, user.Role.ToString()));
    }

    [HttpPost("resend-confirmation")]
    public async Task<IActionResult> ResendConfirmation([FromBody] ForgotPasswordDto dto)
    {
        await db.PendingRegistrations
            .Where(p => p.Email == dto.Email.ToLower() && p.ExpiresAt < DateTime.UtcNow)
            .ExecuteDeleteAsync();

        var pending = await db.PendingRegistrations
            .FirstOrDefaultAsync(p => p.Email == dto.Email.ToLower());

        if (pending is null)
            return Ok(new { message = "Si el correo tiene una confirmación pendiente, recibirás un nuevo enlace." });

        pending.Token = TokenService.GenerateSecureToken();
        pending.ExpiresAt = DateTime.UtcNow.AddHours(24);
        await db.SaveChangesAsync();

        try
        {
            await emailService.SendConfirmationEmailAsync(pending.Email, pending.Name, pending.Token);
        }
        catch
        {
            return StatusCode(500, new { message = "No se pudo enviar el correo. Inténtalo de nuevo más tarde." });
        }

        return Ok(new { message = "Correo reenviado. Revisa tu bandeja de entrada." });
    }

    [HttpPost("forgot-password")]
    public async Task<IActionResult> ForgotPassword([FromBody] ForgotPasswordDto dto)
    {
        var user = await db.Users.FirstOrDefaultAsync(u => u.Email == dto.Email.ToLower());
        // Respuesta idéntica tanto si el email existe como si no (evita enumeración)
        if (user is null)
            return Ok(new { message = "Si el correo está registrado, recibirás un enlace para restablecer la contraseña." });

        user.PasswordResetToken = TokenService.GenerateSecureToken();
        user.PasswordResetExpiry = DateTime.UtcNow.AddHours(1);
        await db.SaveChangesAsync();

        try
        {
            await emailService.SendPasswordResetEmailAsync(user.Email, user.Name, user.PasswordResetToken);
        }
        catch
        {
            user.PasswordResetToken = null;
            user.PasswordResetExpiry = null;
            await db.SaveChangesAsync();
            return StatusCode(500, new { message = "No se pudo enviar el correo. Inténtalo de nuevo más tarde." });
        }

        return Ok(new { message = "Si el correo está registrado, recibirás un enlace para restablecer la contraseña." });
    }

    [HttpPost("reset-password")]
    public async Task<IActionResult> ResetPassword([FromBody] ResetPasswordDto dto)
    {
        if (string.IsNullOrWhiteSpace(dto.NewPassword) || dto.NewPassword.Length < 8)
            return BadRequest(new { message = "La contraseña debe tener al menos 8 caracteres." });

        var user = await db.Users.FirstOrDefaultAsync(u => u.PasswordResetToken == dto.Token);
        if (user is null || user.PasswordResetExpiry < DateTime.UtcNow)
            return BadRequest(new { message = "El enlace es inválido o ha expirado." });

        user.PasswordHash = BCrypt.Net.BCrypt.HashPassword(dto.NewPassword);
        user.PasswordResetToken = null;
        user.PasswordResetExpiry = null;
        await db.SaveChangesAsync();

        return Ok(new { message = "Contraseña actualizada correctamente. Ya puedes iniciar sesión." });
    }

    [HttpGet("sharing")]
    [Authorize]
    public async Task<IActionResult> GetSharing()
    {
        var user = await db.Users.FindAsync(CurrentUserId);
        if (user is null) return NotFound();
        return Ok(new SharingStatusDto(user.SharingEnabled));
    }

    [HttpPut("sharing")]
    [Authorize]
    public async Task<IActionResult> SetSharing([FromBody] SetSharingDto dto)
    {
        var user = await db.Users.FindAsync(CurrentUserId);
        if (user is null) return NotFound();
        user.SharingEnabled = dto.SharingEnabled;
        await db.SaveChangesAsync();
        return Ok(new SharingStatusDto(user.SharingEnabled));
    }

    [HttpDelete("delete-account")]
    [Authorize]
    public async Task<IActionResult> DeleteAccount()
    {
        var user = await db.Users.FindAsync(CurrentUserId);
        if (user is null) return NotFound();
        if (user.Email == "demo@localiza2.app" || user.Role == UserRole.SuperAdmin)
            return Forbid();

        db.Users.Remove(user);
        await db.SaveChangesAsync();
        return NoContent();
    }
}
