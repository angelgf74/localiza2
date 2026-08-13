---
name: ef-migration-review
description: Reviews EF Core migrations for localiza2api against the location-retention policy, indexes, and breaking changes shared by Android + Web clients. Use when creating or reviewing a migration under localiza2api/Data/Migrations/.
---

# EF Core migration review — localiza2api

Revisión enfocada, más profunda que `/migrate`, para migraciones que ya existen o están por aplicarse.

## Puntos a revisar en cada migración

1. **Retención de ubicaciones** (política en CLAUDE.md): si la migración toca la tabla `Locations` o su lógica de poda (`PruneLocationsAsync`), verifica que sigue respetando:
   - Últimas 3h: 1 registro/min (máx 180)
   - 3–24h: 1/5min (máx 252)
   - 1–30 días: 1/30min (máx 1440)
   - >30 días: purgado
   Una migración que añade columnas NOT NULL sin default a `Locations` puede romper con el volumen histórico existente.

2. **Compatibilidad con clientes activos**: Android (Kotlin) y Web (JS) consumen el mismo API. Un rename o eliminación de columna que un DTO todavía serializa rompe ambos clientes simultáneamente sin versión de API. Verifica DTOs en `localiza2api/DTOs/` referencian la columna antes de eliminarla/renombrarla.

3. **Nullable vs NOT NULL**: columnas nuevas NOT NULL en tablas con filas existentes necesitan `defaultValue` o `defaultValueSql` en el `Up()`, si no falla al aplicar contra datos reales.

4. **Índices**: si la migración añade una FK o columna usada en `Where`/`OrderBy` (patrón visto en `ContactsController`, filtros por `UserId`), confirma que hay índice — repasa `AppDbContext.cs` `OnModelCreating` por convención existente de índices.

5. **Reversibilidad**: revisa que `Down()` no pierda datos silenciosamente sin que quede claro (ej. `DropColumn` en `Down()` de una migración que en `Up()` migra datos de una columna vieja a una nueva — el rollback perdería los datos migrados).

6. **Snapshot**: `AppDbContextModelSnapshot.cs` debe reflejar exactamente el estado tras la migración — un diff inesperado ahí (cambios no relacionados) indica que el modelo tenía drift previo sin migración, hay que investigarlo antes de seguir.

## Salida esperada
Lista corta de hallazgos (si los hay) en formato `archivo:línea — problema — sugerencia`. Si todo está bien, decirlo en una línea, sin alargar innecesariamente.
