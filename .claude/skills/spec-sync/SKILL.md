---
name: spec-sync
description: Detects when changes to Controllers/Models/Migrations leave spec/*.md docs (api.md, database.md, architecture.md, features.md) stale, and updates them. Use after implementing API/schema/feature changes, or when asked to check/update the spec docs.
---

# Spec sync — localiza2

El repo mantiene `spec/` con documentación viva: `api.md`, `database.md`, `architecture.md`, `features.md`, `android.md`, `web.md`, `auth.md`, más `spec/features/`. Tiende a quedar desactualizado tras cambios de código.

## Cuándo usar
- Después de añadir/modificar un endpoint (`localiza2api/Controllers/`) → revisar `spec/api.md`.
- Después de una migración EF Core (`localiza2api/Data/Migrations/`) → revisar `spec/database.md`.
- Después de cambios estructurales (nuevo servicio, cambio de flujo auth, nuevo componente) → revisar `spec/architecture.md`.
- Después de una feature nueva o cambio de UX relevante → revisar `spec/features.md` y `spec/features/`.
- Cambios específicos de Android/Web → `spec/android.md` / `spec/web.md`.

## Proceso
1. Identifica qué cambió (diff de la sesión actual o `git diff`/`git log` reciente si no hay contexto).
2. Abre el/los ficheros spec relevantes y compara contra el código actual — busca endpoints, columnas, flujos o campos mencionados en el spec que ya no coincidan con la implementación.
3. Actualiza SOLO lo que quedó desactualizado por el cambio en curso. No reescribas secciones no afectadas ni cambies el estilo/formato existente del documento.
4. Si el cambio es lo bastante grande como para necesitar una sección nueva, sigue la estructura y tono ya presentes en ese fichero.
5. No toques `spec/constitution`, `spec/COMPARISON.md`, `spec/UNIFICATION_PLAN.md`, `spec/PHASE_1_VERIFICATION.md`, `spec/VISUAL_COMPARISON.md`, `spec/README.md` salvo que el usuario lo pida explícitamente — son documentos de proceso/histórico, no specs vivas del sistema.

Reporta al final qué ficheros spec se actualizaron y qué quedó igual (con motivo, si es relevante).
