# Análisis comparativo: Web vs Android

## Paleta de colores

### Web (dark mode elegante)
- Background: #0f172a (azul oscuro)
- Surface: #1e293b (azul más claro)
- Surface-2: #334155
- Surface-3: #475569
- Accent: #3b82f6 (azul vibrante)
- Accent-dark: #2563eb
- Success: #22c55e (verde)
- Warning: #f59e0b (amarillo/naranja)
- Danger: #ef4444 (rojo)
- Text: #f1f5f9 (blanco)
- Text-muted: #94a3b8 (gris)

### Android (sin personalizar)
- Usa color system-ui por defecto
- No tiene tema oscuro personalizado
- Necesita adoptar la paleta web

## Componentes y patrones

### Navegación
| Aspecto | Web | Android |
|---------|-----|---------|
| Top bar | Navbar fijo (52px) | Toolbar + Material |
| Bottom nav | No | BottomNavigationView |
| Logo/marca | En navbar left | En toolbar |
| Acciones | En navbar right (refresh, ubicar, sharing, logout) | En toolbar menu |

**Conclusión**: Integrar bottom nav de Android en web (mobile) + navbar top en Android similar a web

### Contactos
| Aspecto | Web | Android |
|---------|-----|---------|
| Layout | Sidebar left (260px fixed) | RecyclerView full-screen |
| Estructura del item | Horizontal: avatar + info + status dot | Horizontal: dot + avatar + info + botones |
| Responsivo | En mobile: horizontal scroll | Nativo de mobile |
| Avatar | Círculo 36px | Imagen 48px |
| Info | Nombre, email, meta | Alias, email, status, distancia, batería |
| Acciones | Click en contacto → historial | Botones: historial, editar, eliminar |

**Conclusión**: 
- Web: mostrar más información por defecto (distancia, batería)
- Android: mantener botones accesibles pero hacer layout más limpio
- Unificar tamaño de avatar y espacios

### Modales/Diálogos
| Aspecto | Web | Android |
|---------|-----|---------|
| Emparejamiento | Modal overlay centrado | BottomSheet |
| Historial | Modal similar | BottomSheet |
| Tabs (QR/Email) | Tab buttons inline | ViewPager2 o similar |

**Conclusión**: Adoptar BottomSheet en web (mobile) + mantener modal para desktop

## Tipografía

### Web
- Font stack: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif
- Base: 14px
- Headings: 15px-22px
- Labels: 12px-13px

### Android
- Default Material: Roboto
- Tamaños varían por componente

**Conclusión**: Unificar a system-ui en ambas + definir escala de tamaños

## Espaciado

### Web (variables CSS)
- Padding navbar: 16px
- Padding card: 40px (login), 20px (modal)
- Gap entre elementos: 8px-12px
- Border radius: 10px (grande), 6px (pequeño)

### Android
- Padding: 8dp, 12dp, 16dp
- Card margin: 8dp
- Border radius: 12dp

**Conclusión**: Adoptar escala modular (8px, 12px, 16px, 24px...)

## Animaciones

### Web
- Spin (loading): 0.7s linear infinite
- Pulse (marcador): 2s ease-out infinite
- Transiciones: 0.15s ease

### Android
- Android native animations
- Material motion

**Conclusión**: Aplicar mismas duraciones y easings

## Comportamiento

### Refresh
- Web: automático cada 30s, pausado en background
- Android: background service continuo

**Conclusión**: Mantener como es (requisitos diferentes)

### Historial
- Web: "Cargar más" en página principal
- Android: BottomSheet embebido con MapView

**Conclusión**: Mantener como es (medio diferente)

## Status indicators (frescura)

### Web
- Verde: <5 minutos
- Amarillo: 5-60 minutos
- Gris: >60 minutos offline

### Android
- Mismo esquema (check en dot visual)

**Conclusión**: Estándar ya unificado ✓

## Acción recomendada

### Prioridad ALTA
1. Aplicar paleta de colores dark mode a Android (colors.xml)
2. Unificar tamaños de avatar y espaciado (8/12/16/24px)
3. Unificar border-radius (6px pequeño, 10px grande)
4. Tipografía: system-ui font stack en web, Roboto en Android

### Prioridad MEDIA
1. Navbar top en Android (opcional, BottomNav ya funciona bien)
2. BottomSheet en web (mobile)
3. Unificar duración de animaciones
4. Botones: estilo consistente (primaryButton, outlineButton)

### Prioridad BAJA
1. Avatar 40px (vs 36px/48px actual)
2. Refinamientos de hover/focus
3. Tooltip consistency

## Cambios NO recomendados

- Cambiar bottom nav de Android: es estándar móvil
- Cambiar RecyclerView en Android a sidebar: sería contranaturale
- Cambiar modal overlay web a BottomSheet en desktop: no es UX estándar
