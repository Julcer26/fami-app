# Fami Design System — MASTER (`somosfami.app` & Android App)

## 1. Identidad Visual ("Tactile Notebook & Sticky Note Spatial UI")
Fami combina la calidez táctil de un cuaderno físico familiar y notitas adhesivas (*sticky notes*) con profundidad espacial 3D, bordes de tinta azul marino (`#101535`) y botones físicos con labio tridimensional.

## 2. Paleta de Colores Semántica

### Modo Papel Cálido (Predeterminado)
- `--bg-desk`: `#FAF5EA` (Mesa crema cálida)
- `--surface-sheet`: `#FFFDF9` (Hoja de cuaderno blanca cálida)
- `--surface-cream`: `#F7EFE1` (Crema secundaria)
- `--sticky-yellow`: `#FFF5D6` (Notita adhesiva clásica Fami)
- `--sticky-mint`: `#E6F9F3` (Notita menta suave)
- `--sticky-peach`: `#FDECE6` (Notita durazno cálido)
- `--sticky-sky`: `#E6F1FF` (Notita cielo)
- `--tape-salmon`: `rgba(235, 147, 118, 0.78)` (Cinta adhesiva superior con borde `1px solid #101535`)
- `--ink-primary`: `#101535` (Tinta azul medianoche profundo — contraste > 14:1)
- `--ink-muted`: `#4A5270` (Tinta secundaria pizarra — contraste > 6.5:1)
- `--border-ink`: `#101535` (Borde característico estilo ilustración/cuaderno `1.5px`–`2px`)
- `--shadow-3d`: `#D2C7B4` (Sombra sólida desplazada para hojas y tarjetas)
- `--fami-coral`: `#E54629` (CTA Primario Coral Fami)
- `--fami-coral-lip`: `#9E2B14` (Labio 3D inferior para botón Coral)
- `--fami-emerald`: `#007A58` (Éxito / Ingresos / BCV)
- `--fami-gold`: `#FFB800` (Acento / Destacado)

### Modo Nocturno ("Midnight Ink")
- `--bg-desk`: `#0B0E24`
- `--surface-sheet`: `#131B3E`
- `--surface-cream`: `#1A2452`
- `--sticky-yellow`: `#FFF5D6` (La notita principal conserva su identidad crema cálida o usa variante nocturna con contraste verificado)
- `--ink-primary`: `#F6F7FF`
- `--ink-muted`: `#B4C0E8`
- `--border-ink`: `#384B94`
- `--shadow-3d`: `#060818`

## 3. Tipografía y Escala
- **Fuente Principal (UI y Títulos)**: `'Plus Jakarta Sans', system-ui, -apple-system, sans-serif` (Pesos: 500, 600, 700, 800).
- **Fuente Acento (Notas y Detalles)**: `'Caveat'` o `'Fraunces'` para etiquetas manuscritas puntuales.
- **Escala Base**: `16px` (`1rem`), interlineado `1.6` en cuerpo y `1.12` en titulares `H1`/`H2`.

## 4. Componentes Táctiles y Reglas de Interacción (60fps)
1. **Botones Táctiles 3D (`.btn-3d`)**:
   - Borde `2px solid var(--border-ink)`, radio `14px`, sombra física inferior `0 4px 0 var(--lip-color)` y transición exclusiva en `transform` (`translateY(2px)` al presionar).
2. **Hojas de Cuaderno (`.notebook-sheet`)**:
   - Líneas horizontales sutiles (`repeating-linear-gradient`), margen vertical rojo tenue a la izquierda y perforaciones de espiral.
3. **Notitas Adhesivas (`.sticky-note`)**:
   - Fondo `#FFF5D6`, borde `2px solid #101535`, sombra sólida `4px 5px 0 var(--shadow-3d)` y cinta adhesiva superior `.sticky-tape`.
4. **Accesibilidad y Movimiento**:
   - Contraste AAA en textos principales, áreas táctiles mínimas de `44×44px`, `:focus-visible` claro y soporte completo para `@media (prefers-reduced-motion: reduce)`.
