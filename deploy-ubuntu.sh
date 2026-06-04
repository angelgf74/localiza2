#!/usr/bin/env bash
# deploy-ubuntu.sh — Despliega localiza2api en Ubuntu x86-64 con Nginx + .NET 10
#
# Uso:
#   ./deploy-ubuntu.sh          Actualización normal
#   ./deploy-ubuntu.sh --setup  Primera instalación (configura systemd + genera config nginx)
#
# Requisitos en el servidor (configuración única por un administrador):
#   - Usuario 'webapps' con acceso r/w a /apps/localiza2/
#   - .NET 10 instalado en /usr/bin/dotnet
#   - Nginx instalado
#   - sudo loginctl enable-linger webapps   (para que el servicio arranque con el sistema)
#   - (nginx) ver instrucciones al final de --setup
#
# El fichero appsettings.json SOLO se despliega si no existe aún en el servidor.
# En sucesivas actualizaciones se preserva el del servidor.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
API_SRC_DIR="$SCRIPT_DIR/localiza2api"
PUBLISH_DIR="$SCRIPT_DIR/deploy"

SSH_HOST="agfserver-angel"
REMOTE_API_DIR="/apps/localiza2/api"
REMOTE_APP_DIR="/apps/localiza2/web"
SERVICE_NAME="localiza2api"
KESTREL_PORT="54005"
DOTNET_BIN="/usr/bin/dotnet"
RID="linux-x64"
SETUP_MODE=false

# ── Argumentos ────────────────────────────────────────────────────────────
for arg in "$@"; do
  case "$arg" in
    --setup) SETUP_MODE=true ;;
    *) echo "ERROR: Argumento desconocido: $arg"; echo "Uso: $0 [--setup]"; exit 1 ;;
  esac
done

echo "═══════════════════════════════════════════════════════════"
echo "  localiza2 → deploy en $SSH_HOST"
echo "  API : $REMOTE_API_DIR  (puerto $KESTREL_PORT)"
echo "  APP : $REMOTE_APP_DIR"
if $SETUP_MODE; then
  echo "  Modo: PRIMERA INSTALACIÓN (--setup)"
else
  echo "  Modo: actualización"
fi
echo "═══════════════════════════════════════════════════════════"
echo ""

# ─────────────────────────────────────────────────────────────────────────
# 1. Compilar y publicar localmente
# ─────────────────────────────────────────────────────────────────────────
echo "[1/4] Publicando API para $RID (Release)..."

dotnet publish "$API_SRC_DIR/localiza2api.csproj" \
  --configuration Release \
  --runtime "$RID" \
  --self-contained false \
  --output "$PUBLISH_DIR"

echo "      OK — binarios en $PUBLISH_DIR"

# ─────────────────────────────────────────────────────────────────────────
# 2. Transferir ficheros al servidor
# ─────────────────────────────────────────────────────────────────────────
echo "[2/4] Transfiriendo ficheros al servidor..."

# Determinar si appsettings.json ya existe en el servidor
APPSETTINGS_EXISTS=$(ssh "$SSH_HOST" \
  "[ -f '${REMOTE_API_DIR}/appsettings.json' ] && echo yes || echo no" 2>/dev/null || echo no)

echo "      appsettings.json en servidor: $APPSETTINGS_EXISTS"

# Guardar appsettings.json del servidor antes de limpiar el directorio
if [ "$APPSETTINGS_EXISTS" = "yes" ]; then
  ssh "$SSH_HOST" "cp '${REMOTE_API_DIR}/appsettings.json' /tmp/.localiza2_appsettings.bak"
fi

# Parar el servicio, limpiar el directorio y preparar la transferencia
ssh "$SSH_HOST" "
  systemctl --user stop '${SERVICE_NAME}' 2>/dev/null || true
  mkdir -p '${REMOTE_API_DIR}'
  find '${REMOTE_API_DIR}' -mindepth 1 -delete
"

# Copiar todos los binarios publicados
scp -r "$PUBLISH_DIR/." "$SSH_HOST:${REMOTE_API_DIR}/"

# Restaurar o conservar appsettings.json
if [ "$APPSETTINGS_EXISTS" = "yes" ]; then
  ssh "$SSH_HOST" "
    cp /tmp/.localiza2_appsettings.bak '${REMOTE_API_DIR}/appsettings.json'
    rm /tmp/.localiza2_appsettings.bak
  "
  echo "      appsettings.json del servidor conservado (no sobreescrito)."
else
  echo "      Primera instalación: appsettings.json desplegado del paquete."
fi

echo "      Ficheros copiados."

# ─────────────────────────────────────────────────────────────────────────
# 3a. Primera instalación: configurar systemd + generar config de Nginx
# ─────────────────────────────────────────────────────────────────────────
if $SETUP_MODE; then
  echo "[3/4] Primera instalación — configurando systemd..."

  ssh "$SSH_HOST" bash <<ENDSSH
set -euo pipefail

mkdir -p "\$HOME/.config/systemd/user"

cat > "\$HOME/.config/systemd/user/${SERVICE_NAME}.service" <<'UNIT'
[Unit]
Description=localiza2 API
After=network.target

[Service]
WorkingDirectory=${REMOTE_API_DIR}
ExecStart=${DOTNET_BIN} ${REMOTE_API_DIR}/localiza2api.dll
Restart=always
RestartSec=10
KillSignal=SIGINT
Environment=ASPNETCORE_ENVIRONMENT=Production
Environment=ASPNETCORE_URLS=http://127.0.0.1:${KESTREL_PORT}
SyslogIdentifier=${SERVICE_NAME}

[Install]
WantedBy=default.target
UNIT

systemctl --user daemon-reload
systemctl --user enable "${SERVICE_NAME}"
systemctl --user start  "${SERVICE_NAME}"
echo "  Servicio ${SERVICE_NAME} configurado y arrancado."
ENDSSH

  # Generar configuración de Nginx y subirla al servidor
  echo "      Generando configuración de Nginx..."

  NGINX_CONF_TMP=$(mktemp)
  cat > "$NGINX_CONF_TMP" <<NGINXEOF
# localiza2 — generado por deploy-ubuntu.sh
server {
    listen 80;
    server_name _;

    # Aplicación web estática
    location /localiza2/ {
        alias ${REMOTE_APP_DIR}/;
        index index.html;
        try_files \$uri \$uri/ =404;
    }

    # API REST (.NET Kestrel en localhost:${KESTREL_PORT})
    location /localiza2api/ {
        proxy_pass         http://127.0.0.1:${KESTREL_PORT}/;
        proxy_http_version 1.1;
        proxy_set_header   Upgrade \$http_upgrade;
        proxy_set_header   Connection keep-alive;
        proxy_set_header   Host \$host;
        proxy_set_header   X-Real-IP \$remote_addr;
        proxy_set_header   X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header   X-Forwarded-Proto \$scheme;
        proxy_cache_bypass \$http_upgrade;
        client_max_body_size 10M;
    }
}
NGINXEOF

  scp "$NGINX_CONF_TMP" "$SSH_HOST:/apps/localiza2/localiza2_nginx.conf"
  rm -f "$NGINX_CONF_TMP"

  echo ""
  echo "  ┌─────────────────────────────────────────────────────────────┐"
  echo "  │  ACCIONES MANUALES (ejecutar una sola vez como admin):      │"
  echo "  │                                                              │"
  echo "  │  1. Activar el servicio de usuario en el arranque:          │"
  echo "  │     sudo loginctl enable-linger webapps                     │"
  echo "  │                                                              │"
  echo "  │  2. Instalar y activar la configuración de Nginx:           │"
  echo "  │     sudo cp /apps/localiza2/localiza2_nginx.conf \\          │"
  echo "  │          /etc/nginx/sites-available/localiza2               │"
  echo "  │     sudo ln -sf /etc/nginx/sites-available/localiza2 \\      │"
  echo "  │          /etc/nginx/sites-enabled/localiza2                 │"
  echo "  │     sudo nginx -t && sudo systemctl reload nginx            │"
  echo "  └─────────────────────────────────────────────────────────────┘"
  echo ""

# ─────────────────────────────────────────────────────────────────────────
# 3b. Actualización: reiniciar el servicio existente
# ─────────────────────────────────────────────────────────────────────────
else
  echo "[3/4] Reiniciando servicio $SERVICE_NAME..."
  ssh "$SSH_HOST" "systemctl --user start '${SERVICE_NAME}'"
  echo "      Servicio reiniciado."
fi

# ─────────────────────────────────────────────────────────────────────────
# 4. Health check (espera hasta 60s a que responda la API)
# ─────────────────────────────────────────────────────────────────────────
echo "[4/4] Verificando despliegue (máx. 60s)..."

HTTP_CODE="000"
for i in $(seq 1 12); do
  sleep 5
  HTTP_CODE=$(ssh "$SSH_HOST" \
    "curl -s -o /dev/null -w '%{http_code}' \
      'http://127.0.0.1:${KESTREL_PORT}/api/auth/login' \
      -X POST -H 'Content-Type: application/json' \
      -d '{\"email\":\"test@test.com\",\"password\":\"test\"}' \
      --connect-timeout 5 2>/dev/null || echo 000") || true
  [ "$HTTP_CODE" != "000" ] && [ "$HTTP_CODE" != "502" ] && break
  echo "      Esperando respuesta... ($((i*5))s)"
done

echo ""
if [ "$HTTP_CODE" = "401" ] || [ "$HTTP_CODE" = "400" ] || [ "$HTTP_CODE" = "200" ]; then
  echo "  OK — API respondiendo (HTTP $HTTP_CODE)"
  echo ""
  echo "  URL interna : http://127.0.0.1:$KESTREL_PORT"
  echo "  URL pública : https://angelgf.com.es/localiza2api"
  echo ""
  echo "Despliegue completado."
else
  echo "  ERROR — La API devolvió HTTP $HTTP_CODE"
  echo ""
  echo "  Revisar logs:"
  echo "    ssh $SSH_HOST 'journalctl --user -u $SERVICE_NAME -n 50 --no-pager'"
  exit 1
fi
