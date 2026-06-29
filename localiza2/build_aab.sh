#!/usr/bin/env bash
set -euo pipefail

GRADLE_FILE="app/build.gradle.kts"
GRADLEW="./gradlew"

# Leer versión actual (sin grep -P: no es portable y falla con algunos locales)
VERSION_CODE=$(grep 'versionCode' "$GRADLE_FILE" | grep -oE '[0-9]+')
VERSION_NAME=$(grep 'versionName' "$GRADLE_FILE" | sed -E 's/.*"([^"]+)".*/\1/')

# Calcular nueva versión
NEW_VERSION_CODE=$((VERSION_CODE + 1))
IFS='.' read -r MAJOR MINOR <<< "$VERSION_NAME"
NEW_VERSION_NAME="$MAJOR.$((MINOR + 1))"

echo "Versión actual:  versionCode=$VERSION_CODE  versionName=$VERSION_NAME"
echo "Versión nueva:   versionCode=$NEW_VERSION_CODE  versionName=$NEW_VERSION_NAME"
echo ""

# Actualizar build.gradle.kts
sed -i "s/versionCode = $VERSION_CODE/versionCode = $NEW_VERSION_CODE/" "$GRADLE_FILE"
sed -i "s/versionName = \"$VERSION_NAME\"/versionName = \"$NEW_VERSION_NAME\"/" "$GRADLE_FILE"

echo "Generando AAB production release..."
echo ""

$GRADLEW bundleProductionRelease

AAB_PATH="app/build/outputs/bundle/productionRelease/app-production-release.aab"

if [ ! -f "$AAB_PATH" ]; then
  echo "ERROR: no se generó el AAB en $AAB_PATH"
  exit 1
fi

echo ""
echo "BUILD OK"
echo "Versión: $NEW_VERSION_NAME (versionCode $NEW_VERSION_CODE)"
echo "AAB:     $(pwd)/$AAB_PATH"
