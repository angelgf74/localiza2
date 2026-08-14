# Regla: deploy

Los scripts de deploy tocan producción o dispositivos físicos. Antes de ejecutar cualquiera, pasa por la skill `deploy-guardrail`.

## Destinos

| Script | Destino | Riesgo |
|---|---|---|
| `deploy-ubuntu.sh` | Servidor Ubuntu de producción, systemd `localiza2api`, puerto 54005 | Alto |
| `deploy-web.sh` | Mismo servidor, estáticos en `/apps/localiza2/web`, sin reinicio de servicio | Medio |
| `_deploy.sh [arm64\|arm]` | Raspberry Pi `192.168.0.175`, puerto 55003 | Medio |
| `deploy-usb.bat` / `install-usb.ps1` | Dispositivo Android por USB | Bajo |

## Preflight antes de cualquier deploy a servidor

1. **Nunca ejecutes un script de deploy sin pedir confirmación explícita al usuario**, indicando script, destino, puerto y qué servicio se reinicia.
2. `git status` limpio, o confirmación de que los cambios sin commitear deben ir igualmente.
3. Comprobar la rama: el deploy sale de `master`/`main`, no de una rama de feature a medias.
4. Si hay una migración EF pendiente, recuerda que **el arranque de la API la aplica sola** — ver `database.md`. Confirma que es lo que se quiere antes de reiniciar el servicio.

## `--setup`

`deploy-ubuntu.sh --setup` reconfigura systemd y Nginx. **Solo con instrucción explícita del usuario de que es una primera instalación.** En una actualización normal, nunca.

## Configuración del servidor

`appsettings.json` del servidor **no se sobrescribe**: el script solo lo copia si no existe. Si el cambio necesita una clave nueva, dilo claramente — hay que editarla a mano en el servidor o el arranque fallará.

## Después

- Verificar que el servicio levantó (`systemctl status localiza2api` por SSH) cuando sea razonable.
- Reportar: destino, script, resultado, y acciones manuales pendientes.

## Nunca

- No reintentar un deploy fallido añadiendo `--force` ni flags destructivos.
- No tocar `.gitignore` para "arreglar" un problema de deploy.
- No inventar ni generar credenciales SSH. Si el host `agfserver-angel` no está configurado, dilo y para.
