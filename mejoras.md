# Mejoras pendientes

## UX / Funcionalidad

**5. JWT con refresh token — backend hecho, clientes pendientes** (API 2026-08-14)
Backend completo: entidad `RefreshToken` (hash SHA-256, nunca se guarda en claro), rotación en cada uso, detección de reuso (revoca toda la familia si un token ya rotado se vuelve a presentar — señal de robo), endpoints `POST /api/auth/refresh` y `POST /api/auth/logout`, y `ResetPassword` revoca todos los refresh tokens activos del usuario. Tests en `AuthControllerRefreshTests`.

El JWT de acceso se ha dejado **sin tocar a propósito** (sigue en 30 días) para no romper la app Android en producción, que aún no sabe pedir un refresh. Falta: acortar el JWT (p. ej. 1h) y cablear Android (interceptor OkHttp para refresh automático) y opcionalmente la web — coordinado como un único release, porque acortar el JWT sin que el cliente sepa refrescar sí rompería sesiones.

---

## Funcionalidad nueva

**10. Geofencing / alertas de llegada**
Notificación local en Android cuando un contacto entra o sale de un radio definido. Se puede hacer puramente en cliente (comparar coordenadas en el servicio) sin cambios en la API.

---

## Hecho

- **1. Poda de ubicaciones fuera del hot path** (2026-08-14) — `PruneLocationsService` es un `BackgroundService` que corre cada hora; `UpdateLocation` ya no ejecuta DELETEs en el hot path.
- **2. Polling web con visibilidad de pestaña** (2026-08-14) — `document.addEventListener('visibilitychange', ...)` en `app.js` pausa/reanuda el timer.
- **3. Modo invisible** (2026-08-14) — `User.SharingEnabled` + endpoints `GET/PUT /api/auth/sharing`; `LocationController` respeta el flag al leer ubicaciones.
- **4. Refresco automático del QR en la web** (2026-08-14) — `loadQrCode()` en `app.js`: countdown en vivo (`setInterval` 1s) + renovación automática 30s antes de expirar.
- **6. Alerta de batería baja** (2026-08-14) — web: icono rojo si `batteryLevel <= 20` (`app.js`); Android: color rojo/naranja por umbral en `ContactsAdapter.kt`.
- **7. Rate limiting en login y registro** (2026-08-14) — política `auth` (`AddRateLimiter`, 20/min por IP) en `AuthController`. Extendido además a los endpoints de pairing/token-guessing de `ContactsController` (ver roadmap).
- **8. CORS restrictivo** (2026-08-14) — orígenes limitados a `App:WebUrl` en producción; `AllowAnyOrigin()` solo como fallback si no hay origen configurado.
- **9. Enlace de ubicación temporal** (fecha original de implementación no registrada) — `LocationShareLink`, `POST/DELETE/GET /api/location/share`.
- **11. Historial paginado** (fecha original de implementación no registrada) — parámetro `before` (timestamp) en `GET /api/location/me/history` y `.../contacts/{id}/history`.
- **12. `GetContactsLocations` N+1** (2026-08-14) — usa `SELECT DISTINCT ON ("UserId")` vía SQL crudo (`FromSqlInterpolated`) en vez de cargar todo y agrupar en memoria.
