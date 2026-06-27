# Funcionalidades clave de Localiza2

## Compartición de ubicación en tiempo real
- LocationService: recopila GPS continuamente
- Intervalo adaptativo: 60s (movimiento) o 300s (quieto)
- POST /api/location: envía lat, lon, accuracy, battery

## Emparejamiento de contactos
- QR (24h): token de 20 caracteres, renovación automática
- Email (7 días): invitación con token
- Relación bilateral: ambos ven ubicaciones mutuamente

## Historial de ruta con paginación
- GET /location/contacts/ID/history?limit=50&before=ISO8601
- Mapa con polyline del historial
- Botón 'Cargar más' para scroll infinito

## Modo invisible (SharingEnabled)
- PUT /auth/sharing para desactivar compartición
- Ubicación no se comparte
- Tú sí puedes ver contactos

## Cola offline
- Room Database: PendingLocation
- Sincroniza automáticamente cuando vuelve la red

## Alerta de batería baja
- Indicador si batteryLevel <= 20%
- Incluido en cada ubicación

## Compartición pública temporal
- POST /location/share (5min-24h)
- GET /location/share/TOKEN (sin auth)
- share.html: visor que refresca cada 30s

## Resiliencia
- WatchdogWorker: verifica LocationService cada 15min
- BootReceiver: reinicia en boot
- Reinicio inmediato (3s) si cae

## Poda automática
- Cada hora: comprime historial
- Últimas 3h: 1/min, 3h-24h: 1/5min, 1-30d: 1/30min, >30d: elimina

## Sugerencias integradas
- Formulario para enviar bugs y sugerencias
- POST a https://angelgf.com.es/gestorsugerenciasapi/

