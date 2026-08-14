# Regla: Android (`localiza2/`)

## Versionado — obligatorio

**Todo cambio en el módulo Android incrementa `versionCode` y `versionName`** en `localiza2/app/build.gradle.kts`.

- `versionCode`: entero, +1 siempre. Google Play rechaza un AAB con un `versionCode` ya subido.
- `versionName`: string visible (`"1.24"`).
- `build_aab.sh` hace ambos bumps automáticamente — **no los toques a mano antes de ejecutarlo** o saltarás un número.
- Para builds que no van a la tienda (`assembleDebug`), no hace falta bump.

## Flavors y `API_BASE_URL`

Dimensión `environment`, dos flavors:

| Flavor | `API_BASE_URL` |
|---|---|
| `production` | `https://localiza2-api.angelgf.com.es/` |
| `development` | IP de LAN, puerto 5135 |

`build_aab.sh` genera **solo el flavor `production`**. Si cambias la IP de `development` para pruebas locales, no la commitees mezclada con otro cambio — es config de máquina.

## Firma release

`signingConfigs.release` lee `rootProject.file("keystore.properties")`. El bloque **no tolera que falte el fichero**: `keystoreProperties["storeFile"] as String` lanza NPE. Si el usuario no tiene el keystore, `assembleRelease` y `build_aab.sh` fallan — eso es esperado, no lo "arregles" metiendo valores por defecto ni relajando el cast.

## SDK y toolchain

`compileSdk` / `targetSdk` = 37, `minSdk` = 26, Java/Kotlin JVM target 17. Subir `targetSdk` es un cambio de release, no incidental: comprueba permisos de ubicación en background y foreground services, que es donde Android rompe compatibilidad.

R8 activo en release (`isMinifyEnabled` + `isShrinkResources`). Si añades una librería que use reflexión o serialización, verifica `proguard-rules.pro` — un fallo de R8 solo aparece en el build release, nunca en debug.

## Flujo de release

Usa la skill `/release-android` o `android-release`. Resumen: bump → `./build_aab.sh` → verificar AAB → subir a Play Console. La cuenta demo para revisión de Google Play se siembra desde el servidor (ver `database.md`), no desde la app.
