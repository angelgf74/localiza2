---
description: Create and review an EF Core migration for localiza2api
---

Genera una migración de EF Core para: $ARGUMENTS

Pasos:
1. Revisa `localiza2api/Models/` y `localiza2api/Data/AppDbContext.cs` para confirmar que el cambio de modelo ya está hecho antes de generar la migración. Si no lo está, pregunta o edítalo primero.
2. Corre desde `localiza2api/`:
   ```
   dotnet ef migrations add <NombreDescriptivo>
   ```
   Usa PascalCase descriptivo (ver ejemplos existentes: `AddLocationShareLink`, `AddUserRole`).
3. Abre el fichero de migración generado (`localiza2api/Data/Migrations/<timestamp>_<Nombre>.cs`) y revisa el `Up()`/`Down()`: columnas nullable vs not-null, valores por defecto, índices, y si toca una tabla con muchas filas (`Locations`) evalúa impacto de bloqueo.
4. Compara el diff de `localiza2api/Data/Migrations/AppDbContextModelSnapshot.cs` — confirma que solo cambió lo esperado.
5. NO corras `dotnet ef database update` contra producción automáticamente. Localmente sí puedes, avisando antes.
6. Recuerda: `deploy-ubuntu.sh` NO copia `appsettings.json` en el servidor tras el primer deploy — la migración deberá aplicarse manualmente en el servidor (`dotnet ef database update` con la connection string de producción) o dejar que el servicio la aplique si hay `Database.Migrate()` en `Program.cs` (revisa si existe).

Al terminar, resume: nombre de migración, tablas/columnas afectadas, y si requiere acción manual en el servidor.
