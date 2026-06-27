# Base de Datos: PostgreSQL

## Descripción general

- **Motor**: PostgreSQL 12+
- **Host**: 192.168.0.185:5432
- **Base de datos**: localiza2
- **ORM**: Entity Framework Core (.NET 10)
- **Migraciones**: 6 (desde mayo 2026)

## Esquema de tablas

### Tabla `Users`

Usuarios registrados de la plataforma. Almacena credenciales, tokens de emparejamiento y control de compartición.

| Columna | Tipo | Restricciones | Propósito |
|---------|------|---------------|----------|
| Id | integer | PK, identity | Identificador único |
| Email | varchar(256) | UNIQUE, NOT NULL | Email del usuario |
| PasswordHash | text | NOT NULL | Hash BCrypt |
| Name | varchar(100) | NOT NULL | Nombre del usuario |
| CreatedAt | timestamptz | default UTC | Fecha de creación |
| SharingEnabled | boolean | default true | Control de visibilidad |
| PairingCode | varchar(32) | UNIQUE nullable | Token QR (24h) |
| PairingCodeExpiry | timestamptz | nullable | Expiración de PairingCode |
| PasswordResetToken | varchar(64) | UNIQUE nullable | Token de reset (1h) |
| PasswordResetExpiry | timestamptz | nullable | Expiración de reset |

**Navegaciones**: `Contacts[]`, `Locations[]`

**Índices**: PK(Id), UK(Email), FK(PairingCode), FK(PasswordResetToken)

---

### Tabla `PendingRegistrations`

Usuarios en proceso de confirmación de email (registro de 2 fases).

| Columna | Tipo | Restricciones |
|---------|------|---------------|
| Id | integer | PK, identity |
| Email | varchar(256) | UNIQUE |
| PasswordHash | text | NOT NULL |
| Name | varchar(256) | NOT NULL |
| Token | varchar(256) | UNIQUE |
| ExpiresAt | timestamptz | Expiración (24h) |

**Ciclo de vida**: Creado en `POST /register`, eliminado en `GET /confirm/{token}` exitoso.

---

### Tabla `Contacts`

Relación asimétrica: cada usuario tiene una entrada por contacto en su lista.

| Columna | Tipo | Restricciones | Propósito |
|---------|------|---------------|----------|
| Id | integer | PK | Identificador único |
| UserId | integer | FK → Users(Id) CASCADE | Usuario propietario |
| ContactUserId | integer | FK → Users(Id) SET NULL | Usuario contacto (null = invitación pendiente) |
| Email | varchar(256) | | Email del contacto |
| Alias | varchar(100) | | Nombre personalizado |
| PhotoUrl | text | nullable | Avatar del contacto |
| Status | text | enum: Pending/Accepted/Declined | Estado de la invitación |
| LocationPermissionGranted | boolean | default false | Permiso de compartir ubicación |
| CreatedAt | timestamptz | | Fecha de creación |

**Índice único**: `(UserId, Email)` — un usuario no puede invitar dos veces al mismo email.

**Estados**:
- **Pending**: Invitación enviada, no confirmada aún
- **Accepted**: Ambos usuarios confirmaron, comparten ubicación
- **Declined**: Usuario rechazó la invitación

---

### Tabla `ContactInvitations`

Tokens de invitación por email. Se crean en `POST /contacts/invite`.

| Columna | Tipo | Restricciones |
|---------|------|---------------|
| Id | integer | PK |
| InviterUserId | integer | FK → Users(Id) CASCADE |
| InvitedEmail | varchar(256) | Email del invitado |
| Token | text | UNIQUE |
| ExpiresAt | timestamptz | Expiración (7 días) |

**Lifecycle**: Creada en `POST /contacts/invite`, consumida en `GET /contacts/accept/{token}`.

---

### Tabla `UserLocations`

Historial de ubicaciones GPS con política de poda escalonada.

| Columna | Tipo | Restricciones | Propósito |
|---------|------|---------------|----------|
| Id | integer | PK | Identificador único |
| UserId | integer | FK → Users(Id) CASCADE | Usuario propietario |
| Latitude | double | | Latitud (WGS84) |
| Longitude | double | | Longitud (WGS84) |
| Accuracy | double | nullable | Precisión en metros |
| BatteryLevel | integer | nullable | Nivel de batería (0-100) |
| Timestamp | timestamptz | | Marca de tiempo |

**Índice**: `(UserId, Timestamp)` — optimiza consultas de historial por usuario y rango temporal.

**Poda automática** (cada hora):
```
Últimas 3h:  1 punto/min   (max 180 puntos)
3h–24h:      1 punto/5min  (max 252 puntos)
1–30 días:   1 punto/30min (max 1440 puntos)
> 30 días:   Eliminación
```

---

### Tabla `LocationShareLinks`

Tokens públicos para compartir ubicación sin autenticación.

| Columna | Tipo | Restricciones | Propósito |
|---------|------|---------------|----------|
| Id | integer | PK | Identificador único |
| Token | varchar(64) | UNIQUE | Token URL-safe (base64 sin +/=) |
| UserId | integer | FK → Users(Id) CASCADE | Usuario propietario |
| ExpiresAt | timestamptz | | Expiración (5min–24h) |
| CreatedAt | timestamptz | | Fecha de creación |

**Acceso**: `GET /api/location/share/{token}` — anónimo, devuelve última ubicación del usuario.

---

## Historial de migraciones

| Fecha | Nombre | Cambios |
|-------|--------|---------|
| 2026-05-17 | `InitialCreate` | Crea todas las tablas base con índices |
| 2026-05-26 | `AddUserPairingCode` | Añade `PairingCode` y `PairingCodeExpiry` a Users |
| 2026-05-26 | `AddUserPasswordReset` | Añade `PasswordResetToken` y `PasswordResetExpiry` a Users |
| 2026-05-26 | `AddBatteryLevelToUserLocation` | Añade `BatteryLevel` nullable a UserLocations |
| 2026-06-10 | `AddSharingEnabledToUser` | Añade `SharingEnabled` boolean a Users |
| 2026-06-11 | `AddLocationShareLink` | Crea tabla `LocationShareLinks` completa |

## Consultas características

### Obtener última ubicación de todos los contactos
```sql
SELECT DISTINCT ON ("UserId") *
FROM "UserLocations"
WHERE "UserId" = ANY(ARRAY[contactIds])
ORDER BY "UserId", "Timestamp" DESC;
```
Usa `DISTINCT ON` de PostgreSQL para eficiencia (evita `GroupBy`).

### Obtener historial con paginación temporal
```sql
SELECT *
FROM "UserLocations"
WHERE "UserId" = @userId
  AND "Timestamp" < @before
ORDER BY "Timestamp" DESC
LIMIT @limit;
```

### Poda de ubicaciones antiguas
```sql
DELETE FROM "UserLocations"
WHERE "Timestamp" < NOW() - INTERVAL '30 days';
```
Se ejecuta cada hora automáticamente.

## Relaciones de integridad

```
Users
  ├─ Contacts (UserId → Id) CASCADE
  ├─ UserLocations (UserId → Id) CASCADE
  ├─ LocationShareLinks (UserId → Id) CASCADE
  └─ Invitaciones enviadas (FK en ContactInvitations)

Contacts
  ├─ UserId → Users (CASCADE)
  └─ ContactUserId → Users (SET NULL)

ContactInvitations
  └─ InviterUserId → Users (CASCADE)
```
