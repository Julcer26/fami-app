# Guía Oficial para Completar el Formulario "Seguridad de los Datos" (Data Safety) en Google Play Console

Al enviar **Fami** (`app.somosfami`) a revisión en Google Play Console, debes rellenar la sección **Seguridad de los datos** (Data Safety). Si las respuestas no concuerdan con los permisos declarados en el código, Google rechazará la aplicación.

A continuación tienes la respuesta exacta para cada pregunta del formulario:

---

## 1. Visión General (Overview)

1. **¿Tu aplicación recopila o comparte alguno de los tipos de datos de usuario obligatorios?**
   - **Respuesta:** **SÍ**.
2. **¿Se cifran en tránsito todos los datos de usuario que recopila tu aplicación?**
   - **Respuesta:** **SÍ** (Todos los datos viajan mediante HTTPS/TLS 1.3 hacia Firebase y Cloud Functions; además, las preferencias locales sensibles se cifran en el dispositivo con Android Keystore AES-256-GCM).
3. **¿Proporcionas una forma para que los usuarios soliciten que se eliminen sus datos?**
   - **Respuesta:** **SÍ** (Desde la app en *Ajustes > General > Eliminar mi Cuenta y Datos* y mediante URL web).
4. **Agrega el enlace web para la solicitud de eliminación:**
   - **URL a pegar:** `https://somosfami.app/delete-account.html`

---

## 2. Tipos de Datos Específicos que Recopila Fami

Marca únicamente las siguientes categorías en el cuestionario:

### A. Información Personal (Personal Info)
* **Dirección de correo electrónico (Email address):**
  - ¿Se recopila? **SÍ**.
  - ¿Se comparte con terceros? **NO**.
  - ¿El procesamiento es efímero? **NO** (se almacena en Firebase Auth para autenticación).
  - ¿Es obligatoria para usar la app? **SÍ**.
  - **Finalidades a marcar:**
    - `Funcionalidad de la aplicación (App functionality)`
    - `Gestión de cuentas (Account management)`

* **Nombre o Alias de Usuario (Name):**
  - ¿Se recopila? **SÍ** (Alias para el perfil personal o familiar).
  - ¿Se comparte? **NO**.
  - ¿Es efímero? **NO**.
  - ¿Es obligatorio? **SÍ**.
  - **Finalidades a marcar:**
    - `Funcionalidad de la aplicación (App functionality)`
    - `Personalización (Personalization)`

---

### B. Información Financiera (Financial Info)
* **Otra información financiera (Other financial info):**
  - *Nota:* Fami no recopila números de tarjetas bancarias ni claves de bancos. Solo almacena los registros de ingresos, gastos, billeteras y presupuestos ingresados por el usuario.
  - ¿Se recopila? **SÍ**.
  - ¿Se comparte con terceros? **NO**.
  - ¿Es efímero? **NO** (se sincroniza en Firebase Realtime Database para el balance del usuario o su hogar).
  - ¿Es obligatoria? **SÍ** (es la función principal de la app).
  - **Finalidades a marcar:**
    - `Funcionalidad de la aplicación (App functionality)`

---

### C. Fotos y Videos (Photos and Videos)
* **Fotos (Photos):**
  - ¿Se recopila? **SÍ** (Fotos de facturas y tickets de compra tomadas con la cámara o cargadas de la galería).
  - ¿Se comparte con terceros? **NO**.
  - ¿Es efímero? **NO** (cuando el usuario activa "Guardar foto del recibo", se almacena en Firebase Storage para consulta del comprobante).
  - ¿Es obligatoria? **NO** (Opcional; el usuario decide si escanea un recibo o registra el gasto por texto/voz).
  - **Finalidades a marcar:**
    - `Funcionalidad de la aplicación (App functionality)`

---

### D. Dispositivo u otros identificadores (Device or other IDs)
* **IDs del dispositivo o de la app:**
  - ¿Se recopila? **SÍ** (Token de Firebase Cloud Messaging `fcmToken` para notificar tareas asignadas).
  - ¿Se comparte? **NO**.
  - ¿Es efímero? **NO**.
  - ¿Es obligatorio? **SÍ**.
  - **Finalidades a marcar:**
    - `Funcionalidad de la aplicación (App functionality)`
    - `Prevención de fraudes y seguridad (Fraud prevention, security, and compliance)`

---

## 3. Datos que NO debes marcar (Marcar NO)
- **Ubicación (Location):** NO (no se solicita `ACCESS_FINE_LOCATION` ni `ACCESS_COARSE_LOCATION`).
- **Mensajes o SMS:** NO.
- **Contactos de la agenda:** NO.
- **Archivos de audio:** NO (el dictado utiliza el intent nativo `RecognizerIntent` del sistema Android; Fami no graba ni sube archivos de audio a sus servidores).
- **Salud y acondicionamiento físico:** NO.
- **Datos biométricos:** NO (el sensor de huella dactilar se valida localmente en el hardware del teléfono mediante la API `BiometricPrompt` de Android; Fami jamás accede ni almacena datos biométricos).

---

## 4. URL de la Política de Privacidad para pegar en Google Play Console
Pega exactamente este enlace en el apartado **Contenido de la aplicación > Política de privacidad**:

👉 `https://somosfami.app/privacy.html`
