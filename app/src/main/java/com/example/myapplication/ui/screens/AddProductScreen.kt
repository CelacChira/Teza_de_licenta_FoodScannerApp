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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.AppColors
import com.example.myapplication.ENumberInfo
import com.example.myapplication.ENumberManager
import com.example.myapplication.ImpactSanatate
import com.example.myapplication.Norme
import com.example.myapplication.ProductManager
import com.example.myapplication.SavedProduct
import com.example.myapplication.VerticalScrollbar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    eNumberManager: ENumberManager,
    productManager: ProductManager,
    colors: AppColors,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var productName by remember { mutableStateOf("") }
    var eNumberInput by remember { mutableStateOf("") }
    var foundENumbers by remember { mutableStateOf<List<Pair<String, ENumberInfo>>>(emptyList()) }
    val listState = rememberLazyListState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Adauga produs manual",
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
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = productName,
                onValueChange = { productName = it },
                label = { Text("Nume produs") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("ex: Ciocolata cu lapte") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.primary,
                    unfocusedBorderColor = colors.border,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = eNumberInput,
                onValueChange = { eNumberInput = it },
                label = { Text("E-uri (separate prin virgula)") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("ex: E100, E250, E621") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.primary,
                    unfocusedBorderColor = colors.border,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (productName.isBlank()) {
                        Toast.makeText(context, "Introdu numele produsului", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val foundNumbers = eNumberManager.findENumbersInText(eNumberInput)
                    foundENumbers = foundNumbers

                    if (foundNumbers.isNotEmpty()) {
                        val product = SavedProduct(
                            id = System.currentTimeMillis().toString(),
                            name = productName,
                            eNumbers = foundNumbers.map { it.first },
                            scanDate = productManager.getCurrentDateTime(),
                            riskLevel = eNumberManager.getOverallRisk(foundNumbers),
                            healthScore = eNumberManager.calculateHealthScore(foundNumbers)
                        )

                        productManager.saveProduct(product)
                        Toast.makeText(context, "Produs salvat: $productName", Toast.LENGTH_SHORT).show()
                        onBack()
                    } else {
                        Toast.makeText(context, "Nu s-au gasit E-uri valide", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Salveaza produs")
            }

            if (foundENumbers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "E-uri gasite: ${foundENumbers.size}",
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Box(modifier = Modifier.weight(1f)) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(foundENumbers) { (eNumber, info) ->
                            ENumberCard(
                                eNumber = eNumber,
                                info = info,
                                eNumberManager = eNumberManager,
                                colors = colors,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }

                    VerticalScrollbar(
                        listState = listState,
                        itemCount = foundENumbers.size
                    )
                }
            }
        }
    }
}

@Composable
fun ENumberCard(
    eNumber: String,
    info: ENumberInfo,
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
            ENumberHeader(eNumber, info, eNumberManager, colors)
            ENumberDetails(info, colors)
        }
    }
}

@Composable
fun ENumberHeader(
    eNumber: String,
    info: ENumberInfo,
    eNumberManager: ENumberManager,
    colors: AppColors
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = eNumber,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = colors.primary
        )

        Box(
            modifier = Modifier
                .background(
                    eNumberManager.getRiskColor(info.impactSanatate.risc),
                    RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = info.impactSanatate.risc.replaceFirstChar { it.uppercase() },
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    Text(
        text = info.nume,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        color = colors.textSecondary,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
fun ENumberDetails(info: ENumberInfo, colors: AppColors) {
    Column(modifier = Modifier.padding(top = 12.dp)) {
        DetailText("Categorie: ", info.categorie.replaceFirstChar { it.uppercase() }, colors)
        DetailText("Origine: ", info.origine.replaceFirstChar { it.uppercase() }, colors)

        InfoText("Utilizare: ${info.utilizare.joinToString(", ")}", colors)

        if (info.impactSanatate.efecteAdverse.isNotEmpty()) {
            InfoText("Efecte adverse: ${info.impactSanatate.efecteAdverse.joinToString(", ")}", colors)
        }

        if (info.impactSanatate.grupaVulnerabila.isNotEmpty()) {
            InfoText("Grupe vulnerabile: ${info.impactSanatate.grupaVulnerabila.joinToString(", ")}", colors)
        }

        InfoText("Doza zilnica: ${info.norme.dozaZilnicaAcceptabila}", colors)

        if (info.alternativa.isNotEmpty()) {
            Text(
                text = "Alternative: ${info.alternativa.joinToString(", ")}",
                fontSize = 14.sp,
                color = Color(0xFF22c55e),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun DetailText(label: String, value: String, colors: AppColors) {
    Text(
        text = buildAnnotatedString {
            withStyle(style = SpanStyle(fontWeight = FontWeight.Medium)) {
                append(label)
            }
            append(value)
        },
        color = colors.textSecondary,
        modifier = Modifier.padding(top = 2.dp)
    )
}

@Composable
fun InfoText(text: String, colors: AppColors) {
    Text(
        text = text,
        fontSize = 14.sp,
        color = colors.textSecondary,
        modifier = Modifier.padding(top = 8.dp)
    )
}
