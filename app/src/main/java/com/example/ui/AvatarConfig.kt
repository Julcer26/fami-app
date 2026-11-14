package com.example.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.database.UserProfile
import com.example.ui.theme.FamiCoral
import com.example.ui.theme.FamiTheme
import kotlin.math.abs

data class AvatarOption(
    val id: String,
    val labelEs: String,
    val labelEn: String,
    val gender: String,   // "F" | "M"
    val ageGroup: String, // "Adulto" | "Menor"
    val drawableRes: Int
)

object AvatarCatalog {
    val ALL: List<AvatarOption> = listOf(
        AvatarOption("woman_1", "Mamá / Mujer", "Mom / Woman", "F", "Adulto", R.drawable.fami_avatar_woman_1),
        AvatarOption("man_1", "Papá / Hombre", "Dad / Man", "M", "Adulto", R.drawable.fami_avatar_man_1),
        AvatarOption("woman_2", "Mujer Lentes", "Woman Glasses", "F", "Adulto", R.drawable.fami_avatar_woman_2),
        AvatarOption("man_2", "Hombre Lentes", "Man Glasses", "M", "Adulto", R.drawable.fami_avatar_man_2),
        AvatarOption("woman_3", "Abuela / Mayor", "Grandma", "F", "Adulto", R.drawable.fami_avatar_woman_3),
        AvatarOption("man_3", "Abuelo / Mayor", "Grandpa", "M", "Adulto", R.drawable.fami_avatar_man_3),
        AvatarOption("girl_1", "Joven (F)", "Teen Girl", "F", "Menor", R.drawable.fami_avatar_girl_1),
        AvatarOption("boy_1", "Joven (M)", "Teen Boy", "M", "Menor", R.drawable.fami_avatar_boy_1),
        AvatarOption("girl_2", "Niña Pequeña", "Little Girl", "F", "Menor", R.drawable.fami_avatar_girl_2),
        AvatarOption("boy_2", "Niño Pequeño", "Little Boy", "M", "Menor", R.drawable.fami_avatar_boy_2),
        AvatarOption("girl_3", "Chica Lentes", "Girl Glasses", "F", "Menor", R.drawable.fami_avatar_girl_3),
        AvatarOption("boy_3", "Chico Rizado", "Curly Boy", "M", "Menor", R.drawable.fami_avatar_boy_3)
    )

    fun getById(id: String?): AvatarOption {
        if (id.isNullOrBlank()) return ALL.first()
        return ALL.find { it.id.equals(id, ignoreCase = true) } ?: ALL.first()
    }

    fun resolveDrawableForMember(
        memberName: String,
        profiles: List<UserProfile> = emptyList(),
        currentUser: String = "",
        currentUserAvatarId: String = "",
        explicitAvatarId: String = ""
    ): Int {
        val clean = memberName.trim()
        if (clean.equals("Todos", ignoreCase = true) || clean.equals("Hogar", ignoreCase = true) || clean.equals("Familia", ignoreCase = true)) {
            return R.drawable.ic_fami_mascot
        }
        if (explicitAvatarId.isNotBlank()) {
            return getById(explicitAvatarId).drawableRes
        }
        if (clean.isNotBlank() && clean.equals(currentUser.trim(), ignoreCase = true) && currentUserAvatarId.isNotBlank()) {
            return getById(currentUserAvatarId).drawableRes
        }
        val matchedProfile = profiles.firstOrNull { it.name.trim().equals(clean, ignoreCase = true) }
        if (matchedProfile != null && matchedProfile.avatarId.isNotBlank()) {
            return getById(matchedProfile.avatarId).drawableRes
        }
        if (clean.isBlank() && currentUserAvatarId.isNotBlank()) {
            return getById(currentUserAvatarId).drawableRes
        }
        val idx = abs(clean.lowercase().hashCode()) % ALL.size
        return ALL[idx].drawableRes
    }
}

@Composable
fun MemberAvatarBadge(
    memberName: String,
    profiles: List<UserProfile> = emptyList(),
    currentUser: String = "",
    currentUserAvatarId: String = "",
    explicitAvatarId: String = "",
    size: Dp = 24.dp,
    borderColor: Color = Color.Unspecified,
    borderWidth: Dp = 1.4.dp,
    modifier: Modifier = Modifier
) {
    val paper = FamiTheme.paper
    val resolvedBorder = if (borderColor == Color.Unspecified) {
        if (memberName.trim().equals("Todos", ignoreCase = true)) FamiCoral else paper.border
    } else {
        borderColor
    }
    val drawableRes = AvatarCatalog.resolveDrawableForMember(
        memberName = memberName,
        profiles = profiles,
        currentUser = currentUser,
        currentUserAvatarId = currentUserAvatarId,
        explicitAvatarId = explicitAvatarId
    )
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(paper.stickyYellow)
            .border(borderWidth, resolvedBorder, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = drawableRes),
            contentDescription = memberName.ifBlank { "Avatar" },
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}
