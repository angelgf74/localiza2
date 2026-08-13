---
name: android-release
description: Full Android release flow for localiza2 — version bump, AAB build, checklist for Play Store submission. Use when the user wants to prepare, build, or ship a new Android release/version.
---

# Android release — localiza2

Flujo completo para preparar un release de `localiza2/` (Kotlin, flavor `production`, target actual SDK 37).

## 1. Estado previo
- `git status` en `localiza2/` — confirma que los cambios a incluir en el release ya están commiteados o el usuario sabe que se incluirán sin commitear.
- Lee commits recientes (`git log --oneline -10`) para saber qué entra en esta versión y redactar notas.

## 2. Versión
- `localiza2/app/build.gradle.kts` tiene `versionCode` y `versionName`.
- `./build_aab.sh` (desde `localiza2/`) hace el bump automático: `versionCode += 1`, `versionName` incrementa el minor (`1.24` → `1.25`). No lo hagas a mano salvo que el usuario pida un bump distinto (ej. major).
- CLAUDE.md exige incrementar versión en cada cambio — no te saltes este paso ni ofrezcas build sin bump.

## 3. Build
```
cd localiza2
./build_aab.sh
```
Requiere `localiza2/keystore.properties` (no está en git). Si falla por falta de keystore, no lo inventes — pide al usuario que lo configure.

Genera: `app/build/outputs/bundle/productionRelease/app-production-release.aab`.

## 4. Material de listing
- `localiza2/playstore/` (gitignored) guarda assets/notas de Play Store si existen — revisa si hay que actualizar release notes con lo que cambió.
- Si hay cambios de comportamiento visibles para el usuario, redacta notas de versión breves en español, tono de review de Google Play (conciso, sin jerga técnica).

## 5. Checklist final antes de entregar
- [ ] versionCode/versionName incrementados y coinciden entre `build.gradle.kts` y el AAB generado
- [ ] AAB compiló sin errores, flavor `productionRelease`
- [ ] Notas de versión redactadas si hay cambios visibles
- [ ] Cuenta demo (`demo@localiza2.app`) sigue funcionando si el release toca auth/permisos — sugiere probarla o usar `/demo-reset`
- [ ] Subida a Play Console es manual del usuario — no lo hagas tú

Reporta al final: versión nueva, ruta AAB, y checklist marcado.
