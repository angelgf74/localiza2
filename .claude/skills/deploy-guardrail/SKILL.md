---
name: deploy-guardrail
description: Preflight checks and guardrails before running any localiza2 deploy script (deploy-ubuntu.sh, deploy-web.sh, _deploy.sh, install-usb.ps1). Use whenever the user asks to deploy, publish, or ship to a server/device.
---

# Deploy guardrail — localiza2

Los 4 scripts de deploy tocan sistemas compartidos o dispositivos físicos. Nunca los corras sin este preflight.

## Scripts y destinos

| Script | Destino | Riesgo |
|---|---|---|
| `deploy-ubuntu.sh` | Servidor Ubuntu prod, SSH `agfserver-angel`, systemd `localiza2api`, puerto 54005 | Alto — API en producción |
| `deploy-web.sh` | Mismo servidor, `/apps/localiza2/web`, solo estáticos, sin reinicio de servicio | Medio |
| `_deploy.sh [arm64\|arm]` | Raspberry Pi `192.168.0.175`, puerto 55003 | Medio — probablemente entorno personal/pruebas |
| `install-usb.ps1` | Dispositivo Android por USB | Bajo — local, reversible |

## Preflight obligatorio (antes de CUALQUIER script de servidor)

1. `git status` — nada sin commitear que no se quiera desplegar, o confirmación explícita de que sí.
2. Confirmar rama — deploy normalmente sale de `main`/`master`, no de una rama de feature a medias.
3. Decir en voz alta al usuario: qué script, qué destino, qué puerto/servicio se reinicia, y esperar confirmación antes de ejecutar.
4. Para `deploy-ubuntu.sh`: NUNCA usar `--setup` salvo que el usuario diga explícitamente que es primera instalación — reconfigura systemd y nginx.
5. Recordar siempre: `appsettings.json` del servidor Ubuntu NO se sobrescribe en actualizaciones normales (solo se copia si no existe). Si el cambio requiere nueva config/secreto, el usuario debe actualizarla a mano en el servidor.
6. Si hay migración EF Core pendiente (ver `ef-migration-review`), aplicarla ANTES o inmediatamente después del deploy, coordinado — un deploy de API con migración pendiente sin aplicar puede romper en producción.

## Tras el deploy
- Verificar que el servicio arrancó (`systemctl status localiza2api` vía SSH si es ubuntu) cuando sea razonable.
- Reportar resultado: destino, script, éxito/fallo, acciones manuales pendientes (ej. actualizar appsettings, aplicar migración).

## Nunca
- No reintentar un deploy fallido con flags destructivos o `--force`.
- No editar el `.gitignore` para "solucionar" un problema de deploy sin que el usuario lo pida.
- No asumir credenciales SSH ni generarlas — si el host `agfserver-angel` no está configurado en el entorno, decirlo y parar.
