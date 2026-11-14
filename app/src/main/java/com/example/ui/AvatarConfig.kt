package com.example.ui

import com.example.R

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
}
