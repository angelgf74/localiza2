# 003 - Emparejamiento por QR - Plan

## Enfoque

Backend genera token de 24h con expiracion explícita.
Frontend (Android + web) renderiza QR con qrcode lib.
Countdown + renovacion automatica en cliente.
Aceptacion crea relacion bilateral atomica en DB.

## Implementacion

1. Backend (POST /contacts/pair/qr)
   - Genera PairingCode (20 chars, aleatorio, unico)
   - Actualiza User.PairingCode + PairingCodeExpiry (NOW + 24h)
   - Respuesta: {token, inviterName, expiresAt}

2. Android (PairContactBottomSheet)
   - Tab 1: POST /pair/qr
   - ZXing.encode(QR con URL)
   - Countdown: calcular diferencia expiresAt - now
   - Cada segundo: actualizar UI
   - 30s antes de expirar: POST /pair/qr (renovar)

3. Web (pair.html)
   - qrcodejs.makeCode(URL)
   - Countdown similar
   - Renovacion similar

4. Backend (POST /contacts/pair/accept)
   - Busca User por PairingCode
   - Valida expiracion
   - CreateBilateralContactAsync(A, B)
   - Elimina PairingCode
   - Respuesta: {message, contactName}

## Decisiones

- Renovacion en cliente vs servidor: cliente, para UX responsive
- Timeout 24h vs otro: 24h equilibra seguridad + practicidad (alguien puede volver a escanear al dia siguiente)
- Unico QR por usuario: evita confusión, simplifica UI

## Riesgos

- Red cae durante countdown: QR sigue valido, se renueva al reconectar
- Usuario no acepta a tiempo: token expira, debe pedir nuevo QR
- Usuario escanea QR multiple veces: POST /pair/accept es idempotente (actualiza, no duplica)
