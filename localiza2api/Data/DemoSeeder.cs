using System.Security.Cryptography;
using localiza2api.Models;
using Microsoft.EntityFrameworkCore;

namespace localiza2api.Data;

/// <summary>
/// Siembra de forma idempotente la cuenta demo usada por la revisión de Google Play,
/// junto con un contacto emparejado y un historial de ubicaciones de ejemplo, para que
/// el revisor vea la función principal (localización en tiempo real) funcionando.
///
/// Solo actúa si <c>Demo:Password</c> está configurado (p. ej. en appsettings.json de
/// producción, que no está en git). Todas las operaciones comprueban existencia previa,
/// así que ejecutarlo en cada arranque no duplica datos.
/// </summary>
public static class DemoSeeder
{
    public static async Task SeedAsync(AppDbContext db, IConfiguration config)
    {
        var email = (config["Demo:Email"] ?? "demo@localiza2.app").ToLower();
        var password = config["Demo:Password"];
        if (string.IsNullOrWhiteSpace(password))
            return; // Sin contraseña configurada no se siembra nada (p. ej. en desarrollo).

        // 1. Cuenta demo principal: se crea o se le reajusta la contraseña en cada arranque
        //    para garantizar que coincide con la configurada en Play Console.
        var demo = await db.Users.FirstOrDefaultAsync(u => u.Email == email);
        if (demo is null)
        {
            demo = new User { Email = email, Name = "Demo", PasswordHash = BCrypt.Net.BCrypt.HashPassword(password) };
            db.Users.Add(demo);
        }
        else if (!BCrypt.Net.BCrypt.Verify(password, demo.PasswordHash))
        {
            // Solo re-hashear (e invalidar tokens ya emitidos) si la contraseña configurada
            // cambió de verdad; si no, cada arranque del servidor cerraría la sesión demo.
            demo.PasswordHash = BCrypt.Net.BCrypt.HashPassword(password);
            demo.TokenVersion++;
        }
        await db.SaveChangesAsync();

        // 2. Contacto demo (un "amigo" que comparte su ubicación). No necesita iniciar
        //    sesión, así que su contraseña es aleatoria e inservible.
        const string friendEmail = "demo-contacto@localiza2.app";
        var friend = await db.Users.FirstOrDefaultAsync(u => u.Email == friendEmail);
        if (friend is null)
        {
            friend = new User
            {
                Email = friendEmail,
                Name = "Ana García",
                PasswordHash = BCrypt.Net.BCrypt.HashPassword(RandomPassword()),
                SharingEnabled = true
            };
            db.Users.Add(friend);
            await db.SaveChangesAsync();
        }

        // 3. Relación mutua aceptada y con permiso de ubicación concedido en ambos sentidos.
        if (!await db.Contacts.AnyAsync(c => c.UserId == demo.Id && c.ContactUserId == friend.Id))
        {
            db.Contacts.Add(new Contact
            {
                UserId = demo.Id,
                ContactUserId = friend.Id,
                Email = friend.Email,
                Alias = "Ana",
                Status = ContactStatus.Accepted,
                LocationPermissionGranted = true
            });
        }
        if (!await db.Contacts.AnyAsync(c => c.UserId == friend.Id && c.ContactUserId == demo.Id))
        {
            db.Contacts.Add(new Contact
            {
                UserId = friend.Id,
                ContactUserId = demo.Id,
                Email = demo.Email,
                Alias = "Demo",
                Status = ContactStatus.Accepted,
                LocationPermissionGranted = true
            });
        }
        await db.SaveChangesAsync();

        // 4. Historial de ubicaciones de ejemplo (una ruta corta por el centro de Madrid).
        //    Se genera cada vez que el usuario correspondiente no tenga ninguna ubicación,
        //    de modo que el revisor siempre vea un punto reciente en el mapa.
        if (!await db.UserLocations.AnyAsync(l => l.UserId == friend.Id))
            db.UserLocations.AddRange(BuildTrack(friend.Id, 40.4168, -3.7038));

        if (!await db.UserLocations.AnyAsync(l => l.UserId == demo.Id))
            db.UserLocations.AddRange(BuildTrack(demo.Id, 40.4155, -3.7074));

        await db.SaveChangesAsync();
    }

    // Genera una ruta de 20 puntos separados ~3 min, terminando en el instante actual,
    // para que la "última ubicación" aparezca como reciente/en vivo.
    private static IEnumerable<UserLocation> BuildTrack(int userId, double startLat, double startLng)
    {
        var now = DateTime.UtcNow;
        for (var i = 0; i < 20; i++)
        {
            yield return new UserLocation
            {
                UserId = userId,
                Latitude = startLat + i * 0.0006,
                Longitude = startLng + i * 0.0004,
                Accuracy = 8 + i % 5,
                BatteryLevel = 90 - i,
                Timestamp = now.AddMinutes(-(19 - i) * 3)
            };
        }
    }

    private static string RandomPassword() =>
        Convert.ToBase64String(RandomNumberGenerator.GetBytes(24));
}
