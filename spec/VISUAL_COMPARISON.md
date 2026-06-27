# Comparacion Visual: Web vs Android

## Paleta de colores ACTUAL

Web (Implementado):
✓ Dark theme elegante
✓ Colores: azul #3b82f6, verde #22c55e, amarillo #f59e0b, rojo #ef4444
✓ Fondos: #0f172a, #1e293b, #334155
✓ Texto: #f1f5f9 (blanco), #94a3b8 (gris)

Android (POR HACER):
✗ Colores por defecto Material (purple, teal)
✗ Sin theme oscuro personalizado
✗ Necesita adoptar paleta Web

ACCION: Crear colors.xml + themes.xml + styles.xml en Android

---

## Espaciado ACTUAL

Web (Bien):
✓ Navbar: 52px altura, padding 16px
✓ Sidebar: 260px ancho
✓ Cards: padding 20px, margin 8px
✓ Gaps: 8px, 12px (consistente)
✓ Radius: 10px (grande), 6px (pequeño)

Android (Inconsistente):
✗ RecyclerView padding 8dp
✗ Card margin 8dp, radius 12dp
✗ Item padding 12dp (inconsistente)
✗ Avatar 48dp (vs 36dp web)
✗ Hardcoded valores en layouts

ACCION: Crear dimens.xml, aplicar a todos los layouts

---

## Tipografia ACTUAL

Web (Bien):
✓ Font stack: -apple-system, BlinkMacSystemFont, Segoe UI, Roboto, sans-serif
✓ Tamaños: 14px base, 13px labels, 15px headings

Android (Por defecto Material):
✓ Roboto (correcto)
✓ Tamaños varían (16sp body, 13sp secondary)

ACCION: Crear escala de tipografia en dimens.xml, aplicar TextAppearances

---

## Componentes ACTUAL

### Botones

Web:
✓ Primary: azul (#3b82f6), padding 11px, radius 6px, sin borde
✓ Outline: transparent, borde #334155, sin fondo
✓ Icon: transparent, borde, padding 6px

Android:
✗ MaterialButton default
✗ Sin estilo personalizado consistente

ACCION: Crear MaterialButtonStyle.xml con 3 tipos

### Inputs

Web:
✓ Background: #0f172a, border #334155, radius 6px
✓ Focus: border #3b82f6
✓ Padding: 10px 12px

Android:
✓ Material TextInputLayout (OK)

ACCION: Unificar colores en theme

### Cards

Web:
✓ Background: #1e293b, border #334155, radius 10px, shadow
✓ Items contactos: horizontal layout consistente

Android:
✗ CardView radius 12dp (inconsistente)
✗ Elevation 2dp (vs 0 web)

ACCION: Cambiar radius a 10dp, elevation a 0dp

---

## Indicadores de estado ACTUAL

### Frescura (online/offline)

Web y Android (IGUAL):
✓ Verde #22c55e: < 5 minutos
✓ Amarillo #f59e0b: 5-60 minutos
✓ Gris #475569: > 60 minutos offline

ACCION: Usar colores web en Android (ya definidos)

### Loading

Web:
✓ Spinner SVG rotando 0.7s

Android:
✓ ProgressBar estándar

ACCION: Mantener como es (plataforma-dependiente)

---

## Interacciones ACTUAL

Web:
✓ Hover effects: background cambia
✓ Transiciones: 0.15s
✓ Tooltips en hover
✓ Toast notifications centered

Android:
✓ Ripple effects (Material)
✓ BottomSheets con animación

ACCION: Mantener plataforma-nativo, asegurar duración consistente (0.15-0.7s)

---

## Layout ACTUAL

Web:
  [Navbar: 52px]
  [Sidebar 260px] [Mapa]
  [Refresh indicator]
  [Contact list vertical] [Mapa osmdroid]
  [Footer]
  Mobile responsive: sidebar abajo, horizontal scroll

Android:
  [Toolbar 56dp + menu]
  [NavHostFragment]
    - ContactsFragment: RecyclerView completa
    - MapFragment: mapa osmdroid completo
  [BottomNav 56dp]

ACCION: Mantener ambos diseños nativos, solo unificar colores/espacios

---

## Status indicators (Navbar/Toolbar) ACTUAL

Web Navbar:
  Logo | Username + [geo badge] | [Refresh] [Locate] [Sharing] [Share] [Logout]

Android Toolbar:
  Toolbar title + menu (Ayuda, Toggle, Sugerencias, Compartir, Eliminar)

ACCION: Mantener como es (diferentes constrains UI)

---

## Resumen de cambios

PRIORIDAD ALTA (Visual coherence):
1. Android colors.xml: dark theme web
2. Android styles.xml: TextAppearances + MaterialButtonStyle
3. Android dimens.xml: escala 8px modular
4. Aplicar a todos los layouts

PRIORIDAD MEDIA:
1. Actualizar border-radius a 6/10px
2. Unificar elevacion/shadow (0dp en Android)
3. Tipografia: escala explícita

PRIORIDAD BAJA:
1. Animaciones: duración consistente
2. Refinamientos hover/focus

NO CAMBIAR:
1. Navegacion: cada plataforma es nativa
2. RecyclerView vs Sidebar: arquitectura diferente
3. Modal vs BottomSheet: UX patterns diferentes

TIEMPO ESTIMADO: 6-8 horas
