# Phase 1: Verificacion - Dark Theme Web en Android

Estado: COMPLETADO ✅

## Archivos creados/modificados

1. colors.xml — Paleta web personalizada
2. themes.xml — Material 3 Dark con colores personalizados
3. styles.xml (NEW) — TextAppearances y componentes
4. dimens.xml (NEW) — Escala modular 8px
5. values-night/colors.xml — Dark mode forzado

## Cambios visuales esperados inmediatamente

Al compilar y ejecutar Android con estos cambios, la app verá:

### COLORES (cambio más notable)
- [ANTES] Purpura, teal, colores por defecto Material
- [AHORA] Azul #3b82f6, verde #22c55e, rojo #ef4444 (como web)

### COMPONENTES Material 3
- [ANTES] Material 3 default con colores purple_500
- [AHORA] Material 3 Dark con accent_primary (#3b82f6) en todos los botones/switches

### TOOLBAR (AppBarLayout)
- [ANTES] Purple tint
- [AHORA] bg_surface (#1e293b) con texto text_primary (#f1f5f9)

### BOTTOM NAVIGATION
- [ANTES] Purple cuando seleccionado
- [AHORA] Azul (#3b82f6) cuando seleccionado

### FLOATING ACTION BUTTON (FAB)
- [ANTES] Purple
- [AHORA] Azul (#3b82f6)

### DIALOGS Y MODALS
- [ANTES] Material 3 default
- [AHORA] Fondo bg_primary (#0f172a), superficie bg_surface (#1e293b)

### BACKGROUND APP
- [ANTES] Default (gris/blanco dependiendo de sistema)
- [AHORA] bg_primary (#0f172a) oscuro

### TEXTINPUTLAYOUT (formularios)
- [ANTES] Colores por defecto
- [AHORA] Fondo bg_primary, borde bg_surface_3, focus azul (#3b82f6)

### TEXT COLORS
- [ANTES] Colores por defecto Material
- [AHORA] text_primary (#f1f5f9) para headings, text_muted (#94a3b8) para secundario

## Lo que NO cambia aun (esperando Phase 2)

Los layouts XML aun tienen:
- Hardcoded padding "8dp" (será @dimen/spacing_sm en Phase 2)
- Hardcoded margin "12dp" (será @dimen/spacing_md en Phase 2)
- Hardcoded cardCornerRadius "12dp" (será @dimen/radius_md en Phase 2)
- Hardcoded avatar sizes "48dp" (será @dimen/avatar_size_md en Phase 2)

Estos cambios automaticos estan listos en dimens.xml pero se aplicaran en Phase 2.

## Como verificar

1. Abrir Android Studio
2. Sincronizar Gradle
3. Compilar y ejecutar en emulator o dispositivo real
4. Navegar por:
   - LoginFragment: fondo bg_primary, inputs bg_primary con border
   - MainActivity: toolbar bg_surface, bottomnav con accents azules
   - ContactsFragment: FAB azul, botones azules
   - MapFragment: mapa con tokens de colores correctos

## Comparacion visual esperada

| Elemento | Antes | Ahora (Phase 1) |
|----------|-------|-----------------|
| App background | Gris/variable | #0f172a (oscuro) |
| Toolbar | Purple | #1e293b (superficie) |
| Botones | Purple | #3b82f6 (azul web) |
| FAB | Purple | #3b82f6 (azul web) |
| Bottom nav active | Purple | #3b82f6 (azul web) |
| Dialogs | White/Material | #1e293b (superficie) |
| Text | Negro/gris | #f1f5f9 (blanco) |
| Success color | Teal | #22c55e (verde) |
| Warning color | No | #f59e0b (amarillo) |
| Error color | Red | #ef4444 (rojo vibrante) |

## Proximos pasos (Phase 2)

Aplicar dimens.xml a layouts:
- item_contact.xml: cambiar hardcoded a @dimen/
- fragment_contacts.xml: padding/margin
- Otros layouts: radius, spacing, sizes

Estimacion: 2-3 horas
