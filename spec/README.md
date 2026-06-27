# Especificaciones de Localiza2

Documentación técnica completa de **Localiza2**: plataforma de compartición de ubicación en tiempo real.

## Índice de documentos

- **[architecture.md](architecture.md)** — Visión general del sistema, componentes, flujo de datos y tecnologías
- **[database.md](database.md)** — Esquema PostgreSQL, tablas, relaciones e historial de migraciones
- **[api.md](api.md)** — Referencia REST: endpoints, autenticación, parámetros y respuestas
- **[android.md](android.md)** — Especificaciones de la app Android: build, permisos, servicios y UI
- **[web.md](web.md)** — Especificaciones del frontend web: páginas, mapa y lógica de cliente
- **[auth.md](auth.md)** — Flujos de autenticación: registro, login, recuperación y emparejamiento
- **[features.md](features.md)** — Funcionalidades clave: historial, modo invisible, offline, etc.

## Inicio rápido

### Componentes
1. **Android** (`localiza2/`) — App Kotlin con Gradle
2. **API** (`localiza2api/`) — .NET 10 con PostgreSQL
3. **Web** (`localiza2web/`) — HTML/CSS/JS puro

### URLs de producción
- API: `https://localiza2-api.angelgf.com.es/`
- Web: `https://localiza2-app.angelgf.com.es/`

### Versión actual
- Android: v1.18 (versionCode 19)
- API: .NET 10
- Web: Vanilla HTML/CSS/JS

## Cambios recientes

| Commit | Descripción |
|--------|-------------|
| f67ac67 | Historial paginado + enlace de ubicación temporal |
| defc74b | Optimización: DISTINCT ON en lugar de GroupBy |
| 51ac101 | Modo invisible, QR countdown, alerta de batería |
