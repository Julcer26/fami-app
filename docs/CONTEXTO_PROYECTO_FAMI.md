# 📘 Contexto Maestro del Proyecto — Fami (`app.somosfami`)

> **Última actualización:** 23 de noviembre de 2026  
> **Repositorio oficial:** `Julcer26/fami-app` (rama `main`)  
> **Nota histórica:** El repositorio `Julcer26/Nido` es legado y NO debe modificarse para entregas nuevas; toda la marca, código, web y releases actuales son **Fami** (`app.somosfami`, `https://somosfami.app`, `hola@somosfami.app`, `@hola.fami`).

---

## 1. Estado Actual de Producción
- **Versión publicada:** `v3.10.0` (`versionCode = 26`, `minSdk = 24 / Android 7.0+`, `targetSdk = 35`, `compileSdk = 36`).
- **APK oficial firmado:** `https://github.com/Julcer26/fami-app/releases/download/v3.10.0/Fami_v3.10.0.apk`
- **Manifiesto OTA:** [`fami-version.json`](../fami-version.json) (verificación criptográfica `SHA-256` obligatoria antes de instalar).
- **CI/CD:** [`.github/workflows/android-build.yml`](../.github/workflows/android-build.yml) corre `./gradlew testDebugUnitTest`, compila APKs Debug/Release, publica el Release en GitHub y actualiza `fami-version.json` con un commit `[skip ci]`.

---

## 2. Arquitectura Técnica

### Stack Android (`app/`)
- **Lenguaje y UI:** Kotlin 2.x + Jetpack Compose + Material 3 + estética *Tactile Notebook & Sticky Note UI* (`LightPaperColors` y `DarkPaperColors` en `app/src/main/java/com/example/ui/theme/`).
- **Estado y Reactividad:** `FamilyViewModel.kt` con `StateFlow` / `collectAsStateWithLifecycle()`, `DataStoreManager.kt` para preferencias locales y `LocalCryptoBox.kt` (Android Keystore **AES-256-GCM** + PIN con hash **SHA-256** salado de 128 bits).
- **Datos en Tiempo Real:** `FamilyRepository.kt` conectado a **Firebase Realtime Database** (`/families/{familyId}/...` y `/users/{uid}/...`), **Firebase Auth** (Google Sign-In y correo/contraseña), **Firebase Storage** (fotos de comprobantes) y **Firebase Cloud Messaging (FCM)** (`notifyAssignedChore`).
- **Inteligencia Artificial:** `GeminiParser.kt` y `FinanceAiAnalyzer.kt` llaman al proxy autenticado en **Firebase Cloud Functions** (`functions/`, endpoint `geminiGenerate` con `Bearer <Firebase ID Token>`), evitando exponer claves de API en el APK.

### Pantallas y Módulos Clave
| Archivo | Responsabilidad |
| :--- | :--- |
| `app/src/main/java/com/example/MainActivity.kt` | Gate principal de navegación (`OnboardingScreen` -> `AuthScreen` -> `MainScreen`), `DashboardTab`, selector de país/moneda, captura/galería de recibos y notificaciones. |
| `app/src/main/java/com/example/ui/OnboardingScreen.kt` | Asistente inicial: selección de país/moneda, modo (`familiar` vs `individual`), rol (solo en modo familiar) y presupuesto base. |
| `app/src/main/java/com/example/AuthScreen.kt` | Registro e inicio de sesión (Correo y Google Sign-In), creación o unión por Código Familiar, enlace a términos/privacidad y botón para volver a cambiar modo/país. |
| `app/src/main/java/com/example/viewmodel/FamilyViewModel.kt` | Lógica central de sesión, RBAC, modo `individual`/`familiar`, multi-moneda, billeteras, transacciones, tareas, compras, metas, notas, widget auto-unlock y tickets de soporte. |
| `app/src/main/java/com/example/database/FamilyRepository.kt` | Acceso RTDB, validación de existencia de `familyId` al unirse (`verifyJoinedFamilyHasExistingData`), alertas de presupuesto multi-moneda (`checkBudgetAlert`) y defaults. |
| `app/src/main/java/com/example/widget/WidgetQuickActivity.kt` | Actividad transparente lanzada desde el widget de escritorio en 1 toque (Escribir, Voz, Cámara, Galería) sin repetir huella biométrica. |
| `app/src/main/java/com/example/ui/ModernAccountsTab.kt` | Libro mayor, billeteras multi-moneda con logos bancarios, movimientos, presupuestos por categoría, pagos recurrentes, visor de recibos y exportación CSV. |
| `app/src/main/java/com/example/ui/TasksTab.kt` | Tareas con filtros por miembro (en modo familiar), prioridades con contraste Dark Mode, listas renombrables y notificaciones push. |
| `app/src/main/java/com/example/ui/ShoppingTab.kt` | Listas de compras colaborativas o personales con paso a gasto en billetera. |
| `app/src/main/java/com/example/ui/GoalsAndNotesTab.kt` & `StickyNotesComponents.kt` | Alcancía de metas multi-moneda y libreta de notitas adhesivas con checklists y privacidad opcional. |
| `app/src/main/java/com/example/ui/ConfigScreen.kt` | Ajustes generales (selector Modo Familiar / Individual, tema, idioma, PIN/Huella, actualización OTA, eliminación de cuenta), billeteras, miembros y soporte. |
| `app/src/main/java/com/example/ui/LegalHelpComponents.kt` | Modal legal in-app (Privacidad, Términos, Info Legal) y centro de ayuda con envío de tickets a `hola@somosfami.app`. |

---

## 3. Reglas Críticas y Decisiones Ya Resueltas (NO Regresar)
1. **R8 / Minify en Release (`app/build.gradle.kts`):** `isMinifyEnabled = false` y `isShrinkResources = false` se mantienen desactivados (con `proguard-rules.pro` y `res/raw/keep.xml` endurecidos) porque R8 causó un crash al abrir en `v3.9.7` con Firebase/Compose. No reactivar sin pruebas en dispositivo físico + Robolectric.
2. **Widget sin huella repetida (`v3.9.9`):** `WidgetQuickActivity` usa `viewModel.unlockForWidgetIfSignedIn()` para reutilizar la sesión activa de Firebase Auth y abrir directo la acción solicitada (Voz, Cámara, Galería o Nota flotante) sin exigir huella en cada toque.
3. **Orden Onboarding -> Auth (`v3.10.0`):** Los usuarios nuevos no autenticados ven `OnboardingScreen` primero y luego `AuthScreen`; quienes inician sesión con una cuenta existente marcan `setOnboardingCompleted(true)` y restauran su modo/perfil desde la nube (`syncLocalProfileAndModeFromCloud`).
4. **Coherencia de Modo Individual (`v3.10.0`):** Cuando `managementMode == "individual"`, `isAdminRole`, `canEditBudgets()`, `canManageMembers()` y `canDeleteTransaction()` siempre retornan `true`, y la UI oculta todos los elementos colaborativos (Código Familiar, pestaña Miembros, asignación de tareas, toggles `"Solo para mí"`, avatares de autor).
5. **Multi-Moneda en Recibos (`v3.10.0`):** `GeminiParser.resolveSupportedCurrency()` soporta todas las divisas de `CountryCurrencyConfig.SUPPORTED_COUNTRIES`. Tanto `ModernAccountsTab`, `FinanceAiAnalyzer` como `FamilyRepository.checkBudgetAlert` dividen `item.amount` por `tx.bcvRateUsed` cuando `tx.currency != "USD"`.
6. **Contraste Modo Nocturno (`Midnight Ink`):** Nunca usar `FamiCoralDeep` sobre superficies oscuras (`sheet`, `stickyPeach`, `stickyYellow`); usar siempre `if (paper.isDark) FamiCoralLight else FamiCoralDeep`.
7. **Web (`public/` y `docs/`):** `public/index.html`, `public/privacy.html`, `public/terms.html` y `public/delete-account.html` deben mantenerse **100% idénticos** a sus copias en `docs/`.

---

## 4. Pruebas Automatizadas (`app/src/test/java/com/example/`)
- `MainActivityLaunchTest.kt`: Verifica arranque sin crashes de `MainActivity` y `WidgetQuickActivity` vía Robolectric.
- `GeminiParserTest.kt`: Verifica extracción JSON de Gemini (incluyendo pensamiento/code fences) y resolución multi-moneda (`VES`, `COP`, `MXN`, `ARS`, `EUR`, `USD`).
- `LocalCryptoBoxTest.kt`, `OtaUpdateManagerTest.kt`, `CountryCurrencyConfigTest.kt`, `TaskNotificationHelperTest.kt`, `FamiStringsLocalizationTest.kt`.

---

## 5. Historial Reciente de Releases
- **`v3.10.0` (Build 26):** Auditoría integral — Onboarding->Auth, validación de Código Familiar, eliminación de perfil fantasma `"Administrador"`, selector y coherencia total Modo Familiar/Individual, multi-moneda completo en IA/recibos/presupuestos, filtros por miembro en tareas, contraste Dark Mode, cero iconos deprecados, legal unificado (`hola@somosfami.app`) y landing page SEO/AEO actualizada.
- **`v3.9.9` (Build 25):** Eliminación de solicitud repetida de huella/PIN al usar el widget de escritorio.
- **`v3.9.8` (Build 24):** Corrección de crash al iniciar app en build Release y adición de `MainActivityLaunchTest`.
