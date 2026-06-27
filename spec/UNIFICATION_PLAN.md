# Plan de Unificacion Visual: Web vs Android

Objetivo: Que ambas versiones se parezcan lo maximo posible manteniendo cada plataforma nativa.

Alcance: No cambiar arquitectura (web sigue SPA, Android sigue Material), solo visual.

## Fase 1: Paleta de colores (ALTA PRIORIDAD)

Android: Aplicar dark theme personalizado en colors.xml

De:
  purple_200, purple_500, purple_700, teal_200, white, black (por defecto)

A:
  bg_primary: #0f172a
  bg_surface: #1e293b
  bg_surface_2: #334155
  bg_surface_3: #475569
  accent_primary: #3b82f6
  accent_dark: #2563eb
  success: #22c55e (verde, online)
  warning: #f59e0b (amarillo, offline reciente)
  danger: #ef4444 (rojo, error)
  text_primary: #f1f5f9
  text_muted: #94a3b8
  text_hint: #64748b
  map_bg: #1a2744

Crear themes.xml con Material Components:
  colorPrimary: @color/accent_primary
  colorSurface: @color/bg_surface
  colorOnSurface: @color/text_primary
  android:colorBackground: @color/bg_primary

## Fase 2: Espaciado y tamaños (ALTA PRIORIDAD)

Crear dimens.xml con escala modular (8px base):
  spacing_xs: 4dp
  spacing_sm: 8dp
  spacing_md: 12dp
  spacing_lg: 16dp
  spacing_xl: 24dp
  radius_sm: 6dp
  radius_md: 10dp
  avatar_size_small: 36dp
  avatar_size_medium: 40dp
  avatar_size_large: 48dp
  text_size_xs: 11sp
  text_size_sm: 12sp
  text_size_body: 14sp
  text_size_title: 16sp

Actualizar layouts Android:
  - Cambiar hardcoded 8dp -> @dimen/spacing_sm
  - Cambiar hardcoded 12dp -> @dimen/spacing_md
  - Cambiar hardcoded 16dp -> @dimen/spacing_lg
  - Cambiar avatar sizes a @dimen/avatar_size_medium

Web: Ya tiene espaciado consistente. Agregar variables explicitas en CSS:
  --spacing-xs: 4px
  --spacing-sm: 8px
  --spacing-md: 12px
  etc.

## Fase 3: Botones (MEDIA PRIORIDAD)

Android: Crear styles para botones en styles.xml
  Widget.Localiza2.Button.Primary: accent_primary, radius_sm, padding 10px
  Widget.Localiza2.Button.Outline: transparent, border surface_3, radius_sm
  Widget.Localiza2.IconButton: transparent, border surface_3

Aplicar a layouts.

Web: Ya tiene .btn-primary, .btn-outline, .icon-btn. Perfecto.

## Fase 4: Cards (MEDIA PRIORIDAD)

Android item_contact.xml:
  De: cardElevation 2dp, radius 12dp
  A: cardElevation 0dp, radius @dimen/radius_md, backgroundTint @color/bg_surface

Web: Ya usa surface background, radius correcto.

## Fase 5: Tipografia (BAJA PRIORIDAD)

Android: Usar Roboto Bold para headings, Regular para body.
Web: Ya tiene system-ui font stack. OK.

## Fase 6: Animaciones (BAJA PRIORIDAD)

Android: spin .7s, pulse 2s (si no coinciden)
Web: Ya tiene spin .7s, pulse 2s. OK.

## Cambios NO hacer

- NO cambiar navegacion (web navbar+sidebar, Android toolbar+bottomnav)
- NO cambiar RecyclerView structure
- NO cambiar modal architecture

## Cambios SÍ hacer

- SÍ colores dark theme
- SÍ espaciado modular 8px
- SÍ border radius 6/10px
- SÍ estilo de botones
- SÍ tipografia Roboto

## Estimacion

- Phase 1 (colores): 2 horas
- Phase 2 (espaciado): 2 horas
- Phase 3 (botones): 1 hora
- Phase 4 (cards): 1 hora
- Testing: 1 hora
Total: ~7 horas

## Verificacion

Screenshots:
1. Web login vs Android login
2. Web app vs Android ContactsFragment
3. Web modal emparejamiento vs Android BottomSheet
4. Comparar paleta colores, espaciado, tipografia
