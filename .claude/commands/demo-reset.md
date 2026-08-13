---
description: Re-seed the demo@localiza2.app account used for Play Store review
---

Verifica/re-siembra la cuenta demo usada por revisión de Google Play.

Contexto (`localiza2api/Data/DemoSeeder.cs`):
- Cuenta principal: `demo@localiza2.app` (configurable vía `Demo:Email`).
- Password se toma de `Demo:Password` en config — si no está seteada, `SeedAsync` no hace nada (es un no-op silencioso en desarrollo).
- Es idempotente: cada arranque del API vuelve a hashear la password configurada y verifica el contacto `demo-contacto@localiza2.app` ("Ana García") y su historial de ubicaciones de ejemplo.

Pasos:
1. Pregunta si el objetivo es local o producción.
2. **Local**: confirma que `Demo:Password` está en `localiza2api/appsettings.Development.json` (no está en git); si falta, avisa que el seeder no hará nada.
3. **Producción**: el reseed ocurre solo reiniciando el servicio en el servidor (`systemctl restart localiza2api`, dentro de `/deploy-api ubuntu` o manual por SSH) — la lógica corre en el arranque del API, no hay endpoint dedicado. Confirma con el usuario antes de reiniciar el servicio en producción, ya que corta el servicio brevemente para usuarios reales.
4. Tras el reinicio/arranque, no hay forma de verificar desde aquí sin credenciales — pide al usuario que confirme login con `demo@localiza2.app` en la app o web.

No inventes un endpoint HTTP de reseed si no existe — la única vía es el arranque del proceso.
