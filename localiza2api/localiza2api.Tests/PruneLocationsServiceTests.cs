using localiza2api.Models;
using localiza2api.Services;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Logging.Abstractions;

namespace localiza2api.Tests;

[Collection("Postgres")]
public class PruneLocationsServiceTests(PostgresFixture fixture)
{
    private static User NewUser(string email) => new()
    {
        Email = email,
        PasswordHash = "x",
        Name = "Test"
    };

    private async Task<int> CreateUserAsync(string email)
    {
        await using var db = fixture.CreateContext();
        var user = NewUser(email);
        db.Users.Add(user);
        await db.SaveChangesAsync();
        return user.Id;
    }

    private async Task RunPruneAsync()
    {
        var service = new PruneLocationsService(fixture.ScopeFactory, NullLogger<PruneLocationsService>.Instance);
        await service.PruneAsync(CancellationToken.None);
    }

    private async Task<List<UserLocation>> GetLocationsAsync(int userId)
    {
        await using var db = fixture.CreateContext();
        return await db.UserLocations
            .Where(l => l.UserId == userId)
            .OrderBy(l => l.Timestamp)
            .ToListAsync();
    }

    [Fact]
    public async Task Elimina_ubicaciones_de_mas_de_30_dias()
    {
        var userId = await CreateUserAsync($"{Guid.NewGuid()}@test.local");
        var now = DateTime.UtcNow;

        await using (var db = fixture.CreateContext())
        {
            db.UserLocations.Add(new UserLocation { UserId = userId, Latitude = 1, Longitude = 1, Timestamp = now.AddDays(-31) });
            db.UserLocations.Add(new UserLocation { UserId = userId, Latitude = 1, Longitude = 1, Timestamp = now.AddDays(-40) });
            await db.SaveChangesAsync();
        }

        await RunPruneAsync();

        var remaining = await GetLocationsAsync(userId);
        Assert.Empty(remaining);
    }

    [Fact]
    public async Task Comprime_ubicaciones_de_1_a_30_dias_a_1_por_bucket_de_30min_conservando_la_mas_reciente()
    {
        var userId = await CreateUserAsync($"{Guid.NewGuid()}@test.local");
        // Los buckets de 30min se alinean a las :00/:30 (floor sobre epoch/1800), así que
        // ancla el bucket a un múltiplo exacto de 30 minutos para que ambos puntos caigan dentro.
        var tenDaysAgo = DateTime.UtcNow.AddDays(-10);
        var epoch = new DateTimeOffset(tenDaysAgo).ToUnixTimeSeconds();
        var bucketStart = DateTimeOffset.FromUnixTimeSeconds(epoch / 1800 * 1800).UtcDateTime;

        await using (var db = fixture.CreateContext())
        {
            db.UserLocations.Add(new UserLocation { UserId = userId, Latitude = 1, Longitude = 1, Accuracy = 10, Timestamp = bucketStart.AddMinutes(2) });
            db.UserLocations.Add(new UserLocation { UserId = userId, Latitude = 2, Longitude = 2, Accuracy = 20, Timestamp = bucketStart.AddMinutes(25) });
            await db.SaveChangesAsync();
        }

        await RunPruneAsync();

        var remaining = await GetLocationsAsync(userId);
        var single = Assert.Single(remaining);
        Assert.Equal(20, single.Accuracy); // Se queda el de mayor Id/Timestamp del bucket.
    }

    [Fact]
    public async Task Comprime_ubicaciones_de_3_a_24h_a_1_por_bucket_de_5min()
    {
        var userId = await CreateUserAsync($"{Guid.NewGuid()}@test.local");
        var now = DateTime.UtcNow;
        var withinWindow = now.AddHours(-10);

        await using (var db = fixture.CreateContext())
        {
            for (var i = 0; i < 4; i++)
                db.UserLocations.Add(new UserLocation { UserId = userId, Latitude = i, Longitude = i, Timestamp = withinWindow.AddSeconds(i * 10) });
            await db.SaveChangesAsync();
        }

        await RunPruneAsync();

        var remaining = await GetLocationsAsync(userId);
        Assert.Single(remaining);
    }

    [Fact]
    public async Task No_toca_ubicaciones_de_menos_de_3h_si_ya_estan_a_1_por_minuto()
    {
        var userId = await CreateUserAsync($"{Guid.NewGuid()}@test.local");
        var now = DateTime.UtcNow;

        await using (var db = fixture.CreateContext())
        {
            // Tres puntos en minutos distintos, dentro de las últimas 3h: ninguno debería podarse.
            db.UserLocations.Add(new UserLocation { UserId = userId, Latitude = 1, Longitude = 1, Timestamp = now.AddMinutes(-5) });
            db.UserLocations.Add(new UserLocation { UserId = userId, Latitude = 2, Longitude = 2, Timestamp = now.AddMinutes(-3) });
            db.UserLocations.Add(new UserLocation { UserId = userId, Latitude = 3, Longitude = 3, Timestamp = now.AddMinutes(-1) });
            await db.SaveChangesAsync();
        }

        await RunPruneAsync();

        var remaining = await GetLocationsAsync(userId);
        Assert.Equal(3, remaining.Count);
    }
}
