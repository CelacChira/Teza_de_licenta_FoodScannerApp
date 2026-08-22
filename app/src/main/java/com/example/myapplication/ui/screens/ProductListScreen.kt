package com.example.myapplication.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.platform.LocalContext
import com.example.myapplication.AppColors
import com.example.myapplication.ENumberManager
import com.example.myapplication.ProductManager
import com.example.myapplication.SavedProduct
import com.example.myapplication.VerticalScrollbar
import com.example.myapplication.HealthScoreBadge
import com.example.myapplication.HealthScoreBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
    productManager: ProductManager,
    eNumberManager: ENumberManager,
    colors: AppColors,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var savedProducts by remember { mutableStateOf<List<SavedProduct>>(emptyList()) }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        savedProducts = productManager.getSavedProducts()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Lista produselor (${savedProducts.size})",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Inapoi", tint = colors.textPrimary)
                    }
                },
                actions = {
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { expanded = true }) {
                            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sorteaza", tint = colors.textPrimary)
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Sortare dupa nume") },
                                onClick = {
                                    savedProducts = savedProducts.sortedBy { it.name }
                                    expanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Sortare dupa scor sanatate") },
                                onClick = {
                                    savedProducts = savedProducts.sortedByDescending { it.healthScore }
                                    expanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Sortare dupa risc") },
                                onClick = {
                                    val riskOrder = listOf("Foarte ridicat", "Ridicat", "Moderat", "Scazut", "Foarte scazut", "Necunoscut")
                                    savedProducts = savedProducts.sortedBy { riskOrder.indexOf(it.riskLevel) }
                                    expanded = false
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.surface
                )
            )
        }
    ) { innerPadding ->
        if (savedProducts.isEmpty()) {
            EmptyProductListView(Modifier.padding(innerPadding), colors)
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(colors.background)
            ) {
                ProductStatsSection(savedProducts, colors)

                FrequencyStatisticsCard(
                    productManager = productManager,
                    eNumberManager = eNumberManager,
                    products = savedProducts,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                Box(modifier = Modifier.weight(1f)) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(savedProducts) { product ->
                            EnhancedProductCard(
                                product = product,
                                eNumberManager = eNumberManager,
                                colors = colors,
                                onDelete = {
                                    productManager.deleteProduct(product.id)
                                    savedProducts = productManager.getSavedProducts()
                                    Toast.makeText(context, "Produs sters", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }

                    VerticalScrollbar(
                        listState = listState,
                        itemCount = savedProducts.size
                    )
                }
            }
        }
    }
}

@Composable
fun ProductStatsSection(products: List<SavedProduct>, colors: AppColors) {
    val averageScore = if (products.isNotEmpty()) {
        products.map { it.healthScore }.average().toInt()
    } else 0

    val riskDistribution = products.groupBy { it.riskLevel }
        .mapValues { it.value.size }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Statistici produse",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Scor mediu sanatate:", fontSize = 14.sp, color = colors.textPrimary)
                Text(
                    "$averageScore%",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        averageScore >= 80 -> Color(0xFF22c55e)
                        averageScore >= 60 -> Color(0xFF84cc16)
                        averageScore >= 40 -> Color(0xFFeab308)
                        averageScore >= 20 -> Color(0xFFf97316)
                        else -> Color(0xFFef4444)
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text("Distributie risc:", fontSize = 14.sp, color = colors.textPrimary)
            Spacer(modifier = Modifier.height(4.dp))

            riskDistribution.forEach { (risk, count) ->
                RiskDistributionItem(risk, count, products.size, colors)
            }
        }
    }
}

@Composable
fun FrequencyStatisticsCard(
    productManager: ProductManager,
    eNumberManager: ENumberManager,
    products: List<SavedProduct>,
    modifier: Modifier = Modifier
) {
    val warnings = remember(products, eNumberManager) {
        productManager.getFrequencyWarnings(eNumberManager)
    }

    if (warnings.isNotEmpty()) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFEF3C7)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "E-uri frecvente in produsele tale",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD97706),
                        fontSize = 14.sp
                    )
                }

                warnings.take(3).forEach { warning ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${warning.eNumber} - ${warning.eNumberInfo?.nume ?: ""}",
                            fontSize = 12.sp,
                            color = Color(0xFF92400E),
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .background(
                                    Color(0xFFF59E0B),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${warning.count}x",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RiskDistributionItem(risk: String, count: Int, total: Int, colors: AppColors) {
    val percentage = if (total > 0) (count * 100 / total) else 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = risk,
            fontSize = 12.sp,
            color = colors.textSecondary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "$count ($percentage%)",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = colors.textPrimary
        )
    }
}

@Composable
fun EmptyProductListView(modifier: Modifier = Modifier, colors: AppColors) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.ShoppingCart,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = colors.textSecondary
            )
            Text(
                text = "Nu ai produse salvate",
                fontSize = 18.sp,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

@Composable
fun EnhancedProductCard(
    product: SavedProduct,
    eNumberManager: ENumberManager,
    colors: AppColors,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = product.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )

                HealthScoreBadge(
                    score = product.healthScore,
                    eNumberManager = eNumberManager,
                    colors = colors
                )
            }

            Text(
                text = "Scanat: ${product.scanDate}",
                fontSize = 14.sp,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )

            HealthScoreBar(
                score = product.healthScore,
                eNumberManager = eNumberManager,
                colors = colors,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            )

            Text(
                text = "E-uri identificate:",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                product.eNumbers.forEach { eNumber ->
                    val riskLevel = eNumberManager.getENumberInfo(eNumber)?.impactSanatate?.risc
                        ?: "Necunoscut"

                    Box(
                        modifier = Modifier
                            .background(
                                eNumberManager.getRiskColor(riskLevel),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = eNumber,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Risc general: ",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textPrimary
                )
                Box(
                    modifier = Modifier
                        .background(
                            eNumberManager.getRiskColor(product.riskLevel),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = product.riskLevel,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE74C3C)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sterge produs")
            }
        }
    }
}
