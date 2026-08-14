using localiza2api.Data;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.DependencyInjection;
using Testcontainers.PostgreSql;

namespace localiza2api.Tests;

// Contenedor Postgres compartido por toda la colección de tests: PruneLocationsService
// usa SQL crudo específico de Postgres (to_timestamp, floor, extract epoch), incompatible
// con el proveedor InMemory de EF Core.
public class PostgresFixture : IAsyncLifetime
{
    private readonly PostgreSqlContainer _container = new PostgreSqlBuilder("postgres:17-alpine").Build();

    private ServiceProvider _services = null!;

    // PruneLocationsService recibe un IServiceScopeFactory y crea su propio scope/contexto
    // internamente, igual que en producción (Program.cs).
    public IServiceScopeFactory ScopeFactory => _services.GetRequiredService<IServiceScopeFactory>();

    public async Task InitializeAsync()
    {
        await _container.StartAsync();

        var services = new ServiceCollection();
        services.AddDbContext<AppDbContext>(opt => opt.UseNpgsql(_container.GetConnectionString()));
        _services = services.BuildServiceProvider();

        using var scope = _services.CreateScope();
        var db = scope.ServiceProvider.GetRequiredService<AppDbContext>();
        await db.Database.MigrateAsync();
    }

    public async Task DisposeAsync()
    {
        await _services.DisposeAsync();
        await _container.DisposeAsync();
    }

    // El caller es responsable de hacer Dispose (p. ej. con `using`).
    public AppDbContext CreateContext() =>
        new(new DbContextOptionsBuilder<AppDbContext>().UseNpgsql(_container.GetConnectionString()).Options);
}

[CollectionDefinition("Postgres")]
public class PostgresCollection : ICollectionFixture<PostgresFixture>;
