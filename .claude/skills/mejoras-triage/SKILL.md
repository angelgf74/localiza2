---
name: mejoras-triage
description: Reads mejoras.md, helps pick or scope the next pending improvement, and marks items done after implementing. Use when the user asks "qué mejoro" / "siguiente tarea" / references mejoras.md, or after finishing an item from that list.
---

# Mejoras triage — localiza2

`mejoras.md` (raíz del repo) es una lista de mejoras pendientes en español, agrupadas por categoría (Rendimiento, UX/Funcionalidad, Seguridad, Funcionalidad nueva...). No hay tracking automático — es responsabilidad de esta skill mantenerla honesta.

## Al pedir "qué hago siguiente" / elegir tarea
1. Lee `mejoras.md` completo.
2. Resume las opciones agrupadas por categoría, en una línea cada una.
3. Si el usuario no prioriza, sugiere basándote en:
   - Seguridad primero si hay algo sin mitigar (ej. rate limiting, CORS).
   - Luego impacto/esfuerzo bajo (quick wins) antes que features grandes.
   - Señala dependencias entre ítems si las hay (ej. algo que toca el mismo fichero que otro pendiente).
4. Al implementar un ítem, usa el número/título de `mejoras.md` como referencia en el trabajo, pero no lo copies en comentarios de código.

## Al terminar de implementar un ítem
1. Marca el ítem en `mejoras.md` como hecho: mover a una sección `## Hecho` al final del fichero (créala si no existe) con la fecha, en vez de solo borrarlo — conserva histórico.
2. Si el ítem generó cambios de API/schema/spec, recuerda usar `spec-sync` y, si aplica, `/migrate`.
3. No borres ítems relacionados que quedaron parcialmente resueltos — anota qué falta.

## Formato al mover a Hecho
```
## Hecho

- **N. Título** (YYYY-MM-DD) — breve nota de cómo se resolvió, si difiere del enfoque original propuesto.
```

No inventes ítems nuevos en `mejoras.md` sin que el usuario los pida — esta skill gestiona la lista existente, no genera roadmap.
