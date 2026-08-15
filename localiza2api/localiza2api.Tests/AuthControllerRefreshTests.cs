using localiza2api.Controllers;
using localiza2api.DTOs;
using localiza2api.Models;
using localiza2api.Services;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging.Abstractions;

namespace localiza2api.Tests;

// EmailService nunca se invoca en login/refresh/logout/reset-password (con token ya
// generado), así que un IHttpClientFactory que lanza deja claro si eso deja de ser cierto.
file class ThrowingHttpClientFactory : IHttpClientFactory
{
    public HttpClient CreateClient(string name) => throw new InvalidOperationException(
        "EmailService no debería invocarse en estos flujos.");
}

[Collection("Postgres")]
public class AuthControllerRefreshTests(PostgresFixture fixture)
{
    private const string Password = "Sup3rSecret!";

    private static IConfiguration BuildConfig() => new ConfigurationBuilder()
        .AddInMemoryCollection(new Dictionary<string, string?>
        {
            ["Jwt:Key"] = "test-signing-key-at-least-32-bytes-long!!",
            ["Jwt:Issuer"] = "localiza2-tests",
            ["Jwt:Audience"] = "localiza2-tests"
        })
        .Build();

    private static AuthController BuildController(localiza2api.Data.AppDbContext db)
    {
        var config = BuildConfig();
        var emailService = new EmailService(config, new ThrowingHttpClientFactory(), NullLogger<EmailService>.Instance);
        var tokenService = new TokenService(config);
        return new AuthController(db, emailService, tokenService, config);
    }

    private static async Task<User> CreateUserAsync(localiza2api.Data.AppDbContext db, string email)
    {
        var user = new User { Email = email, PasswordHash = BCrypt.Net.BCrypt.HashPassword(Password), Name = "Test" };
        db.Users.Add(user);
        await db.SaveChangesAsync();
        return user;
    }

    private static async Task<LoginResponseDto> LoginAsync(AuthController controller, string email)
    {
        var result = await controller.Login(new LoginDto(email, Password));
        return (LoginResponseDto)((OkObjectResult)result).Value!;
    }

    [Fact]
    public async Task Login_emite_access_token_y_refresh_token_guardando_solo_el_hash()
    {
        await using var db = fixture.CreateContext();
        var user = await CreateUserAsync(db, $"{Guid.NewGuid()}@test.local");
        var controller = BuildController(db);

        var body = await LoginAsync(controller, user.Email);

        Assert.False(string.IsNullOrWhiteSpace(body.Token));
        Assert.False(string.IsNullOrWhiteSpace(body.RefreshToken));

        await using var verifyDb = fixture.CreateContext();
        var stored = await verifyDb.RefreshTokens.SingleAsync(r => r.UserId == user.Id);
        Assert.Equal(TokenService.HashToken(body.RefreshToken), stored.TokenHash);
        Assert.Null(stored.RevokedAt);
    }

    [Fact]
    public async Task Refresh_rota_el_token_y_revoca_el_anterior()
    {
        await using var db = fixture.CreateContext();
        var user = await CreateUserAsync(db, $"{Guid.NewGuid()}@test.local");
        var controller = BuildController(db);
        var loginBody = await LoginAsync(controller, user.Email);

        var refreshResult = await controller.Refresh(new RefreshRequestDto(loginBody.RefreshToken));
        var ok = Assert.IsType<OkObjectResult>(refreshResult);
        var refreshBody = Assert.IsType<RefreshResponseDto>(ok.Value);

        Assert.NotEqual(loginBody.RefreshToken, refreshBody.RefreshToken);

        await using var verifyDb = fixture.CreateContext();
        var oldHash = TokenService.HashToken(loginBody.RefreshToken);
        var oldStored = await verifyDb.RefreshTokens.SingleAsync(r => r.TokenHash == oldHash);
        Assert.NotNull(oldStored.RevokedAt);

        var newHash = TokenService.HashToken(refreshBody.RefreshToken);
        var newStored = await verifyDb.RefreshTokens.SingleAsync(r => r.TokenHash == newHash);
        Assert.Null(newStored.RevokedAt);
        Assert.Equal(newHash, oldStored.ReplacedByTokenHash);
    }

    [Fact]
    public async Task Refresh_con_token_ya_rotado_revoca_toda_la_familia()
    {
        await using var db = fixture.CreateContext();
        var user = await CreateUserAsync(db, $"{Guid.NewGuid()}@test.local");
        var controller = BuildController(db);
        var loginBody = await LoginAsync(controller, user.Email);

        await controller.Refresh(new RefreshRequestDto(loginBody.RefreshToken)); // Rotación legítima.
        var reuseResult = await controller.Refresh(new RefreshRequestDto(loginBody.RefreshToken)); // Reuso = robo.

        Assert.IsType<UnauthorizedObjectResult>(reuseResult);

        await using var verifyDb = fixture.CreateContext();
        var activeCount = await verifyDb.RefreshTokens
            .Where(r => r.UserId == user.Id && r.RevokedAt == null)
            .CountAsync();
        Assert.Equal(0, activeCount); // Incluida la rotación legítima: toda la familia cae.
    }

    [Fact]
    public async Task Refresh_con_token_expirado_devuelve_401()
    {
        await using var db = fixture.CreateContext();
        var user = await CreateUserAsync(db, $"{Guid.NewGuid()}@test.local");

        var raw = TokenService.GenerateRefreshTokenValue();
        db.RefreshTokens.Add(new RefreshToken
        {
            UserId = user.Id,
            TokenHash = TokenService.HashToken(raw),
            ExpiresAt = DateTime.UtcNow.AddDays(-1)
        });
        await db.SaveChangesAsync();

        var controller = BuildController(db);
        var result = await controller.Refresh(new RefreshRequestDto(raw));

        Assert.IsType<UnauthorizedObjectResult>(result);
    }

    [Fact]
    public async Task Refresh_con_token_desconocido_devuelve_401()
    {
        await using var db = fixture.CreateContext();
        var controller = BuildController(db);

        var result = await controller.Refresh(new RefreshRequestDto("token-que-nunca-existio"));

        Assert.IsType<UnauthorizedObjectResult>(result);
    }

    [Fact]
    public async Task Logout_revoca_el_refresh_token_y_deja_de_servir_para_refrescar()
    {
        await using var db = fixture.CreateContext();
        var user = await CreateUserAsync(db, $"{Guid.NewGuid()}@test.local");
        var controller = BuildController(db);
        var loginBody = await LoginAsync(controller, user.Email);

        var logoutResult = await controller.Logout(new RefreshRequestDto(loginBody.RefreshToken));
        Assert.IsType<NoContentResult>(logoutResult);

        var refreshResult = await controller.Refresh(new RefreshRequestDto(loginBody.RefreshToken));
        Assert.IsType<UnauthorizedObjectResult>(refreshResult);
    }

    [Fact]
    public async Task ResetPassword_revoca_todos_los_refresh_tokens_activos()
    {
        string email = $"{Guid.NewGuid()}@test.local";
        string resetToken;
        LoginResponseDto loginBody;

        await using (var db = fixture.CreateContext())
        {
            var user = await CreateUserAsync(db, email);
            var controller = BuildController(db);
            loginBody = await LoginAsync(controller, user.Email);

            resetToken = TokenService.GenerateSecureToken();
            user.PasswordResetToken = resetToken;
            user.PasswordResetExpiry = DateTime.UtcNow.AddHours(1);
            await db.SaveChangesAsync();
        }

        // ExecuteUpdateAsync (usado para revocar en bloque) no refresca entidades ya
        // trackeadas en el mismo DbContext: cada paso usa un contexto nuevo, como en
        // producción cada petición HTTP tiene su propio scope de DbContext.
        await using (var db = fixture.CreateContext())
        {
            var controller = BuildController(db);
            var resetResult = await controller.ResetPassword(new ResetPasswordDto(resetToken, "NuevaClave123"));
            Assert.IsType<OkObjectResult>(resetResult);
        }

        await using (var db = fixture.CreateContext())
        {
            var controller = BuildController(db);
            var refreshResult = await controller.Refresh(new RefreshRequestDto(loginBody.RefreshToken));
            Assert.IsType<UnauthorizedObjectResult>(refreshResult);
        }
    }
}
