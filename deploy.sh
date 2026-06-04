#!/usr/bin/env bash
# Publica localiza2api en Raspberry Pi con Ubuntu + Nginx
# Uso: ./deploy.sh [arm64|arm]
# arm64 = Raspberry Pi 4/5 con Ubuntu 64-bit (por defecto)
# arm   = Raspberry Pi 2/3 con Ubuntu 32-bit

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
API_DIR="$SCRIPT_DIR/localiza2api"
PUBLISH_DIR="$SCRIPT_DIR/deploy"

PI_HOST="192.168.0.175"
PI_USER="angel"
PI_APP_DIR="/home/angel/localiza2api"
SERVICE_NAME="localiza2api"
KESTREL_PORT="55003"
NGINX_LOCATION="/localiza2api"
RID="${1:-linux-arm64}"

echo "==> Arquitectura: $RID"
echo "==> Destino: $PI_USER@$PI_HOST:$PI_APP_DIR"
echo ""
echo 
# ── 1. Compilar y publicar localmente ──────────────────────────────────────
echo "[1/4] Publicando la API para $RID..."

dotnet publish "$API_DIR/localiza2api.csproj" \
  --configuration Release \
  --runtime "$RID" \
  --self-contained false \
  --output "$PUBLISH_DIR"

echo "      Publicación completada en $PUBLISH_DIR"

# ── 2. Copiar ficheros a la Raspberry Pi ──────────────────────────────────
echo "[2/4] Copiando ficheros al servidor..."

REMOTE_APPSETTINGS="$PI_APP_DIR/appsettings.json"
REMOTE_APPSETTINGS_BAK="/tmp/appsettings.json.deploy.bak"

# Guardar appsettings.json del servidor si existe
ssh "$PI_USER@$PI_HOST" "
  if [ -f $REMOTE_APPSETTINGS ]; then
    cp $REMOTE_APPSETTINGS $REMOTE_APPSETTINGS_BAK
    echo '  appsettings.json del servidor guardado.'
  fi
  rm -rf $PI_APP_DIR && mkdir -p $PI_APP_DIR
"

scp -r "$PUBLISH_DIR/." "$PI_USER@$PI_HOST:$PI_APP_DIR/"

# Restaurar appsettings.json del servidor si había uno previo
ssh "$PI_USER@$PI_HOST" "
  if [ -f $REMOTE_APPSETTINGS_BAK ]; then
    cp $REMOTE_APPSETTINGS_BAK $REMOTE_APPSETTINGS
    rm $REMOTE_APPSETTINGS_BAK
    echo '  appsettings.json del servidor restaurado.'
  else
    echo '  Sin appsettings.json previo: se usa el de desarrollo.'
  fi
"

echo "      Ficheros copiados."

# ── 3. Configurar systemd y Nginx en la Pi ────────────────────────────────
echo "[3/4] Configurando servidor..."

ssh "$PI_USER@$PI_HOST" bash <<ENDSSH
set -euo pipefail

# ── Instalar .NET runtime si no está ──────────────────────────────
if ! command -v dotnet &>/dev/null; then
  echo "  Instalando .NET runtime..."
  wget -q https://dot.net/v1/dotnet-install.sh -O /tmp/dotnet-install.sh
  chmod +x /tmp/dotnet-install.sh
  /tmp/dotnet-install.sh --channel 10.0 --runtime dotnet --install-dir /home/angel/.dotnet
  echo 'export DOTNET_ROOT=\$HOME/.dotnet' >> /home/angel/.bashrc
  echo 'export PATH=\$PATH:\$HOME/.dotnet' >> /home/angel/.bashrc
fi
DOTNET_BIN=\$(command -v dotnet 2>/dev/null || echo "/home/angel/.dotnet/dotnet")

# ── Instalar Nginx si no está ─────────────────────────────────────
if ! command -v nginx &>/dev/null; then
  echo "  Instalando Nginx..."
  sudo apt-get update -q
  sudo apt-get install -y -q nginx
fi

# ── Crear servicio systemd ────────────────────────────────────────
echo "  Creando servicio systemd: $SERVICE_NAME"
sudo tee /etc/systemd/system/$SERVICE_NAME.service > /dev/null <<EOF
[Unit]
Description=localiza2 API
After=network.target postgresql.service

[Service]
WorkingDirectory=$PI_APP_DIR
ExecStart=\$DOTNET_BIN $PI_APP_DIR/localiza2api.dll
Restart=always
RestartSec=10
KillSignal=SIGINT
User=angel
Environment=ASPNETCORE_ENVIRONMENT=Production
Environment=ASPNETCORE_URLS=http://127.0.0.1:$KESTREL_PORT
Environment=DOTNET_ROOT=/home/angel/.dotnet
SyslogIdentifier=$SERVICE_NAME

[Install]
WantedBy=multi-user.target
EOF

# ── Configurar Nginx ──────────────────────────────────────────────
echo "  Configurando Nginx..."
sudo tee /etc/nginx/sites-available/$SERVICE_NAME > /dev/null <<'EOF'
server {
    listen 80;
    server_name _;

    location /localiza2api/ {
        proxy_pass         http://127.0.0.1:$KESTREL_PORT/;
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
EOF

# Activar el sitio si no lo está
if [ ! -L /etc/nginx/sites-enabled/$SERVICE_NAME ]; then
  sudo ln -sf /etc/nginx/sites-available/$SERVICE_NAME /etc/nginx/sites-enabled/$SERVICE_NAME
fi

# Quitar el sitio por defecto de Nginx si sigue activo
if [ -L /etc/nginx/sites-enabled/default ]; then
  sudo rm /etc/nginx/sites-enabled/default
fi

# ── Instalar PostgreSQL si no está ───────────────────────────────
if ! command -v psql &>/dev/null; then
  echo "  Instalando PostgreSQL..."
  sudo apt-get install -y -q postgresql postgresql-contrib
  sudo systemctl enable postgresql
  sudo systemctl start postgresql
fi

# ── Crear base de datos si no existe ─────────────────────────────
DB_EXISTS=\$(sudo -u postgres psql -tAc "SELECT 1 FROM pg_database WHERE datname='localiza2'" 2>/dev/null || echo "")
if [ -z "\$DB_EXISTS" ]; then
  echo "  Creando base de datos localiza2..."
  sudo -u postgres psql -c "CREATE DATABASE localiza2;"
  sudo -u postgres psql -c "CREATE USER postgres WITH SUPERUSER PASSWORD '2delfi.nes';" 2>/dev/null || true
fi

# ── Verificar configuración de Nginx ─────────────────────────────
sudo nginx -t

# ── Activar y reiniciar servicios ────────────────────────────────
echo "  Reiniciando servicios..."
sudo systemctl daemon-reload
sudo systemctl enable $SERVICE_NAME
sudo systemctl restart $SERVICE_NAME
sudo systemctl reload nginx

echo ""
echo "  Estado del servicio:"
sudo systemctl status $SERVICE_NAME --no-pager -l || true
ENDSSH

# ── 4. Verificar que responde ──────────────────────────────────────────────
echo "[4/4] Verificando despliegue (espera hasta 60s)..."
HTTP_CODE="000"
for i in $(seq 1 12); do
  sleep 5
  HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
    "http://$PI_HOST/localiza2api/api/auth/login" \
    -X POST -H "Content-Type: application/json" \
    -d '{"email":"test@test.com","password":"test"}' \
    --connect-timeout 5 || echo "000")
  [ "$HTTP_CODE" != "000" ] && [ "$HTTP_CODE" != "502" ] && break
  echo "      Esperando... ($((i*5))s)"
done

if [ "$HTTP_CODE" = "401" ] || [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "400" ]; then
  echo ""
  echo "  API respondiendo correctamente (HTTP $HTTP_CODE)"
  echo ""
  echo "  URL local:  http://$PI_HOST/localiza2api"
  echo "  URL pública: https://angelgf.com.es/localiza2api"
  echo ""
  echo "Despliegue completado."
else
  echo ""
  echo "  AVISO: La API devolvió HTTP $HTTP_CODE"
  echo "  Revisa los logs con:"
  echo "    ssh $PI_USER@$PI_HOST 'journalctl -u $SERVICE_NAME -n 50 --no-pager'"
  exit 1
fi
