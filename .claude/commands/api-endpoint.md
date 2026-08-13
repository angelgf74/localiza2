---
description: Scaffold a new controller/DTO/route following existing localiza2api patterns
---

Crea un nuevo endpoint de API para: $ARGUMENTS

Antes de escribir código, lee estos ficheros como referencia de patrón (elige el más parecido al recurso pedido):
- `localiza2api/Controllers/ContactsController.cs` — CRUD con `[Authorize]`, `CurrentUserId` desde `ClaimTypes.NameIdentifier`, DTOs de salida.
- `localiza2api/Controllers/LocationController.cs` — endpoint de alta frecuencia con lógica de dominio.
- `localiza2api/Controllers/AdminController.cs` — endpoint restringido por rol.
- `localiza2api/DTOs/` — convención de DTOs (records, sufijo `Dto`).
- `localiza2api/Data/AppDbContext.cs` — para saber si el recurso ya tiene entidad/DbSet o hace falta migración.

Sigue estas convenciones del proyecto:
1. Controller en `localiza2api/Controllers/<Nombre>Controller.cs`, ruta `[Route("api/<recurso>")]`, `[ApiController]`.
2. `[Authorize]` a nivel de clase salvo que el endpoint sea público (login/registro); en ese caso decir explícitamente por qué.
3. DTOs de request/response en `localiza2api/DTOs/`, como `record`.
4. Si el recurso necesita nueva tabla/columna, NO generes la migración tú directamente — indica que hace falta y sugiere usar `/migrate <nombre>` después.
5. Si el endpoint expone datos de ubicación, respeta la política de retención descrita en CLAUDE.md (3h/24h/30d).
6. Registra el controller si `Program.cs` necesita algo adicional (policies, DI de servicios) — revisa `localiza2api/Program.cs` primero.

Al terminar, muestra un resumen corto: ficheros creados/editados y si falta migración o registro en `Program.cs`.
