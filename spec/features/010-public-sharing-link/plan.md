# 010 - Comparticion publica - Plan

## Enfoque

Token opaco de 32 bytes (base64). LocationShareLink almacena (Token, UserId, ExpiresAt). GET anónimo devuelve ubicacion actual del usuario. share.html refresca cada 30s.

## Implementacion

1. Backend: POST /location/share
   - Body: expiresInMinutes (5-1440, defecto 60)
   - Genera token = base64(random 32 bytes) sin +/=
   - INSERT LocationShareLink
   - Respuesta: {token, url, expiresAt}

2. Backend: GET /location/share/TOKEN
   - Busca LocationShareLink
   - SELECT ultima UserLocation del UserId
   - Respuesta: {name, lat, lon, accuracy, timestamp, expiresAt}

3. Web: share.html
   - Query string: ?token=...
   - Leaflet map
   - fetch cada 30s
   - Si 404: "Comparticion expirada"

## Decisiones

- Sin autenticacion: link publico, intent=transient
- Token opaco: no revela userid ni email
- 30s refresco: balance latencia-load

## Riesgos

- Alguien comparte URL publica: es intencional, riesgo aceptado
- Token muy corto (5min): puede expirar while-viewing: mensaje OK
