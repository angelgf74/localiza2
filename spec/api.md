# API REST: Referencia de endpoints

Base URL: https://localiza2-api.angelgf.com.es/

## AuthController - POST /register
Registra nuevo usuario (crea PendingRegistration con token).
- Body: {email, password, name}
- Respuesta: 200 {message} | 409 (ya existe) | 500

## AuthController - GET /confirm/{token}
Confirma email y crea User definitivo.
- Parámetros: token
- Respuesta: 302 redirect a confirm.html?status=ok|expired|invalid
- Acciones: Crea User, acepta invitaciones pendientes

## AuthController - POST /login
Autentica y devuelve JWT.
- Body: {email, password}
- Respuesta: 200 {token, userId, name, email} | 401
- JWT: HMAC-SHA256, 30 días

## AuthController - POST /resend-confirmation
Reenvía email de confirmación.
- Body: {email}
- Respuesta: 200 {message}

## AuthController - POST /forgot-password
Genera token de reset (1h).
- Body: {email}
- Respuesta: 200 {message} (siempre, anti-enumeración)

## AuthController - POST /reset-password
Cambia contraseña con token.
- Body: {token, newPassword}
- Respuesta: 200 {message} | 400 (inválido)

## AuthController - GET /sharing
Obtiene estado de compartición.
- Auth: Bearer JWT
- Respuesta: 200 {sharingEnabled}

## AuthController - PUT /sharing
Activa/desactiva compartición.
- Auth: Bearer JWT
- Body: {sharingEnabled}
- Respuesta: 200 {sharingEnabled}

## AuthController - DELETE /delete-account
Elimina cuenta permanentemente (cascada).
- Auth: Bearer JWT
- Respuesta: 204

## ContactsController - GET /contacts
Lista de contactos.
- Auth: Bearer JWT
- Respuesta: 200 ContactDto[]

## ContactsController - POST /invite
Invita contacto por email.
- Auth: Bearer JWT
- Body: {email, alias}
- Respuesta: 200 {message} | 409 (duplicado)

## ContactsController - GET /accept/{token}
Acepta invitación (anónimo).
- Parámetros: token
- Auth: Ninguna
- Respuesta: 200 {message} | 400 | 404

## ContactsController - PUT /{id}
Edita alias/foto de contacto.
- Auth: Bearer JWT
- Parámetros: id
- Body: {alias, photoUrl}
- Respuesta: 204 | 404

## ContactsController - DELETE /{id}
Elimina contacto.
- Auth: Bearer JWT
- Parámetros: id
- Respuesta: 204 | 404

## ContactsController - POST /pair/qr
Genera código QR (24h).
- Auth: Bearer JWT
- Respuesta: 200 {token, inviterName, expiresAt}

## ContactsController - DELETE /pair/qr
Cancela QR activo.
- Auth: Bearer JWT
- Respuesta: 204

## ContactsController - GET /pair/info/{token}
Info del QR (sin auth).
- Parámetros: token
- Respuesta: 200 {inviterName, type} | 404

## ContactsController - POST /pair/accept
Acepta invitación QR.
- Auth: Bearer JWT
- Body: {token}
- Respuesta: 200 {message, contactName} | 400 | 403 | 404

## LocationController - POST /location
Envía ubicación.
- Auth: Bearer JWT
- Body: {latitude, longitude, accuracy, batteryLevel}
- Respuesta: 204
- Nota: Ignora si SharingEnabled=false

## LocationController - GET /me
Última ubicación propia.
- Auth: Bearer JWT
- Respuesta: 200 {latitude, longitude, accuracy, timestamp} | 404

## LocationController - GET /me/history
Historial propio con paginación.
- Auth: Bearer JWT
- Query: limit=50, before=ISO8601
- Respuesta: 200 LocationPointDto[]

## LocationController - GET /contacts
Última ubicación de todos los contactos.
- Auth: Bearer JWT
- Respuesta: 200 ContactLocationDto[]

## LocationController - GET /contacts/{contactId}
Última ubicación de un contacto.
- Auth: Bearer JWT
- Parámetros: contactId
- Respuesta: 200 ContactLocationDto | 404

## LocationController - GET /contacts/{contactId}/history
Historial de contacto con paginación.
- Auth: Bearer JWT
- Parámetros: contactId
- Query: limit=50, before=ISO8601
- Respuesta: 200 LocationPointDto[] | 404

## LocationController - POST /share
Crea enlace público temporal.
- Auth: Bearer JWT
- Body: {expiresInMinutes} (5-1440, defecto 60)
- Respuesta: 200 {token, url, expiresAt}

## LocationController - DELETE /share/{token}
Elimina enlace de compartición.
- Auth: Bearer JWT
- Parámetros: token
- Respuesta: 204 | 404

## LocationController - GET /share/{token}
Obtiene ubicación compartida (sin auth).
- Parámetros: token
- Auth: Ninguna
- Respuesta: 200 {name, latitude, longitude, accuracy, timestamp, expiresAt} | 404

Rate Limiting: 10 peticiones/minuto por IP en AuthController
