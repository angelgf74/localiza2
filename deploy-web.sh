#!/usr/bin/env bash
# deploy-web.sh — Publica la web app (localiza2web) en el servidor Ubuntu
#
# Uso:
#   ./deploy-web.sh
#
# Copia los ficheros estáticos de localiza2web/ a /apps/localiza2/web en el
# servidor. No requiere reiniciar ningún servicio — nginx sirve los ficheros
# directamente.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
WEB_SRC_DIR="$SCRIPT_DIR/localiza2web"

SSH_HOST="agfserver-angel"
REMOTE_WEB_DIR="/apps/localiza2/web"

echo "═══════════════════════════════════════════════════════════"
echo "  localiza2 web → deploy en $SSH_HOST"
echo "  Origen : $WEB_SRC_DIR"
echo "  Destino: $REMOTE_WEB_DIR"
echo "═══════════════════════════════════════════════════════════"
echo ""

# ── 1. Verificar que los ficheros fuente existen ───────────────────────────
if [ ! -f "$WEB_SRC_DIR/index.html" ]; then
  echo "ERROR: no se encuentra $WEB_SRC_DIR/index.html"
  exit 1
fi

# ── 2. Crear directorio en el servidor si no existe ───────────────────────
echo "[1/2] Preparando directorio remoto..."
ssh "$SSH_HOST" "mkdir -p '$REMOTE_WEB_DIR'"

# ── 3. Copiar ficheros ─────────────────────────────────────────────────────
echo "[2/2] Transfiriendo ficheros..."
scp -r "$WEB_SRC_DIR/." "$SSH_HOST:$REMOTE_WEB_DIR/"

echo ""
echo "  OK — Web publicada en $REMOTE_WEB_DIR"
echo "  URL: https://angelgf.com.es/localiza2/"
echo ""
echo "Despliegue completado."
