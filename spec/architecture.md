# Arquitectura de Localiza2

## Visión general del sistema

```
┌─────────────────────────────────────────────────────────────────┐
│                      LOCALIZA2: Real-time Location Sharing       │
└─────────────────────────────────────────────────────────────────┘

┌──────────────────┐         ┌──────────────────┐      ┌──────────┐
│  Android App     │         │   Web Dashboard  │      │  Public  │
│  (Kotlin)        │         │  (HTML/CSS/JS)   │      │ Location │
│  • LocationSvc   │         │  • Leaflet Map   │      │  Viewer  │
│  • JWT Token     │         │  • JWT Token     │      │ (No Auth)│
│  • Room Offline  │         │  • SessionStore  │      │          │
└────────┬─────────┘         └────────┬─────────┘      └──────┬───┘
         │                            │                       │
         │  HTTPS                     │  HTTPS                │
         │  Bearer JWT                │  Bearer JWT           │  Share Token
         │                            │                       │
         └────────────┬───────────────┴───────────┬───────────┘
                      │                           │
                      ▼                           ▼
         ┌──────────────────────────────────────────┐
         │      .NET 10 REST API                    │
         │  (localiza2-api.angelgf.com.es)          │
         │  • AuthController                        │
         │  • ContactsController                    │
         │  • LocationController                    │
         │  • Rate Limiting: 10/min/IP              │
         │  • CORS: Dynamic origin                  │
         └────────────────┬─────────────────────────┘
                          │
                          │ EF Core
                          │ Npgsql
                          ▼
         ┌──────────────────────────────────────────┐
         │   PostgreSQL Database                    │
         │   (192.168.0.185:5432 / localiza2)       │
         │                                          │
         │  Tables:                                 │
         │  • Users (9 cols)                        │
         │  • PendingRegistrations (4 cols)         │
         │  • Contacts (8 cols)                     │
         │  • ContactInvitations (4 cols)           │
         │  • UserLocations (7 cols)                │
         │  • LocationShareLinks (4 cols)           │
         └──────────────────────────────────────────┘
```

## Tecnologías por componente

| Capa | Tecnología | Versión |
|------|-----------|---------|
| **Android** | Kotlin | 2.2.10 |
| | Gradle | 9.2.1 |
| | Retrofit | 2.11.0 |
| | Room | 2.7.1 |
| | Play Services Location | 21.3.0 |
| | osmdroid | 6.1.20 |
| **API** | .NET | 10 |
| | EF Core | latest |
| | PostgreSQL (Npgsql) | 8.0.x |
| **Web** | Vanilla JS | ES2020 |
| | Leaflet | latest |

## URLs de producción

- **API**: `https://localiza2-api.angelgf.com.es/`
- **Web**: `https://localiza2-app.angelgf.com.es/`
- **API Docs**: `https://localiza2-api.angelgf.com.es/scalar`

## Política de retención de ubicaciones

Ejecución automática cada hora:

| Período | Resolución | Máximo |
|---------|-----------|--------|
| Últimas 3h | 1 pt/min | 180 |
| 3h–24h | 1 pt/5min | 252 |
| 1–30 días | 1 pt/30min | 1440 |
| > 30 días | Eliminación | 0 |
