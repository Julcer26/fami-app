# Política y Procedimiento de Eliminación de Cuentas y Datos

**Aplicación:** Fami — Finanzas, Hogar y Libreta con IA (`app.somosfami`)  
**URL de Solicitud Web:** `https://somosfami.app/delete-account.html`  
**Contacto Directo:** `hola@somosfami.app`  
**Cumplimiento:** Requisito de Eliminación de Cuentas de Google Play Store (RGPD, LGPD y Data Deletion Policy)

---

## 1. Compromiso de Control y Soberanía de Datos
En **Fami**, creemos firmemente que tú eres el único dueño de tu información personal y financiera. Conforme a las directrices oficiales de **Google Play** y a las leyes globales de protección de datos (RGPD, LGPD, CCPA), proporcionamos vías claras, inmediatas y transparentes para que cualquier usuario pueda eliminar su cuenta y todos los datos asociados.

---

## 2. Métodos para Solicitar la Eliminación

### Método 1: Directamente desde la Aplicación (Recomendado e Inmediato)
1. Abre la aplicación **Fami** en tu dispositivo Android.
2. Toca el botón de **Ajustes** (icono de engranaje en la esquina superior derecha).
3. En la pestaña **General**, desplázate hasta el final de la pantalla.
4. Pulsa el botón rojo **"Eliminar mi Cuenta y Datos"**.
5. Confirma la acción en el diálogo de seguridad.
6. Tu cuenta de Firebase Authentication, tus preferencias locales cifradas y tus datos personales se eliminarán en tiempo real.

---

### Método 2: Vía Web (Sin necesidad de tener instalada la App)
Si ya has desinstalado la aplicación o no tienes acceso a tu teléfono móvil:
1. Visita nuestro portal oficial de eliminación de datos:  
   👉 **[https://somosfami.app/delete-account.html](https://somosfami.app/delete-account.html)**
2. Rellena el formulario con:
   - Tu correo electrónico registrado en Fami.
   - Tu código de familia o espacio (`familyId`), si lo recuerdas.
   - Motivo de la solicitud (opcional).
3. Pulsa **"Solicitar Eliminación Definitiva de Datos"** para enviar la solicitud verificada a `hola@somosfami.app`.

---

### Método 3: Por Correo Electrónico
Puedes enviar un correo directamente a **`hola@somosfami.app`** con el asunto:  
`"Solicitud de Eliminación de Cuenta - Fami"` indicando el correo electrónico con el que te registraste. Nuestro equipo procesará y confirmará la eliminación en un plazo máximo de 72 horas hábiles.

---

## 3. ¿Qué datos se eliminan específicamente?

Al procesarse la eliminación de tu cuenta, se eliminan permanentemente los siguientes datos:
- **Identidad Digital:** Cuenta de usuario en Firebase Authentication (correo electrónico, UID único y credenciales).
- **Perfil de Usuario:** Alias visible, avatar, rol y preferencias de interfaz.
- **Mapeos Familiares:** Desvinculación inmediata del código de familia o eliminación completa del nodo si eres el único miembro.
- **Archivos Multimedia:** Fotografías de recibos, facturas o comprobantes subidos por ti a Firebase Storage.
- **Registros Personales:** Notas privadas, tareas asignadas exclusivamente y metas personales creadas por tu usuario.

---

## 4. Datos que pueden conservarse temporalmente y periodos de retención
- **Registros agregados de auditoría de seguridad:** Logs anonimizados de conexión para prevención de fraudes (retenidos por un máximo de 30 días en los registros de Google Cloud y posteriormente purgados).
- **Transacciones familiares compartidas:** Si perteneces a un grupo familiar activo con otros miembros, los registros contables compartidos del hogar se mantienen para no alterar el balance histórico de los demás integrantes, desvinculando cualquier identificador personal de tu cuenta.
- **Cero publicidad:** No retenemos ningún dato con fines publicitarios ni existen SDKs de rastreo publicitario en Fami.
