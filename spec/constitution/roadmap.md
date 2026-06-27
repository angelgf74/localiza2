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
