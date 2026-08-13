---
description: Verify no secrets are staged before commit
---

Verifica que no haya secretos en camino a un commit.

1. Corre `git status` y `git diff --cached --stat` (staged) y `git status --porcelain` (untracked).
2. Comprueba explícitamente que NINGUNO de estos ficheros esté staged o a punto de añadirse:
   - `localiza2api/appsettings.json`, `localiza2api/appsettings.Development.json`
   - `localiza2/keystore.properties`, `localiza2/keystore/`, `**/*.jks`, `**/*.keystore`
   - `localiza2/local.properties`
   - `info.txt`, `localiza2_demo.mp4`, `_deploy.sh`, `deploy-usb.bat`, `localiza2/playstore/`
   - `localiza2api/Properties/launchSettings.json`
3. Estos ya están en `.gitignore` — si `git status` los muestra como trackeados o staged, es una señal de alarma (alguien los añadió con `git add -f` o el .gitignore cambió). Repórtalo, no los quites tú mismo sin confirmar con el usuario.
4. Además, revisa el CONTENIDO de cualquier fichero nuevo que sí esté staged (`.cs`, `.json`, `.md`, scripts) por patrones típicos de secreto: connection strings con password, `JWT`/`ApiKey`/`Secret` con valores no-placeholder, tokens Brevo.
5. Si todo limpio, dilo en una línea. Si algo sospechoso, lista fichero+línea exacta y espera instrucción — no hagas `git rm --cached` sin confirmar.
