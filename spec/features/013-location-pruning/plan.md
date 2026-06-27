# 013 - Poda de ubicaciones - Plan

## Enfoque

Servicio background cada hora ejecuta SQL native con DELETE + DISTINCT ON para seleccionar puntos a mantener.

## Implementacion

1. PruneLocationsService (Background Service, cada hora)
   - Query nativa PostgreSQL con DISTINCT ON
   - Ultimas 3h: select todos (no delete)
   - 3h-24h: DISTINCT ON cada 5min
   - 1-30d: DISTINCT ON cada 30min
   - >30d: DELETE todos

2. SQL:
   DELETE FROM UserLocations WHERE UserId IN (select ids) AND Timestamp < NOW() - INTERVAL '30 days'
   DELETE FROM UserLocations WHERE ... AND Timestamp BETWEEN 1d AND 30d AND (SELECT ... DISTINCT ON per 30min)

## Decisiones

- Nativa SQL vs EF Core: nativa porque DISTINCT ON es PostgreSQL-specific
- Cada hora vs cada dia: cada hora mantiene datos frescos, costo minimal
- Politica fija vs configurable: fija, simplifica, backlog puede hacer configurable

## Riesgos

- Transacciones largas: puede lock tabla durante minutos en BD grande
- Mitigacion: ejecutar fuera de peak hours, usar low-priority queries
