# Guia: Pruebas Locales - Android + Web + API

Fecha: 2026-06-27
Objetivo: Probar ambas versiones (Android via USB, Web en localhost) contra API local

## Arquitectura de prueba

```
Tu PC (Windows):
├─ API .NET:        localhost:5000
├─ Web HTML:        localhost:8000 (python -m http.server)
└─ Dispositivo Android (USB)
   └─ Conecta a:    http://10.0.2.2:5000 (alias de localhost en Android)
```

---

## PASO 1: Verificar que el dispositivo Android este conectado por USB

### En Windows (PowerShell):
```powershell
adb devices
```

Deberia ver:
```
List of attached devices
ABC123DEF456  device
```

Si no aparece:
1. Conecta dispositivo por USB
2. Habilita "Depuracion por USB" en ajustes del telefono
3. Acepta el dialogo de confianza en el telefono
4. Vuelve a ejecutar `adb devices`

---

## PASO 2: Compilar APK debug con sabor 'development'

### En PowerShell, desde C:\Desarrollo\Proyectos\localiza2:
```powershell
cd localiza2
./gradlew installDevelopmentDebug
```

Esto:
1. Compila APK con API_BASE_URL = http://10.0.2.2:5000
2. La instala directamente en el dispositivo USB

Espera 2-3 minutos. Debera terminar con:
```
BUILD SUCCESSFUL in Xs
```

---

## PASO 3: Levantar API .NET local

### En PowerShell NUEVA, desde C:\Desarrollo\Proyectos\localiza2:
```powershell
cd localiza2api
dotnet run
```

Deberia ver algo como:
```
info: Microsoft.Hosting.Lifetime[14]
      Now listening on: http://localhost:5000
      Now listening on: https://localhost:5001
```

**NO CIERRES esta ventana.** Mantiene la API corriendo.

### Verificar API funciona (en otra PowerShell):
```powershell
curl http://localhost:5000/scalar
```

Deberia devolver HTML (OpenAPI docs).

---

## PASO 4: Levantar servidor web local

### En PowerShell NUEVA, desde C:\Desarrollo\Proyectos\localiza2\localiza2web:
```powershell
cd localiza2web
python -m http.server 8000
```

Deberia ver:
```
Serving HTTP on 0.0.0.0 port 8000
```

**NO CIERRES esta ventana.** Mantiene el servidor corriendo.

---

## PASO 5: Abrir la web en navegador

### En tu PC:
1. Abre navegador (Chrome, Firefox, Edge)
2. Ve a: `http://localhost:8000`
3. Deberia cargar login

### La web detectara automaticamente que esta en localhost
- Si ve `http://localhost:8000` → API es `http://localhost:5000`
- Si ve `https://localiza2-app.angelgf.com.es` → API es `https://localiza2-api.angelgf.com.es`

---

## PASO 6: Probar la app Android

### En el dispositivo:
1. Abre la app "localiza2" (deberia haber sido instalada)
2. Deberia mostrar pantalla de login

### Verificar que se conecta a API local:
- Intenta hacer login
- Si ve error de conexion: revisa que API este corriendo (paso 3)
- Si ve "email/password invalido": ¡API esta respondiendo! (crea una cuenta primero)

---

## Workflow de pruebas

### Primera vez (crear cuenta):

1. Web: Login → "Crear cuenta" → email@test.com, password, nombre
2. Verifica email en terminal (si hay log del API)
3. Web: Abre email de confirmacion (simula clickeando)
4. Web: Login con las credenciales

5. Repetir en Android

---

## Problemas comunes

### "Connection refused" en Android
**Problema**: API no esta corriendo
**Solucion**: 
- Verifica que PowerShell del paso 3 tenga `localhost:5000` en escucha
- Reinicia: `cd localiza2api && dotnet run`

### "Cannot GET /" en navegador
**Problema**: Servidor web no esta corriendo
**Solucion**:
- Verifica que PowerShell del paso 4 tenga puerto 8000 escuchando
- Reinicia: `cd localiza2web && python -m http.server 8000`

### "API responded 503"
**Problema**: API.NET tiene error
**Solucion**:
- Revisa logs en PowerShell del paso 3
- Puede faltar appsettings.json (requiere DB settings)
- Si falta: habla con admin, necesita credenciales

### "CORS error" en navegador
**Problema**: API no permite requests desde localhost:8000
**Solucion**:
- API tiene CORS configured dinamicamente
- Verifica que `App:WebUrl` en appsettings.json incluya `localhost:8000`
- O configurarlo como: `http://localhost:8000`

### Emulador Android en lugar de dispositivo real
Si no tienes dispositivo USB, puedes usar emulador:
```powershell
cd localiza2
./gradlew installDevelopmentDebug
```

Si Android Studio abre emulador automaticamente, instala en el:
```powershell
adb install -r localiza2/app/build/outputs/apk/development/debug/app-development-debug.apk
```

---

## Terminales requeridas

Mantén estas PowerShell abiertas DURANTE las pruebas:

Terminal 1: API .NET
```
cd localiza2api && dotnet run
```

Terminal 2: Servidor web
```
cd localiza2web && python -m http.server 8000
```

Terminal 3 (opcional): Monitorear logs adb
```
adb logcat
```

---

## Cambios de codigo en tiempo real

### Android:
Despues de cambiar codigo:
```powershell
./gradlew installDevelopmentDebug
```

Reinstala automaticamente en el dispositivo.

### Web:
Recarga el navegador (F5). Los cambios en app.js se aplican inmediatamente.

### API:
Detiene `dotnet run` (Ctrl+C) y reinicia. Compila en ~3 segundos.

---

## Probar Phase 1 (Dark Theme)

Despues de compilar e instalar APK (paso 2):

1. Abre la app Android
2. Deberia ver:
   - Fondo oscuro #0f172a
   - Inputs con borde gris
   - Boton "Entrar" en azul #3b82f6
   - Texto en blanco

Comparar con web (abrir localhost:8000):
3. Deberia verse IGUAL en colores

Si ve Purple (antes): reconstruye el APK
```powershell
./gradlew clean
./gradlew installDevelopmentDebug
```

---

## Cleanup despues de pruebas

Desinstalar app del dispositivo (opcional):
```powershell
adb uninstall es.angelgf.localiza2
```

Terminar PowerShells:
- Ctrl+C en cada una
