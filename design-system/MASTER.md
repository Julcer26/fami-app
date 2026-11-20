# Fami Design System — MASTER (`somosfami.app` & Android App `app.somosfami`)

## 1. Identidad Visual ("Tactile Notebook & Sticky Note Spatial UI")
Fami combina la calidez táctil de un cuaderno físico familiar/personal y notitas adhesivas (*sticky notes*) con profundidad espacial 3D, bordes entintados y botones físicos con labio tridimensional (`Tactile3DButton`).

---

## 2. Paleta de Colores Semántica (`Color.kt` & `Theme.kt` Sync)

### Modo Papel Cálido (`LightPaperColors` — Predeterminado)
- `--bg-desk` / `desk`: `#FAF5EA` (Mesa crema cálida)
- `--surface-sheet` / `sheet`: `#FFFDF9` (Hoja de cuaderno blanca cálida)
- `--surface-cream` / `sheetCream`: `#F7EFE1` (Crema secundaria)
- `--sticky-yellow` / `stickyYellow`: `#FFF5D6` (Notita adhesiva clásica Fami)
- `--sticky-mint` / `stickyMint`: `#E6F9F3` (Notita menta suave)
- `--sticky-peach` / `stickyPeach`: `#FDECE6` (Notita durazno cálido)
- `--sticky-sky` / `stickySky`: `#E6F1FF` (Notita cielo)
- `--tape-salmon` / `tapeSalmon`: `rgba(235, 147, 118, 0.78)` (`#EB9376` al 78% con borde entintado)
- `--ink-primary` / `ink`: `#101535` (Tinta azul medianoche profundo — contraste > 14:1)
- `--ink-muted` / `inkMuted`: `#4A5270` (Tinta secundaria pizarra — contraste > 6.5:1)
- `--border-ink` / `border`: `#101535` (Borde característico estilo cuaderno `1.5px`–`2px`)
- `--shadow-3d` / `shadow3D`: `#D2C7B4` (Sombra sólida desplazada para hojas y tarjetas)
- `--lip-paper` / `lipPaper`: `#C8BCA6` (Labio 3D inferior para superficies crema)
- `--ruled-line` / `ruledLine`: `rgba(99, 130, 190, 0.14)` (Renglón horizontal de cuaderno)
- `--margin-line` / `marginLine`: `rgba(229, 70, 41, 0.25)` (Margen vertical izquierdo coral)
- `--fami-coral`: `#E54629` (CTA Primario Coral Fami)
- `--fami-coral-light`: `#FF8C6B` (Coral luminoso para énfasis en Modo Nocturno)
- `--fami-coral-deep`: `#B53018` (Coral profundo para texto sobre superficies claras)
- `--fami-coral-lip` / `Lip3DCoral`: `#9E2B14` (Labio 3D inferior para botón Coral)
- `--fami-emerald`: `#00A87E` en app / `#007A58` en texto web sobre claro (Éxito / Ingresos / Tasas)
- `--fami-gold`: `#FFB800` (Acento / Destacado)

### Modo Nocturno ("Midnight Ink" — `DarkPaperColors`)
- `--bg-desk` / `desk`: `#0B0E24` (Mesa nocturna azul medianoche)
- `--surface-sheet` / `sheet`: `#131B3E` (Hoja nocturna)
- `--surface-cream` / `sheetCream`: `#1A2452` (Superficie secundaria nocturna)
- `--sticky-yellow` / `stickyYellow`: `#242B4D` (Notita nocturna cálida)
- `--sticky-mint` / `stickyMint`: `#133638` (Notita menta nocturna)
- `--sticky-peach` / `stickyPeach`: `#33202A` (Notita durazno nocturna)
- `--sticky-sky` / `stickySky`: `#162B4D` (Notita cielo nocturna)
- `--ink-primary` / `ink`: `#F6F7FF` (Tinta blanca cálida — contraste AAA)
- `--ink-muted` / `inkMuted`: `#B8C3E6` (Tinta secundaria nocturna — contraste > 7:1)
- `--border-ink` / `border`: `#4E63B8` (Borde entintado nocturno)
- `--shadow-3d` / `shadow3D`: `#050713` (Sombra 3D profunda)
- `--lip-paper` / `lipPaper`: `#080B1C` (Labio 3D nocturno)
- `--ruled-line` / `ruledLine`: `rgba(116, 146, 224, 0.16)`
- `--margin-line` / `marginLine`: `rgba(245, 118, 86, 0.30)`
- **Regla de contraste en Modo Nocturno**: Nunca usar `FamiCoralDeep` (`#B53018`) sobre `sheet`, `stickyPeach` o `stickyYellow` oscuros; usar siempre `FamiCoralLight` (`#FF8C6B`) o `paper.ink` (`#F6F7FF`).

---

## 3. Tipografía y Escala
- **Fuente Principal (UI y Títulos)**: `'Plus Jakarta Sans', system-ui, -apple-system, sans-serif` (Pesos: 500, 600, 700, 800, 900).
- **Fuente Monoespaciada / Tabular (Montos, Tasas y Código Familiar)**: `'JetBrains Mono', monospace` con `font-variant-numeric: tabular-nums` para alineación decimal limpia en balances multi-moneda.
- **Escala Base**: `16px` (`1rem`), interlineado `1.6` en cuerpo y `1.12` en titulares `H1`/`H2`.

---

## 4. Componentes Táctiles y 10 Reglas de Oro UX (`ui-ux-pro-max`)
1. **Contraste Semántico Estricto**: Ratio mínimo `4.5:1` en texto normal y `7:1` en cifras financieras tanto en Papel Cálido como en Midnight Ink.
2. **Áreas Táctiles Mínimas**: Todo botón, chip, pestaña o interruptor (`.btn-3d`, `.theme-btn`, selectores de modo/moneda) debe medir al menos `44×44px` (`48.dp` en Android).
3. **Coherencia de Modos (`Familiar` vs `Individual`)**: En Modo Individual se ocultan selectores de asignación a terceros, toggles `"Solo para mí"`, pestaña de Miembros y botones de Código Familiar, garantizando además permisos completos de edición al titular.
4. **Hojas de Cuaderno (`.notebook-sheet` / `NotebookPaperSheet`)**: Renglones horizontales sutiles, margen vertical coral a la izquierda y perforaciones de espiral superior.
5. **Notitas Adhesivas (`.sticky-note` / `StickyNoteSheet`)**: Cinta adhesiva superior `.sticky-tape`, borde entintado y sombra desplazada sólida.
6. **Botones Táctiles 3D (`.btn-3d` / `Tactile3DButton`)**: Borde `2px solid var(--border-ink)`, radio `14px`, labio inferior `0 4px 0 var(--lip-color)` y respuesta física `translateY(2px)` al presionar.
7. **Animaciones a 60fps sin Layout Thrashing**: Animar únicamente `transform` y `opacity`; respetar siempre `@media (prefers-reduced-motion: reduce)`.
8. **Navegación por Teclado y Lectores de Pantalla**: Estados `:focus-visible` de alto contraste (`3px solid var(--fami-coral)`) y `contentDescription` / `aria-label` descriptivos.
9. **Multi-Moneda Transparente**: Mostrar siempre el símbolo y código ISO de la moneda real (`USD`, `VES`, `COP`, `MXN`, `ARS`, `EUR`, etc.) en recibos, ítems y presupuestos.
10. **Cero Fricción Innecesaria**: El widget de escritorio reutiliza la sesión autenticada de Firebase sin exigir huella en cada toque, y los formularios aceptan tanto punto como coma decimal.
