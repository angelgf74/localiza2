---
description: Bump Android version and build the release AAB
---

Prepara un release de la app Android.

1. Lee `localiza2/app/build.gradle.kts` y localiza las líneas actuales de `versionCode` y `versionName`.
2. Si el usuario no ha corrido ya `./build_aab.sh` (que incrementa versión automáticamente: `versionCode += 1`, `versionName` sube el minor, ej. `1.24` → `1.25`), pregunta si quiere que lo ejecutes tú:
   ```
   cd localiza2 && ./build_aab.sh
   ```
   Esto compila el flavor `productionRelease` y requiere `keystore.properties` (no está en git, debe existir localmente).
3. Confirma que el AAB se generó en `app/build/outputs/bundle/productionRelease/app-production-release.aab`.
4. Recuerda al usuario: `localiza2/playstore/` guarda material de listing/notas — si hay cambios de usuario visibles, sugiere actualizar notas de versión ahí.
5. NO subas nada a Play Console tú mismo — eso es una acción manual del usuario fuera de este entorno.

Resume al final: versionCode/versionName nuevo y ruta del AAB generado.
