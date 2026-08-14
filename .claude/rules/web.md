# Regla: dashboard web (`localiza2web/`)

HTML/CSS/JS *vanilla*. **Sin paso de build, sin bundler, sin `node_modules`.** Los ficheros se sirven tal cual desde el servidor.

No introduzcas npm, TypeScript, frameworks ni un pipeline de build sin pedirlo antes al usuario: `deploy-web.sh` copia estáticos y nada más, así que un build rompería el deploy.

## Ficheros

| Fichero | Rol |
|---|---|
| `index.html` + `app.js` | Dashboard principal (mapa, contactos) |
| `admin.html` + `admin.js` | Panel de administración, solo `SuperAdmin` |
| `pair.html` + `pair.js` | Emparejamiento por QR / invitación |
| `landing.html`, `privacy.html`, `confirm.html`, `share.html` | Páginas estáticas |
| `style.css` | Estilos compartidos |

## `API_BASE`

Cada script declara su base. Patrón en `app.js` y `admin.js`:

```js
const API_BASE = isDev ? 'http://localhost:5135' : 'https://localiza2-api.angelgf.com.es';
```

Si cambias la URL de la API, cámbiala en **los tres** ficheros JS (`app.js`, `admin.js`, `pair.js` — este último está fijado a producción sin condicional). Es fácil dejar uno atrás.

## Token JWT

Vive en `localStorage`, se envía en `Authorization: Bearer`. No se guarda en la base de datos. No lo escribas en cookies ni lo pases por query string — la URL acaba en logs de Nginx y en el historial del navegador.

## CORS

El origen del dashboard tiene que estar en `App:WebUrl` de la configuración de la API. Un fallo de CORS en producción normalmente significa que esa clave está mal en el `appsettings.json` del servidor, no que haya que relajar la política CORS del código.
