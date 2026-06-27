# 010 - Enlace de ubicacion compartida (anonimo)

Estado: implementado OK

## Que hace

Usuario genera un link temporal (5min-24h) con su ubicacion actual. Puede compartirlo por WhatsApp/email sin crear contacto. Cualquiera abre el link (share.html?token=...) y ve ubicacion en mapa, sin iniciar sesion. Cada refresh muestra ubicacion actual. Countdown indica cuanto tiempo quedan.

## Por que

Casos puntuales: "dame mi ubicacion para venir a buscarme" (5min), "aqui duermosesta noche" (12h). Sin sobrecargar el modelo de contactos.

## Criterios de aceptacion

- [x] POST /location/share genera LocationShareLink (token, 5min-24h)
- [x] Token es URL-safe (32 bytes base64)
- [x] GET /location/share/TOKEN devuelve ultima ubicacion sin auth
- [x] share.html renderiza mapa
- [x] Refresca cada 30s automaticamente
- [x] Countdown visible
- [x] Token expirado: mensaje "Comparticion expirada"
- [x] DELETE /location/share/TOKEN revoca link

## Fuera de alcance

- Multiples links simultaneos
- Analytics de cuantas veces se vio
