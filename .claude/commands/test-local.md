---
description: Spin up API + web locally and verify Android USB connection, per TESTING_LOCAL.md
---

Levanta el entorno de pruebas local siguiendo `TESTING_LOCAL.md`.

1. Lee `TESTING_LOCAL.md` completo para confirmar puertos/pasos actuales (puede haber cambiado desde la última vez).
2. API: `cd localiza2api && dotnet run` (por defecto `localhost:5135` o el puerto de `launchSettings.json`, que no está en git — verifica si existe).
3. Web: `cd localiza2web && python -m http.server 8000`.
4. Android por USB (si aplica): corre `adb devices` y confirma que el dispositivo aparece como `device` (no `unauthorized`). La app en el emulador/dispositivo debe apuntar a `http://10.0.2.2:5135` para alcanzar el localhost del PC.
5. Lanza API y web como procesos en background (usa `run_in_background`) para no bloquear la sesión, y reporta las URLs activas.

Si algún puerto ya está en uso o `appsettings.Development.json` no existe, repórtalo en vez de improvisar credenciales.
