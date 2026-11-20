# Política de Privacidad de Fami (Finanzas, Hogar y Libreta con IA)

**Última actualización:** 20 de noviembre de 2026  
**Fecha de entrada en vigor:** 20 de noviembre de 2026  
**Aplicación:** Fami — Finanzas, Hogar y Libreta con IA (`app.somosfami`)  
**Contacto oficial de privacidad:** `hola@somosfami.app`  
**Sitio web oficial:** `https://somosfami.app`  

---

## 1. Introducción y Alcance
En **Fami** ("nosotros", "nuestro" o "la Aplicación"), protegemos la privacidad y seguridad de tu información personal y familiar. Esta Política de Privacidad explica de forma transparente cómo recopilamos, utilizamos, almacenamos, ciframos y eliminamos tus datos cuando utilizas nuestra aplicación móvil Android y nuestro sitio web oficial (`https://somosfami.app`).

Al descargar, instalar o utilizar Fami, aceptas las prácticas descritas en esta política.

---

## 2. Información que Recopilamos y Cifrado Local

### A. Información que proporcionas voluntariamente
1. **Datos de Registro y Cuenta:**
   - Dirección de correo electrónico y credenciales gestionadas mediante Google Firebase Authentication (Google Sign-In o correo/contraseña).
   - Alias visible (ej. "Papá", "Mamá", "Julio"), avatar seleccionado, rol y modo de organización (`familiar` o `individual`).
   - Código identificador del espacio (`familyId`) para sincronización entre miembros autorizados del hogar.

2. **Datos Financieros y Multi-Moneda:**
   - Ingresos, gastos, desglose de artículos de recibos, billeteras multi-moneda, límites de presupuesto por categoría, pagos recurrentes y metas de ahorro.
   - Divisas configuradas (USD, VES, COP, MXN, ARS, EUR, CLP, PEN, BRL, etc.) y tasas referenciales aplicadas.

3. **Libreta, Tareas y Compras:**
   - Tareas del hogar (con prioridad, fecha límite y miembro asignado), listas de compras y notitas adhesivas (compartidas con el hogar o marcadas como privadas *"Solo para mí"*).

4. **Imágenes de Recibos y Facturas:**
   - Fotografías de comprobantes o tickets que decides escanear o adjuntar voluntariamente.

### B. Cifrado Local en el Dispositivo (`LocalCryptoBox`)
- Las preferencias sensibles guardadas en el dispositivo se cifran con **AES-256-GCM** respaldado por el hardware **Android Keystore** (`LocalCryptoBox`).
- Tu PIN de acceso local de 5 dígitos nunca se guarda en texto plano: se almacena únicamente como un **hash SHA-256 con sal criptográfica aleatoria de 128 bits**.

---

## 3. Permisos del Dispositivo y su Finalidad

| Permiso Android | Finalidad Específica |
| :--- | :--- |
| `android.permission.INTERNET` | Sincronización en tiempo real con Firebase, validación de actualizaciones OTA y consulta de tasas de cambio oficiales. |
| `android.permission.CAMERA` | Tomar fotos de facturas o comprobantes para su extracción automática con IA. **Nunca accedemos a la cámara sin tu acción directa.** |
| `android.permission.RECORD_AUDIO` / Dictado | Dictar gastos o notas mediante el reconocedor de voz cuando pulsas el botón de micrófono. |
| `android.permission.USE_BIOMETRIC` | Desbloqueo local mediante huella o biometría. **Los datos biométricos permanecen en el hardware seguro de tu teléfono y jamás salen de él.** |
| `android.permission.POST_NOTIFICATIONS` | Enviar avisos de tareas asignadas (FCM) y recordatorios de pagos o presupuestos. |
| `android.permission.REQUEST_INSTALL_PACKAGES` | Instalar actualizaciones directas (OTA) oficiales tras verificar su firma criptográfica SHA-256. |

---

## 4. Uso de Inteligencia Artificial (Proxy Seguro hacia Google Gemini)

Fami procesa las funciones de IA a través de un **proxy autenticado en Firebase Cloud Functions** (`Bearer ID Token`) conectado a **Google Gemini**, evitando exponer llaves de API en el dispositivo:
- **Lectura Inteligente de Recibos y Lenguaje Natural:** Identifica comercio, fecha, moneda, categoría y desglose de artículos.
- **Diagnóstico Financiero:** Analiza el resumen mensual de tu hoja para ofrecer sugerencias de ahorro.
- **Compromiso de Privacidad IA:** Tus datos no se venden a anunciantes ni existen SDKs publicitarios de terceros en Fami.

---

## 5. Almacenamiento y Seguridad en la Nube

1. **Cifrado en Tránsito y Reposo:** Toda comunicación utiliza `HTTPS / TLS 1.3`. Los datos en **Firebase Realtime Database** y **Firebase Storage** se almacenan cifrados en reposo con AES-256.
2. **Aislamiento por Espacio y Membresía:** Las reglas de seguridad (*Firebase Security Rules*) restringen la lectura y escritura exclusivamente a los usuarios autenticados que pertenecen al espacio (`familyId`) o a su propio nodo `/users/{uid}`.
3. **Cero Rastreadores Publicitarios:** Fami no vende, no alquila ni comparte tus datos personales o financieros con corredores de datos ni redes publicitarias.

---

## 6. Retención y Eliminación de Datos

Conservamos tu información únicamente mientras mantengas tu cuenta activa. En cualquier momento puedes eliminar tu cuenta y todos tus datos:
1. **Desde la Aplicación (Inmediato):** Entra a `Ajustes > General > Eliminar mi Cuenta y Datos`.
2. **Vía Web o Correo Electrónico:** Completa el formulario en `https://somosfami.app/delete-account.html` o escribe a `hola@somosfami.app`.

---

## 7. Contacto y Derechos del Usuario (ARCO / RGPD)

Para ejercer tus derechos de acceso, rectificación, cancelación, oposición o portabilidad, contáctanos en:
* **Responsable:** Equipo Oficial Fami  
* **Correo de Soporte:** `hola@somosfami.app`  
* **Sitio Web:** `https://somosfami.app`  
