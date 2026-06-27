# 001 - Comparticion de ubicacion en tiempo real

Estado: implementado OK

## Que hace

El usuario autoriza recopilacion continua de GPS en segundo plano. La app envia ubicacion a la API regularmente. Contactos ven marcador actualizado en mapa. Si desactiva comparticion, ubicacion deja de enviarse pero sigue viendo la de sus contactos.

## Por que

Funcionalidad central: permite a grupos saber donde esta cada uno. Resuelve coordinacion, seguridad, colaboracion.

## Criterios de aceptacion

- [x] App solicita permiso ACCESS_FINE_LOCATION
- [x] LocationService recolecta GPS cada 60s (movimiento) o 300s (quieto)
- [x] POST /api/location envia lat, lon, accuracy, batteryLevel
- [x] Ubicacion incluye timestamp del servidor
- [x] Si sharingEnabled=false, ubicacion no se envia
- [x] Si red cae, ubicaciones se encolan en Room
- [x] Al volver la red, cola se sincroniza automaticamente
- [x] WatchdogWorker reinicia LocationService si muere
- [x] BootReceiver inicia servicio despues de reboot

## Fuera de alcance

- Geofencing: alertas entrada/salida de zona
- Notificaciones push: conectado/desconectado (feature 015)
- Historial comprimido: feature 013
