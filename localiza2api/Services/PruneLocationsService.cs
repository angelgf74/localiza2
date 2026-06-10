using localiza2api.Data;
using Microsoft.EntityFrameworkCore;

namespace localiza2api.Services;

public class PruneLocationsService(IServiceScopeFactory scopeFactory, ILogger<PruneLocationsService> logger)
    : BackgroundService
{
    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        await Task.Delay(TimeSpan.FromMinutes(1), stoppingToken);
        while (!stoppingToken.IsCancellationRequested)
        {
            await PruneAsync(stoppingToken);
            await Task.Delay(TimeSpan.FromHours(1), stoppingToken);
        }
    }

    private async Task PruneAsync(CancellationToken ct)
    {
        try
        {
            using var scope = scopeFactory.CreateScope();
            var db    = scope.ServiceProvider.GetRequiredService<AppDbContext>();
            var now   = DateTime.UtcNow;
            var cut30d = now.AddDays(-30);
            var cut24h = now.AddHours(-24);
            var cut3h  = now.AddHours(-3);

            await db.Database.ExecuteSqlAsync($"""
                DELETE FROM "UserLocations" WHERE "Timestamp" < {cut30d}
                """, ct);

            await db.Database.ExecuteSqlAsync($"""
                DELETE FROM "UserLocations"
                WHERE "Timestamp" >= {cut30d}
                  AND "Timestamp" < {cut24h}
                  AND "Id" NOT IN (
                    SELECT MAX("Id") FROM "UserLocations"
                     WHERE "Timestamp" >= {cut30d}
                       AND "Timestamp" < {cut24h}
                     GROUP BY "UserId", to_timestamp(floor(extract(epoch FROM "Timestamp") / 1800) * 1800)
                  )
                """, ct);

            await db.Database.ExecuteSqlAsync($"""
                DELETE FROM "UserLocations"
                WHERE "Timestamp" >= {cut24h}
                  AND "Timestamp" < {cut3h}
                  AND "Id" NOT IN (
                    SELECT MAX("Id") FROM "UserLocations"
                     WHERE "Timestamp" >= {cut24h}
                       AND "Timestamp" < {cut3h}
                     GROUP BY "UserId", to_timestamp(floor(extract(epoch FROM "Timestamp") / 300) * 300)
                  )
                """, ct);

            await db.Database.ExecuteSqlAsync($"""
                DELETE FROM "UserLocations"
                WHERE "Timestamp" >= {cut3h}
                  AND "Id" NOT IN (
                    SELECT MAX("Id") FROM "UserLocations"
                     WHERE "Timestamp" >= {cut3h}
                     GROUP BY "UserId", to_timestamp(floor(extract(epoch FROM "Timestamp") / 60) * 60)
                  )
                """, ct);

            logger.LogInformation("Poda de ubicaciones completada.");
        }
        catch (OperationCanceledException) { }
        catch (Exception ex)
        {
            logger.LogError(ex, "Error en la poda de ubicaciones.");
        }
    }
}
