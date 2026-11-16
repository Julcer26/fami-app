package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.database.Note
import com.example.database.NoteCheckItem
import com.example.database.UserProfile
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class StickyColorOption(
    val key: String,
    val label: String,
    val tapeColor: Color
)

object StickyNotePalette {
    val CATEGORIES = listOf("General", "Ideas", "Hogar", "Pendientes", "Finanzas")

    val COLORS = listOf(
        StickyColorOption("yellow", "Amarilla", Color(0xFFF59E0B)),
        StickyColorOption("peach", "Durazno", FamiCoralSoft),
        StickyColorOption("mint", "Menta", Color(0xFF10B981)),
        StickyColorOption("sky", "Cielo", Color(0xFF3B82F6)),
        StickyColorOption("lavender", "Lavanda", Color(0xFF8B5CF6))
    )

    fun resolveColor(colorKey: String, isCompleted: Boolean, paper: FamiPaperPalette): Color {
        if (isCompleted) return paper.sheetCream
        return when (colorKey.lowercase()) {
            "peach" -> paper.stickyPeach
            "mint" -> paper.stickyMint
            "sky" -> paper.stickySky
            "lavender" -> paper.stickyLavender
            else -> paper.stickyYellow
        }
    }

    fun resolveTapeColor(colorKey: String, isCompleted: Boolean, paper: FamiPaperPalette): Color {
        if (isCompleted) return paper.border.copy(alpha = 0.25f)
        val base = COLORS.firstOrNull { it.key.equals(colorKey, ignoreCase = true) }?.tapeColor ?: FamiCoralSoft
        return if (paper.isDark) base.copy(alpha = 0.55f) else base.copy(alpha = 0.65f)
    }
}

@Composable
fun NotesToolbar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedFilter: String,
    onSelectFilter: (String) -> Unit,
    isGridMode: Boolean,
    onToggleGridMode: () -> Unit,
    totalCount: Int,
    pinnedCount: Int,
    completedCount: Int
) {
    val paper = FamiTheme.paper
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = {
                    Text("Buscar notitas...", fontSize = 12.5.sp, color = paper.inkMuted)
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Buscar",
                        tint = paper.inkMuted,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Limpiar búsqueda",
                                tint = paper.inkMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = paper.sheet,
                    unfocusedContainerColor = paper.sheet,
                    focusedBorderColor = FamiCoral,
                    unfocusedBorderColor = paper.border.copy(alpha = 0.65f),
                    focusedTextColor = paper.ink,
                    unfocusedTextColor = paper.ink
                ),
                modifier = Modifier.weight(1f)
            )

            Tactile3DButton(
                onClick = onToggleGridMode,
                faceColor = if (isGridMode) paper.stickySky else paper.sheet,
                lipColor = paper.lipPaper,
                borderColor = paper.border,
                cornerRadius = 12.dp,
                lipHeight = 3.dp,
                contentPadding = PaddingValues(horizontal = 11.dp, vertical = 12.dp)
            ) {
                Icon(
                    imageVector = if (isGridMode) Icons.Default.ViewAgenda else Icons.Default.GridView,
                    contentDescription = if (isGridMode) "Vista Lista" else "Vista Tablero",
                    tint = paper.ink,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        val filterOptions = buildList {
            add("Todas" to "Todas ($totalCount)")
            if (pinnedCount > 0) add("Fijadas" to "📌 Fijadas ($pinnedCount)")
            StickyNotePalette.CATEGORIES.forEach { cat -> add(cat to cat) }
            if (completedCount > 0) add("Archivadas" to "✓ Archivadas ($completedCount)")
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            filterOptions.forEach { (id, label) ->
                val isSelected = selectedFilter == id
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) FamiCoral else paper.sheet)
                        .border(
                            width = if (isSelected) 1.5.dp else 1.2.dp,
                            color = if (isSelected) paper.border else paper.border.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { onSelectFilter(id) }
                        .padding(horizontal = 11.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isSelected) Color.White else paper.ink
                    )
                }
            }
        }
    }
}

@Composable
fun StickyNoteCard(
    note: Note,
    cleanAuthor: String,
    profiles: List<UserProfile>,
    currentUser: String,
    currentUserAvatarId: String,
    compact: Boolean,
    onOpenEdit: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleComplete: () -> Unit,
    onToggleCheckItem: (Int) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val paper = FamiTheme.paper
    val context = LocalContext.current
    val noteBg = StickyNotePalette.resolveColor(note.colorKey, note.isCompleted, paper)
    val tapeBg = StickyNotePalette.resolveTapeColor(note.colorKey, note.isCompleted, paper)
    val dateTimestamp = maxOf(note.updatedDate, note.createdDate)
    val formattedDate = remember(dateTimestamp) {
        if (dateTimestamp > 0L) {
            SimpleDateFormat("dd MMM", Locale("es", "ES")).format(Date(dateTimestamp))
        } else ""
    }
    var confirmDelete by remember { mutableStateOf(false) }

    StickyNoteSheet(
        modifier = modifier,
        noteColor = noteBg,
        tapeColor = tapeBg,
        borderColor = paper.border,
        shadowColor = paper.shadow3D,
        onClick = onOpenEdit
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (compact) 11.dp else 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Cabecera de notita: categoría + privacidad + fijar + copiar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(7.dp))
                            .background(paper.sheet.copy(alpha = if (paper.isDark) 0.55f else 0.78f))
                            .border(1.dp, paper.border.copy(alpha = 0.45f), RoundedCornerShape(7.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = note.category.ifBlank { "General" }.uppercase(),
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Black,
                            color = paper.ink,
                            maxLines = 1
                        )
                    }
                    Icon(
                        imageVector = if (note.isPrivate) Icons.Default.Lock else Icons.Default.Home,
                        contentDescription = if (note.isPrivate) "Nota privada" else "Nota familiar",
                        tint = paper.inkMuted,
                        modifier = Modifier.size(12.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = onTogglePin,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = if (note.isPinned) "Desfijar nota" else "Fijar nota",
                            tint = if (note.isPinned) FamiCoral else paper.inkMuted.copy(alpha = 0.75f),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    if (!compact) {
                        IconButton(
                            onClick = {
                                val fullText = buildString {
                                    append(note.title)
                                    if (note.content.isNotBlank()) append("\n").append(note.content)
                                    if (note.checklist.isNotEmpty()) {
                                        append("\n")
                                        note.checklist.forEach { item ->
                                            append(if (item.isDone) "[x] " else "[ ] ").append(item.text).append("\n")
                                        }
                                    }
                                }.trim()
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText(note.title, fullText))
                                Toast.makeText(context, "Notita copiada", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copiar nota",
                                tint = paper.inkMuted,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // Título + Contenido
            Text(
                text = note.title,
                fontSize = if (compact) 13.5.sp else 15.sp,
                fontWeight = FontWeight.Black,
                color = if (note.isCompleted) paper.inkMuted else paper.ink,
                textDecoration = if (note.isCompleted) TextDecoration.LineThrough else null,
                maxLines = if (compact) 2 else 3,
                overflow = TextOverflow.Ellipsis
            )

            if (note.content.isNotBlank()) {
                Text(
                    text = note.content,
                    fontSize = if (compact) 11.5.sp else 12.5.sp,
                    lineHeight = if (compact) 16.sp else 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (note.isCompleted) paper.inkMuted.copy(alpha = 0.7f) else paper.ink.copy(alpha = 0.9f),
                    maxLines = if (compact) 4 else 6,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Checklist interactiva directa sobre la notita
            if (note.checklist.isNotEmpty()) {
                val previewLimit = if (compact) 4 else 8
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    note.checklist.take(previewLimit).forEachIndexed { idx, item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onToggleCheckItem(idx) }
                                .padding(vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = if (item.isDone) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                contentDescription = null,
                                tint = if (item.isDone) FamiEmerald else FamiCoral,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = item.text,
                                fontSize = if (compact) 11.sp else 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (item.isDone) paper.inkMuted else paper.ink,
                                textDecoration = if (item.isDone) TextDecoration.LineThrough else null,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (note.checklist.size > previewLimit) {
                        Text(
                            text = "+${note.checklist.size - previewLimit} ítems más...",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = paper.inkMuted
                        )
                    }
                }
            }

            HorizontalDivider(color = paper.border.copy(alpha = 0.16f))

            // Pie de notita: autor + fecha + archivar/completar + eliminar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    MemberAvatarBadge(
                        memberName = cleanAuthor,
                        profiles = profiles,
                        currentUser = currentUser,
                        currentUserAvatarId = currentUserAvatarId,
                        size = 20.dp,
                        showAliasBelow = true,
                        aliasText = cleanAuthor,
                        aliasFontSize = 8.5.sp,
                        aliasColor = paper.inkMuted,
                        aliasMaxWidth = 52.dp
                    )
                    if (formattedDate.isNotBlank() && !compact) {
                        Text(
                            text = "• $formattedDate",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = paper.inkMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(if (note.isCompleted) FamiEmerald else paper.sheet)
                            .border(
                                1.4.dp,
                                if (note.isCompleted) FamiEmerald else paper.border.copy(alpha = 0.6f),
                                CircleShape
                            )
                            .clickable { onToggleComplete() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (note.isCompleted) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Reactivar nota",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { confirmDelete = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Eliminar notita",
                            tint = AlertRed.copy(alpha = 0.85f),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = paper.sheet,
            shape = RoundedCornerShape(22.dp),
            title = { Text("¿Eliminar notita?", fontWeight = FontWeight.Black, color = paper.ink) },
            text = {
                Text(
                    "Se eliminará '${note.title}' de tu libreta.",
                    fontSize = 13.sp,
                    color = paper.inkMuted
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmDelete = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                ) {
                    Text("Eliminar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text("Cancelar", color = paper.inkMuted)
                }
            }
        )
    }
}

@Composable
fun StickyNoteEditorDialog(
    initialNote: Note?,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        content: String,
        colorKey: String,
        category: String,
        isPinned: Boolean,
        isPrivate: Boolean,
        checklist: List<NoteCheckItem>
    ) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val paper = FamiTheme.paper
    var title by remember(initialNote) { mutableStateOf(initialNote?.title.orEmpty()) }
    var content by remember(initialNote) { mutableStateOf(initialNote?.content.orEmpty()) }
    var colorKey by remember(initialNote) { mutableStateOf(initialNote?.colorKey?.ifBlank { "yellow" } ?: "yellow") }
    var category by remember(initialNote) { mutableStateOf(initialNote?.category?.ifBlank { "General" } ?: "General") }
    var isPinned by remember(initialNote) { mutableStateOf(initialNote?.isPinned ?: false) }
    var isPrivate by remember(initialNote) { mutableStateOf(initialNote?.isPrivate ?: true) }
    var checklist by remember(initialNote) { mutableStateOf(initialNote?.checklist ?: emptyList()) }
    var newCheckText by remember { mutableStateOf("") }

    val previewBg = StickyNotePalette.resolveColor(colorKey, false, paper)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.93f)
                .fillMaxHeight(0.86f),
            shape = RoundedCornerShape(24.dp),
            color = previewBg,
            border = BorderStroke(2.dp, paper.border)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Cinta adhesiva decorativa superior + cabecera
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(84.dp)
                            .height(10.dp)
                            .background(StickyNotePalette.resolveTapeColor(colorKey, false, paper))
                            .border(0.8.dp, paper.border.copy(alpha = 0.4f))
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialNote != null) "✏️ Editar Notita" else "📝 Nueva Notita",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = paper.ink
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { isPinned = !isPinned }) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Fijar notita",
                                tint = if (isPinned) FamiCoral else paper.inkMuted
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = paper.ink)
                        }
                    }
                }

                HorizontalDivider(color = paper.border.copy(alpha = 0.2f))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Selector de color de notita
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Color de la notita",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = paper.inkMuted
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StickyNotePalette.COLORS.forEach { opt ->
                                val swatchBg = StickyNotePalette.resolveColor(opt.key, false, paper)
                                val selected = colorKey.equals(opt.key, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(swatchBg)
                                        .border(
                                            width = if (selected) 2.5.dp else 1.4.dp,
                                            color = if (selected) FamiCoral else paper.border,
                                            shape = CircleShape
                                        )
                                        .clickable { colorKey = opt.key },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = opt.label,
                                            tint = paper.ink,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Selector de categoría
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Categoría",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = paper.inkMuted
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            StickyNotePalette.CATEGORIES.forEach { cat ->
                                val sel = category == cat
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (sel) FamiCoral else paper.sheet)
                                        .border(1.2.dp, paper.border.copy(alpha = 0.65f), RoundedCornerShape(10.dp))
                                        .clickable { category = cat }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = cat,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (sel) Color.White else paper.ink
                                    )
                                }
                            }
                        }
                    }

                    // Título y cuerpo
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Título de la notita") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = paper.sheet,
                            unfocusedContainerColor = paper.sheet,
                            focusedBorderColor = FamiCoral,
                            unfocusedBorderColor = paper.border.copy(alpha = 0.6f),
                            focusedTextColor = paper.ink,
                            unfocusedTextColor = paper.ink
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("Escribe tu nota o recordatorio...") },
                        minLines = 3,
                        maxLines = 6,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = paper.sheet,
                            unfocusedContainerColor = paper.sheet,
                            focusedBorderColor = FamiCoral,
                            unfocusedBorderColor = paper.border.copy(alpha = 0.6f),
                            focusedTextColor = paper.ink,
                            unfocusedTextColor = paper.ink
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Checklist interactiva opcional
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = paper.sheet,
                        border = BorderStroke(1.2.dp, paper.border.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Checklist,
                                    contentDescription = null,
                                    tint = FamiCoral,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Lista de verificación (${checklist.size})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = paper.ink
                                )
                            }

                            checklist.forEachIndexed { idx, item ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    IconButton(
                                        onClick = {
                                            checklist = checklist.mapIndexed { i, it ->
                                                if (i == idx) it.copy(isDone = !it.isDone) else it
                                            }
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (item.isDone) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                            contentDescription = null,
                                            tint = if (item.isDone) FamiEmerald else FamiCoral,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Text(
                                        text = item.text,
                                        fontSize = 12.5.sp,
                                        color = if (item.isDone) paper.inkMuted else paper.ink,
                                        textDecoration = if (item.isDone) TextDecoration.LineThrough else null,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = {
                                            checklist = checklist.filterIndexed { i, _ -> i != idx }
                                        },
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Quitar ítem",
                                            tint = paper.inkMuted,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }

                            val addCheckItemAction = {
                                val clean = newCheckText.trim()
                                if (clean.isNotBlank()) {
                                    checklist = checklist + NoteCheckItem(text = clean, isDone = false)
                                    newCheckText = ""
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedTextField(
                                    value = newCheckText,
                                    onValueChange = { newCheckText = it },
                                    placeholder = { Text("Agregar punto a la lista...", fontSize = 11.5.sp) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.Sentences,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(onDone = { addCheckItemAction() }),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = addCheckItemAction,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(FamiCoral)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Agregar ítem", tint = Color.White)
                                }
                            }
                        }
                    }

                    // Visibilidad: Privada vs Familiar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(paper.sheet)
                            .border(1.2.dp, paper.border.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isPrivate) Icons.Default.Lock else Icons.Default.Home,
                                contentDescription = null,
                                tint = FamiCoral,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (isPrivate) "Nota privada" else "Compartida con el hogar",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = paper.ink
                                )
                                Text(
                                    text = if (isPrivate) "Solo tú puedes ver esta notita" else "Visible para todos en tu grupo familiar",
                                    fontSize = 10.sp,
                                    color = paper.inkMuted
                                )
                            }
                        }
                        Switch(
                            checked = !isPrivate,
                            onCheckedChange = { shared -> isPrivate = !shared }
                        )
                    }
                }

                HorizontalDivider(color = paper.border.copy(alpha = 0.2f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (initialNote != null && onDelete != null) {
                        TextButton(onClick = onDelete) {
                            Text("Eliminar", color = AlertRed, fontWeight = FontWeight.Black)
                        }
                    } else {
                        TextButton(onClick = onDismiss) {
                            Text("Cancelar", color = paper.inkMuted, fontWeight = FontWeight.Bold)
                        }
                    }

                    Tactile3DButton(
                        onClick = {
                            val finalTitle = title.trim().ifBlank {
                                content.trim().lineSequence().firstOrNull()?.takeIf { it.isNotBlank() }?.take(36)
                                    ?: checklist.firstOrNull()?.text?.take(36)
                                    ?: ""
                            }
                            if (finalTitle.isNotBlank()) {
                                onSave(
                                    finalTitle,
                                    content.trim(),
                                    colorKey,
                                    category,
                                    isPinned,
                                    isPrivate,
                                    checklist
                                )
                            }
                        },
                        faceColor = FamiCoral,
                        lipColor = Lip3DCoral,
                        borderColor = paper.border,
                        cornerRadius = 12.dp,
                        lipHeight = 3.5.dp,
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 9.dp)
                    ) {
                        Text("Guardar Notita", fontWeight = FontWeight.Black, color = Color.White, fontSize = 12.5.sp)
                    }
                }
            }
        }
    }
}
