# Mejoras pendientes

## Rendimiento

**1. Poda de ubicaciones fuera del hot path**
`PruneLocationsAsync` ejecuta 4 DELETEs en PostgreSQL en cada `POST /api/location` (cada 60 s por usuario). Con varios usuarios activos es ruido constante. Alternativa: ejecutar la poda en un `BackgroundService` de .NET cada hora, o al menos solo cuando la inserción supera un umbral de filas.

**2. Polling web con visibilidad de pestaña**
`setInterval` sigue llamando a la API cada 30 s aunque la pestaña esté en segundo plano. Añadir un listener de `document.visibilitychange` para pausar/reanudar el timer tiene coste mínimo y reduce carga en el servidor.

---

## UX / Funcionalidad

**3. Modo invisible**
Un toggle "Dejar de compartir mi ubicación" sin cerrar sesión. En la API sería un campo `User.SharingEnabled`; el servicio Android seguiría corriendo pero sin enviar.

**4. Refresco automático del QR en la web**
El código de emparejamiento expira a los 15 min pero en `pair.html` solo hay un botón manual. Un countdown + refresco automático evita que el usuario se quede con un QR caducado sin saberlo.

**5. JWT con refresh token**
El token expira a las 24 h y el usuario queda desconectado sin aviso. Un refresh token de larga duración (7-30 días) con renovación silenciosa es el patrón estándar para apps móviles.

**6. Alerta de batería baja**
Ya se envía `batteryLevel` en cada actualización y se muestra en el mapa. Un indicador visual destacado (ej. icono rojo) cuando el contacto está por debajo del 15 % sería útil sin cambios en la API.

---

## Seguridad

**7. Rate limiting en login y registro**
No hay protección contra fuerza bruta en `POST /api/auth/login` ni en `POST /api/auth/register`. Con `AspNetCoreRateLimit` o el middleware de .NET 8+ (`AddRateLimiter`) se resuelve en pocas líneas.

**8. CORS restrictivo**
`AllowAnyOrigin()` está bien para desarrollo pero en producción sería preferible limitar a `localiza2-app.angelgf.com.es`.

---

## Funcionalidad nueva

**9. Enlace de ubicación temporal**
Compartir una URL de solo lectura con alguien que no tiene cuenta (caduca en X horas). Útil para "dile a alguien dónde estás" sin que se registre.

**10. Geofencing / alertas de llegada**
Notificación local en Android cuando un contacto entra o sale de un radio definido. Se puede hacer puramente en cliente (comparar coordenadas en el servicio) sin cambios en la API.

**11. Historial paginado**
`GET /api/location/me/history` devuelve los últimos 50 por defecto, sin paginación. Con un parámetro `before` (timestamp) se habilita scroll infinito hacia atrás en la app y la web.

---

## Deuda técnica

**12. `GetContactsLocations` hace N+1 implícito**
El `GroupBy` de LINQ sobre `UserLocations` carga todos los registros y filtra en memoria. Un `DISTINCT ON` en PostgreSQL sería más eficiente con muchos contactos activos.
