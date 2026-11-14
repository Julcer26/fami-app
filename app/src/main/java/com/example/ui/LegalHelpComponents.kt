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
                                    text = "Resumen clave: En Fami no vendemos tus datos. Tus registros financieros, notas y recibos pertenecen exclusivamente a tu cuenta y grupo familiar, y se transmiten cifrados mediante HTTPS/TLS.",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = paper.ink,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                            LegalSectionCard(
                                title = "1. Datos que recopilamos",
                                body = "• Cuenta: correo electrónico y método de acceso (Google o contraseña) gestionados de forma segura con Firebase Authentication.\n• Perfil y Hogar: alias elegido, avatar, rol familiar y código de hogar.\n• Finanzas y Notas: movimientos, billeteras, metas de ahorro, tareas, compras y notitas creadas por ti.\n• Comprobantes: fotos seleccionadas o capturadas solo cuando tú decides escanear una factura."
                            )
                            LegalSectionCard(
                                title = "2. Permisos del dispositivo",
                                body = "• Cámara y Galería: únicamente al pulsar Escanear o Galería para leer facturas.\n• Micrófono / Dictado: únicamente al pulsar el botón de voz en nota rápida.\n• Biometría (Huella): se valida localmente en el hardware de tu teléfono; Fami nunca lee ni envía tus huellas.\n• Notificaciones: recordatorios opcionales de pagos próximos."
                            )
                            LegalSectionCard(
                                title = "3. Inteligencia Artificial y Almacenamiento",
                                body = "Las lecturas de comprobantes y análisis usan Google Gemini de forma aislada y sin fines publicitarios. Los datos se alojan en Google Cloud / Firebase con cifrado en reposo y reglas de acceso restringidas a los miembros autorizados de tu hogar."
                            )
                            LegalSectionCard(
                                title = "4. Eliminación de cuenta y contacto",
                                body = "Puedes eliminar tu cuenta y credenciales en cualquier momento desde Ajustes > General > Eliminar mi Cuenta y Datos, o escribirnos a $SUPPORT_EMAIL."
                            )
                        }
                        "terms" -> {
                            StickyNoteSheet(noteColor = paper.stickyYellow) {
                                Text(
                                    text = "Aviso importante: Fami es una herramienta de organización presupuestaria y productividad doméstica. No constituye asesoramiento financiero, contable, legal ni bancario.",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = paper.ink,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                            LegalSectionCard(
                                title = "1. Aceptación y Uso del Servicio",
                                body = "Al usar Fami aceptas estos Términos y Condiciones para la administración personal o familiar de ingresos, gastos, listas, metas y notas."
                            )
                            LegalSectionCard(
                                title = "2. Tasas Referenciales y Cálculos",
                                body = "Las conversiones multi-moneda utilizan tasas públicas referenciales (como BCV u otras fuentes disponibles). Verifica siempre los importes críticos antes de tomar decisiones económicas."
                            )
                            LegalSectionCard(
                                title = "3. Asistencia por IA",
                                body = "El escaneo automático de recibos y el análisis financiero con IA pueden presentar márgenes de error; revisa y confirma los datos extraídos antes de guardarlos."
                            )
                            LegalSectionCard(
                                title = "4. Grupos Familiares",
                                body = "Quien comparte el Código Familiar otorga acceso a los registros compartidos del cuaderno dentro de ese grupo. Las notas marcadas como privadas solo son visibles por su autor."
                            )
                            LegalSectionCard(
                                title = "5. Propiedad Intelectual y Contacto",
                                body = "La marca Fami, logotipos, ilustraciones y software pertenecen a sus desarrolladores. Para consultas legales o soporte escribe a $SUPPORT_EMAIL."
                            )
                        }
                        else -> {
                            LegalSectionCard(
                                title = "Información de la Aplicación",
                                body = "• Aplicación: Fami — Cuaderno de Gestión Familiar\n• Paquete: ${BuildConfig.APPLICATION_ID}\n• Versión instalada: v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})\n• Correo de soporte: $SUPPORT_EMAIL\n• Sitio oficial: somosfami.app"
                            )
                            LegalSectionCard(
                                title = "Cumplimiento y Transparencia",
                                body = "Fami cumple con las políticas de seguridad y privacidad de datos de Google Play. Todos los accesos a datos familiares están protegidos por autenticación Firebase y verificación de membresía."
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
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(15.dp), tint = FamiCoral)
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
                                onSuccess = {
                                    isSubmitting = false
                                    feedbackSuccess = "$ticketType registrada correctamente. ¡Gracias por ayudarnos a mejorar Fami!"
                                    subjectInput = ""
                                    descriptionInput = ""
                                },
                                onError = { err ->
                                    isSubmitting = false
                                    feedbackError = err
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
                    Icon(Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isSubmitting) "Enviando..." else "Enviar al Soporte Fami",
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
                        onClick = {
                            val sub = subjectInput.trim().ifBlank { "$ticketType en $affectedSection" }
                            val body = buildString {
                                append("Tipo: $ticketType\n")
                                append("Sección: $affectedSection\n")
                                append("Detalle:\n${descriptionInput.trim()}\n\n")
                                append("---\n$deviceSummary")
                            }
                            try {
                                val mailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:$SUPPORT_EMAIL")
                                    putExtra(Intent.EXTRA_SUBJECT, "[Fami $ticketType] $sub")
                                    putExtra(Intent.EXTRA_TEXT, body)
                                }
                                context.startActivity(mailIntent)
                            } catch (_: Exception) {
                                Toast.makeText(
                                    context,
                                    "No hay app de correo configurada. Usa 'Copiar' o envía directo arriba.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        },
                        border = BorderStroke(1.2.dp, paper.border.copy(alpha = 0.55f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Email, contentDescription = null, tint = paper.ink, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Por Correo", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = paper.ink)
                    }

                    OutlinedButton(
                        onClick = {
                            val clipText = "[$ticketType - $affectedSection]\n${subjectInput.trim()}\n${descriptionInput.trim()}\n$deviceSummary"
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("Soporte Fami", clipText))
                            Toast.makeText(context, "Texto de soporte copiado", Toast.LENGTH_SHORT).show()
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
                    question = "¿Cómo comparto el cuaderno con mi familia?",
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
                        Icon(Icons.Default.MenuBook, contentDescription = null, tint = paper.ink, modifier = Modifier.size(15.dp))
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
