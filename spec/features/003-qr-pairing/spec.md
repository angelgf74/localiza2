# 003 - Emparejamiento bilateral por QR

Estado: implementado OK

## Que hace

Un usuario (A) genera un codigo QR en la app o web. QR contiene un token de 20 caracteres, valido por 24 horas. Otro usuario (B) escanea el QR con su app, se autentica si es necesario, y acepta automáticamente. Ambos se vuelven contactos mutuos y pueden verse las ubicaciones.

Countdown visible cuenta hacia expiración del QR. Se renueva automaticamente 30s antes de expirar.

## Por que

Mas rapido y seguro que email. No requiere escribir email manualmente. Token corta duración evita que un QR viejero se reutilice.

## Criterios de aceptacion

- [x] POST /api/contacts/pair/qr genera PairingCode (20 chars, 24h)
- [x] QR apunta a pair.html?token=CODIGO
- [x] App Android: ZXing genera QR visual
- [x] Web: qrcodejs genera QR visual
- [x] Countdown visible: segundos hasta expiracion
- [x] Renovacion automatica: 30s antes, genera nuevo QR
- [x] DELETE /api/contacts/pair/qr cancela QR activo
- [x] POST /api/contacts/pair/accept crea contactos bilaterales
- [x] Si B no autenticado: pair.html renderiza login/registro inline
- [x] Tras aceptar: ambos ven ubicaciones mutuamente

## Fuera de alcance

- Multiples QR simultaneos: un codigo por usuario a la vez
- Notificacion de solicitud pendiente: se acepta inmediatamente
