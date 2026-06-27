# 001 - Comparticion de ubicacion - Plan

## Enfoque

Foreground Service en Android que:
1. Solicita FusedLocationProviderClient para GPS
2. Analiza distancia y tiempo para cambiar intervalo (adaptativo)
3. POST /api/location en cada actualizacion
4. Encola en Room si falla red (ConnectivityManager callback)
5. Sync automatico al recuperar conectividad (Mutex)
6. WorkManager (WatchdogWorker) verifica cada 15min que servicio este vivo

## Implementacion

1. LocationService (Foreground Service)
   - onCreate: inicia FusedLocationProviderClient
   - onStartCommand: crea notificacion persistente, schedula WatchdogWorker
   - onLocationChanged: POST /api/location con retry logic
   - onConnectivity: flushPendingLocations() si Room tiene datos
   
2. WatchdogWorker (CoroutineWorker)
   - Check: LocationService.isRunning == false
   - Si false: startForegroundService(LocationService)
   - Periodic: cada 15 min (KEEP policy)
   - Immediate: 3s delay si cae (REPLACE policy)

3. BootReceiver
   - Escucha BOOT_COMPLETED, QUICKBOOT_POWERON, MY_PACKAGE_REPLACED
   - scheduleImmediateRestart() + schedulePeriodicWatch()

4. PendingLocation (Room DAO)
   - insert, getOldest, deleteByIds, count
   - Sync en background thread con Mutex

## Decisiones

- LocationService vs JobService: Foreground Service porque requiere notificacion visible (ley Android 8+)
- Intervalo adaptativo vs fijo: adaptativo ahorra bateria sin perder datos en movimiento
- Room vs SharedPreferences: Room para volumen de datos (potencialmente 100s de ubicaciones)
- WorkManager vs Timer: WorkManager respeta Device Doze, garantiza ejecucion

## Riesgos

- Fabricante OEM puede matar el proceso: mitigacion con BootReceiver + WatchdogWorker periódico
- Permiso ACCESS_BACKGROUND_LOCATION denegado: UI muestra dialogo con instrucciones por fabricante
- Bateria agotada: no hay mitigacion, de responsabilidad del usuario
- Red inestable: cola offline maneja esto, reintenta al reconectarse
