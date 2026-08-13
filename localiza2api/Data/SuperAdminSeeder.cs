using localiza2api.Models;
using Microsoft.EntityFrameworkCore;

namespace localiza2api.Data;

/// <summary>
/// Siembra de forma idempotente la cuenta de super administrador usada para acceder
/// al panel de administración de solo lectura (listado de usuarios y sus ubicaciones).
///
/// Solo actúa si <c>SuperAdmin:Email</c> y <c>SuperAdmin:Password</c> están configurados
/// (p. ej. en appsettings.json de producción, que no está en git). Es idempotente: si la
/// cuenta ya existe, solo le reajusta la contraseña y garantiza que su rol sea SuperAdmin.
/// </summary>
public static class SuperAdminSeeder
{
    public static async Task SeedAsync(AppDbContext db, IConfiguration config)
    {
        var email = config["SuperAdmin:Email"]?.ToLower();
        var password = config["SuperAdmin:Password"];
        if (string.IsNullOrWhiteSpace(email) || string.IsNullOrWhiteSpace(password))
            return; // Sin credenciales configuradas no se siembra nada (p. ej. en desarrollo).

        var admin = await db.Users.FirstOrDefaultAsync(u => u.Email == email);
        if (admin is null)
        {
            admin = new User
            {
                Email = email,
                Name = config["SuperAdmin:Name"] ?? "Administrador",
                PasswordHash = BCrypt.Net.BCrypt.HashPassword(password),
                Role = UserRole.SuperAdmin
            };
            db.Users.Add(admin);
        }
        else
        {
            admin.PasswordHash = BCrypt.Net.BCrypt.HashPassword(password);
            admin.Role = UserRole.SuperAdmin;
        }
        await db.SaveChangesAsync();
    }
}
