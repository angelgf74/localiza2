# 013 - Poda automatica de ubicaciones

Estado: implementado OK

## Que hace

Cada hora, servicio PruneLocationsService comprime historial segun edad:
- Ultimas 3h: 1 punto/minuto (max 180)
- 3h-24h: 1 punto/5 minutos (max 252)
- 1-30 dias: 1 punto/30 minutos (max 1440)
- Mayor de 30d: eliminacion total

Comprime sin perder datos recientes. Historial de hace 3 semanas mostrara viajes principales, no cada paso.

## Por que

Ahorra espacio BD. Sin poda, 1 punto/min = 1440/dia * 30 dias = 43k puntos/usuario/mes. Con poda: ~1700 puntos/mes. 25x mas eficiente.

## Criterios de aceptacion

- [x] PruneLocationsService ejecuta cada hora
- [x] SQL DELETE comprime por rango temporal y densidad
- [x] Ultimas 3h: sin compresion
- [x] 3h-24h: 1/5min
- [x] 1-30d: 1/30min
- [x] >30d: elimina todo
- [x] Sin impacto en queries concurrentes (indices)

## Fuera de alcance

- Compresion adaptativa por usuario (backlog)
- Configuracion de politica de usuario (backlog)
