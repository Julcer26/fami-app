package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.currency.CountryCurrencyConfig
import com.example.currency.CountryInfo
import com.example.ui.theme.*
import kotlinx.coroutines.launch

data class OnboardingStep(
    val badge: String,
    val title: String,
    val description: String,
    val imageRes: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onComplete: (selectedCountry: CountryInfo) -> Unit
) {
    val steps = remember {
        listOf(
            OnboardingStep(
                badge = "PASO 1: CONOCERNOS",
                title = "¡Hola! Soy Fami",
                description = "Tu asesor financiero inteligente. Creado para que tu familia tome el control total de sus ingresos y gastos con la calidez y el orden que merecen.",
                imageRes = R.drawable.fami_onboarding_1
            ),
            OnboardingStep(
                badge = "PASO 2: TU PLAN",
                title = "Tu Moneda Local + Divisas",
                description = "Monitorea tus gastos en la moneda de tu país y conviértelos a USD o EUR en vivo con la tasa oficial diaria de tu banco central.",
                imageRes = R.drawable.fami_onboarding_2
            ),
            OnboardingStep(
                badge = "PASO 3: TU CAMINO",
                title = "Escanea Facturas con IA",
                description = "Toma una foto a cualquier ticket de supermercado o factura de servicios. Fami IA desglosará productos, montos e IVA en segundos.",
                imageRes = R.drawable.fami_onboarding_3
            ),
            OnboardingStep(
                badge = "¡COMUNIDAD FAMI!",
                title = "El Hogar en Sincronía",
                description = "Invita a tu pareja o seres queridos con un código simple. Compartan presupuestos, tareas del hogar y listas de compras en tiempo real.",
                imageRes = R.drawable.fami_onboarding_4
            )
        )
    }

    val pagerState = rememberPagerState(pageCount = { steps.size })
    val coroutineScope = rememberCoroutineScope()
    var selectedCountry by remember { mutableStateOf(CountryCurrencyConfig.SUPPORTED_COUNTRIES.first()) }
    var showCountryDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PaperDeskBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Top Bar con Logo Oficial Fami + Botón 3D de País
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_fami_logo_wide),
                    contentDescription = "Fami Logo Oficial",
                    modifier = Modifier.height(28.dp),
                    contentScale = ContentScale.FillHeight
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Botón 3D Selector de País
                    Tactile3DButton(
                        onClick = { showCountryDialog = true },
                        faceColor = PaperStickyYellow,
                        lipColor = Lip3DGold,
                        contentColor = FamiNavy,
                        lipHeight = 3.dp,
                        cornerRadius = 10.dp,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(selectedCountry.flag, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "${selectedCountry.localCurrency}/${selectedCountry.defaultSecondary}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = FamiNavy
                        )
                    }

                    // Botón Omitir
                    if (pagerState.currentPage < steps.size - 1) {
                        Tactile3DButton(
                            onClick = { onComplete(selectedCountry) },
                            faceColor = PaperSheetWhite,
                            lipColor = Lip3DPaper,
                            contentColor = FamiNavy,
                            lipHeight = 3.dp,
                            cornerRadius = 10.dp,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("Omitir", color = FamiNavy, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Carousel Pager dentro de una Hoja de Cuaderno con Espiral
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                val step = steps[page]
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    NotebookPaperSheet(
                        modifier = Modifier.fillMaxWidth(),
                        paperColor = PaperSheetWhite,
                        showSpiralTop = true,
                        showRuledLines = true,
                        showMarginLine = true,
                        cornerRadius = 18.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 28.dp, end = 18.dp, top = 12.dp, bottom = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Etiqueta Adhesiva de Paso
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(PaperStickyPeach)
                                    .border(1.5.dp, FamiNavy, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "📓 ${step.badge}",
                                    color = FamiCoralDark,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.8.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Marco Polaroid de Papel para la Mascota Completa
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(230.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(PaperSheetCream)
                                    .border(1.8.dp, FamiNavy, RoundedCornerShape(16.dp))
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = step.imageRes),
                                    contentDescription = step.title,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Fit
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Título en Tinta Navy Oficial (#101535)
                            Text(
                                text = step.title,
                                color = FamiNavy,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Descripción sobre las líneas del cuaderno
                            Text(
                                text = step.description,
                                color = FamiNavyMuted,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 19.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Indicador de Páginas (Dots con borde de tinta)
            Row(
                modifier = Modifier.padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(steps.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(10.dp)
                            .width(if (isSelected) 26.dp else 10.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) FamiCoral else PaperSheetWhite)
                            .border(1.5.dp, FamiNavy, CircleShape)
                    )
                }
            }

            // Botón Táctil 3D Principal
            Tactile3DButton(
                onClick = {
                    if (pagerState.currentPage < steps.size - 1) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    } else {
                        onComplete(selectedCountry)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                faceColor = FamiCoral,
                lipColor = Lip3DCoral,
                contentColor = Color.White,
                lipHeight = 5.dp,
                cornerRadius = 14.dp,
                contentPadding = PaddingValues(vertical = 14.dp, horizontal = 20.dp)
            ) {
                Text(
                    text = if (pagerState.currentPage == steps.size - 1) "¡ABRIR MI CUADERNO FAMI!" else "SIGUIENTE HOJA",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.6.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = if (pagerState.currentPage == steps.size - 1) Icons.Default.Check else Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Modal Hoja de Papel para seleccionar País
        if (showCountryDialog) {
            AlertDialog(
                onDismissRequest = { showCountryDialog = false },
                title = {
                    Text("🌍 Selecciona tu País", fontWeight = FontWeight.Black, color = FamiNavy)
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                    ) {
                        Text(
                            "Fami adaptará la moneda de tu cuaderno y la tasa oficial en vivo a tu país:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = FamiNavyMuted
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        CountryCurrencyConfig.SUPPORTED_COUNTRIES.forEach { country ->
                            val isChosen = country.code == selectedCountry.code
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isChosen) PaperStickyPeach else PaperSheetCream)
                                    .border(
                                        width = if (isChosen) 2.dp else 1.2.dp,
                                        color = if (isChosen) FamiCoral else FamiNavy,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        selectedCountry = country
                                        showCountryDialog = false
                                    }
                                    .padding(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(country.flag, fontSize = 20.sp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(country.name, color = FamiNavy, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                                            Text("${country.localCurrency} • ${country.rateProvider}", color = FamiNavyMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                    if (isChosen) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = FamiCoral, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Tactile3DButton(
                        onClick = { showCountryDialog = false },
                        faceColor = FamiNavy,
                        lipColor = Lip3DNavy,
                        contentColor = Color.White,
                        lipHeight = 3.dp,
                        cornerRadius = 10.dp,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("LISTO", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                },
                containerColor = PaperSheetWhite,
                shape = RoundedCornerShape(18.dp)
            )
        }
    }
}
