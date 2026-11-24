package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.BuildConfig
import com.example.ui.theme.*
import com.example.viewmodel.FamilyViewModel

private const val SUPPORT_EMAIL = "hola@somosfami.app"

fun openSafeExternalUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(
            context,
            "No se encontró un navegador disponible para abrir el enlace.",
            Toast.LENGTH_SHORT
        ).show()
    }
}

@Composable
fun LegalDocumentDialog(
    initialTab: String = "privacy", // "privacy", "terms", "legal"
    onDismiss: () -> Unit
) {
    val paper = FamiTheme.paper
    val context = LocalContext.current
    var activeTab by remember(initialTab) { mutableStateOf(initialTab) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.86f),
            shape = RoundedCornerShape(24.dp),
            color = paper.sheet,
            border = BorderStroke(2.dp, paper.border)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 10.dp, top = 14.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.Gavel,
                            contentDescription = null,
                            tint = FamiCoral,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Privacidad y Legal",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black,
                                color = paper.ink
                            )
                            Text(
                                text = "Fami • Documentación oficial en la app",
                                fontSize = 10.5.sp,
                                color = paper.inkMuted
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = paper.ink)
                    }
                }

                // Selector de pestaña interna
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(paper.sheetCream)
                        .border(1.3.dp, paper.border.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        "privacy" to "Privacidad",
                        "terms" to "Términos",
                        "legal" to "Info Legal"
                    ).forEach { (id, label) ->
                        val selected = activeTab == id
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 42.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (selected) FamiCoral else Color.Transparent)
                                .clickable { activeTab = id }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Black,
                                color = if (selected) Color.White else paper.ink
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = paper.border.copy(alpha = 0.18f))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (activeTab) {
                        "privacy" -> {
                            StickyNoteSheet(noteColor = paper.stickyMint) {
                                Text(
                                    text = "Resumen clave: En Fami no vendemos tus datos ni incluimos rastreadores publicitarios de terceros (0 ad SDKs). Tus registros pertenecen exclusivamente a tu cuenta y espacio (Individual o Familiar), cifrados localmente con AES-256-GCM y en tránsito con HTTPS/TLS.",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = paper.ink,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                            LegalSectionCard(
                                title = "1. Datos que recopilamos y Cifrado Local",
                                body = "• Cuenta: correo electrónico y método de acceso (Google o contraseña) gestionados con Firebase Authentication.\n• Cifrado en el dispositivo: las credenciales sensibles y el hash de tu PIN (SHA-256 con sal única) se resguardan mediante Android Keystore con cifrado AES-256-GCM (LocalCryptoBox).\n• Finanzas y Libreta: movimientos, billeteras multi-moneda, metas, tareas, compras y notitas.\n• Comprobantes: fotos capturadas o elegidas solo cuando decides adjuntar o escanear un recibo (almacenadas en Firebase Storage).\n• Analítica First-Party Anónima: al instalar Fami y aceptar nuestros términos, se activan contadores mensuales anónimos y agregados de uso por función (ej. uso del Widget, Escáner IA, Tareas o Tema Oscuro) sin datos personales, sin montos ni descripciones y con 0 SDKs publicitarios."
                            )
                            LegalSectionCard(
                                title = "2. Permisos del dispositivo",
                                body = "• Cámara y Galería: únicamente al pulsar Escanear o Galería para leer facturas.\n• Micrófono / Dictado: únicamente al pulsar el botón de voz en nota rápida o widget.\n• Biometría (Huella): se valida localmente en el hardware de tu teléfono; Fami nunca lee ni transmite datos biométricos.\n• Notificaciones (FCM) y Actualizaciones OTA: avisos de tareas asignadas, pagos próximos y descarga verificada por firma SHA-256."
                            )
                            LegalSectionCard(
                                title = "3. Inteligencia Artificial con Proxy Seguro",
                                body = "Las lecturas de comprobantes, dictados y diagnósticos financieros se procesan a través de un proxy autenticado en Firebase Cloud Functions hacia Google Gemini (Bearer ID Token), sin exponer llaves de API en el dispositivo ni usar tus datos para publicidad."
                            )
                            LegalSectionCard(
                                title = "4. Eliminación de cuenta y contacto",
                                body = "Puedes eliminar tu cuenta y todos tus datos de forma inmediata desde Ajustes > General > Eliminar mi Cuenta y Datos, a través del portal web https://somosfami.app/delete-account.html, o escribiendo a $SUPPORT_EMAIL."
                            )
                        }
                        "terms" -> {
                            StickyNoteSheet(noteColor = paper.stickyYellow) {
                                Text(
                                    text = "Aviso importante: Fami es una herramienta de organización presupuestaria y productividad doméstica (Familiar e Individual). No constituye asesoramiento financiero, contable, legal ni bancario.",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = paper.ink,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                            LegalSectionCard(
                                title = "1. Aceptación, Telemetría Anónima y Modos de Uso",
                                body = "Al instalar y usar Fami aceptas estos Términos y Condiciones para la administración personal (Modo Individual) o compartida (Modo Familiar) de tus finanzas y libreta, así como el registro automático de contadores anónimos y agregados de uso por función para mejorar la experiencia sin publicidad invasiva."
                            )
                            LegalSectionCard(
                                title = "2. Tasas Referenciales y Multi-Moneda",
                                body = "Las conversiones multi-moneda utilizan tasas públicas referenciales (como BCV, TRM o tasas internacionales). Verifica siempre los importes críticos antes de realizar operaciones financieras."
                            )
                            LegalSectionCard(
                                title = "3. Asistencia por IA",
                                body = "El escaneo automático de recibos, el dictado por voz y el análisis financiero con IA son herramientas de apoyo que pueden presentar márgenes de error; revisa y confirma los importes antes de guardarlos."
                            )
                            LegalSectionCard(
                                title = "4. Grupos Familiares y Privacidad",
                                body = "Quien comparte el Código Familiar otorga acceso a los registros compartidos del hogar dentro de ese grupo. Los elementos marcados como privados ('Solo para mí') solo son visibles por su autor."
                            )
                            LegalSectionCard(
                                title = "5. Propiedad Intelectual y Contacto",
                                body = "La marca Fami, logotipos, mascota Fami Robot, diseño de libreta y software pertenecen a sus creadores. Para consultas legales o soporte escribe a $SUPPORT_EMAIL."
                            )
                        }
                        else -> {
                            LegalSectionCard(
                                title = "Información de la Aplicación",
                                body = "• Aplicación: Fami — Finanzas, Hogar y Libreta con IA\n• Paquete: ${BuildConfig.APPLICATION_ID}\n• Versión instalada: v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})\n• Correo oficial de soporte: $SUPPORT_EMAIL\n• Sitio oficial: https://somosfami.app\n• Portal de baja de datos: https://somosfami.app/delete-account.html"
                            )
                            LegalSectionCard(
                                title = "Arquitectura de Seguridad y Cumplimiento",
                                body = "• Cifrado local: Android Keystore AES-256-GCM (LocalCryptoBox) + PIN con hash SHA-256 salado.\n• Tránsito y Nube: HTTPS/TLS + reglas de seguridad de Firebase Realtime Database y Storage aisladas por UID y membresía.\n• Actualizaciones OTA: verificación criptográfica de integridad SHA-256 antes de instalar.\n• Cero rastreadores publicitarios de terceros."
                            )
                        }
                    }
                }

                HorizontalDivider(color = paper.border.copy(alpha = 0.18f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            val url = when (activeTab) {
                                "terms" -> "https://somosfami.app/terms.html"
                                else -> "https://somosfami.app/privacy.html"
                            }
                            openSafeExternalUrl(context, url)
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(15.dp), tint = FamiCoral)
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Ver web", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FamiCoral)
                    }

                    Tactile3DButton(
                        onClick = onDismiss,
                        faceColor = FamiCoral,
                        lipColor = Lip3DCoral,
                        borderColor = paper.border,
                        cornerRadius = 12.dp,
                        lipHeight = 3.dp,
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
                    ) {
                        Text("Entendido", fontWeight = FontWeight.Black, color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun LegalSectionCard(title: String, body: String) {
    val paper = FamiTheme.paper
    NotebookPaperSheet(
        modifier = Modifier.fillMaxWidth(),
        sheetColor = paper.sheetCream,
        showRuledLines = false,
        showMarginLine = false
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = title,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Black,
                color = paper.ink
            )
            Text(
                text = body,
                fontSize = 12.sp,
                lineHeight = 17.5.sp,
                color = paper.inkMuted
            )
        }
    }
}

@Composable
fun HelpSupportContent(
    viewModel: FamilyViewModel,
    onOpenGuide: () -> Unit,
    onOpenLegal: (String) -> Unit
) {
    val paper = FamiTheme.paper
    val context = LocalContext.current

    var ticketType by remember { mutableStateOf("Falla") } // "Falla" or "Sugerencia"
    var affectedSection by remember { mutableStateOf("General") }
    var subjectInput by remember { mutableStateOf("") }
    var descriptionInput by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var feedbackSuccess by remember { mutableStateOf<String?>(null) }
    var feedbackError by remember { mutableStateOf<String?>(null) }

    val sections = listOf("General", "Inicio", "Finanzas", "Tareas/Compras", "Metas y Notas", "Modo Oscuro", "Otro")
    val deviceSummary = remember {
        "Fami v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) • Android ${Build.VERSION.RELEASE} (${Build.MANUFACTURER} ${Build.MODEL})"
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Cabecera informativa en nota adhesiva
        StickyNoteSheet(
            modifier = Modifier.fillMaxWidth(),
            noteColor = paper.stickySky
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.SupportAgent,
                    contentDescription = null,
                    tint = FamiCoral,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Centro de Ayuda y Soporte Fami",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Black,
                        color = paper.ink
                    )
                    Text(
                        text = "¿Encontraste una falla o tienes una idea? Envíanos tu reporte aquí o a $SUPPORT_EMAIL.",
                        fontSize = 11.sp,
                        color = paper.inkMuted
                    )
                }
            }
        }

        // Formulario de reporte de falla o sugerencia
        NotebookPaperSheet(
            modifier = Modifier.fillMaxWidth(),
            sheetColor = paper.sheet,
            showRuledLines = false,
            showMarginLine = false
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Enviar Reporte o Sugerencia",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = paper.ink
                )

                // Selector tipo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "Falla" to "🐛 Reportar Falla",
                        "Sugerencia" to "💡 Sugerencia"
                    ).forEach { (id, label) ->
                        val selected = ticketType == id
                        Surface(
                            onClick = { ticketType = id },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) (if (id == "Falla") paper.stickyPeach else paper.stickyYellow) else paper.sheetCream,
                            border = BorderStroke(
                                if (selected) 2.dp else 1.2.dp,
                                if (selected) FamiCoral else paper.border.copy(alpha = 0.45f)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = paper.ink,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "Sección relacionada:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = paper.inkMuted
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    sections.forEach { sec ->
                        val sel = affectedSection == sec
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (sel) FamiCoral else paper.sheetCream)
                                .border(1.dp, paper.border.copy(alpha = 0.5f), RoundedCornerShape(9.dp))
                                .clickable { affectedSection = sec }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = sec,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (sel) Color.White else paper.ink
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = subjectInput,
                    onValueChange = {
                        subjectInput = it
                        feedbackError = null
                    },
                    label = { Text(if (ticketType == "Falla") "Resumen de la falla" else "Título de tu idea") },
                    placeholder = {
                        Text(
                            if (ticketType == "Falla") "Ej: No abre un botón en Ajustes" else "Ej: Recordatorio semanal",
                            fontSize = 12.sp
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = descriptionInput,
                    onValueChange = {
                        descriptionInput = it
                        feedbackError = null
                    },
                    label = { Text("Cuéntanos los detalles") },
                    placeholder = {
                        Text(
                            if (ticketType == "Falla") "Qué estabas haciendo y qué ocurrió..." else "Cómo te gustaría que funcione...",
                            fontSize = 12.sp
                        )
                    },
                    minLines = 3,
                    maxLines = 5,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Diagnóstico adjunto: $deviceSummary",
                    fontSize = 10.sp,
                    color = paper.inkMuted
                )

                feedbackSuccess?.let { msg ->
                    Surface(
                        color = paper.stickyMint,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.2.dp, FamiEmerald)
                    ) {
                        Text(
                            text = "✅ $msg",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = paper.ink,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                feedbackError?.let { err ->
                    Surface(
                        color = paper.alertBg,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.2.dp, AlertRed)
                    ) {
                        Text(
                            text = "⚠️ $err",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = AlertRed,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                fun openSupportEmailApp(ticketId: String? = null): Boolean {
                    val sub = subjectInput.trim().ifBlank { "$ticketType en $affectedSection" }
                    val idSuffix = ticketId?.let { " #$it" }.orEmpty()
                    val fullSubject = "[Fami $ticketType$idSuffix] $sub"
                    val body = buildString {
                        if (!ticketId.isNullOrBlank()) append("Ticket: #$ticketId\n")
                        append("Tipo: $ticketType\n")
                        append("Sección: $affectedSection\n")
                        append("Detalle:\n${descriptionInput.trim()}\n\n")
                        append("---\n$deviceSummary")
                    }
                    val mailtoUri = Uri.parse(
                        "mailto:$SUPPORT_EMAIL?subject=${Uri.encode(fullSubject)}&body=${Uri.encode(body)}"
                    )
                    val sendToIntent = Intent(Intent.ACTION_SENDTO, mailtoUri).apply {
                        putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL))
                        putExtra(Intent.EXTRA_SUBJECT, fullSubject)
                        putExtra(Intent.EXTRA_TEXT, body)
                    }
                    return try {
                        context.startActivity(Intent.createChooser(sendToIntent, "Enviar reporte a $SUPPORT_EMAIL"))
                        true
                    } catch (_: Exception) {
                        Toast.makeText(
                            context,
                            "No hay app de correo configurada. Usa 'Copiar' y escríbenos a $SUPPORT_EMAIL.",
                            Toast.LENGTH_LONG
                        ).show()
                        false
                    }
                }

                Tactile3DButton(
                    onClick = {
                        val sub = subjectInput.trim()
                        val desc = descriptionInput.trim()
                        if (sub.isBlank() || desc.isBlank()) {
                            feedbackError = "Por favor escribe un resumen y los detalles antes de enviar."
                            return@Tactile3DButton
                        }
                        if (!isSubmitting) {
                            isSubmitting = true
                            feedbackError = null
                            feedbackSuccess = null
                            viewModel.submitSupportTicket(
                                type = ticketType,
                                section = affectedSection,
                                subject = sub,
                                description = desc,
                                appVersion = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                                deviceInfo = deviceSummary,
                                onSuccess = { result ->
                                    isSubmitting = false
                                    if (result.webhookDelivered) {
                                        feedbackSuccess = "Reporte #${result.ticketId} enviado a $SUPPORT_EMAIL. ¡Gracias por ayudarnos a mejorar Fami!"
                                        subjectInput = ""
                                        descriptionInput = ""
                                    } else {
                                        val opened = openSupportEmailApp(result.ticketId)
                                        if (opened) {
                                            feedbackSuccess = "Reporte #${result.ticketId} preparado para enviar a $SUPPORT_EMAIL."
                                        } else {
                                            feedbackError = "No se encontró app de correo. Toca 'Copiar' y envíalo a $SUPPORT_EMAIL."
                                        }
                                    }
                                },
                                onError = {
                                    isSubmitting = false
                                    val opened = openSupportEmailApp()
                                    if (opened) {
                                        feedbackSuccess = "Correo preparado para enviar directamente a $SUPPORT_EMAIL."
                                    } else {
                                        feedbackError = "No se encontró app de correo. Toca 'Copiar' y envíalo a $SUPPORT_EMAIL."
                                    }
                                }
                            )
                        }
                    },
                    faceColor = FamiCoral,
                    lipColor = Lip3DCoral,
                    borderColor = paper.border,
                    cornerRadius = 12.dp,
                    lipHeight = 3.5.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Email, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isSubmitting) "Preparando correo..." else "Enviar a $SUPPORT_EMAIL",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { openSupportEmailApp() },
                        border = BorderStroke(1.2.dp, paper.border.copy(alpha = 0.55f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Email, contentDescription = null, tint = paper.ink, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Abrir Correo", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = paper.ink)
                    }

                    OutlinedButton(
                        onClick = {
                            val clipText = "Para: $SUPPORT_EMAIL\n[$ticketType - $affectedSection]\n${subjectInput.trim()}\n${descriptionInput.trim()}\n$deviceSummary"
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("Soporte Fami", clipText))
                            Toast.makeText(context, "Reporte copiado para $SUPPORT_EMAIL", Toast.LENGTH_SHORT).show()
                        },
                        border = BorderStroke(1.2.dp, paper.border.copy(alpha = 0.55f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = paper.ink, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Copiar", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = paper.ink)
                    }
                }
            }
        }

        // Preguntas frecuentes rápidas y accesos útiles
        NotebookPaperSheet(
            modifier = Modifier.fillMaxWidth(),
            sheetColor = paper.sheet,
            showRuledLines = false,
            showMarginLine = false
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Preguntas Frecuentes y Accesos",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = paper.ink
                )
                HelpFaqItem(
                    question = "¿Cómo cambio mi PIN o activo la huella?",
                    answer = "En Ajustes > General > Método de Acceso puedes elegir Huella o PIN y tocar 'Cambiar PIN de 5 dígitos'."
                )
                HelpFaqItem(
                    question = "¿Cómo comparto mi espacio con mi familia?",
                    answer = "En Ajustes > General > Código Familiar copia tu código y compártelo para que se unan desde la pantalla inicial."
                )
                HelpFaqItem(
                    question = "¿Mis notas de la libreta son privadas?",
                    answer = "Por defecto sí. Al crear o editar una notita puedes activar o desactivar si deseas compartirla con el hogar."
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Tactile3DButton(
                        onClick = onOpenGuide,
                        faceColor = paper.stickyMint,
                        lipColor = Lip3DMint,
                        borderColor = paper.border,
                        cornerRadius = 11.dp,
                        lipHeight = 3.dp,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = paper.ink, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Guía de uso", fontSize = 11.sp, fontWeight = FontWeight.Black, color = paper.ink)
                    }

                    Tactile3DButton(
                        onClick = { onOpenLegal("privacy") },
                        faceColor = paper.stickyYellow,
                        lipColor = Lip3DGold,
                        borderColor = paper.border,
                        cornerRadius = 11.dp,
                        lipHeight = 3.dp,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PrivacyTip, contentDescription = null, tint = paper.ink, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Privacidad y Legal", fontSize = 11.sp, fontWeight = FontWeight.Black, color = paper.ink)
                    }
                }
            }
        }

        // Comunidad y Redes Sociales Oficiales (@hola.fami)
        NotebookPaperSheet(
            modifier = Modifier.fillMaxWidth(),
            sheetColor = paper.sheetCream,
            showRuledLines = false,
            showMarginLine = false
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Síguenos en Redes Oficiales (@hola.fami)",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Black,
                    color = paper.ink
                )
                Text(
                    text = "Tips de finanzas familiares, novedades de Fami Robot y tutoriales rápidos.",
                    fontSize = 11.sp,
                    color = paper.inkMuted
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Tactile3DButton(
                        onClick = { openSafeExternalUrl(context, "https://www.instagram.com/hola.fami") },
                        faceColor = paper.stickyPeach,
                        lipColor = Lip3DCoral,
                        borderColor = paper.border,
                        cornerRadius = 11.dp,
                        lipHeight = 3.dp,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = "Instagram", tint = FamiCoral, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Instagram", fontSize = 11.5.sp, fontWeight = FontWeight.Black, color = paper.ink)
                    }

                    Tactile3DButton(
                        onClick = { openSafeExternalUrl(context, "https://www.tiktok.com/@hola.fami") },
                        faceColor = if (paper.isDark) Color(0xFF233269) else FamiMidnight,
                        lipColor = paper.lipNavy,
                        borderColor = paper.border,
                        cornerRadius = 11.dp,
                        lipHeight = 3.dp,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.MusicNote, contentDescription = "TikTok", tint = Color.White, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("TikTok", fontSize = 11.5.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun HelpFaqItem(question: String, answer: String) {
    val paper = FamiTheme.paper
    var expanded by remember { mutableStateOf(false) }
    Surface(
        onClick = { expanded = !expanded },
        shape = RoundedCornerShape(11.dp),
        color = paper.sheetCream,
        border = BorderStroke(1.dp, paper.border.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = question,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = paper.ink,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = paper.inkMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = answer,
                    fontSize = 11.5.sp,
                    color = paper.inkMuted,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
