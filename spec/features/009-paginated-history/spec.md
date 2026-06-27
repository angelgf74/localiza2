# 009 - Historial de ruta con paginacion

Estado: implementado OK

## Que hace

Usuario ve historial completo de donde estuvo cada contacto (o el mismo). GET /history?limit=50&before=ISO8601 devuelve 50 puntos anteriores a esa fecha. En mapa: polyline conecta puntos, punto inicial pequeno, punto final resaltado. Boton "Cargar mas" ejecuta nueva query con timestamp del ultimo punto. Scroll infinito hacia el pasado.

## Por que

Ubicacion actual no basta para saber ruta. Historial con paginacion permite:
- Rastrear donde estuvo alguien durante el dia
- Reconstruir trayecto de viaje
- Econom de datos: no carga 30 dias de puntos de una vez (carga 50/pagina)

## Criterios de aceptacion

- [x] GET /location/contacts/ID/history devuelve array ordenado DESC
- [x] Query params: limit=50, before=ISO8601
- [x] Android: ContactHistoryBottomSheet con mapa embebido
- [x] Android: polyline conecta puntos
- [x] Android: boton "Cargar mas" pagination
- [x] Web: index.html Click contacto carga historial
- [x] Web: mapa embebido con polyline
- [x] Web: boton "Cargar mas"
- [x] Indice DB: (UserId, Timestamp) optimiza queries

## Fuera de alcance

- Exportar historial a CSV
- Replay de movimiento animado
- Calorimetria de distancia (feature backlog)
