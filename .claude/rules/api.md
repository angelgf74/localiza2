# Regla: API .NET (`localiza2api/`)

.NET 10, C#, PostgreSQL vía Npgsql + EF Core. Corre en el puerto 54005 detrás de Nginx.

## Autorización — patrón fijo

Todo controller que toque datos de usuario lleva `[Authorize]` a nivel de clase y obtiene el usuario así:

```csharp
private int CurrentUserId => int.Parse(User.FindFirstValue(ClaimTypes.NameIdentifier)!);
```

**Nunca aceptes un `userId` desde el body, la query o la ruta para identificar al llamante.** El id sale siempre del claim del JWT. Un endpoint que reciba `userId` del cliente y lo use sin comprobar que coincide con `CurrentUserId` es un IDOR.

Endpoints de administración: `[Authorize(Roles = "SuperAdmin")]` (ver `AdminController.cs`). El rol vive en `User.Role` (`UserRole` enum, persistido como string).

## Acceso a ubicaciones ajenas

Leer la ubicación de otro usuario **exige un contacto mutuo aceptado**:

```csharp
c.Status == ContactStatus.Accepted
```

Cualquier endpoint nuevo que devuelva posiciones de terceros replica esa comprobación. Sin ella se filtra la ubicación en tiempo real de personas — es el peor fallo posible en esta app.

## Cuentas protegidas

`demo@localiza2.app` y los usuarios `SuperAdmin` están protegidos frente a borrado/modificación en `AuthController.cs`. Si añades operaciones destructivas sobre usuarios, respeta esa misma exclusión.

## Estructura

| Carpeta | Contenido |
|---|---|
| `Controllers/` | `Auth`, `Contacts`, `Location`, `Admin` |
| `DTOs/` | Un fichero por controller (`AuthDtos.cs`, …) |
| `Models/` | Entidades EF |
| `Services/` | `EmailService` (Brevo), `TokenService` (JWT), `PruneLocationsService` (BackgroundService) |
| `Data/` | `AppDbContext`, `Migrations/`, `DemoSeeder`, `SuperAdminSeeder` |

Endpoints nuevos: DTOs en el fichero del controller correspondiente, nunca modelos EF expuestos directamente en la respuesta. Skill `/api-endpoint` genera el andamiaje.

## Middleware — el orden importa

En `Program.cs`:

```
UseForwardedHeaders → UseCors → UseRateLimiter → UseAuthentication → UseAuthorization → MapControllers
```

`UseForwardedHeaders()` va **primero**. La API está detrás de Nginx; sin él `RemoteIpAddress` es siempre `127.0.0.1` y el rate limiter por-IP de la política `auth` (20 req/min) pasa a ser un límite global compartido por todos los clientes. No reordenes ese bloque.

`KnownProxies`/`KnownIPNetworks` están deliberadamente vacíos porque solo Nginx en localhost alcanza Kestrel. Esa suposición deja de valer si Kestrel se expone directamente: si eso cambia, hay que declarar el proxy de confianza.

## CORS

Los orígenes salen de `App:WebUrl` (o se derivan de `App:BaseUrl` sustituyendo `-api.` por `-app.`). En Development se permite además `localhost`/`127.0.0.1`. El fallback a `AllowAnyOrigin()` solo aplica si no hay orígenes configurados — no lo conviertas en el camino normal.

## Rate limiting

Política `auth`: ventana fija, 20 peticiones/minuto por IP, sin cola, responde 429. Aplícala a endpoints de login/registro/reset. Si añades un endpoint de autenticación nuevo, decórelo con esa política.

## Migraciones al arrancar

`Program.cs` ejecuta `db.Database.MigrateAsync()` en el arranque, seguido de `DemoSeeder` y `SuperAdminSeeder` (ambos idempotentes y condicionados a que exista la config correspondiente). Consecuencia práctica: **desplegar la API aplica las migraciones pendientes automáticamente**. Ver `database.md`.
