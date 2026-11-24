# 🤖 Fami — Finanzas, Hogar y Libreta con IA (`app.somosfami`)

> **Sitio Web Oficial:** [somosfami.app](https://somosfami.app)  
> **Última Versión Estable:** `v3.13.0` (Build `29`) — [Descargar APK Oficial](https://github.com/Julcer26/fami-app/releases/download/v3.13.0/Fami_v3.13.0.apk)  
> **Redes Oficiales:** Instagram [@hola.fami](https://www.instagram.com/hola.fami) · TikTok [@hola.fami](https://www.tiktok.com/@hola.fami)  
> **Soporte y Privacidad:** `hola@somosfami.app` · [Política de Privacidad](https://somosfami.app/privacy.html) · [Eliminar Cuenta](https://somosfami.app/delete-account.html)

**Fami** es una aplicación Android de finanzas familiares y personales con estética de cuaderno en papel y notitas adhesivas (*Tactile Notebook UI*), potenciada por **Google Gemini IA**, billeteras multi-moneda con tasas oficiales en tiempo real y un **widget de escritorio en 1 toque**.

---

## ✨ Características Principales (`v3.13.0`)

1. **⚡ Widget de Escritorio en 1 Toque (`WidgetQuickActivity`)**:
   - Selector de billeteras con **26+ logos oficiales de bancos** (Banesco, Mercantil, BDV, Provincial, BNC, Bancamiga, Zelle, Binance, Zinli, Wally, Bancolombia, Nequi, Nu y más).
   - Apertura directa para **Escribir** (nota flotante), **Voz IA**, **Escanear factura** o **Galería** reutilizando la sesión autenticada sin exigir huella en cada toque.
2. **👨‍👩‍👧‍👦 Modo Familiar y 👤 Modo Individual Coherentes**:
   - **Modo Individual**: interfaz limpia 100% personal, sin códigos familiares, avatares de terceros ni selectores de asignación, con control total del titular.
   - **Modo Familiar**: espacio colaborativo en tiempo real con código familiar verificado, roles, filtros de tareas por miembro y elementos privados (*"Solo para mí"*).
   - Intercambiable en cualquier momento desde **Ajustes > General**.
3. **📸 Inteligencia Artificial con Proxy Seguro (`GeminiParser` & `FinanceAiAnalyzer`)**:
   - Escáner de facturas/tickets con desglose línea por línea en su moneda original y registro por voz/texto natural.
   - Todas las llamadas a Gemini pasan por **Firebase Cloud Functions** (`geminiGenerate`) autenticadas con `Bearer ID Token` (cero llaves de API en el cliente).
4. **💱 Multi-Moneda Real y Tasas Oficiales**:
   - Soporte completo para `USD`, `VES` (BCV), `EUR`, `COP`, `MXN`, `ARS`, `CLP`, `PEN`, `BRL`, `PAB`, `USDT` y más.
   - Conversión cruzada en transferencias entre billeteras, cálculo exacto de presupuestos multi-moneda y exportación a CSV.
5. **🔒 Seguridad y Cero Rastreadores Publicitarios**:
   - Cifrado local con **Android Keystore AES-256-GCM** (`LocalCryptoBox`) y PIN protegido con hash **SHA-256 salado**.
   - Actualizaciones directas **OTA** verificadas criptográficamente con **SHA-256** (`OtaUpdateManager` + `fami-version.json`).
   - **0 SDKs publicitarios** de terceros.

---

## 🧭 Documentación del Proyecto

| Documento | Descripción |
| :--- | :--- |
| **[`docs/CONTEXTO_PROYECTO_FAMI.md`](docs/CONTEXTO_PROYECTO_FAMI.md)** | **Guía maestra de arquitectura, estado actual, decisiones técnicas y handoff para nuevos chats o desarrolladores.** |
| [`design-system/MASTER.md`](design-system/MASTER.md) | Sistema de diseño oficial (Modo Papel Cálido, Modo Nocturno *Midnight Ink*, tipografía y 10 Reglas de Oro UX). |
| [`docs/legal/POLITICA_DE_PRIVACIDAD.md`](docs/legal/POLITICA_DE_PRIVACIDAD.md) | Política de Privacidad oficial (`somosfami.app/privacy.html`). |
| [`docs/legal/TERMINOS_Y_CONDICIONES.md`](docs/legal/TERMINOS_Y_CONDICIONES.md) | Términos y Condiciones de Uso (`somosfami.app/terms.html`). |
| [`docs/legal/ELIMINACION_DE_DATOS.md`](docs/legal/ELIMINACION_DE_DATOS.md) | Política y proceso de eliminación de cuenta (`somosfami.app/delete-account.html`). |
| [`docs/legal/GUIA_DATA_SAFETY_GOOGLE_PLAY.md`](docs/legal/GUIA_DATA_SAFETY_GOOGLE_PLAY.md) | Guía exacta para el formulario Data Safety de Google Play Console. |
| [`fami-version.json`](fami-version.json) | Manifiesto OTA con la última versión, URL del APK y firma SHA-256. |

---

## 🚀 CI/CD y Despliegue

- **Compilación, Tests y Releases Android**: Cada push a `main` ejecuta [`.github/workflows/android-build.yml`](.github/workflows/android-build.yml), corre los tests unitarios y Robolectric (`./gradlew testDebugUnitTest`), compila los APKs firmados, publica el GitHub Release y actualiza `fami-version.json`.
- **Backend Firebase**: Reglas de Realtime Database (`database.rules.json`), Storage (`storage.rules`) y Cloud Functions (`functions/`) se despliegan mediante `./deploy-firebase.sh`.
- **Sitio Web (`somosfami.app`)**: Sincronizado de forma idéntica entre `public/` (Firebase Hosting) y `docs/` (GitHub Pages).
