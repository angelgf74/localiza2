# 001 - Comparticion de ubicacion - Tareas

- [x] LocationService: crear Foreground Service con FusedLocationProviderClient
- [x] LocationService: intervalo adaptativo (60s/300s segun distancia/tiempo)
- [x] LocationService: POST /api/location con retry logic
- [x] PendingLocation: entidad y DAO en Room
- [x] PendingLocation: encolamiento si falla red
- [x] ConnectivityManager.NetworkCallback: detectar reconexion
- [x] flushPendingLocations(): sincronizar cola con Mutex
- [x] WatchdogWorker: CoroutineWorker que verifica si servicio vivo
- [x] WatchdogWorker: policy KEEP (periodico) + REPLACE (inmediato)
- [x] BootReceiver: escuchar BOOT_COMPLETED y similares
- [x] BatteryOptimizationHelper: instrucciones OEM por fabricante
- [x] MainActivity: solicitar permisos en tiempo de ejecucion
- [x] SessionManager: inyectar token JWT en header Authorization
- [x] Verificar criterios de aceptacion en dispositivo real
- [x] Mover feature a "Hecho" en constitution/roadmap.md

## Mantenimiento (recurrente)

- [ ] Revisar logs de LocationService si usuarios reportan ubicacion desactualizada
- [ ] Ajustar intervalo adaptativo si bateria se agota rapido
- [ ] Monitorear crashes de WatchdogWorker en Android 14+
