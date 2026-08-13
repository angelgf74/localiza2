# install-usb.ps1 — Compila e instala localiza2 en dispositivo Android via USB
# Uso: .\install-usb.ps1

param(
    [switch]$Release = $false,
    [switch]$Clean = $false
)

$APP_DIR = Split-Path -Parent $MyInvocation.MyCommand.Path
$PACKAGE = "es.angelgf.localiza2"
$ACTIVITY = "$PACKAGE.ui.auth.AuthActivity"

Write-Host "════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "  localiza2 - Install via USB" -ForegroundColor Cyan
Write-Host "════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""

# Verificar que adb esté disponible
Write-Host "[1/4] Verificando adb..."
try {
    $adbVersion = adb version 2>&1 | Select-Object -First 1
    Write-Host "      $adbVersion" -ForegroundColor Green
} catch {
    Write-Host "ERROR: adb no encontrado en PATH" -ForegroundColor Red
    Write-Host "Instala Android SDK Platform-Tools y reinicia la terminal." -ForegroundColor Red
    exit 1
}

# Listar dispositivos conectados
Write-Host ""
Write-Host "[2/4] Dispositivos conectados:"
$devices = adb devices | Select-Object -Skip 1 | Where-Object { $_ -match '\s+(device|emulator)' }
if (-not $devices) {
    Write-Host "ERROR: No hay dispositivos Android conectados." -ForegroundColor Red
    Write-Host "Conecta un dispositivo via USB con depuración habilitada." -ForegroundColor Yellow
    exit 1
}
$devices | ForEach-Object { Write-Host "      $_" -ForegroundColor Green }

# Limpiar si se pide
if ($Clean) {
    Write-Host ""
    Write-Host "[3/4] Limpiando compilación anterior..."
    & "$APP_DIR\gradlew.bat" clean --quiet
}

# Compilar APK
$buildType = if ($Release) { "Production" } else { "Development" }
Write-Host ""
Write-Host "[3/4] Compilando APK ($buildType)..."
$flavor = if ($Release) { "Production" } else { "Development" }
$task = if ($Release) { "assembleProductionRelease" } else { "assembleDevelopmentDebug" }

& "$APP_DIR\gradlew.bat" $task --quiet 2>$null
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Compilación fallida" -ForegroundColor Red
    exit 1
}

# Encontrar el APK generado
$apkDir = if ($Release) {
    "$APP_DIR\app\build\outputs\apk\production\release"
} else {
    "$APP_DIR\app\build\outputs\apk\development\debug"
}
$apk = Get-ChildItem -Path $apkDir -Name "*.apk" | Select-Object -First 1

if (-not $apk) {
    Write-Host "ERROR: APK no encontrado en $apkDir" -ForegroundColor Red
    exit 1
}

$apkPath = "$apkDir\$apk"
Write-Host "      APK: $apk" -ForegroundColor Green

# Desinstalar versión anterior (opcional, pero limpia)
Write-Host ""
Write-Host "[4/4] Instalando en dispositivo..."
Write-Host "      Desinstalando versión anterior..."
adb uninstall $PACKAGE 2>$null

Write-Host "      Instalando APK..."
adb install "$apkPath"
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Instalación fallida" -ForegroundColor Red
    exit 1
}

# Iniciar la app
Write-Host "      Iniciando app..."
adb shell am start -n "$PACKAGE/$ACTIVITY"

Write-Host ""
Write-Host "════════════════════════════════════════════════" -ForegroundColor Green
Write-Host "  OK - App instalada y ejecutándose" -ForegroundColor Green
Write-Host "════════════════════════════════════════════════" -ForegroundColor Green
