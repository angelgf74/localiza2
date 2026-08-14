# Regla: base de datos y migraciones

PostgreSQL. Esquema en `localiza2api/Data/AppDbContext.cs`, migraciones en `localiza2api/Data/Migrations/`.

## Migraciones

```bash
cd localiza2api
dotnet ef migrations add <Nombre>
dotnet ef database update
```

Revisa cada migración con la skill `ef-migration-review` antes de darla por buena.

**`Program.cs` aplica migraciones automáticamente al arrancar** (`db.Database.MigrateAsync()`). Por tanto:

- Una migración commiteada y desplegada **se aplica sola** en producción, sin paso manual.
- Eso hace que las migraciones destructivas sean especialmente peligrosas: un `DropColumn` llega a producción en cuanto se reinicia el servicio.
- Antes de desplegar una migración que borre o renombre columnas, confirma con el usuario y asegúrate de que hay copia de seguridad de la base de datos.

Los clientes Android y web comparten el mismo contrato. Una columna eliminada o renombrada rompe versiones antiguas de la app que siguen instaladas en dispositivos de usuarios — Android no se actualiza a la vez que el servidor. Para cambios que rompan compatibilidad: añade primero, migra los datos, retira la columna vieja en una release posterior.

## Índices existentes — no los quites

| Entidad | Índice |
|---|---|
| `User` | `Email` único; `PairingCode` único filtrado (`IS NOT NULL`); `PasswordResetToken` único filtrado |
| `Contact` | `(UserId, Email)` único |
| `UserLocation` | `(UserId, Timestamp)` — sostiene consultas de histórico y la poda |
| `PendingRegistration` | `Token` único, `Email` único |
| `LocationShareLink` | `Token` único |

El índice `(UserId, Timestamp)` es el que hace viable `PruneLocationsService`. Sin él la poda hace *seq scan* sobre toda la tabla de ubicaciones cada hora.

## Política de retención de ubicaciones

La implementa `Services/PruneLocationsService.cs`: un `BackgroundService` que arranca 1 minuto después del inicio y **corre cada hora** (no en cada insert). Cuatro sentencias SQL, en este orden:

| Antigüedad | Se conserva |
|---|---|
| > 30 días | nada — se borra |
| 1–30 días | 1 registro por *bucket* de 30 min (1800 s) |
| 3–24 h | 1 registro por *bucket* de 5 min (300 s) |
| < 3 h | 1 registro por *bucket* de 1 min (60 s) |

Cada bloque conserva el `MAX("Id")` de su bucket, agrupando por `("UserId", bucket)`. Si tocas los intervalos, cambia el `floor(extract(epoch …) / N)` y la ventana `Timestamp` de forma coherente: las cuatro ventanas deben seguir siendo contiguas y sin solaparse, o se borran registros que deberían sobrevivir.

Los errores de poda se capturan y se registran; no tumban el servicio. Si añades trabajo ahí, mantén ese `try/catch`.

## Borrado en cascada

- `Contact.User`, `UserLocation.User`, `ContactInvitation.InviterUser`, `LocationShareLink.User` → `Cascade`.
- `Contact.ContactUser` → `SetNull` (el contacto sobrevive si el usuario apuntado desaparece).

Borrar un `User` arrastra sus ubicaciones, contactos, invitaciones y enlaces de compartición. Tenlo presente antes de exponer un borrado de cuenta.

## Seeders

`DemoSeeder` y `SuperAdminSeeder` son idempotentes y solo actúan si su configuración está presente (`Demo:Password`, `SuperAdmin:Email` + `SuperAdmin:Password`). Corren en cada arranque. La cuenta demo es la que revisa Google Play — skill `/demo-reset`.
