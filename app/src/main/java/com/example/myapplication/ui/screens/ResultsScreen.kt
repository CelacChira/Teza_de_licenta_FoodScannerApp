package com.example.myapplication.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.AppColors
import com.example.myapplication.ENumberInfo
import com.example.myapplication.ENumberManager
import com.example.myapplication.FrequencyWarning
import com.example.myapplication.FrequencyWarningDialog
import com.example.myapplication.HealthScoreBar
import com.example.myapplication.ProductManager
import com.example.myapplication.SavedProduct
import com.example.myapplication.VerticalScrollbar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ENumberResultsScreenWithFrequency(
    eNumbers: List<Pair<String, ENumberInfo>>,
    eNumberManager: ENumberManager,
    productManager: ProductManager,
    colors: AppColors,
    onBack: () -> Unit,
    onSaveProduct: (String) -> Unit
) {
    var showSaveDialog by remember { mutableStateOf(false) }
    var showFrequencyWarning by remember { mutableStateOf(false) }
    var frequencyWarnings by remember { mutableStateOf<List<FrequencyWarning>>(emptyList()) }
    var productName by remember { mutableStateOf("") }
    val context = LocalContext.current
    val listState = rememberLazyListState()

    val healthScore = remember(eNumbers) {
        eNumberManager.calculateHealthScore(eNumbers)
    }

    if (showSaveDialog) {
        SaveProductDialog(
            productName = productName,
            colors = colors,
            onProductNameChange = { productName = it },
            onConfirm = {
                if (productName.isNotBlank()) {
                    val product = SavedProduct(
                        id = System.currentTimeMillis().toString(),
                        name = productName,
                        eNumbers = eNumbers.map { it.first },
                        scanDate = productManager.getCurrentDateTime(),
                        riskLevel = eNumberManager.getOverallRisk(eNumbers),
                        healthScore = eNumberManager.calculateHealthScore(eNumbers)
                    )

                    onSaveProduct(productName)

                    val newWarnings = productManager.checkForNewWarnings(product)
                    if (newWarnings.isNotEmpty()) {
                        frequencyWarnings = newWarnings.mapNotNull { eNumber ->
                            val info = eNumberManager.getENumberInfo(eNumber)
                            val count = productManager.getFrequencyWarnings(eNumberManager)
                                .find { it.eNumber == eNumber }?.count ?: 0
                            if (count > 0) FrequencyWarning(eNumber, count, info) else null
                        }
                        showFrequencyWarning = true
                    }

                    showSaveDialog = false
                } else {
                    Toast.makeText(context, "Introdu un nume pentru produs", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { showSaveDialog = false }
        )
    }

    if (showFrequencyWarning && frequencyWarnings.isNotEmpty()) {
        FrequencyWarningDialog(
            warnings = frequencyWarnings,
            colors = colors,
            onDismiss = {
                showFrequencyWarning = false
                onBack()
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Rezultate E-uri gasite (${eNumbers.size})",
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
                    IconButton(onClick = { showSaveDialog = true }) {
                        Icon(Icons.Default.Save, contentDescription = "Salveaza produs", tint = colors.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(colors.background)
        ) {
            HealthSummaryCard(
                eNumbers = eNumbers,
                healthScore = healthScore,
                eNumberManager = eNumberManager,
                colors = colors,
                modifier = Modifier.padding(16.dp)
            )

            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(eNumbers) { (eNumber, info) ->
                        ENumberCard(
                            eNumber = eNumber,
                            info = info,
                            eNumberManager = eNumberManager,
                            colors = colors,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                VerticalScrollbar(
                    listState = listState,
                    itemCount = eNumbers.size
                )
            }
        }
    }
}

@Composable
fun HealthSummaryCard(
    eNumbers: List<Pair<String, ENumberInfo>>,
    healthScore: Int,
    eNumberManager: ENumberManager,
    colors: AppColors,
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
            Text(
                text = "Rezumat sanatate",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Scor general sanatate",
                        fontSize = 14.sp,
                        color = colors.textSecondary
                    )
                    Text(
                        text = eNumberManager.getHealthScoreDescription(healthScore),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = eNumberManager.getHealthScoreColor(healthScore)
                    )
                }
                Text(
                    text = "$healthScore%",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = eNumberManager.getHealthScoreColor(healthScore)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            HealthScoreBar(
                score = healthScore,
                eNumberManager = eNumberManager,
                colors = colors
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
            ) {
                InfoItem("Total E-uri", eNumbers.size.toString(), colors)
                InfoItem("Risc", eNumberManager.getOverallRisk(eNumbers), colors)
                InfoItem(
                    "Recomandare",
                    if (healthScore >= 60) "Bun" else if (healthScore >= 40) "Moderat" else "Slab",
                    colors
                )
            }
        }
    }
}

@Composable
fun InfoItem(label: String, value: String, colors: AppColors) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = colors.textSecondary
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary
        )
    }
}

@Composable
fun SaveProductDialog(
    productName: String,
    colors: AppColors,
    onProductNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Salveaza produsul", color = colors.textPrimary) },
        text = {
            Column {
                Text("Introdu numele produsului:", color = colors.textPrimary)
                OutlinedTextField(
                    value = productName,
                    onValueChange = onProductNameChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    placeholder = { Text("ex: Ciocolata ") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.border,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Salveaza")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuleaza")
            }
        },
        containerColor = colors.cardBackground
    )
}
