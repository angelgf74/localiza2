# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Rules

Detailed per-area rules live in `.claude/rules/` and are imported here:

@.claude/rules/secrets.md
@.claude/rules/git.md
@.claude/rules/android.md
@.claude/rules/api.md
@.claude/rules/database.md
@.claude/rules/web.md
@.claude/rules/deploy.md

## Project Overview

**Localiza2** is a real-time location-sharing app with three components:
- **`localiza2/`** — Android app (Kotlin, Gradle)
- **`localiza2api/`** — REST API (.NET 10, C#, PostgreSQL)
- **`localiza2web/`** — Web dashboard (vanilla HTML/CSS/JS, no build step)

## Build & Run Commands

### Android App
```bash
cd localiza2
./gradlew assembleDebug          # Debug APK
./gradlew assembleRelease        # Release APK (requires keystore.properties)
./build_aab.sh                   # Release AAB — also increments versionCode/versionName
```
**Always increment `versionCode` and `versionName` in `app/build.gradle.kts` when making changes.** `build_aab.sh` does this automatically for AAB builds.

### .NET API
```bash
cd localiza2api
dotnet build                     # Build
dotnet run                       # Run locally (port 5000 or from launchSettings)
dotnet publish -c Release -r linux-x64 --self-contained false -o ../deploy
```

### Deployment
```bash
./deploy-ubuntu.sh               # Deploy API to Ubuntu server
./deploy-ubuntu.sh --setup       # First-time setup (creates systemd service)
./deploy-web.sh                  # Deploy web frontend
./_deploy.sh linux-arm64         # Deploy to Raspberry Pi (64-bit)
```

## Architecture

### Data Flow
```
Android App (Kotlin)
  └─ JWT auth + GPS updates → .NET API (port 54005, proxied by Nginx)
                                └─ PostgreSQL DB
Web Dashboard (HTML/JS)
  └─ JWT auth + REST calls  → .NET API
```

### Location Retention Policy
`PruneLocationsService` (a `BackgroundService`) prunes GPS history hourly — not on insert:
- Last 3h: 1 record/min (max 180)
- 3–24h: 1 record/5min (max 252)
- 1–30 days: 1 record/30min (max 1440)
- >30 days: purged

### Authentication
JWT tokens are issued on login and sent in the `Authorization: Bearer` header. Tokens are NOT stored in the database—only client-side (Android DataStore / web localStorage).

### Contact Pairing
Contacts pair via email invite or QR code. QR pairing tokens expire in 15 minutes. Both users must confirm to establish a mutual contact relationship before locations are shared.

## Key Files

| File | Purpose |
|------|---------|
| `localiza2/app/build.gradle.kts` | Android versions, SDK levels, signing config, dependencies |
| `localiza2api/Program.cs` | DI setup, JWT config, CORS, EF Core, OpenAPI |
| `localiza2api/Data/AppDbContext.cs` | EF Core schema, indexes, relationships |
| `localiza2api/Controllers/` | Auth, Contacts, Location endpoints |
| `localiza2api/Services/` | EmailService (Brevo), TokenService (JWT) |
| `localiza2api/appsettings.json` | **Not in git** — DB connection, JWT secret, email API key |
| `localiza2/keystore.properties` | **Not in git** — Android signing credentials |

## Database Migrations (.NET EF Core)

```bash
cd localiza2api
dotnet ef migrations add <MigrationName>
dotnet ef database update
```

## Production URLs

- API: `https://localiza2-api.angelgf.com.es/`
- Web app: `https://localiza2-app.angelgf.com.es/`
- API docs (Scalar): `https://localiza2-api.angelgf.com.es/scalar`

## Secrets (never commit)

- `localiza2api/appsettings.json` — JWT secret, PostgreSQL connection string, Brevo email API key
- `localiza2/keystore.properties` — Android signing keystore path and passwords
- `localiza2/keystore/localiza2-release.jks` — Android keystore binary
- `info.txt` — credentials reference file at repo root
