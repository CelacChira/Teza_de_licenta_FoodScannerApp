package com.example.myapplication.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.AppColors
import com.example.myapplication.SavedProduct

@Composable
fun MainContent(
    eNumberInput: String,
    colors: AppColors,
    isDarkMode: Boolean,
    recentProducts: List<SavedProduct>,
    onENumberInputChange: (String) -> Unit,
    onScanClick: () -> Unit,
    onSearchClick: () -> Unit,
    onBarcodeScanClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            HeroSection(colors = colors, isDarkMode = isDarkMode)
        }

        item {
            PremiumActionButton(
                text = "Scanează ingrediente",
                colors = colors,
                isDarkMode = isDarkMode,
                gradient = if (isDarkMode) {
                    listOf(Color(0xFF22C55E), Color(0xFF16A34A))
                } else {
                    listOf(Color(0xFF34D399), Color(0xFF22C55E))
                },
                iconContent = {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = if (isDarkMode) Color.White else Color(0xFF0F172A),
                        modifier = Modifier.size(22.dp)
                    )
                },
                onClick = onScanClick
            )
        }

        item {
            PremiumActionButton(
                text = "Scanează cod de bare",
                colors = colors,
                isDarkMode = isDarkMode,
                gradient = if (isDarkMode) {
                    listOf(Color(0xFF334155), Color(0xFF1E293B))
                } else {
                    listOf(Color(0xFFE2E8F0), Color.White)
                },
                iconContent = {
                    BarcodeMiniIcon(
                        color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                    )
                },
                onClick = onBarcodeScanClick
            )
        }

        item {
            ManualInputCard(
                eNumberInput = eNumberInput,
                colors = colors,
                isDarkMode = isDarkMode,
                onENumberInputChange = onENumberInputChange,
                onSearchClick = onSearchClick
            )
        }

        item {
            RecentScansSection(
                colors = colors,
                products = recentProducts
            )
        }
    }
}

@Composable
private fun HeroSection(
    colors: AppColors,
    isDarkMode: Boolean
) {
    val heroGradient = if (isDarkMode) {
        Brush.linearGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A)))
    } else {
        Brush.linearGradient(listOf(Color.White, Color(0xFFF1F5F9)))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .background(heroGradient)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Scanează rapid produsele și verifică ingredientele",
                color = colors.textPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 26.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HeroBadge(icon = Icons.Default.Restaurant, label = "Food", isDarkMode = isDarkMode)
                HeroBarcodeBadge(isDarkMode = isDarkMode)
                HeroBadge(icon = Icons.Default.Favorite, label = "Health", isDarkMode = isDarkMode)
            }
        }
    }
}

@Composable
private fun HeroBadge(
    icon: ImageVector,
    label: String,
    isDarkMode: Boolean
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isDarkMode) Color.White.copy(alpha = 0.08f) else Color(0xFFECFDF5)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF22C55E),
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                color = if (isDarkMode) Color.White else Color(0xFF047857),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun HeroBarcodeBadge(isDarkMode: Boolean) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isDarkMode) Color.White.copy(alpha = 0.08f) else Color(0xFFECFDF5)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            BarcodeMiniIcon(color = Color(0xFF22C55E))
            Text(
                text = "Barcode",
                color = if (isDarkMode) Color.White else Color(0xFF047857),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun PremiumActionButton(
    text: String,
    colors: AppColors,
    isDarkMode: Boolean,
    gradient: List<Color>,
    iconContent: @Composable () -> Unit,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.7f),
        label = "buttonScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .scale(scale)
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.horizontalGradient(gradient))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            iconContent()
            Text(
                text = text,
                color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun BarcodeMiniIcon(color: Color) {
    val bars = listOf(14.dp, 14.dp, 14.dp, 10.dp, 12.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        bars.forEach { barHeight ->
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(barHeight)
                    .background(color, RoundedCornerShape(1.dp))
            )
        }
    }
}

@Composable
fun ManualInputCard(
    eNumberInput: String,
    colors: AppColors,
    isDarkMode: Boolean,
    onENumberInputChange: (String) -> Unit,
    onSearchClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Caută manual E-uri",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )

            TextField(
                value = eNumberInput,
                onValueChange = onENumberInputChange,
                placeholder = { Text("E100, E250...", color = colors.textSecondary) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = if (isDarkMode) Color(0xFF334155) else Color(0xFFF1F5F9),
                    unfocusedContainerColor = if (isDarkMode) Color(0xFF334155) else Color(0xFFF1F5F9),
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = Color(0xFF22C55E)
                )
            )

            Button(
                onClick = onSearchClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF22C55E),
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 1.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Caută acum", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun RecentScansSection(
    colors: AppColors,
    products: List<SavedProduct>
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Produse recente",
            color = colors.textPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )

        if (products.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
            ) {
                Text(
                    text = "Nu ai scanări recente încă.",
                    modifier = Modifier.padding(16.dp),
                    color = colors.textSecondary
                )
            }
        } else {
            products.take(4).forEach { product ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
                    border = BorderStroke(1.dp, colors.border.copy(alpha = 0.6f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = product.name,
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                maxLines = 1
                            )
                            Text(
                                text = product.scanDate,
                                color = colors.textSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF22C55E).copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = product.riskLevel,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                color = Color(0xFF15803D),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScannerActionButtons(
    isProcessing: Boolean,
    hasDetectedText: Boolean,
    onCapture: () -> Unit,
    onAnalyze: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onCapture,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
            enabled = !isProcessing
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Captureaza",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Button(
            onClick = onAnalyze,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
            enabled = hasDetectedText && !isProcessing
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Analizeaza",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
