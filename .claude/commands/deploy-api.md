---
description: Deploy localiza2api to a target environment (ubuntu, pi, usb) with confirmation
---

Despliega la API al destino indicado en $ARGUMENTS (ubuntu | pi | usb). Si no se especifica, pregunta cuál.

Antes de ejecutar CUALQUIER script de deploy:
1. `git status` — confirma que no hay cambios sin commitear que el usuario no quiera desplegar.
2. Muestra qué script se va a correr y qué hace, según destino:
   - **ubuntu** → `./deploy-ubuntu.sh` (servidor Ubuntu vía SSH `agfserver-angel`, puerto Kestrel 54005, systemd `localiza2api`). `--setup` SOLO en primera instalación. Recuerda: `appsettings.json` del servidor NUNCA se sobrescribe en actualizaciones normales.
   - **pi** → `./_deploy.sh [arm64|arm]` (Raspberry Pi en `192.168.0.175`, puerto 55003).
   - **usb** → no aplica a la API; redirige a `/release-android` o `install-usb.ps1` (eso es Android, no API).
3. Pide confirmación explícita al usuario antes de ejecutar — esto toca un servidor compartido en producción.
4. Ejecuta el script solo tras confirmación.
5. Si el script falla a mitad, NO reintentes automáticamente con flags destructivos; reporta el error exacto y pregunta cómo proceder.

Al terminar, resume: destino, script usado, resultado (OK/error) y si se aplicó `--setup`.
