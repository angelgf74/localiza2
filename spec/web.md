# Web Frontend: Especificaciones

## Páginas principales

### index.html + app.js - SPA Principal
- Mapa Leaflet con contactos
- Sidebar: lista de contactos con distancia, batería, estado
- Navbar: ayuda, sugerencias, compartir ubicación, toggle sharing
- Refresco automático: cada 30 segundos
- Pausa en background: visibilitychange event
- Zoom automático al entrar

### pair.html + pair.js - Emparejamiento y Reset
- Parámetro ?token= → Aceptar invitación QR/email
- Parámetro ?reset= → Reset de contraseña
- Login/registro inline si no autenticado

### confirm.html - Confirmación de Email
- Parámetro ?status=ok|expired|invalid
- Deep link: localiza2://login (abre app nativa)

### share.html - Visor Público
- Parámetro ?token= → Ubicación compartida
- Refresco cada 30s automáticos
- Sin autenticación requerida

## Mapa Leaflet + OpenStreetMap

### Marcadores
- Propio: azul con pulso
- Contactos: 8 colores rotando con inicial
- Frescura: verde (<5min) / amarillo (5-60min) / gris (>60min)

### Modos
- Normal: muestra todos los contactos
- Historial: polyline + marcadores intermedios + punto final
- Filtrado: chips para mostrar/ocultar contactos

## Autenticación
- Token: sessionStorage (lz2_token)
- Bearer: header Authorization: Bearer <token>
- 401: Logout automático

## Endpoints

Contactos, ubicaciones, emparejamiento, historial, compartición pública
(Ver spec/api.md para detalles completos)

## Performance
- Refresco: 30s (pausa en background)
- Historial: 50 puntos por página
- Zoom automático: muestra todos los contactos
