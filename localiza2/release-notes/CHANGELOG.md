# Changelog

## 1.21 (versionCode 22)

### Novedades para el usuario
- **Historial de ruta con tiempo por punto**: al tocar cualquier punto del recorrido se muestra un bocadillo con cuánto hace que el contacto estuvo allí («hace 5 min»). El punto más antiguo se marca en verde y el más reciente en azul.
- **Lista de contactos unificada con la web**: ahora muestra el tiempo desde la última actualización de ubicación, el nivel de batería con código de color, y botones de acceso directo a historial, edición y eliminación.
- **Cerrar sesión**: nueva opción en el menú de la barra superior.
- **Interfaz más clara**: tema oscuro unificado, tipografía y colores consistentes en todas las pantallas, y diálogos de permisos/batería con mejor contraste y legibilidad.

### Cambios técnicos
- Se desactiva el InfoWindow por defecto de la polyline del historial (causaba un bocadillo vacío al tocar la línea).
- CORS en desarrollo acepta cualquier puerto de localhost.
- Diálogos migrados a `MaterialAlertDialogBuilder` para respetar el tema oscuro.
