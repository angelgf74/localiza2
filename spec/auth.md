# Autenticacion y Flujos de seguridad

## Registro de usuario (2 fases - Double Opt-in)

### Fase 1: Solicitud
- POST /api/auth/register
- Crea PendingRegistration (24 horas)
- Envía email con token de confirmación

### Fase 2: Confirmación
- GET /api/auth/confirm/TOKENAQUI
- Crea User definitivo
- Acepta invitaciones pendientes automáticamente
- Respuesta: 302 redirect a confirm.html

## Login
- POST /api/auth/login (email, password)
- Devuelve JWT (HMAC-SHA256, 30 días)
- Cliente almacena en sessionStorage

## Recuperación de contraseña
- POST /api/auth/forgot-password (email)
- Genera PasswordResetToken (1 hora)
- POST /api/auth/reset-password (token, newPassword)

## Emparejamiento QR
- POST /api/contacts/pair/qr → genera PairingCode (24h)
- QR apunta a pair.html?token=TOKENAQUI
- POST /api/contacts/pair/accept → crea contacto bilateral

## Emparejamiento por Email
- POST /api/contacts/invite → crea ContactInvitation (7 días)
- Email con enlace pair.html
- GET /api/contacts/accept/TOKENAQUI → acepta

## Control de visibilidad
- PUT /api/auth/sharing {sharingEnabled}
- Si false: ubicación no se comparte (POST ignora silenciosamente)

## Compartición pública anónima
- POST /api/location/share → genera LocationShareLink (5min-24h)
- GET /api/location/share/TOKENAQUI (sin auth)
- share.html refresca cada 30s

## Rate Limiting
- 10 peticiones/minuto por IP en AuthController
- POST /register, /login, /resend-confirmation, /forgot-password

