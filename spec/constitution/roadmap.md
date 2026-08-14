# Roadmap

_Orden y estado de las features. Es la vista de "qué hay hecho, qué toca ahora y qué viene". Cada entrada apunta a su carpeta en `features/`._

## Hecho ✅

_Features completadas, en orden de implementación._

1. **001 · Compartición de ubicación en tiempo real** — Recolección de GPS en segundo plano, envío a API, visualización en mapa.
2. **002 · Emparejamiento bilateral por email** — Invitación con token, aceptación, relación mutua.
3. **003 · Emparejamiento bilateral por QR** — Generador de QR, código de 24h, escaneo y aceptación.
4. **004 · Historial de ruta** — Almacenamiento de ubicaciones, consulta ordenada por timestamp.
5. **005 · Visualización en mapa web** — Dashboard SPA con Leaflet, centrado automático, zoom dinámico.
6. **006 · Modo invisible (SharingEnabled)** — Toggle de compartición, ubicación oculta pero visible para contactos.
7. **007 · QR con countdown** — Contador visible de expiración, renovación automática 30s antes.
8. **008 · Alerta de batería baja** — Indicador si batteryLevel <= 20%, incluido en cada ubicación.
9. **009 · Historial paginado** — Cursores temporales, scroll infinito hacia el pasado, 50 puntos/página.
10. **010 · Enlace de ubicación compartida** — Token temporal público (5min-24h), visor anónimo en share.html.
11. **011 · Intervalo adaptativo de GPS** — 60s (movimiento) / 300s (quieto), ahorro de batería automático.
12. **012 · Resiliencia con WatchdogWorker** — Reinicio automático del servicio si cae, periodic + immediate.
13. **013 · Poda automática de ubicaciones** — Compresión escalonada: últimas 3h (1/min), 3h-24h (1/5min), 1-30d (1/30min), >30d elimina.
14. **014 · Cola offline con Room** — Sincronización automática de ubicaciones cuando vuelve la red.

## Siguiente 🔜

_Lo próximo a abordar. Idealmente una sola feature "en curso" a la vez._

15. **015 · Notificaciones push** — Alertas cuando contacto se conecta/desconecta, bajo nivel de batería.

## Correcciones pendientes 🔧

_Detectadas en análisis de seguridad/calidad de 2026-08-14. Marcar `[x]` al implementar._

- [x] **Validar longitud mínima de password en `Register`** — `AuthController.cs`. `ResetPassword` exige 8 caracteres, `Register` no valida nada; password vacío o de 1 carácter pasa hoy. Fix trivial.
- [x] **Rate limiting en `ContactsController.AcceptPairing`** — prueba de `PairingCode` sin límite de tasa (solo `AuthController` lleva `[EnableRateLimiting("auth")]`). Aplicado también a `pair/info/{token}` y `accept/{token}` (misma superficie de token-guessing).
- [x] **Rate limiting en `LocationController`** — `POST /api/location` y lecturas sin límite de tasa. Nueva política `location` (60/min por IP) a nivel de clase.
- [x] **Revocación de JWT tras cambio de contraseña** — `ResetPassword` cambia `PasswordHash` pero no invalida tokens ya emitidos; uno robado sigue válido hasta 30 días. Resuelto con `User.TokenVersion` (claim `tv` en el JWT, verificado en `OnTokenValidated` contra la BD) en vez de un sistema completo de refresh token — mismo efecto, migración `AddUserTokenVersion`. Seeders demo/superadmin solo incrementan la versión si la contraseña configurada cambió de verdad (si no, cada restart cerraría esas sesiones).
- [x] **Tests para `PruneLocationsService`** — lógica de buckets (60s/300s/1800s) sin cobertura; cero tests en todo el proyecto (.NET/Android/web). Nuevo proyecto `localiza2api.Tests` (xUnit + Testcontainers.PostgreSql, Postgres real vía Docker — el SQL crudo de la poda no es compatible con el proveedor InMemory). 4 tests cubriendo los 4 tramos de retención.
- [x] **Tests para emparejamiento bilateral de contactos** — creación de las 2 filas `Contact` al aceptar invitación/QR, sin cobertura. 4 tests en `ContactsControllerPairingTests`: QR feliz, auto-emparejamiento rechazado, código expirado, invitación por email.
- [ ] **Corregir `CLAUDE.md`: almacenamiento de token** — dice "Android DataStore / web localStorage"; código real usa `EncryptedSharedPreferences` (Android) y `sessionStorage` (web).
- [ ] **Depurar `mejoras.md`** — 9 de 12 ítems ya implementados en código actual (poda, visibilitychange, CORS, rate limit auth, paginación...); pasar por skill `mejoras-triage`.

## Backlog / ideas 💡

_Sin comprometer ni ordenar del todo. Ideas que respetan la constitución._

- **Dark mode** — Tema oscuro en web y Android basado en preferencias del sistema.
- **Multiidioma (i18n)** — Soporte para ES, EN, PT. Cuerdas en archivos de recursos.
- **Actualización de foto de perfil** — Avatar opcional sincronizado entre contactos.
- **Estadísticas de distancia** — Distancia total viajada, promedio de desplazamiento diario.
- **Interfaz de configuración mejorada** — Cambio de contraseña, gestión de privacidad, eliminación de datos.
- **Integración con calendarios** — Compartición temporal de ubicación durante eventos.
- **Deep links / redirección inteligente** — link → app si instalada, sino web.
- **Caché de tiles offline** — Descargar porción de mapa para funcionar sin internet.

> Cada feature nueva se crea como `features/NNN-nombre-feature/` con `spec.md`, `plan.md` y `tasks.md` antes de tocar código.
