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
            .background(FamiMidnight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Selector de País
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = FamiDarkSurfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, FamiDarkBorder),
                    modifier = Modifier.clickable { showCountryDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(selectedCountry.flag, fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${selectedCountry.localCurrency} / ${selectedCountry.defaultSecondary}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Botón Omitir
                if (pagerState.currentPage < steps.size - 1) {
                    TextButton(onClick = { onComplete(selectedCountry) }) {
                        Text("Omitir", color = DarkOnSurfaceVariant, fontSize = 13.sp)
                    }
                } else {
                    Spacer(modifier = Modifier.width(40.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Carousel Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                val step = steps[page]
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Ilustración de la Mascota Fami
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .height(260.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = FamiDarkSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, FamiDarkBorder)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = step.imageRes),
                                contentDescription = step.title,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                                    .clip(RoundedCornerShape(20.dp)),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Badge de Paso
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = FamiCoral.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, FamiCoral.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = step.badge,
                            color = FamiCoralLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Título
                    Text(
                        text = step.title,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Descripción
                    Text(
                        text = step.description,
                        color = DarkOnSurfaceVariant,
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }

            // Indicador de Páginas (Dots)
            Row(
                modifier = Modifier.padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(steps.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(8.dp)
                            .width(if (isSelected) 24.dp else 8.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) FamiCoral else FamiDarkBorder)
                    )
                }
            }

            // Botón de Acción Principal
            Button(
                onClick = {
                    if (pagerState.currentPage < steps.size - 1) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    } else {
                        onComplete(selectedCountry)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FamiCoral)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (pagerState.currentPage == steps.size - 1) "¡Comenzar con Fami!" else "Siguiente",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
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
        }

        // Modal para seleccionar País
        if (showCountryDialog) {
            AlertDialog(
                onDismissRequest = { showCountryDialog = false },
                title = {
                    Text("Selecciona tu País", fontWeight = FontWeight.Bold, color = Color.White)
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Text(
                            "Fami adaptará la moneda principal y la tasa oficial en vivo a tu país:",
                            fontSize = 13.sp,
                            color = DarkOnSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        CountryCurrencyConfig.SUPPORTED_COUNTRIES.forEach { country ->
                            val isChosen = country.code == selectedCountry.code
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isChosen) FamiCoral.copy(alpha = 0.15f) else FamiDarkSurface,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isChosen) FamiCoral else FamiDarkBorder
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        selectedCountry = country
                                        showCountryDialog = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(country.flag, fontSize = 20.sp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(country.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                            Text("${country.localCurrency} • ${country.rateProvider}", color = DarkOnSurfaceVariant, fontSize = 11.sp)
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
                    TextButton(onClick = { showCountryDialog = false }) {
                        Text("Cerrar", color = FamiCoralLight)
                    }
                },
                containerColor = FamiDarkSurfaceElevated,
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}
