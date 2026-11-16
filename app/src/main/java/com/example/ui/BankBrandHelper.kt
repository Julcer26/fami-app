package com.example.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*
import java.text.Normalizer

@Immutable
enum class BankEmblemStyle {
    BANESCO,
    MERCANTIL,
    BDV,
    BBVA_PROVINCIAL,
    BNC,
    BANCAMIGA,
    BANPLUS,
    BANCARIBE,
    ZELLE,
    BINANCE,
    PAYPAL,
    ZINLI,
    WALLY,
    BGENERAL,
    BOFA,
    CHASE,
    WELLS_FARGO,
    BANCOLOMBIA,
    NEQUI,
    NUBANK,
    MERCADO_PAGO,
    SANTANDER,
    WISE,
    REVOLUT,
    CASH,
    SAVINGS,
    GENERIC_BANK
}

@Immutable
data class BankBrandInfo(
    val id: String,
    val canonicalName: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val badgeBgColor: Color,
    val textOnBadge: Color,
    val monogram: String,
    val emblemStyle: BankEmblemStyle,
    @DrawableRes val logoResId: Int? = null,
    val defaultCurrencyHint: String? = null
)

object BankBrandHelper {

    private fun normalizeText(input: String): String {
        val temp = Normalizer.normalize(input.trim().lowercase(), Normalizer.Form.NFD)
        return temp.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
    }

    fun resolveBankBrand(walletName: String, currency: String = ""): BankBrandInfo {
        val norm = normalizeText(walletName)
        val curr = currency.trim().uppercase()

        return when {
            norm.contains("banesco") && (norm.contains("panama") || norm.contains("pty") || curr == "USD") -> BankBrandInfo(
                id = "banesco_panama",
                canonicalName = "Banesco Panamá",
                primaryColor = Color(0xFF007953),
                secondaryColor = Color(0xFFD9232E),
                badgeBgColor = Color(0xFFFFFFFF),
                textOnBadge = Color(0xFF003366),
                monogram = "B",
                emblemStyle = BankEmblemStyle.BANESCO,
                logoResId = R.drawable.bank_logo_banesco_pa,
                defaultCurrencyHint = "USD"
            )
            norm.contains("banesco") -> BankBrandInfo(
                id = "banesco",
                canonicalName = "Banesco",
                primaryColor = Color(0xFF007953),
                secondaryColor = Color(0xFFD9232E),
                badgeBgColor = Color(0xFFFFFFFF),
                textOnBadge = Color(0xFF007953),
                monogram = "B",
                emblemStyle = BankEmblemStyle.BANESCO,
                logoResId = R.drawable.bank_logo_banesco,
                defaultCurrencyHint = "VES"
            )
            norm.contains("mercantil") -> BankBrandInfo(
                id = "mercantil",
                canonicalName = "Mercantil",
                primaryColor = Color(0xFF004B93),
                secondaryColor = Color(0xFFF37021),
                badgeBgColor = Color(0xFF004B93),
                textOnBadge = Color.White,
                monogram = "M",
                emblemStyle = BankEmblemStyle.MERCANTIL,
                logoResId = R.drawable.bank_logo_mercantil,
                defaultCurrencyHint = "VES"
            )
            norm.contains("venezuela") || norm.contains("bdv") -> BankBrandInfo(
                id = "bdv",
                canonicalName = "Banco de Venezuela",
                primaryColor = Color(0xFFE31837),
                secondaryColor = Color(0xFF003893),
                badgeBgColor = Color(0xFFFFFFFF),
                textOnBadge = Color(0xFF003893),
                monogram = "BV",
                emblemStyle = BankEmblemStyle.BDV,
                logoResId = R.drawable.bank_logo_bdv,
                defaultCurrencyHint = "VES"
            )
            norm.contains("provincial") || norm.contains("bbva") -> BankBrandInfo(
                id = "bbva_provincial",
                canonicalName = "BBVA Provincial",
                primaryColor = Color(0xFF072146),
                secondaryColor = Color(0xFF00A9E0),
                badgeBgColor = Color(0xFF072146),
                textOnBadge = Color.White,
                monogram = "BBVA",
                emblemStyle = BankEmblemStyle.BBVA_PROVINCIAL,
                logoResId = R.drawable.bank_logo_bbva,
                defaultCurrencyHint = "VES"
            )
            norm.contains("bnc") || norm.contains("nacional de credito") -> BankBrandInfo(
                id = "bnc",
                canonicalName = "BNC",
                primaryColor = Color(0xFF002D72),
                secondaryColor = Color(0xFFF58220),
                badgeBgColor = Color(0xFF002D72),
                textOnBadge = Color.White,
                monogram = "BNC",
                emblemStyle = BankEmblemStyle.BNC,
                logoResId = R.drawable.bank_logo_bnc,
                defaultCurrencyHint = "VES"
            )
            norm.contains("bancamiga") -> BankBrandInfo(
                id = "bancamiga",
                canonicalName = "Bancamiga",
                primaryColor = Color(0xFF003B71),
                secondaryColor = Color(0xFF6CBE45),
                badgeBgColor = Color(0xFF003B71),
                textOnBadge = Color.White,
                monogram = "BA",
                emblemStyle = BankEmblemStyle.BANCAMIGA,
                logoResId = R.drawable.bank_logo_bancamiga,
                defaultCurrencyHint = "VES"
            )
            norm.contains("banplus") -> BankBrandInfo(
                id = "banplus",
                canonicalName = "Banplus",
                primaryColor = Color(0xFF006699),
                secondaryColor = Color(0xFF8CC63F),
                badgeBgColor = Color(0xFF006699),
                textOnBadge = Color.White,
                monogram = "B+",
                emblemStyle = BankEmblemStyle.BANPLUS,
                logoResId = R.drawable.bank_logo_banplus,
                defaultCurrencyHint = "VES"
            )
            norm.contains("bancaribe") || norm.contains("bfc") || norm.contains("fondo comun") || norm.contains("sofitasa") || norm.contains("plaza") || norm.contains("tesoro") || norm.contains("bicentenario") -> BankBrandInfo(
                id = "bancaribe",
                canonicalName = walletName.trim().ifBlank { "Banco" },
                primaryColor = Color(0xFF008C95),
                secondaryColor = Color(0xFFFDB913),
                badgeBgColor = Color(0xFF008C95),
                textOnBadge = Color.White,
                monogram = walletName.trim().take(2).uppercase().ifBlank { "BC" },
                emblemStyle = BankEmblemStyle.BANCARIBE,
                logoResId = if (norm.contains("bancaribe")) R.drawable.bank_logo_bancaribe else null,
                defaultCurrencyHint = "VES"
            )
            norm.contains("zelle") -> BankBrandInfo(
                id = "zelle",
                canonicalName = "Zelle",
                primaryColor = Color(0xFF6D1ED4),
                secondaryColor = Color(0xFFA259FF),
                badgeBgColor = Color(0xFF6D1ED4),
                textOnBadge = Color.White,
                monogram = "Z",
                emblemStyle = BankEmblemStyle.ZELLE,
                logoResId = R.drawable.bank_logo_zelle,
                defaultCurrencyHint = "USD"
            )
            norm.contains("binance") || norm.contains("usdt") || norm.contains("cripto") || norm.contains("crypto") -> BankBrandInfo(
                id = "binance",
                canonicalName = "Binance",
                primaryColor = Color(0xFFF3BA2F),
                secondaryColor = Color(0xFF181A20),
                badgeBgColor = Color(0xFF181A20),
                textOnBadge = Color(0xFFF3BA2F),
                monogram = "BN",
                emblemStyle = BankEmblemStyle.BINANCE,
                logoResId = R.drawable.bank_logo_binance,
                defaultCurrencyHint = "USD"
            )
            norm.contains("paypal") -> BankBrandInfo(
                id = "paypal",
                canonicalName = "PayPal",
                primaryColor = Color(0xFF003087),
                secondaryColor = Color(0xFF0079C1),
                badgeBgColor = Color(0xFF003087),
                textOnBadge = Color.White,
                monogram = "P",
                emblemStyle = BankEmblemStyle.PAYPAL,
                logoResId = R.drawable.bank_logo_paypal,
                defaultCurrencyHint = "USD"
            )
            norm.contains("zinli") -> BankBrandInfo(
                id = "zinli",
                canonicalName = "Zinli",
                primaryColor = Color(0xFF662D91),
                secondaryColor = Color(0xFF00C4B3),
                badgeBgColor = Color(0xFF662D91),
                textOnBadge = Color.White,
                monogram = "Z",
                emblemStyle = BankEmblemStyle.ZINLI,
                logoResId = R.drawable.bank_logo_zinli,
                defaultCurrencyHint = "USD"
            )
            norm.contains("wally") -> BankBrandInfo(
                id = "wally",
                canonicalName = "Wally",
                primaryColor = Color(0xFFFF4B4B),
                secondaryColor = Color(0xFF1A1E2E),
                badgeBgColor = Color(0xFFFF4B4B),
                textOnBadge = Color.White,
                monogram = "W",
                emblemStyle = BankEmblemStyle.WALLY,
                logoResId = R.drawable.bank_logo_wally,
                defaultCurrencyHint = "USD"
            )
            norm.contains("yappy") -> BankBrandInfo(
                id = "yappy",
                canonicalName = "Yappy",
                primaryColor = Color(0xFF00549F),
                secondaryColor = Color(0xFFE31837),
                badgeBgColor = Color(0xFF00549F),
                textOnBadge = Color.White,
                monogram = "Y",
                emblemStyle = BankEmblemStyle.BGENERAL,
                logoResId = R.drawable.bank_logo_yappy,
                defaultCurrencyHint = "USD"
            )
            norm.contains("banco general") || norm.contains("bgeneral") || norm.contains("banistmo") || norm.contains("bac") -> BankBrandInfo(
                id = "bgeneral",
                canonicalName = "Banco General",
                primaryColor = Color(0xFF00549F),
                secondaryColor = Color(0xFFE31837),
                badgeBgColor = Color(0xFF00549F),
                textOnBadge = Color.White,
                monogram = "BG",
                emblemStyle = BankEmblemStyle.BGENERAL,
                logoResId = R.drawable.bank_logo_bgeneral,
                defaultCurrencyHint = "USD"
            )
            norm.contains("bank of america") || norm.contains("bofa") -> BankBrandInfo(
                id = "bofa",
                canonicalName = "Bank of America",
                primaryColor = Color(0xFF012169),
                secondaryColor = Color(0xFFE31837),
                badgeBgColor = Color(0xFFFFFFFF),
                textOnBadge = Color(0xFF012169),
                monogram = "BA",
                emblemStyle = BankEmblemStyle.BOFA,
                logoResId = R.drawable.bank_logo_bofa,
                defaultCurrencyHint = "USD"
            )
            norm.contains("chase") || norm.contains("jpmorgan") -> BankBrandInfo(
                id = "chase",
                canonicalName = "Chase",
                primaryColor = Color(0xFF117ACA),
                secondaryColor = Color(0xFF0A4D82),
                badgeBgColor = Color(0xFF117ACA),
                textOnBadge = Color.White,
                monogram = "C",
                emblemStyle = BankEmblemStyle.CHASE,
                logoResId = R.drawable.bank_logo_chase,
                defaultCurrencyHint = "USD"
            )
            norm.contains("wells fargo") || norm.contains("wellsfargo") -> BankBrandInfo(
                id = "wells_fargo",
                canonicalName = "Wells Fargo",
                primaryColor = Color(0xFFD71E28),
                secondaryColor = Color(0xFFFFCD41),
                badgeBgColor = Color(0xFFD71E28),
                textOnBadge = Color(0xFFFFCD41),
                monogram = "WF",
                emblemStyle = BankEmblemStyle.WELLS_FARGO,
                logoResId = R.drawable.bank_logo_wellsfargo,
                defaultCurrencyHint = "USD"
            )
            norm.contains("bancolombia") || norm.contains("davivienda") -> BankBrandInfo(
                id = "bancolombia",
                canonicalName = "Bancolombia",
                primaryColor = Color(0xFF2C2A29),
                secondaryColor = Color(0xFFFDDA24),
                badgeBgColor = Color(0xFF2C2A29),
                textOnBadge = Color(0xFFFDDA24),
                monogram = "BC",
                emblemStyle = BankEmblemStyle.BANCOLOMBIA,
                logoResId = R.drawable.bank_logo_bancolombia,
                defaultCurrencyHint = "COP"
            )
            norm.contains("nequi") -> BankBrandInfo(
                id = "nequi",
                canonicalName = "Nequi",
                primaryColor = Color(0xFF200020),
                secondaryColor = Color(0xFFDA0081),
                badgeBgColor = Color(0xFF200020),
                textOnBadge = Color.White,
                monogram = "N",
                emblemStyle = BankEmblemStyle.NEQUI,
                logoResId = R.drawable.bank_logo_nequi,
                defaultCurrencyHint = "COP"
            )
            norm.contains("nubank") || norm == "nu" || norm.startsWith("nu ") || norm.contains("banco nu") -> BankBrandInfo(
                id = "nubank",
                canonicalName = "Nu",
                primaryColor = Color(0xFF820AD1),
                secondaryColor = Color(0xFFB55FE6),
                badgeBgColor = Color(0xFF820AD1),
                textOnBadge = Color.White,
                monogram = "nu",
                emblemStyle = BankEmblemStyle.NUBANK,
                logoResId = R.drawable.bank_logo_nubank
            )
            norm.contains("mercado pago") || norm.contains("mercadopago") -> BankBrandInfo(
                id = "mercadopago",
                canonicalName = "Mercado Pago",
                primaryColor = Color(0xFF009EE3),
                secondaryColor = Color(0xFF2D3277),
                badgeBgColor = Color(0xFF009EE3),
                textOnBadge = Color.White,
                monogram = "MP",
                emblemStyle = BankEmblemStyle.MERCADO_PAGO,
                logoResId = R.drawable.bank_logo_mercadopago
            )
            norm.contains("santander") -> BankBrandInfo(
                id = "santander",
                canonicalName = "Santander",
                primaryColor = Color(0xFFEC0000),
                secondaryColor = Color(0xFFFFFFFF),
                badgeBgColor = Color(0xFFEC0000),
                textOnBadge = Color.White,
                monogram = "S",
                emblemStyle = BankEmblemStyle.SANTANDER,
                logoResId = R.drawable.bank_logo_santander
            )
            norm.contains("wise") -> BankBrandInfo(
                id = "wise",
                canonicalName = "Wise",
                primaryColor = Color(0xFF163300),
                secondaryColor = Color(0xFF9FE870),
                badgeBgColor = Color(0xFF9FE870),
                textOnBadge = Color(0xFF163300),
                monogram = "W",
                emblemStyle = BankEmblemStyle.WISE,
                logoResId = R.drawable.bank_logo_wise,
                defaultCurrencyHint = "USD"
            )
            norm.contains("revolut") -> BankBrandInfo(
                id = "revolut",
                canonicalName = "Revolut",
                primaryColor = Color(0xFF191C1F),
                secondaryColor = Color(0xFF0075EB),
                badgeBgColor = Color(0xFF191C1F),
                textOnBadge = Color.White,
                monogram = "R",
                emblemStyle = BankEmblemStyle.REVOLUT,
                logoResId = R.drawable.bank_logo_revolut,
                defaultCurrencyHint = "EUR"
            )
            norm.contains("efectivo") || norm.contains("cash") || norm.contains("billetera fisica") || norm.contains("cartera") || norm.contains("divisas") -> BankBrandInfo(
                id = "efectivo",
                canonicalName = "Efectivo",
                primaryColor = Color(0xFF0D8A5E),
                secondaryColor = Color(0xFFB6F2DC),
                badgeBgColor = Color(0xFF0D8A5E),
                textOnBadge = Color.White,
                monogram = "$",
                emblemStyle = BankEmblemStyle.CASH
            )
            norm.contains("ahorro") || norm.contains("alcancia") || norm.contains("boveda") || norm.contains("reserva") -> BankBrandInfo(
                id = "ahorros",
                canonicalName = "Ahorros",
                primaryColor = Color(0xFFD97706),
                secondaryColor = Color(0xFFFEF3C7),
                badgeBgColor = Color(0xFFD97706),
                textOnBadge = Color.White,
                monogram = "★",
                emblemStyle = BankEmblemStyle.SAVINGS
            )
            else -> {
                val words = walletName.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
                val initials = when {
                    words.size >= 2 -> "${words[0].first().uppercaseChar()}${words[1].first().uppercaseChar()}"
                    words.size == 1 && words[0].length >= 2 -> words[0].take(2).uppercase()
                    words.size == 1 -> words[0].uppercase()
                    else -> "BK"
                }
                val isUsd = curr == "USD" || curr == "EUR"
                BankBrandInfo(
                    id = "generic_${norm.take(12)}",
                    canonicalName = walletName.trim().ifBlank { "Banco" },
                    primaryColor = if (isUsd) Color(0xFF1E40AF) else Color(0xFF0F766E),
                    secondaryColor = if (isUsd) Color(0xFF60A5FA) else Color(0xFF2DD4BF),
                    badgeBgColor = if (isUsd) Color(0xFF1E40AF) else Color(0xFF0F766E),
                    textOnBadge = Color.White,
                    monogram = initials,
                    emblemStyle = BankEmblemStyle.GENERIC_BANK
                )
            }
        }
    }

    /**
     * Lista de sugerencias rápidas de bancos/plataformas oficiales para crear billeteras en 1 toque.
     */
    fun getQuickBankPresets(localCurrency: String): List<Pair<String, String>> {
        val local = localCurrency.uppercase().ifBlank { "VES" }
        return if (local == "VES") {
            listOf(
                "Banesco Venezuela" to "VES",
                "Banesco Panamá" to "USD",
                "Mercantil" to "VES",
                "Banco de Venezuela" to "VES",
                "BBVA Provincial" to "VES",
                "BNC" to "VES",
                "Bancamiga" to "VES",
                "Zelle" to "USD",
                "Binance USDT" to "USD",
                "Zinli" to "USD",
                "Wally" to "USD",
                "Efectivo USD" to "USD"
            )
        } else {
            listOf(
                "Banesco Panamá" to "USD",
                "Zelle" to "USD",
                "Binance USDT" to "USD",
                "Banco General" to "USD",
                "Bank of America" to "USD",
                "Chase" to "USD",
                "Bancolombia" to local,
                "Nequi" to local,
                "Nu" to local,
                "Mercado Pago" to local,
                "PayPal" to "USD",
                "Efectivo" to local
            )
        }
    }
}

/**
 * Insignia vectorial oficial del banco o plataforma financiera.
 * Renderiza el emblema geométrico característico de cada institución a 60fps sin latencia de red.
 */
@Composable
fun BankLogoBadge(
    walletName: String,
    currency: String = "",
    size: Dp = 26.dp,
    modifier: Modifier = Modifier
) {
    val brand = remember(walletName, currency) {
        BankBrandHelper.resolveBankBrand(walletName, currency)
    }
    val shape = RoundedCornerShape((size.value * 0.28f).dp)

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(if (brand.logoResId != null) Color.White else brand.badgeBgColor)
            .border(1.dp, FamiTheme.paper.border.copy(alpha = 0.85f), shape),
        contentAlignment = Alignment.Center
    ) {
        val resId = brand.logoResId
        if (resId != null) {
            Image(
                painter = painterResource(id = resId),
                contentDescription = brand.canonicalName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else when (brand.emblemStyle) {
            BankEmblemStyle.BANESCO -> {
                // Emblema oficial Banesco: Arcos gemelos (Verde Esmeralda + Rojo + Punto Azul) sobre fondo blanco
                Canvas(modifier = Modifier.fillMaxSize().padding(size * 0.14f)) {
                    val w = this.size.width
                    val h = this.size.height
                    val strokeW = (w * 0.16f).coerceAtLeast(2f)

                    // Arco verde izquierdo/central
                    drawArc(
                        color = Color(0xFF007953),
                        startAngle = 190f,
                        sweepAngle = 160f,
                        useCenter = false,
                        topLeft = Offset(w * 0.04f, h * 0.14f),
                        size = Size(w * 0.56f, h * 0.65f),
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )
                    // Arco rojo derecho
                    drawArc(
                        color = Color(0xFFD9232E),
                        startAngle = 195f,
                        sweepAngle = 155f,
                        useCenter = false,
                        topLeft = Offset(w * 0.38f, h * 0.20f),
                        size = Size(w * 0.56f, h * 0.60f),
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )
                    // Base tipográfica azul marino Banesco
                    drawRoundRect(
                        color = Color(0xFF003366),
                        topLeft = Offset(w * 0.12f, h * 0.72f),
                        size = Size(w * 0.76f, h * 0.16f),
                        cornerRadius = CornerRadius(strokeW, strokeW)
                    )
                }
            }

            BankEmblemStyle.MERCANTIL -> {
                // Emblema Mercantil: Anillos entrelazados naranja y blanco sobre azul real
                Canvas(modifier = Modifier.fillMaxSize().padding(size * 0.15f)) {
                    val w = this.size.width
                    val h = this.size.height
                    val strokeW = (w * 0.16f).coerceAtLeast(2f)
                    drawCircle(
                        color = Color.White,
                        radius = w * 0.28f,
                        center = Offset(w * 0.38f, h * 0.50f),
                        style = Stroke(width = strokeW)
                    )
                    drawCircle(
                        color = Color(0xFFF37021),
                        radius = w * 0.28f,
                        center = Offset(w * 0.64f, h * 0.50f),
                        style = Stroke(width = strokeW)
                    )
                }
            }

            BankEmblemStyle.BDV -> {
                // Emblema Banco de Venezuela: Prisma triangular Rojo y Azul sobre blanco
                Canvas(modifier = Modifier.fillMaxSize().padding(size * 0.16f)) {
                    val w = this.size.width
                    val h = this.size.height
                    val redPath = Path().apply {
                        moveTo(w * 0.15f, h * 0.82f)
                        lineTo(w * 0.52f, h * 0.14f)
                        lineTo(w * 0.62f, h * 0.54f)
                        close()
                    }
                    val bluePath = Path().apply {
                        moveTo(w * 0.52f, h * 0.14f)
                        lineTo(w * 0.88f, h * 0.82f)
                        lineTo(w * 0.42f, h * 0.82f)
                        close()
                    }
                    drawPath(bluePath, color = Color(0xFF003893))
                    drawPath(redPath, color = Color(0xFFE31837))
                }
            }

            BankEmblemStyle.BBVA_PROVINCIAL -> {
                // BBVA Provincial: Azul oscuro con barra diagonal celeste y monograma BBVA
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = this.size.width
                    val h = this.size.height
                    drawRect(
                        color = Color(0xFF00A9E0),
                        topLeft = Offset(0f, h * 0.78f),
                        size = Size(w, h * 0.22f)
                    )
                }
                Text(
                    text = "BBVA",
                    color = Color.White,
                    fontSize = (size.value * 0.28f).sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1
                )
            }

            BankEmblemStyle.BNC -> {
                // BNC: Rombo tricolor (Naranja + Verde sobre Azul Marino)
                Canvas(modifier = Modifier.fillMaxSize().padding(size * 0.16f)) {
                    val w = this.size.width
                    val h = this.size.height
                    val topTriangle = Path().apply {
                        moveTo(w * 0.5f, h * 0.08f)
                        lineTo(w * 0.90f, h * 0.48f)
                        lineTo(w * 0.10f, h * 0.48f)
                        close()
                    }
                    val bottomTriangle = Path().apply {
                        moveTo(w * 0.10f, h * 0.54f)
                        lineTo(w * 0.90f, h * 0.54f)
                        lineTo(w * 0.5f, h * 0.92f)
                        close()
                    }
                    drawPath(topTriangle, color = Color(0xFFF58220))
                    drawPath(bottomTriangle, color = Color(0xFF00A651))
                }
            }

            BankEmblemStyle.BANCAMIGA -> {
                // Bancamiga: Arco dinámico verde lima + blanco sobre azul profundo
                Canvas(modifier = Modifier.fillMaxSize().padding(size * 0.15f)) {
                    val w = this.size.width
                    val h = this.size.height
                    val strokeW = (w * 0.18f).coerceAtLeast(2f)
                    drawArc(
                        color = Color(0xFF6CBE45),
                        startAngle = 200f,
                        sweepAngle = 150f,
                        useCenter = false,
                        topLeft = Offset(w * 0.10f, h * 0.12f),
                        size = Size(w * 0.80f, h * 0.76f),
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )
                }
                Text(
                    text = "B",
                    color = Color.White,
                    fontSize = (size.value * 0.42f).sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.offset(y = (size.value * 0.04f).dp)
                )
            }

            BankEmblemStyle.ZELLE -> {
                // Zelle oficial: 'Z' blanca con línea vertical atravesando arriba y abajo sobre púrpura Zelle
                Canvas(modifier = Modifier.fillMaxSize().padding(size * 0.20f)) {
                    val w = this.size.width
                    val h = this.size.height
                    val strokeW = (w * 0.15f).coerceAtLeast(2f)
                    // Barra vertical central de Zelle
                    drawLine(
                        color = Color.White,
                        start = Offset(w * 0.50f, h * 0.02f),
                        end = Offset(w * 0.50f, h * 0.98f),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                    // Trazo en Z
                    val zPath = Path().apply {
                        moveTo(w * 0.22f, h * 0.24f)
                        lineTo(w * 0.78f, h * 0.24f)
                        lineTo(w * 0.22f, h * 0.76f)
                        lineTo(w * 0.78f, h * 0.76f)
                    }
                    drawPath(
                        path = zPath,
                        color = Color.White,
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )
                }
            }

            BankEmblemStyle.BINANCE -> {
                // Binance oficial: Diamante amarillo #F3BA2F sobre fondo oscuro #181A20
                Canvas(modifier = Modifier.fillMaxSize().padding(size * 0.16f)) {
                    val w = this.size.width
                    val h = this.size.height
                    val gold = Color(0xFFF3BA2F)
                    val strokeW = (w * 0.13f).coerceAtLeast(1.8f)

                    val outerDiamond = Path().apply {
                        moveTo(w * 0.50f, h * 0.08f)
                        lineTo(w * 0.92f, h * 0.50f)
                        lineTo(w * 0.50f, h * 0.92f)
                        lineTo(w * 0.08f, h * 0.50f)
                        close()
                    }
                    drawPath(outerDiamond, color = gold, style = Stroke(width = strokeW))

                    val innerDiamond = Path().apply {
                        moveTo(w * 0.50f, h * 0.34f)
                        lineTo(w * 0.66f, h * 0.50f)
                        lineTo(w * 0.50f, h * 0.66f)
                        lineTo(w * 0.34f, h * 0.50f)
                        close()
                    }
                    drawPath(innerDiamond, color = gold)
                }
            }

            BankEmblemStyle.CHASE -> {
                // Chase Octagon
                Canvas(modifier = Modifier.fillMaxSize().padding(size * 0.18f)) {
                    val w = this.size.width
                    val h = this.size.height
                    val strokeW = (w * 0.17f).coerceAtLeast(2f)
                    val oct = Path().apply {
                        moveTo(w * 0.30f, h * 0.08f)
                        lineTo(w * 0.70f, h * 0.08f)
                        lineTo(w * 0.92f, h * 0.30f)
                        lineTo(w * 0.92f, h * 0.70f)
                        lineTo(w * 0.70f, h * 0.92f)
                        lineTo(w * 0.30f, h * 0.92f)
                        lineTo(w * 0.08f, h * 0.70f)
                        lineTo(w * 0.08f, h * 0.30f)
                        close()
                    }
                    drawPath(oct, color = Color.White, style = Stroke(width = strokeW))
                }
            }

            BankEmblemStyle.BOFA -> {
                // Bank of America: Bandera estilizada roja y azul
                Canvas(modifier = Modifier.fillMaxSize().padding(size * 0.18f)) {
                    val w = this.size.width
                    val h = this.size.height
                    val barH = (h * 0.16f).coerceAtLeast(2f)
                    drawRoundRect(
                        color = Color(0xFFE31837),
                        topLeft = Offset(w * 0.10f, h * 0.18f),
                        size = Size(w * 0.80f, barH),
                        cornerRadius = CornerRadius(barH, barH)
                    )
                    drawRoundRect(
                        color = Color(0xFFE31837),
                        topLeft = Offset(w * 0.22f, h * 0.44f),
                        size = Size(w * 0.68f, barH),
                        cornerRadius = CornerRadius(barH, barH)
                    )
                    drawRoundRect(
                        color = Color(0xFF012169),
                        topLeft = Offset(w * 0.10f, h * 0.70f),
                        size = Size(w * 0.80f, barH),
                        cornerRadius = CornerRadius(barH, barH)
                    )
                }
            }

            BankEmblemStyle.BANCOLOMBIA -> {
                // Bancolombia: Franjas amarillo, azul y rojo sobre fondo oscuro
                Canvas(modifier = Modifier.fillMaxSize().padding(size * 0.18f)) {
                    val w = this.size.width
                    val h = this.size.height
                    val barH = (h * 0.17f).coerceAtLeast(2f)
                    drawRoundRect(
                        color = Color(0xFFFDDA24),
                        topLeft = Offset(w * 0.08f, h * 0.18f),
                        size = Size(w * 0.84f, barH),
                        cornerRadius = CornerRadius(barH, barH)
                    )
                    drawRoundRect(
                        color = Color(0xFF0033A0),
                        topLeft = Offset(w * 0.16f, h * 0.44f),
                        size = Size(w * 0.76f, barH),
                        cornerRadius = CornerRadius(barH, barH)
                    )
                    drawRoundRect(
                        color = Color(0xFFCE1126),
                        topLeft = Offset(w * 0.08f, h * 0.70f),
                        size = Size(w * 0.84f, barH),
                        cornerRadius = CornerRadius(barH, barH)
                    )
                }
            }

            BankEmblemStyle.CASH -> {
                // Efectivo: Billete esmeralda con sello central
                Canvas(modifier = Modifier.fillMaxSize().padding(size * 0.16f)) {
                    val w = this.size.width
                    val h = this.size.height
                    drawRoundRect(
                        color = Color(0xFFB6F2DC),
                        topLeft = Offset(w * 0.06f, h * 0.20f),
                        size = Size(w * 0.88f, h * 0.60f),
                        cornerRadius = CornerRadius(4f, 4f),
                        style = Stroke(width = (w * 0.10f).coerceAtLeast(1.5f))
                    )
                }
                Text(
                    text = "$",
                    color = Color.White,
                    fontSize = (size.value * 0.44f).sp,
                    fontWeight = FontWeight.Black
                )
            }

            else -> {
                // Monograma oficial de alta precisión con acento secundario
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = this.size.width
                    val h = this.size.height
                    drawCircle(
                        color = brand.secondaryColor.copy(alpha = 0.28f),
                        radius = w * 0.42f,
                        center = Offset(w * 0.82f, h * 0.18f)
                    )
                }
                Text(
                    text = brand.monogram,
                    color = brand.textOnBadge,
                    fontSize = if (brand.monogram.length >= 3) (size.value * 0.28f).sp else (size.value * 0.42f).sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Botón/Píldora táctil 3D para seleccionar billetera con el logo oficial del banco,
 * micro-animación de escala a 60fps y estado activo de alto contraste.
 */
@Composable
fun BankWalletSelectorChip(
    walletName: String,
    currency: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    balancePreview: String? = null,
    compact: Boolean = false
) {
    val paper = FamiTheme.paper
    val brand = remember(walletName, currency) {
        BankBrandHelper.resolveBankBrand(walletName, currency)
    }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.96f
            selected -> 1.0f
            else -> 0.99f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "walletChipScale"
    )

    val containerColor by animateColorAsState(
        targetValue = when {
            selected && paper.isDark -> Color(0xFF283566)
            selected -> Color(0xFFFFECA8) // Dorado papel activo cálido
            else -> paper.sheet
        },
        animationSpec = tween(durationMillis = 160),
        label = "walletChipBg"
    )

    val borderColor by animateColorAsState(
        targetValue = if (selected) paper.border else paper.border.copy(alpha = 0.45f),
        animationSpec = tween(durationMillis = 160),
        label = "walletChipBorder"
    )

    val lipColor = when {
        selected -> brand.primaryColor
        else -> paper.shadow3D
    }

    val cornerRadius = if (compact) 11.dp else 13.dp
    val lipHeight = if (selected) 2.8.dp else 1.6.dp
    val pressOffset = if (isPressed) (lipHeight.value * 0.6f).dp else 0.dp

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .height(IntrinsicSize.Min)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        // Labio inferior 3D con el color de marca del banco cuando está seleccionado
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(top = lipHeight)
                .clip(RoundedCornerShape(cornerRadius))
                .background(lipColor)
                .border(
                    width = if (selected) 1.4.dp else 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(cornerRadius)
                )
        )

        // Cara frontal de la píldora de billetera
        Surface(
            modifier = Modifier
                .padding(bottom = lipHeight)
                .offset(y = pressOffset),
            shape = RoundedCornerShape(cornerRadius),
            color = containerColor,
            border = BorderStroke(if (selected) 1.6.dp else 1.1.dp, borderColor)
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = if (compact) 8.dp else 10.dp,
                    vertical = if (compact) 5.dp else 6.dp
                ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BankLogoBadge(
                    walletName = walletName,
                    currency = currency,
                    size = if (compact) 20.dp else 23.dp
                )

                Column(verticalArrangement = Arrangement.Center) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = walletName,
                            fontSize = if (compact) 11.sp else 11.5.sp,
                            fontWeight = if (selected) FontWeight.Black else FontWeight.Bold,
                            color = paper.ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Surface(
                            color = if (selected) brand.primaryColor else brand.primaryColor.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(5.dp)
                        ) {
                            Text(
                                text = currency.uppercase(),
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Black,
                                color = if (selected) Color.White else if (paper.isDark) Color.White else brand.primaryColor,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }

                    if (!balancePreview.isNullOrBlank()) {
                        Text(
                            text = balancePreview,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = paper.inkMuted,
                            maxLines = 1
                        )
                    }
                }

                if (selected) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(FamiCoral)
                            .border(1.dp, paper.border, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Seleccionada",
                            tint = Color.White,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }
        }
    }
}
