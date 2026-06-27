# 003 - Emparejamiento por QR - Tareas

- [x] Backend: POST /contacts/pair/qr genera PairingCode (20 chars, 24h)
- [x] Backend: POST /contacts/pair/accept valida y crea contactos bilaterales
- [x] Backend: DELETE /contacts/pair/qr cancela QR activo
- [x] Android: integrar ZXing para generacion de QR
- [x] Android: PairContactBottomSheet con tab QR
- [x] Android: Countdown hacia expiracion
- [x] Android: Renovacion automatica 30s antes
- [x] Web: integrar qrcodejs
- [x] Web: pair.html con QR
- [x] Web: countdown y renovacion
- [x] Web: pair.js renderiza login/registro inline si no autenticado
- [x] Web: POST /pair/accept tras aceptar QR
- [x] Backend: GET /contacts/pair/info/TOKEN devuelve metadata (nombre, tipo)
- [x] Prueba: escanear QR con dos dispositivos
- [x] Prueba: countdown y renovacion visual
- [x] Prueba: QR expira correctamente

## Mantenimiento (recurrente)

- [ ] Revisar logs si usuarios reportan QR no escaneable
- [ ] Ajustar timeout si se quejan de expiración prematura
