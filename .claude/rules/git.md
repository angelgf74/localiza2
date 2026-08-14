# Regla: git

## Repositorio local

Rama por defecto: `master`. **No hay `origin` configurado** — `git remote -v` está vacío. Si el usuario pide un push, dilo y pregunta por la URL del remoto; no inventes uno ni lo añadas por tu cuenta.

## Commits

Commitea o pushea **solo cuando el usuario lo pida**.

Formato usado en el historial: `tipo(ámbito): descripción en español`.

- Tipos vistos: `feat`, `fix`, `style`, `release`.
- Ámbitos: `api`, `web`, `android` (o combinados: `feat(api,web):`). El ámbito es opcional.
- Descripción en español, minúscula inicial, imperativo o sustantivo.
- Los commits de release siguen el patrón `release: AAB v1.24 para Play Store + …`.

Ejemplos reales:

```
feat(api,web): roles de usuario + panel admin, seeders demo/superadmin
fix(api): rate limiting por-IP real tras Nginx y protección cuenta demo
release: AAB v1.24 para Play Store + SDK 37
```

## Antes de commitear

Pasa siempre por la comprobación de secretos (`/secrets-check`, reglas en `secrets.md`). Es la comprobación más importante de este repositorio: contiene keystore de firma, claves JWT y credenciales de base de datos de producción.

Si el commit toca el módulo Android, verifica que `versionCode`/`versionName` se han incrementado (`android.md`).

## Operaciones destructivas

`git reset --hard`, `git push --force`, `git checkout --`, `git clean`: pregunta antes. No hay remoto del que recuperar nada.
