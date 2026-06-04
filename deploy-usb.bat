@echo off
setlocal EnableDelayedExpansion

set "VARIANT=%~1"
if "%VARIANT%"=="" set "VARIANT=debug"
if /I "%VARIANT%"=="debug"   goto :variant_ok
if /I "%VARIANT%"=="release" goto :variant_ok
echo [ERROR] Variante invalida: "%VARIANT%". Usa debug o release.
exit /b 1
:variant_ok

set "PROJECT_DIR=%~dp0localiza2"
set "APP_ID=es.angelgf.localiza2"

if /I "%VARIANT%"=="debug" (
    set "GRADLE_TASK=assembleDebug"
    set "APK_PATH=%PROJECT_DIR%\app\build\outputs\apk\debug\app-debug.apk"
) else (
    set "GRADLE_TASK=assembleRelease"
    set "APK_PATH=%PROJECT_DIR%\app\build\outputs\apk\release\app-release.apk"
)

echo.
echo === localiza2 - Deploy USB [%VARIANT%] ===
echo.

where adb >nul 2>&1
if errorlevel 1 (
    echo [ERROR] adb no encontrado en PATH.
    echo         Instala Android SDK Platform-Tools y anyadelo al PATH.
    exit /b 1
)

echo [1/3] Buscando dispositivo USB...
set "DEVICE="
for /f "skip=1 tokens=1,2" %%A in ('adb devices') do (
    if "%%B"=="device" if "!DEVICE!"=="" set "DEVICE=%%A"
)
if "!DEVICE!"=="" (
    echo [ERROR] No se encontro ningun dispositivo conectado.
    echo         Comprueba que el USB debugging este activado.
    exit /b 1
)
echo       Dispositivo: !DEVICE!

echo.
echo [2/3] Compilando con Gradle (%GRADLE_TASK%)...
pushd "%PROJECT_DIR%"
call gradlew.bat %GRADLE_TASK% --quiet
if errorlevel 1 (
    popd
    echo [ERROR] La compilacion fallo.
    exit /b 1
)
popd

if not exist "%APK_PATH%" (
    echo [ERROR] APK no encontrado: %APK_PATH%
    exit /b 1
)

echo.
echo [3/3] Instalando en !DEVICE!...
adb -s !DEVICE! install -r "%APK_PATH%"
if errorlevel 1 (
    echo [ERROR] La instalacion fallo.
    exit /b 1
)

adb -s !DEVICE! shell am start -n "%APP_ID%/.ui.auth.AuthActivity" >nul 2>&1

echo.
echo [OK] localiza2 instalada y en ejecucion.
echo.
endlocal
exit /b 0