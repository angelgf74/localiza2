# Android: Especificaciones de la app

## Build Configuration

Archivo: localiza2/app/build.gradle.kts

### Identidad de la app
- applicationId: es.angelgf.localiza2
- compileSdk: 35
- targetSdk: 35
- minSdk: 26 (Android 8.0+)
- versionCode: 19
- versionName: 1.18

### Configuración de compilación
- Language: Kotlin 2.2.10
- Gradle: 9.2.1
- Java/Kotlin target: 17
- ViewBinding: enabled
- BuildConfig: enabled

## Permisos declarados (11 total)

- INTERNET, ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION
- ACCESS_BACKGROUND_LOCATION, FOREGROUND_SERVICE, FOREGROUND_SERVICE_LOCATION
- POST_NOTIFICATIONS, RECEIVE_BOOT_COMPLETED, REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
- WAKE_LOCK, ACCESS_NETWORK_STATE

## Services

### LocationService (Foreground Service)
- Intervalo adaptativo: 60s (en movimiento) / 300s (quieto)
- Cola offline con Room (PendingLocation)
- Notificación persistente
- FusedLocationProviderClient para GPS
- Batería incluida en cada actualización
- WatchdogWorker: resiliencia automática

### WatchdogWorker
- CoroutineWorker de WorkManager
- Periódico: cada 15 min
- Inmediato: 3s delay con EXPEDITED
- Reinicia LocationService si está muerto

### BootReceiver
- Escucha: BOOT_COMPLETED, QUICKBOOT_POWERON, MY_PACKAGE_REPLACED
- Acción: arrancar LocationService y schedular WatchdogWorker

## Fragmentos principales

- LoginFragment: Email + password
- RegisterFragment: Registro con confirmación de email
- ForgotPasswordFragment: Recuperar contraseña
- ContactsFragment: Lista de contactos con FAB
- ContactHistoryBottomSheet: Mapa embebido con historial paginado
- PairContactBottomSheet: QR (con countdown) + Email
- MapFragment: Mapa osmdroid con marcadores de contactos
- HelpBottomSheet: Ayuda integrada
- SuggestionsBottomSheet: Enviar sugerencias a API externa

## ViewModels

- AuthViewModel: registro, login, reset password
- ContactsViewModel: lista de contactos, invitaciones
- MapViewModel: ubicaciones, historial con paginación
- MapSharedViewModel: transporte de HistoryRequest entre fragmentos

## Datos

### SessionManager
- EncryptedSharedPreferences (AES256-GCM/SIV)
- Almacena: token JWT, userId, name, email

### BatteryOptimizationHelper
- Detecta fabricante OEM (Xiaomi, Huawei, Samsung, OnePlus, OPPO, etc.)
- Muestra instrucciones específicas para desactivar restricciones de batería

### Room Database
- PendingLocation: cola offline de ubicaciones
- Propósito: sincronizar ubicaciones cuando vuelve la red

## APIs

### ApiService (17 endpoints)
- Auth, Contacts, Location
- Retrofit + OkHttp con interceptor de Bearer token
- Base URL: https://localiza2-api.angelgf.com.es/

### SuggestionsApiService
- POST a https://angelgf.com.es/gestorsugerenciasapi/api/Sugerencias
- Sin autenticación
