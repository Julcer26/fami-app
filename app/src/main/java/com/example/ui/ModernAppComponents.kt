package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*

// ============================================================================
// FAMI v3.0 — CABECERA DE HOJA DE CUADERNO + BOTÓN 3D TÁCTIL
// ============================================================================
@Composable
fun ModernTabHeader(
    title: String,
    subtitle: String,
    actionText: String? = null,
    actionIcon: ImageVector = Icons.Default.Add,
    onActionClick: (() -> Unit)? = null
) {
    NotebookPaperSheet(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        sheetColor = PaperSheetWhite,
        showRuledLines = true,
        showMarginLine = true,
        showSpiralTop = true
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mascota Fami redimensionada con Nano Banana (sin recortes)
                Image(
                    painter = painterResource(id = R.drawable.ic_fami_mascot),
                    contentDescription = "Mascota Fami",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.5.dp, FamiMidnight, RoundedCornerShape(12.dp))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_fami_logo_wide),
                            contentDescription = "Fami Logo",
                            modifier = Modifier.height(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = PaperStickyYellow,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, FamiMidnight)
                        ) {
                            Text(
                                text = "CUADERNO",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                color = FamiMidnight,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = FamiMidnight,
                        letterSpacing = (-0.4).sp
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = FamiMidnight.copy(alpha = 0.72f),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (actionText != null && onActionClick != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Tactile3DButton(
                    onClick = onActionClick,
                    faceColor = FamiCoral,
                    lipColor = Lip3DCoral,
                    cornerRadius = 12.dp,
                    lipHeight = 4.dp,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = actionIcon,
                        contentDescription = actionText,
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = actionText.uppercase(),
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// ============================================================================
// PESTAÑAS SEPARADORAS 3D DE CUADERNO (3D KEYCAP FILTER PILLS)
// ============================================================================
@Composable
fun ModernFilterPills(
    options: List<Pair<String, String>>,
    selectedId: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (id, label) ->
            val isSelected = selectedId == id
            Tactile3DButton(
                onClick = { onSelect(id) },
                modifier = Modifier.weight(1f),
                faceColor = if (isSelected) FamiCoral else PaperSheetWhite,
                lipColor = if (isSelected) Lip3DCoral else Lip3DPaper,
                borderColor = FamiMidnight,
                cornerRadius = 12.dp,
                lipHeight = if (isSelected) 4.dp else 3.dp,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 9.dp)
            ) {
                Text(
                    text = label,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isSelected) Color.White else FamiMidnight,
                    maxLines = 1
                )
            }
        }
    }
}

// ============================================================================
// CHIPS 3D DE SELECCIÓN DE LISTAS / SECCIONES DE CUADERNO
// ============================================================================
@Composable
fun ModernListSelectorChips(
    listNames: List<String> = emptyList(),
    lists: List<String> = listNames,
    selectedList: String,
    itemCounts: Map<String, Int> = emptyMap(),
    onSelectList: (String) -> Unit,
    onAddListClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(lists) { listName ->
            val isSelected = listName == selectedList
            val count = itemCounts[listName]
            Tactile3DButton(
                onClick = { onSelectList(listName) },
                faceColor = if (isSelected) FamiMidnight else PaperSheetWhite,
                lipColor = if (isSelected) Lip3DNavy else Lip3DPaper,
                borderColor = FamiMidnight,
                cornerRadius = 12.dp,
                lipHeight = 3.5.dp,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Text(
                    text = listName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isSelected) Color.White else FamiMidnight
                )
                if (count != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isSelected) FamiCoral else PaperStickyYellow)
                            .border(1.dp, FamiMidnight, CircleShape)
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = count.toString(),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isSelected) Color.White else FamiMidnight
                        )
                    }
                }
            }
        }

        if (onAddListClick != null) {
            item {
                Tactile3DButton(
                    onClick = onAddListClick,
                    faceColor = PaperStickyPeach,
                    lipColor = Lip3DPaper,
                    borderColor = FamiMidnight,
                    cornerRadius = 12.dp,
                    lipHeight = 3.5.dp,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Nueva Hoja",
                        tint = FamiCoral,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Nueva Hoja",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Black,
                        color = FamiMidnight
                    )
                }
            }
        }
    }
}

// ============================================================================
// ESTADO VACÍO EN NOTA ADHESIVA CON STICKER OFICIAL FAMI (SIN RECORTAR)
// ============================================================================
@Composable
fun ModernEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    StickyNoteSheet(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        noteColor = PaperStickyYellow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.fami_sticker_love),
                contentDescription = "Fami Sticker",
                modifier = Modifier
                    .size(92.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.5.dp, FamiMidnight, RoundedCornerShape(18.dp))
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = FamiMidnight
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = FamiMidnight.copy(alpha = 0.75f),
                fontWeight = FontWeight.Medium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            if (actionText != null && onActionClick != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Tactile3DButton(
                    onClick = onActionClick,
                    faceColor = FamiCoral,
                    lipColor = Lip3DCoral,
                    cornerRadius = 12.dp,
                    lipHeight = 4.dp
                ) {
                    Text(
                        text = actionText.uppercase(),
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}
