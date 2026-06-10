#!/usr/bin/env bash
set -euo pipefail

GRADLE_FILE="app/build.gradle.kts"
GRADLEW="./gradlew"

# Leer versión actual
VERSION_CODE=$(grep 'versionCode' "$GRADLE_FILE" | grep -oP '\d+')
VERSION_NAME=$(grep 'versionName' "$GRADLE_FILE" | grep -oP '"\K[^"]+')

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

echo "Generando AAB release..."
echo ""

$GRADLEW bundleRelease

AAB_PATH="app/build/outputs/bundle/release/app-release.aab"

echo ""
echo "BUILD OK"
echo "Versión: $NEW_VERSION_NAME (versionCode $NEW_VERSION_CODE)"
echo "AAB:     $(pwd)/$AAB_PATH"
