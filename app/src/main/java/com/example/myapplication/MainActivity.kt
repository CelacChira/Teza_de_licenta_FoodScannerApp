package com.example.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.myapplication.ui.components.MainContent
import com.example.myapplication.ui.components.ScannerActionButtons
import com.example.myapplication.ui.screens.AddProductScreen
import com.example.myapplication.ui.screens.ProductListScreen
import com.example.myapplication.ui.screens.ENumberResultsScreenWithFrequency
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainScreen()
            }
        }
    }
}

// ==================== Theme Manager ====================
class ThemeManager(context: Context) {
    private val prefs = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)

    fun isDarkMode(): Boolean = prefs.getBoolean("dark_mode", false)

    fun toggleDarkMode() {
        prefs.edit { putBoolean("dark_mode", !isDarkMode()) }
    }
}

// ==================== Theme Colors ====================
data class AppColors(
    val background: Color,
    val surface: Color,
    val primary: Color,
    val secondary: Color,
    val onBackground: Color,
    val onSurface: Color,
    val cardBackground: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val border: Color
)

@Composable
fun getAppColors(isDarkMode: Boolean): AppColors {
    return if (isDarkMode) {
        AppColors(
            background = Color(0xFF0F172A),
            surface = Color(0xFF1E293B),
            primary = Color(0xFF22C55E),
            secondary = Color(0xFF334155),
            onBackground = Color.White,
            onSurface = Color.White,
            cardBackground = Color(0xFF1E293B),
            textPrimary = Color.White,
            textSecondary = Color(0xFF94A3B8),
            border = Color.White.copy(alpha = 0.12f)
        )
    } else {
        AppColors(
            background = Color(0xFFF8FAFC),
            surface = Color.White,
            primary = Color(0xFF22C55E),
            secondary = Color(0xFFE2E8F0),
            onBackground = Color(0xFF111827),
            onSurface = Color(0xFF111827),
            cardBackground = Color.White,
            textPrimary = Color(0xFF111827),
            textSecondary = Color(0xFF64748B),
            border = Color(0xFFE2E8F0)
        )
    }
}

// ==================== Data Models ====================
data class ENumberInfo(
    val nume: String,
    val categorie: String,
    val origine: String,
    val codInternational: String,
    val utilizare: List<String>,
    val impactSanatate: ImpactSanatate,
    val norme: Norme,
    val alternativa: List<String>
)

data class ImpactSanatate(
    val risc: String,
    val efecteAdverse: List<String>,
    val grupaVulnerabila: List<String>,
    val studiiStiintifice: String
)

data class Norme(
    val dozaZilnicaAcceptabila: String,
    val limitaUE: String,
    val statusFDA: String,
    val statusRomania: String
)

data class SavedProduct(
    val id: String,
    val name: String,
    val eNumbers: List<String>,
    val scanDate: String,
    val riskLevel: String,
    val healthScore: Int
)

class ENumberFrequencyManager(context: Context) {
    private val prefs = context.getSharedPreferences("enumber_frequency", Context.MODE_PRIVATE)

    // Pragul de frecvență peste care se afișează notificarea (ex: 3 produse)
    private val frequencythreshould = 3

    // Incrementează contorul pentru un E number
    fun incrementENumberCount(eNumber: String) {
        val currentCount = getENumberCount(eNumber)
        prefs.edit { putInt(eNumber, currentCount + 1) }
    }

    // Decrementează contorul când se șterge un produs
    fun decrementENumberCount(eNumber: String) {
        val currentCount = getENumberCount(eNumber)
        if (currentCount > 0) {
            prefs.edit { putInt(eNumber, currentCount - 1) }
        }
    }

    // Obține contorul pentru un E number
    fun getENumberCount(eNumber: String): Int {
        return prefs.getInt(eNumber, 0)
    }

    // Verifică dacă un E number depășește pragul
    fun checkFrequencyWarning(eNumber: String): Boolean {
        return getENumberCount(eNumber) >= frequencythreshould
    }

    // Obține toate E-urile cu frecvență peste prag
    fun getFrequentENumbers(): Map<String, Int> {
        return prefs.all
            .filterKeys { it.startsWith("E") }
            .mapValues { (it.value as? Int) ?: 0 }
            .filterValues { it >= frequencythreshould }
    }

}

// ==================== Managers ====================
class ProductManager(context: Context) {
    private val prefs = context.getSharedPreferences("saved_products", Context.MODE_PRIVATE)
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    private val frequencyManager = ENumberFrequencyManager(context)

    fun saveProduct(product: SavedProduct) {
        val products = getSavedProducts().toMutableList()
        products.add(product)
        saveProductList(products)

        // Incrementează contoarele pentru toate E-urile din produs
        product.eNumbers.forEach { eNumber ->
            frequencyManager.incrementENumberCount(eNumber)
        }
    }

    fun getSavedProducts(): List<SavedProduct> {
        val jsonString = prefs.getString("products", null) ?: return emptyList()
        return try {
            parseProductsFromJson(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun deleteProduct(productId: String) {
        val products = getSavedProducts()
        val productToDelete = products.find { it.id == productId }

        // Decrementează contoarele pentru E-urile din produsul șters
        productToDelete?.eNumbers?.forEach { eNumber ->
            frequencyManager.decrementENumberCount(eNumber)
        }

        val updatedProducts = products.filterNot { it.id == productId }
        saveProductList(updatedProducts)
    }

    // Metodă nouă pentru a obține avertismente de frecvență
    fun getFrequencyWarnings(eNumberManager: ENumberManager): List<FrequencyWarning> {
        return frequencyManager.getFrequentENumbers()
            .map { (eNumber, count) ->
                val info = eNumberManager.getENumberInfo(eNumber)
                FrequencyWarning(eNumber, count, info)
            }
            .sortedByDescending { it.count }
    }

    // Verifică dacă sunt avertismente noi după salvarea produsului
    fun checkForNewWarnings(product: SavedProduct): List<String> {
        return product.eNumbers.filter { eNumber ->
            frequencyManager.checkFrequencyWarning(eNumber)
        }
    }

    private fun parseProductsFromJson(jsonString: String): List<SavedProduct> {
        val jsonObject = JSONObject(jsonString)
        return jsonObject.keys().asSequence().map { key ->
            val productJson = jsonObject.getJSONObject(key)
            val eNumbersList = parseENumbers(productJson.getJSONObject("eNumbers"))

            SavedProduct(
                id = productJson.getString("id"),
                name = productJson.getString("name"),
                eNumbers = eNumbersList,
                scanDate = productJson.getString("scanDate"),
                riskLevel = productJson.getString("riskLevel"),
                healthScore = productJson.optInt("healthScore", 100)
            )
        }.toList()
    }

    private fun parseENumbers(eNumbersJson: JSONObject): List<String> {
        return eNumbersJson.keys().asSequence()
            .map { eNumbersJson.getString(it) }
            .toList()
    }

    private fun saveProductList(products: List<SavedProduct>) {
        val jsonObject = JSONObject()
        products.forEachIndexed { index, product ->
            jsonObject.put("product_$index", product.toJson())
        }
        prefs.edit { putString("products", jsonObject.toString()) }
    }

    private fun SavedProduct.toJson() = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("eNumbers", JSONObject().apply {
            eNumbers.forEachIndexed { i, eNumber ->
                put("eNumber_$i", eNumber)
            }
        })
        put("scanDate", scanDate)
        put("riskLevel", riskLevel)
        put("healthScore", healthScore)
    }

    fun getCurrentDateTime(): String = dateFormat.format(Date())
}

// Data class pentru notificarea de frecvență
data class FrequencyWarning(
    val eNumber: String,
    val count: Int,
    val eNumberInfo: ENumberInfo?
)
// Composable pentru afișarea dialogului de avertisment
@Composable
fun FrequencyWarningDialog(
    warnings: List<FrequencyWarning>,
    colors: AppColors,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Avertisment Frecvență E-uri", color = colors.textPrimary)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Următoarele E-uri apar frecvent în produsele tale:",
                    color = colors.textPrimary,
                    fontSize = 14.sp
                )

                warnings.forEach { warning ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFEF3C7)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = warning.eNumber,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD97706),
                                    fontSize = 16.sp
                                )
                                Box(
                                    modifier = Modifier
                                        .background(
                                            Color(0xFFF59E0B),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${warning.count}x",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            warning.eNumberInfo?.let { info ->
                                Text(
                                    text = info.nume,
                                    color = Color(0xFF92400E),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }

                Text(
                    "Consumul excesiv al acelorași aditivi poate avea efecte cumulative asupra sănătății.",
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp),
                    style = TextStyle(
                        fontStyle = FontStyle.Italic
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF59E0B)
                )
            ) {
                Text("Am înțeles")
            }
        },
        containerColor = colors.cardBackground
    )
}

class ENumberManager(private val context: Context) {
    private var eNumbersMap: Map<String, ENumberInfo> = emptyMap()
    private val eNumberPattern = "\\b[EЕ][\\s\\-–—]?\\d{3,4}[A-Z]{0,2}\\b".toRegex()

    suspend fun loadENumbers() {
        withContext(Dispatchers.IO) {
            try {
                val jsonString = context.assets.open("e_numbers.json")
                    .bufferedReader()
                    .use { it.readText() }
                eNumbersMap = parseENumbersFromJson(jsonString)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun parseENumbersFromJson(jsonString: String): Map<String, ENumberInfo> {
        val jsonObject = JSONObject(jsonString).getJSONObject("e_numbers")
        return jsonObject.keys().asSequence().associateWith { key ->
            parseENumberInfo(jsonObject.getJSONObject(key))
        }
    }

    private fun parseENumberInfo(json: JSONObject) = ENumberInfo(
        nume = json.optString("nume", "Necunoscut"),
        categorie = json.optString("categorie", "necunoscută"),
        origine = json.optString("origine", "necunoscută"),
        codInternational = json.optString("cod_international", ""),
        utilizare = json.optJSONArray("utilizare")?.toStringList() ?: emptyList(),
        impactSanatate = parseImpactSanatate(json.optJSONObject("impact_sanatate") ?: JSONObject()),
        norme = parseNorme(json.optJSONObject("norme") ?: JSONObject()),
        alternativa = json.optJSONArray("alternativa")?.toStringList() ?: emptyList()
    )

    private fun parseImpactSanatate(json: JSONObject) = ImpactSanatate(
        risc = json.optString("risc", "necunoscut"),
        efecteAdverse = json.optJSONArray("efecte_adverse")?.toStringList() ?: emptyList(),
        grupaVulnerabila = json.optJSONArray("grupa_vulnerabila")?.toStringList() ?: emptyList(),
        studiiStiintifice = json.optString("studii_stiintifice", "")
    )

    private fun parseNorme(json: JSONObject) = Norme(
        dozaZilnicaAcceptabila = json.optString("doza_zilnica_acceptabila", "nedisponibil"),
        limitaUE = json.optString("limita_ue", "nedisponibil"),
        statusFDA = json.optString("status_fda", "nedisponibil"),
        statusRomania = json.optString("status_romania", "nedisponibil")
    )

    private fun JSONArray.toStringList(): List<String> =
        (0 until length()).map { getString(it) }

    private fun normalizeENumber(value: String): String {
        return value
            .uppercase(Locale.ROOT)
            .replace('Е', 'E') // suportă litera E chirilică (vizual identică)
            .replace(Regex("[\\s\\-–—]"), "")
    }

    fun findENumbersInText(text: String): List<Pair<String, ENumberInfo>> {
        return eNumberPattern.findAll(text.uppercase(Locale.ROOT))
            .mapNotNull { match ->
                val eNumber = normalizeENumber(match.value)
                eNumbersMap[eNumber]?.let { eNumber to it }
            }
            .distinctBy { it.first }
            .toList()
    }

    fun getENumberInfo(eNumber: String): ENumberInfo? {
        return eNumbersMap[normalizeENumber(eNumber)]
    }

    fun getRiskColor(risk: String): Color = when (risk.lowercase()) {
        "foarte scăzut" -> Color(0xFF22c55e)
        "scăzut" -> Color(0xFF84cc16)
        "moderat" -> Color(0xFFeab308)
        "ridicat" -> Color(0xFFf97316)
        "foarte ridicat" -> Color(0xFFef4444)
        else -> Color.Gray
    }

    fun getOverallRisk(eNumbers: List<Pair<String, ENumberInfo>>): String {
        if (eNumbers.isEmpty()) return "Necunoscut"

        val riskLevels = eNumbers.map { it.second.impactSanatate.risc }
        return when {
            riskLevels.any { "foarte ridicat" in it.lowercase() } -> "Foarte ridicat"
            riskLevels.any { "ridicat" in it.lowercase() } -> "Ridicat"
            riskLevels.any { "moderat" in it.lowercase() } -> "Moderat"
            riskLevels.any { "scăzut" in it.lowercase() } -> "Scăzut"
            riskLevels.any { "foarte scăzut" in it.lowercase() } -> "Foarte scăzut"
            else -> "Necunoscut"
        }
    }

    fun calculateHealthScore(eNumbers: List<Pair<String, ENumberInfo>>): Int {
        if (eNumbers.isEmpty()) return 100

        var totalScore = 0
        var maxPossibleScore = 0

        eNumbers.forEach { (_, info) ->
            val riskScore = when (info.impactSanatate.risc.lowercase()) {
                "foarte scăzut" -> 90
                "scăzut" -> 70
                "moderat" -> 50
                "ridicat" -> 30
                "foarte ridicat" -> 10
                else -> 50
            }

            val adverseEffectsCount = info.impactSanatate.efecteAdverse.size
            val adjustment = when {
                adverseEffectsCount >= 3 -> 0.7f
                adverseEffectsCount == 2 -> 0.8f
                adverseEffectsCount == 1 -> 0.9f
                else -> 1.0f
            }

            totalScore += (riskScore * adjustment).toInt()
            maxPossibleScore += 100
        }

        return if (maxPossibleScore > 0) {
            (totalScore.toFloat() / maxPossibleScore * 100).toInt().coerceIn(0, 100)
        } else {
            100
        }
    }

    fun getHealthScoreColor(score: Int): Color {
        return when {
            score >= 80 -> Color(0xFF22c55e)
            score >= 60 -> Color(0xFF84cc16)
            score >= 40 -> Color(0xFFeab308)
            score >= 20 -> Color(0xFFf97316)
            else -> Color(0xFFef4444)
        }
    }

    fun getHealthScoreDescription(score: Int): String {
        return when {
            score >= 80 -> "Excelent"
            score >= 60 -> "Bun"
            score >= 40 -> "Moderat"
            score >= 20 -> "Slab"
            else -> "Nesănătos"
        }
    }
}

// ==================== UI State ====================
enum class Screen {
    MAIN, SCANNER, RESULTS, ADD_PRODUCT, PRODUCT_LIST, BARCODE_SCANNER, OFF_RESULTS
}

// ==================== Main Screen ====================
@OptIn(ExperimentalMaterial3Api::class, DelicateCoroutinesApi::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val themeManager = remember { ThemeManager(context) }
    var isDarkMode by remember { mutableStateOf(themeManager.isDarkMode()) }
    val colors = getAppColors(isDarkMode)

    var openFoodFactsProduct by remember { mutableStateOf<OpenFoodFactsProduct?>(null) }

    var currentScreen by remember { mutableStateOf(Screen.MAIN) }
    var eNumberInput by remember { mutableStateOf("") }
    var scannedENumbers by remember { mutableStateOf<List<Pair<String, ENumberInfo>>>(emptyList()) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
        )
    }

    val eNumberManager = remember { ENumberManager(context) }
    val productManager = remember { ProductManager(context) }
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isLoading = true
        eNumberManager.loadENumbers()
        isLoading = false
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            currentScreen = Screen.SCANNER
        } else {
            Toast.makeText(context, "Permisiunea camerei este necesară", Toast.LENGTH_SHORT).show()
        }
    }

    when (currentScreen) {
        Screen.SCANNER -> {
            if (hasCameraPermission) {
                OCRScannerScreen(
                    onTextScanned = { scannedText ->
                        val foundENumbers = eNumberManager.findENumbersInText(scannedText)
                        if (foundENumbers.isNotEmpty()) {
                            scannedENumbers = foundENumbers
                            currentScreen = Screen.RESULTS
                        } else {
                            Toast.makeText(context, "Nu s-au găsit E-uri în text", Toast.LENGTH_LONG).show()
                            currentScreen = Screen.MAIN
                        }
                    },
                    onClose = { currentScreen = Screen.MAIN }
                )
            }
        }
        Screen.RESULTS -> {
            ENumberResultsScreenWithFrequency(  // <- Schimbă numele aici
                eNumbers = scannedENumbers,
                eNumberManager = eNumberManager,
                productManager = productManager,  // <- Adaugă acest parametru
                colors = colors,
                onBack = { currentScreen = Screen.MAIN },
                onSaveProduct = { productName ->
                    val product = SavedProduct(
                        id = System.currentTimeMillis().toString(),
                        name = productName,
                        eNumbers = scannedENumbers.map { it.first },
                        scanDate = productManager.getCurrentDateTime(),
                        riskLevel = eNumberManager.getOverallRisk(scannedENumbers),
                        healthScore = eNumberManager.calculateHealthScore(scannedENumbers)
                    )
                    productManager.saveProduct(product)
                    Toast.makeText(context, "Produs salvat: $productName", Toast.LENGTH_SHORT).show()
                }
            )
        }
        Screen.ADD_PRODUCT -> {
            AddProductScreen(
                eNumberManager = eNumberManager,
                productManager = productManager,
                colors = colors,
                onBack = { currentScreen = Screen.MAIN }
            )
        }
        Screen.PRODUCT_LIST -> {
            ProductListScreen(
                productManager = productManager,
                eNumberManager = eNumberManager,
                colors = colors,
                onBack = { currentScreen = Screen.MAIN }
            )
        }
        Screen.BARCODE_SCANNER -> {
            if (hasCameraPermission) {
                BarcodeScannerScreen(
                    onBarcodeScanned = { barcode ->
                        isLoading = true
                        coroutineScope.launch {
                            val offManager = OpenFoodFactsManager()
                            val product = withContext(Dispatchers.IO) {
                                offManager.searchProductByBarcode(barcode)
                            }

                            isLoading = false
                            if (product != null) {
                                openFoodFactsProduct = product
                                currentScreen = Screen.OFF_RESULTS
                            } else {
                                Toast.makeText(context, "Produs negăsit în OpenFoodFacts", Toast.LENGTH_LONG).show()
                                currentScreen = Screen.MAIN
                            }
                        }
                    },
                    onClose = { currentScreen = Screen.MAIN }
                )
            }
        }

        Screen.OFF_RESULTS -> {
            openFoodFactsProduct?.let { product ->
                OpenFoodFactsProductScreen(
                    product = product,
                    colors = colors,
                    onBack = { currentScreen = Screen.MAIN },
                    onSaveToMyProducts = {
                        val foundENumbers = eNumberManager.findENumbersInText(product.ingredientsText ?: "")
                        if (foundENumbers.isNotEmpty()) {
                            val savedProduct = SavedProduct(
                                id = System.currentTimeMillis().toString(),
                                name = product.productName ?: "Produs OpenFoodFacts",
                                eNumbers = foundENumbers.map { it.first },
                                scanDate = productManager.getCurrentDateTime(),
                                riskLevel = eNumberManager.getOverallRisk(foundENumbers),
                                healthScore = eNumberManager.calculateHealthScore(foundENumbers)
                            )
                            productManager.saveProduct(savedProduct)
                            Toast.makeText(context, "Produs salvat în lista ta", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Nu s-au găsit E-uri pentru a salva", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            } ?: run {
            }
        }
        Screen.MAIN -> {
            val recentProducts = productManager.getSavedProducts().asReversed().take(6)
            MainContentScreen(
                isLoading = isLoading,
                eNumberInput = eNumberInput,
                recentProducts = recentProducts,
                colors = colors,
                isDarkMode = isDarkMode,
                onThemeToggle = {
                    themeManager.toggleDarkMode()
                    isDarkMode = !isDarkMode
                },
                onENumberInputChange = { eNumberInput = it },
                onScanClick = {
                    if (hasCameraPermission) {
                        currentScreen = Screen.SCANNER
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                onSearchClick = {
                    if (eNumberInput.isNotEmpty()) {
                        val foundENumbers = eNumberManager.findENumbersInText(eNumberInput)
                        if (foundENumbers.isNotEmpty()) {
                            scannedENumbers = foundENumbers
                            currentScreen = Screen.RESULTS
                        } else {
                            Toast.makeText(context, "Nu s-au găsit E-uri în textul introdus", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Introduceți E-urile", Toast.LENGTH_SHORT).show()
                    }
                },
                onAddProductClick = { currentScreen = Screen.ADD_PRODUCT },
                onProductListClick = { currentScreen = Screen.PRODUCT_LIST },
                onBarcodeScanClick = {
                    if (hasCameraPermission) {
                        currentScreen = Screen.BARCODE_SCANNER
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }
            )
        }
    }
}

@Composable
fun MainContentScreen(
    isLoading: Boolean,
    eNumberInput: String,
    recentProducts: List<SavedProduct>,
    colors: AppColors,
    isDarkMode: Boolean,
    onThemeToggle: () -> Unit,
    onENumberInputChange: (String) -> Unit,
    onScanClick: () -> Unit,
    onSearchClick: () -> Unit,
    onAddProductClick: () -> Unit,
    onProductListClick: () -> Unit,
    onBarcodeScanClick: () -> Unit
) {
    Scaffold(
        topBar = {
            MainHeader(
                colors = colors,
                isDarkMode = isDarkMode,
                onThemeToggle = onThemeToggle
            )
        },
        bottomBar = {
            BottomNavigationBar(
                colors = colors,
                onAddProductClick = onAddProductClick,
                onProductListClick = onProductListClick
            )
        },
        containerColor = colors.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(colors.background)
        ) {
            if (isLoading) {
                LoadingIndicator()
            } else {
                MainContent(
                    eNumberInput = eNumberInput,
                    colors = colors,
                    isDarkMode = isDarkMode,
                    recentProducts = recentProducts,
                    onENumberInputChange = onENumberInputChange,
                    onScanClick = onScanClick,
                    onSearchClick = onSearchClick,
                    onBarcodeScanClick = onBarcodeScanClick
                )
            }
        }
    }
}

@Composable
fun BottomNavigationBar(
    colors: AppColors,
    onAddProductClick: () -> Unit,
    onProductListClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        shape = RoundedCornerShape(22.dp),
        color = colors.cardBackground,
        tonalElevation = 2.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            BottomTabItem(
                label = "Adaugă produs",
                icon = Icons.Default.AddCircle,
                isActive = true,
                colors = colors,
                modifier = Modifier.weight(1f),
                onClick = onAddProductClick
            )
            BottomTabItem(
                label = "Lista mea",
                icon = Icons.Default.CheckBox,
                isActive = false,
                colors = colors,
                modifier = Modifier.weight(1f),
                onClick = onProductListClick
            )
        }
    }
}

@Composable
fun MainHeader(
    colors: AppColors,
    isDarkMode: Boolean,
    onThemeToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.background)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Scanarea produselor",
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary
        )

        Box(
            modifier = Modifier
                .size(42.dp)
                .border(1.dp, colors.border, RoundedCornerShape(50))
                .background(colors.cardBackground, RoundedCornerShape(50))
                .clickable(onClick = onThemeToggle),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                contentDescription = "Schimba tema",
                tint = colors.textPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun BottomTabItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    colors: AppColors,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val activeBackground = if (isActive) colors.primary.copy(alpha = 0.16f) else Color.Transparent
    val iconAndTextColor = if (isActive) colors.primary else colors.textSecondary

    Column(
        modifier = modifier
            .background(
                activeBackground,
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconAndTextColor,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            color = iconAndTextColor
        )
    }
}

@Composable
fun LoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

// ==================== Scrollbar Component ====================
@SuppressLint("FrequentlyChangingValue")
@Composable
fun BoxScope.VerticalScrollbar(
    listState: LazyListState,
    itemCount: Int
) {
    if (itemCount == 0) return

    val scrollbarAlpha by remember {
        derivedStateOf {
            if (listState.isScrollInProgress) 1f else 0.4f
        }
    }

    Box(
        modifier = Modifier
            .align(Alignment.CenterEnd)
            .fillMaxHeight()
            .width(8.dp)
            .padding(vertical = 4.dp)
    ) {
        val firstVisibleItemIndex = listState.firstVisibleItemIndex
        val firstVisibleItemScrollOffset = listState.firstVisibleItemScrollOffset

        val scrollbarHeight = remember(itemCount) {
            if (itemCount > 0) (1f / itemCount).coerceIn(0.1f, 1f) else 0f
        }

        val scrollbarOffsetY = remember(firstVisibleItemIndex, firstVisibleItemScrollOffset, itemCount) {
            if (itemCount > 0) {
                (firstVisibleItemIndex.toFloat() / itemCount).coerceIn(0f, 1f - scrollbarHeight)
            } else 0f
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .fillMaxHeight(scrollbarHeight)
                .fillMaxWidth()
                .offset(y = with(LocalDensity.current) {
                    (scrollbarOffsetY * (listState.layoutInfo.viewportSize.height - (scrollbarHeight * listState.layoutInfo.viewportSize.height))).toDp()
                })
                .background(
                    color = Color.Gray.copy(alpha = scrollbarAlpha),
                    shape = RoundedCornerShape(4.dp)
                )
        )
    }
}

@Composable
fun HealthScoreBadge(
    score: Int,
    eNumberManager: ENumberManager,
    colors: AppColors,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .background(
                    eNumberManager.getHealthScoreColor(score),
                    RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = "$score%",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = eNumberManager.getHealthScoreDescription(score),
            fontSize = 10.sp,
            color = colors.textSecondary,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
fun HealthScoreBar(
    score: Int,
    eNumberManager: ENumberManager,
    colors: AppColors,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Scor sănătate",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary
            )
            Text(
                text = "${eNumberManager.getHealthScoreDescription(score)} ($score%)",
                fontSize = 12.sp,
                color = colors.textSecondary
            )
        }

        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(colors.border, RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(score / 100f)
                    .height(8.dp)
                    .background(
                        eNumberManager.getHealthScoreColor(score),
                        RoundedCornerShape(4.dp)
                    )
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("0%", fontSize = 10.sp, color = colors.textSecondary)
            Text("50%", fontSize = 10.sp, color = colors.textSecondary)
            Text("100%", fontSize = 10.sp, color = colors.textSecondary)
        }
    }
}


// ==================== OCR Scanner Screen ====================
@Composable
fun OCRScannerScreen(
    onTextScanned: (String) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var isProcessing by remember { mutableStateOf(false) }
    var detectedText by remember { mutableStateOf("") }
    val imageCapture = remember { ImageCapture.Builder().build() }

    Box(modifier = Modifier.fillMaxSize()) {
        CameraPreview(
            cameraProviderFuture = cameraProviderFuture,
            lifecycleOwner = lifecycleOwner,
            imageCapture = imageCapture
        )

        ScannerOverlay(
            detectedText = detectedText,
            isProcessing = isProcessing,
            onClose = onClose,
            onCapture = {
                if (isProcessing) return@ScannerOverlay
                isProcessing = true
                captureAndProcessImage(context, imageCapture) { text ->
                    detectedText = text
                    isProcessing = false
                    if (text.isEmpty()) {
                        Toast.makeText(context, "Nu s-a detectat text.", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onAnalyze = {
                if (detectedText.isNotEmpty()) {
                    onTextScanned(detectedText)
                } else {
                    Toast.makeText(context, "Scanează mai întâi", Toast.LENGTH_SHORT).show()
                }
            }
        )

        if (isProcessing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }
    }
}

@Composable
fun CameraPreview(
    cameraProviderFuture: ListenableFuture<ProcessCameraProvider>,
    lifecycleOwner: LifecycleOwner,
    imageCapture: ImageCapture
) {
    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val executor = ContextCompat.getMainExecutor(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, executor)

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun ScannerOverlay(
    detectedText: String,
    isProcessing: Boolean,
    onClose: () -> Unit,
    onCapture: () -> Unit,
    onAnalyze: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ScannerTopBar(onClose)
        Spacer(modifier = Modifier.weight(1f))
        ScanningFrame()
        Spacer(modifier = Modifier.height(24.dp))

        if (detectedText.isNotEmpty()) {
            DetectedTextPreview(detectedText)
            Spacer(modifier = Modifier.height(16.dp))
        }

        ScannerActionButtons(
            isProcessing = isProcessing,
            hasDetectedText = detectedText.isNotEmpty(),
            onCapture = onCapture,
            onAnalyze = onAnalyze
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun ScannerTopBar(onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Scanare Ingrediente",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        IconButton(
            onClick = onClose,
            modifier = Modifier.background(Color.White, RoundedCornerShape(50))
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Închide",
                tint = Color.Black
            )
        }
    }
}

@Composable
fun ScanningFrame() {
    Box(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .height(200.dp)
            .background(
                Color.White.copy(alpha = 0.1f),
                RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "Poziționați lista și apăsați Capturează",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun DetectedTextPreview(text: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .heightIn(max = 150.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.9f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Text detectat:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C3E50)
            )
            Text(
                text = text.take(200) + if (text.length > 200) "..." else "",
                fontSize = 12.sp,
                color = Color(0xFF4A5568),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

// ==================== Helper Functions ====================
private fun captureAndProcessImage(
    context: Context,
    imageCapture: ImageCapture,
    onTextDetected: (String) -> Unit
) {
    val outputFile = File.createTempFile("ocr_image", ".jpg", context.cacheDir)
    val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                processImageForOCR(context, outputFile.absolutePath, onTextDetected)
            }

            override fun onError(exception: ImageCaptureException) {
                exception.printStackTrace()
                Toast.makeText(context, "Eroare la captură: ${exception.message}", Toast.LENGTH_SHORT).show()
                onTextDetected("")
            }
        }
    )
}

private fun processImageForOCR(
    context: Context,
    imagePath: String,
    onTextDetected: (String) -> Unit
) {
    val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    val image = InputImage.fromFilePath(context, Uri.fromFile(File(imagePath)))

    textRecognizer.process(image)
        .addOnSuccessListener { visionText ->
            onTextDetected(visionText.text)
        }
        .addOnFailureListener { e ->
            e.printStackTrace()
            onTextDetected("")
        }
}

