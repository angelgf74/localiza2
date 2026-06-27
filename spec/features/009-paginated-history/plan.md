# 009 - Historial paginado - Plan

## Enfoque

GET /history con before=timestamp como cursor temporal. Cliente acumula arrays. Mapa dibuja polyline incremental.

## Implementacion

1. Backend: GET /location/contacts/ID/history?limit=50&before=ISO
   - WHERE Timestamp < @before
   - ORDER BY Timestamp DESC LIMIT 50
   - Indice (UserId, Timestamp)

2. Android: ContactHistoryBottomSheet
   - Consulta inicial sin before
   - Dibuja polyline
   - Boton "Cargar mas": POST con before=oldest_timestamp
   - Acumula array, redibuja polyline completa

3. Web: index.html historial modal
   - Igual logica que Android
   - Leaflet polyline

## Decisiones

- Cursor temporal vs offset: cursor es staless, funciona si hay updates/deletes
- DESC vs ASC: DESC (mas reciente primero) es natural para scroll-down-to-past
- Limit 50: balance entre redib UI y llamadas API

## Riesgos

- Timestamps coincidentes: DISTINCT ON (UserId) + ORDER BY Timestamp DESC, Id DESC
- Query muy lenta si indice no existe: verificar (UserId, Timestamp) en migracion
