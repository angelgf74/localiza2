using System.Net;
using System.Net.Http;
using System.Security.Claims;
using localiza2api.Controllers;
using localiza2api.DTOs;
using localiza2api.Models;
using localiza2api.Services;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging.Abstractions;

namespace localiza2api.Tests;

// EmailService nunca se invoca en los flujos de pairing/accept probados aquí, así que un
// IHttpClientFactory que lanza si se usa deja claro si esa suposición deja de ser cierta.
file class ThrowingHttpClientFactory : IHttpClientFactory
{
    public HttpClient CreateClient(string name) => throw new InvalidOperationException(
        "EmailService no debería invocarse en estos flujos de pairing.");
}

[Collection("Postgres")]
public class ContactsControllerPairingTests(PostgresFixture fixture)
{
    private static User NewUser(string email, string name) => new()
    {
        Email = email,
        PasswordHash = "x",
        Name = name
    };

    private static ContactsController BuildController(localiza2api.Data.AppDbContext db, int currentUserId)
    {
        var emailService = new EmailService(new ConfigurationBuilder().Build(),
            new ThrowingHttpClientFactory(), NullLogger<EmailService>.Instance);

        var controller = new ContactsController(db, emailService);
        var claims = new ClaimsPrincipal(new ClaimsIdentity(
        [
            new Claim(ClaimTypes.NameIdentifier, currentUserId.ToString())
        ], "Test"));

        controller.ControllerContext = new ControllerContext
        {
            HttpContext = new DefaultHttpContext { User = claims }
        };
        return controller;
    }

    [Fact]
    public async Task AcceptPairing_por_QR_crea_contacto_bilateral_y_consume_el_codigo()
    {
        await using var db = fixture.CreateContext();

        var inviter = NewUser($"{Guid.NewGuid()}@test.local", "Inviter");
        inviter.PairingCode = TokenService.GenerateSecureToken()[..20];
        inviter.PairingCodeExpiry = DateTime.UtcNow.AddHours(1);
        var invitee = NewUser($"{Guid.NewGuid()}@test.local", "Invitee");
        db.Users.AddRange(inviter, invitee);
        await db.SaveChangesAsync();

        var controller = BuildController(db, invitee.Id);
        var result = await controller.AcceptPairing(new AcceptPairingDto(inviter.PairingCode!));

        Assert.IsType<OkObjectResult>(result);

        await using var verifyDb = fixture.CreateContext();

        var forward = await verifyDb.Contacts.SingleAsync(c => c.UserId == inviter.Id && c.ContactUserId == invitee.Id);
        Assert.Equal(ContactStatus.Accepted, forward.Status);
        Assert.True(forward.LocationPermissionGranted);

        var reciprocal = await verifyDb.Contacts.SingleAsync(c => c.UserId == invitee.Id && c.ContactUserId == inviter.Id);
        Assert.Equal(ContactStatus.Accepted, reciprocal.Status);
        Assert.True(reciprocal.LocationPermissionGranted);

        var reloadedInviter = await verifyDb.Users.SingleAsync(u => u.Id == inviter.Id);
        Assert.Null(reloadedInviter.PairingCode); // Código de un solo uso.
    }

    [Fact]
    public async Task AcceptPairing_rechaza_emparejarse_consigo_mismo()
    {
        await using var db = fixture.CreateContext();

        var user = NewUser($"{Guid.NewGuid()}@test.local", "Self");
        user.PairingCode = TokenService.GenerateSecureToken()[..20];
        user.PairingCodeExpiry = DateTime.UtcNow.AddHours(1);
        db.Users.Add(user);
        await db.SaveChangesAsync();

        var controller = BuildController(db, user.Id);
        var result = await controller.AcceptPairing(new AcceptPairingDto(user.PairingCode!));

        Assert.IsType<BadRequestObjectResult>(result);
    }

    [Fact]
    public async Task AcceptPairing_con_codigo_expirado_devuelve_NotFound()
    {
        await using var db = fixture.CreateContext();

        var inviter = NewUser($"{Guid.NewGuid()}@test.local", "Inviter");
        inviter.PairingCode = TokenService.GenerateSecureToken()[..20];
        inviter.PairingCodeExpiry = DateTime.UtcNow.AddHours(-1); // Expirado.
        var invitee = NewUser($"{Guid.NewGuid()}@test.local", "Invitee");
        db.Users.AddRange(inviter, invitee);
        await db.SaveChangesAsync();

        var controller = BuildController(db, invitee.Id);
        var result = await controller.AcceptPairing(new AcceptPairingDto(inviter.PairingCode!));

        Assert.IsType<NotFoundObjectResult>(result);
    }

    [Fact]
    public async Task AcceptInvitation_por_email_crea_contacto_bilateral_y_borra_la_invitacion()
    {
        await using var db = fixture.CreateContext();

        var inviter = NewUser($"{Guid.NewGuid()}@test.local", "Inviter");
        var invitedEmail = $"{Guid.NewGuid()}@test.local";
        var invited = NewUser(invitedEmail, "Invited");
        db.Users.AddRange(inviter, invited);
        await db.SaveChangesAsync();

        var invitation = new ContactInvitation
        {
            InviterUserId = inviter.Id,
            InvitedEmail = invitedEmail,
            Token = TokenService.GenerateSecureToken(),
            ExpiresAt = DateTime.UtcNow.AddDays(7)
        };
        db.ContactInvitations.Add(invitation);
        await db.SaveChangesAsync();

        var controller = BuildController(db, invited.Id);
        var result = await controller.AcceptInvitation(invitation.Token);

        Assert.IsType<OkObjectResult>(result);

        await using var verifyDb = fixture.CreateContext();

        var reciprocal = await verifyDb.Contacts.SingleAsync(c => c.UserId == invited.Id && c.ContactUserId == inviter.Id);
        Assert.Equal(ContactStatus.Accepted, reciprocal.Status);

        Assert.False(await verifyDb.ContactInvitations.AnyAsync(i => i.Token == invitation.Token));
    }
}
