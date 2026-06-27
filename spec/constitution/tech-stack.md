# Tech stack y convenciones

_Cómo está construido el proyecto y las reglas que todo el código debe respetar. Es la referencia técnica que ningún plan de feature debería contradecir._

## Tecnologías

### Android
- **Lenguaje:** Kotlin 2.2.10
- **Build:** Gradle 9.2.1, AGP 9.2.1
- **SDK:** compileSdk/targetSdk 35, minSdk 26 (Android 8.0+)
- **Tests:** No hay suite automatizada (validación manual)
- **Despliegue:** Google Play Store (AAB con versionCode incremental)

### API
- **Lenguaje:** C# (.NET 10)
- **Framework:** ASP.NET Core + Entity Framework Core
- **Base de datos:** PostgreSQL 12+ con Npgsql
- **Tests:** No hay suite automatizada (validación con Postman/manual)
- **Despliegue:** Ubuntu 22.04 (systemd service), reverse proxy Nginx

### Web
- **Lenguaje:** Vanilla JavaScript (ES2020), HTML, CSS
- **Mapas:** Leaflet.js + OpenStreetMap (MAPNIK tiles)
- **Tests:** No hay suite automatizada (validación manual en navegador)
- **Despliegue:** Static hosting (nginx), sin build step

## Archivos / módulos clave

### Android (`localiza2/`)
- `app/build.gradle.kts` — Configuración de build, dependencias, versionCode/Name
- `app/src/main/java/com/localiza2/services/LocationService.kt` — Recolección de GPS en segundo plano
- `app/src/main/java/com/localiza2/workers/WatchdogWorker.kt` — Resiliencia automática
- `app/src/main/java/com/localiza2/utils/SessionManager.kt` — Almacenamiento seguro de JWT
- `app/src/main/java/com/localiza2/ui/` — Activities, Fragments, ViewModels
- `app/src/main/java/com/localiza2/db/AppDatabase.kt` — Room database (PendingLocation)
- `app/src/main/AndroidManifest.xml` — Permisos, componentes, receivers

### API (`localiza2api/`)
- `Program.cs` — DI, autenticación JWT, CORS, rate limiting
- `Data/AppDbContext.cs` — EF Core schema, migrations, índices
- `Controllers/` — AuthController, ContactsController, LocationController
- `Services/` — TokenService (JWT), EmailService (Brevo), PruneLocationsService
- `Models/` — User, Contact, UserLocation, ContactInvitation, etc.
- `appsettings.json` — Configuración (no en git: secrets)

### Web (`localiza2web/`)
- `index.html` + `app.js` — SPA principal con mapa y contactos
- `pair.html` + `pair.js` — Aceptar invitaciones y reset de contraseña
- `share.html` — Visor público de ubicación compartida
- `style.css` — Estilos responsive

## Comandos

### Android
- `./gradlew assembleDebug` — Debug APK
- `./gradlew assembleRelease` — Release APK (requiere keystore.properties)
- `./build_aab.sh` — Release AAB e incrementa versionCode/Name automáticamente

### API
- `cd localiza2api && dotnet build` — Compilar
- `cd localiza2api && dotnet run` — Ejecutar locally (puerto 5000)
- `cd localiza2api && dotnet publish -c Release -r linux-x64 -o ../deploy` — Publicar

### Web
- Ninguno: archivos estáticos, se sirven directamente

### Deploy
- `./deploy-ubuntu.sh` — Deploy API a Ubuntu
- `./deploy-ubuntu.sh --setup` — Setup inicial (systemd service)
- `./deploy-web.sh` — Deploy web frontend
- `./_deploy.sh linux-arm64` — Deploy a Raspberry Pi 64-bit

## Modelo de datos / dominio

**User** — Usuario registrado. Campos clave:
- `Id` (PK), `Email` (UNIQUE), `PasswordHash` (BCrypt), `Name`
- `SharingEnabled` (bool) — Control de visibilidad
- `PairingCode`, `PasswordResetToken` — Tokens de corta duración

**Contact** — Relación asimétrica entre usuarios. Campos clave:
- `UserId` (FK → User), `ContactUserId` (FK → User, nullable si invitación pendiente)
- `Status` (enum: Pending/Accepted/Declined), `LocationPermissionGranted` (bool)
- Índice único: `(UserId, Email)` — evita duplicados en lista de un usuario

**UserLocation** — Historial de ubicaciones. Campos clave:
- `UserId` (FK), `Latitude`, `Longitude`, `Accuracy`, `BatteryLevel`, `Timestamp`
- Índice: `(UserId, Timestamp)` — optimiza historial por rango temporal
- **Poda automática**: últimas 3h (1 pt/min), 3h-24h (1 pt/5min), 1-30d (1 pt/30min), >30d (elimina)

**LocationShareLink** — Token público temporal. Campos clave:
- `Token` (UNIQUE, 32 bytes base64), `UserId` (FK), `ExpiresAt`
- Acceso anónimo: devuelve última ubicación del usuario

## Convenciones

### Kotlin (Android)
- `camelCase` para variables, funciones, parámetros
- `PascalCase` para clases, data classes, enums
- Corrutinas con `viewModelScope`, `lifecycleScope`, coroutines + Mutex para sincronización
- Retrofit + OkHttp con interceptores (auth, logging)

### C# (.NET)
- `PascalCase` para clases, métodos, propiedades públicas
- `_camelCase` para campos privados
- EF Core con fluent API en `OnModelCreating`, índices explícitos
- Validación en Controllers: modelos no nulos, rango de valores

### JavaScript (Web)
- `camelCase` para variables, funciones
- Funciones anónimas y arrow functions
- Refresh cada 30 segundos (pausa con `visibilitychange`)
- Manejo de errores: 401 → logout automático

### Idioma
- Código: inglés (variables, funciones, clases)
- Comentarios: mínimos (solo por qué, no qué)
- UI: español (strings en JS, HTML)

## Estilo visual

### Colores
- Primario: azul (#2196F3)
- Secundario: verde (#4CAF50)
- Frescura de contacto: verde (<5min) → amarillo (5-60min) → gris (>60min offline)
- Fondo: blanco/light, sin dark mode aún

### Tipografía
- `system-ui` stack: sistema preferida del dispositivo
- Tamaños: base 16px, escalas para mobile responsive

### Responsive
- Mobile: < 768px (viewport único, vertical)
- Tablet+: >= 768px (sidebar + mapa lado a lado)

## Límites duros

- **No subir a git:** `appsettings.json`, `keystore.properties`, `localiza2/keystore/`, `.env*`, `info.txt` (credentials)
- **Contraseñas:** siempre BCrypt, nunca plaintext
- **Tokens JWT:** nunca en cookies (header Authorization), duración máx 30 días
- **API:** siempre HTTPS en producción, CORS dinámico desde config
- **Dependencias:** no añadir sin justificar en PR
- **Versioncode:** OBLIGATORIO incrementar en cada cambio (versionCode + versionName en build.gradle.kts)
- **Datos sensibles:** no loguear tokens, contraseñas, emails en producción
- **Rate limiting:** 10 req/min/IP en endpoints de auth, nunca deshabilitar
- **Seguridad:** no deshabilitar SSL en producción, no omitir pre-commit hooks
