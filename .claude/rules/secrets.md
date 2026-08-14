# Regla: secretos

## Nunca commitear

Estos ficheros contienen credenciales reales y están en `.gitignore`. Si aparecen como *staged* o *tracked*, es señal de alarma (`git add -f` o `.gitignore` alterado): repórtalo al usuario y **para** — no ejecutes `git rm --cached` sin confirmación.

| Fichero | Contiene |
|---|---|
| `localiza2api/appsettings.json` | Connection string PostgreSQL, `Jwt:Key`, API key Brevo, `Demo:Password`, `SuperAdmin:Password` |
| `localiza2api/appsettings.Development.json` | Igual, entorno local |
| `localiza2api/Properties/launchSettings.json` | Puertos y variables locales |
| `localiza2/keystore.properties` | `storePassword`, `keyPassword`, `keyAlias` |
| `localiza2/keystore/`, `**/*.jks`, `**/*.keystore` | Keystore de firma release |
| `localiza2/local.properties` | Ruta SDK local |
| `info.txt` | Fichero de referencia de credenciales |
| `_deploy.sh`, `deploy-usb.bat` | Rutas/hosts privados |
| `localiza2/playstore/`, `localiza2_demo.mp4` | Material de tienda |

## Antes de cualquier commit

1. `git status --porcelain` y `git diff --cached --stat`.
2. Ninguno de los ficheros de arriba en la lista.
3. Revisar el **contenido** de ficheros nuevos staged (`.cs`, `.json`, `.md`, `.sh`, `.js`) buscando:
   - connection strings con `Password=` real
   - valores no-placeholder en claves `Jwt`, `ApiKey`, `Secret`, `Key`
   - tokens Brevo (`xkeysib-…`)
   - contraseñas de la cuenta demo o superadmin

Skill `/secrets-check` automatiza esto.

## Valores que sí pueden ir en git

URLs públicas (`https://localiza2-api.angelgf.com.es/`), IPs de LAN de desarrollo ya presentes en `build.gradle.kts`, y nombres de servicio systemd. No son secretos.

## Config del servidor

`deploy-ubuntu.sh` **no sobrescribe** `appsettings.json` en el servidor: solo lo copia si no existe. Si un cambio necesita una clave nueva de configuración, avisa al usuario de que debe editarla a mano en el servidor — el deploy no la va a llevar.
