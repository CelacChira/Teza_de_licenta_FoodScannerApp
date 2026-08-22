package com.example.myapplication

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.gson.annotations.SerializedName
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

// ==================== Data Classes ====================
data class OpenFoodFactsResponse(
    val status: Int,
    val product: OpenFoodFactsProduct?
)

data class OpenFoodFactsProduct(
    @SerializedName("product_name") val productName: String?,
    @SerializedName("brands") val brands: String?,
    @SerializedName("ingredients_text") val ingredientsText: String?,
    @SerializedName("additives_tags") val additivesTags: List<String>?,
    @SerializedName("image_url") val imageUrl: String?,
    @SerializedName("nutriscore_grade") val nutriscoreGrade: String?,
    @SerializedName("nova_group") val novaGroup: Int?,

    // Informații suplimentare
    @SerializedName("additives_original_tags") val additivesOriginalTags: List<String>?,
    @SerializedName("ingredients_from_palm_oil_tags") val palmOilIngredients: List<String>?,
    @SerializedName("allergens") val allergens: String?,
    @SerializedName("categories") val categories: String?,
    @SerializedName("ecoscore_grade") val ecoscoreGrade: String?,
    @SerializedName("quantity") val quantity: String?,
    @SerializedName("packaging") val packaging: String?
)

// ==================== Retrofit API ====================
interface OpenFoodFactsApi {
    @GET("api/v0/product/{barcode}.json")
    suspend fun getProduct(@Path("barcode") barcode: String): OpenFoodFactsResponse
}

object OpenFoodFactsClient {
    private const val BASE_URL = "https://world.openfoodfacts.org/"

    val api: OpenFoodFactsApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenFoodFactsApi::class.java)
    }
}

// ==================== Manager ====================
class OpenFoodFactsManager {
    suspend fun searchProductByBarcode(barcode: String): OpenFoodFactsProduct? {
        return try {
            val response = OpenFoodFactsClient.api.getProduct(barcode)
            if (response.status == 1) response.product else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

// ==================== Barcode Analyzer ====================
class BarcodeAnalyzer(
    private val onBarcodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {
    private val scanner = BarcodeScanning.getClient()

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        barcode.rawValue?.let { value ->
                            onBarcodeDetected(value)
                        }
                    }
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}

// ==================== Barcode Scanner Screen ====================
@Composable
fun BarcodeScannerScreen(
    onBarcodeScanned: (String) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var isProcessing by remember { mutableStateOf(false) }
    var detectedBarcode by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        BarcodeCameraPreview(
            cameraProviderFuture = cameraProviderFuture,
            lifecycleOwner = lifecycleOwner,
            onBarcodeDetected = { barcode ->
                if (!isProcessing && detectedBarcode.isEmpty()) {
                    detectedBarcode = barcode
                    isProcessing = true
                    onBarcodeScanned(barcode)
                }
            }
        )

        BarcodeScannerOverlay(
            detectedBarcode = detectedBarcode,
            onClose = onClose
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
fun BarcodeCameraPreview(
    cameraProviderFuture: com.google.common.util.concurrent.ListenableFuture<ProcessCameraProvider>,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    onBarcodeDetected: (String) -> Unit
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

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(executor, BarcodeAnalyzer { barcode ->
                            onBarcodeDetected(barcode)
                        })
                    }

                val cameraSelector = androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
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
fun BarcodeScannerOverlay(
    detectedBarcode: String,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Scanare Cod de Bare",
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

        Spacer(modifier = Modifier.weight(1f))

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
                Text(
                    text = if (detectedBarcode.isEmpty())
                        "Poziționați codul de bare în cadru"
                    else
                        "Cod detectat: $detectedBarcode",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

// ==================== OpenFoodFacts Product Screen ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenFoodFactsProductScreen(
    product: OpenFoodFactsProduct,
    colors: AppColors,
    onBack: () -> Unit,
    onSaveToMyProducts: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Informații Produs",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBackIosNew,
                            contentDescription = "Înapoi",
                            tint = colors.textPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSaveToMyProducts) {
                        Icon(
                            Icons.Default.Save,
                            contentDescription = "Salvează în lista mea",
                            tint = colors.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(colors.background)
        ) {
            // Header cu imagine și nume
            item {
                ProductHeaderSection(product, colors)
            }

            // Scoruri și rating-uri
            item {
                ProductScoresSection(product, colors)
            }

            // Informații de bază
            item {
                ProductBasicInfoSection(product, colors)
            }

            // Ingrediente
            item {
                IngredientsSection(product, colors)
            }

            // Aditivi (dacă există)
            item {
                AdditivesSection(product, colors)
            }

            // Informații nutriționale (dacă există)
            item {
                NutritionSection(product, colors)
            }
        }
    }
}

@Composable
fun ProductHeaderSection(product: OpenFoodFactsProduct, colors: AppColors) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = product.productName ?: "Produs fără nume",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )

            if (!product.brands.isNullOrEmpty()) {
                Text(
                    text = product.brands,
                    fontSize = 16.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (!product.quantity.isNullOrEmpty()) {
                Text(
                    text = product.quantity,
                    fontSize = 14.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
fun ProductScoresSection(product: OpenFoodFactsProduct, colors: AppColors) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Nutri-Score
            if (!product.nutriscoreGrade.isNullOrEmpty()) {
                ScoreItem(
                    label = "Nutri-Score",
                    score = product.nutriscoreGrade.uppercase(),
                    color = getNutriscoreColor(product.nutriscoreGrade),
                    colors = colors
                )
            }

            // NOVA Group (grad de procesare)
            if (product.novaGroup != null) {
                ScoreItem(
                    label = "Grad procesare",
                    score = "NOVA ${product.novaGroup}",
                    color = getNovaColor(product.novaGroup),
                    colors = colors
                )
            }

            // Eco-Score (dacă există)
            if (!product.ecoscoreGrade.isNullOrEmpty()) {
                ScoreItem(
                    label = "Eco-Score",
                    score = product.ecoscoreGrade.uppercase(),
                    color = getNutriscoreColor(product.ecoscoreGrade),
                    colors = colors
                )
            }
        }
    }
}

@Composable
fun ScoreItem(label: String, score: String, color: Color, colors: AppColors) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = colors.textSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .background(color, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = score,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ProductBasicInfoSection(product: OpenFoodFactsProduct, colors: AppColors) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Informații generale",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Categorii
            if (!product.categories.isNullOrEmpty()) {
                InfoRow("Categorii:", product.categories, colors)
            }

            // Ambalaj
            if (!product.packaging.isNullOrEmpty()) {
                InfoRow("Ambalaj:", product.packaging, colors)
            }

            // Alergeni
            if (!product.allergens.isNullOrEmpty()) {
                InfoRow("Alergeni:", product.allergens, colors, warning = true)
            }

            // Ingrediente din ulei de palmier
            if (!product.palmOilIngredients.isNullOrEmpty()) {
                InfoRow("Ulei de palmier:", "Da", colors, warning = true)
            }
        }
    }
}

@Composable
fun IngredientsSection(product: OpenFoodFactsProduct, colors: AppColors) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Ingrediente",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (!product.ingredientsText.isNullOrEmpty()) {
                Text(
                    text = product.ingredientsText,
                    fontSize = 14.sp,
                    color = colors.textSecondary,
                    lineHeight = 20.sp
                )
            } else {
                Text(
                    text = "Informații despre ingrediente indisponibile",
                    fontSize = 14.sp,
                    color = colors.textSecondary,
                    fontStyle = FontStyle.Italic
                )
            }
        }
    }
}

@Composable
fun AdditivesSection(product: OpenFoodFactsProduct, colors: AppColors) {
    val additives = product.additivesTags ?: emptyList()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Aditivi alimentari (${additives.size})",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (additives.isNotEmpty()) {
                additives.forEach { additive ->
                    Text(
                        text = "• $additive",
                        fontSize = 14.sp,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            } else {
                Text(
                    text = "Nu s-au găsit aditivi alimentari",
                    fontSize = 14.sp,
                    color = colors.textSecondary,
                    fontStyle = FontStyle.Italic
                )
            }
        }
    }
}

@Composable
fun NutritionSection(product: OpenFoodFactsProduct, colors: AppColors) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Informații nutriționale",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Scanând codul de bare, ai acces la informații complete despre conținutul nutrițional direct pe site-ul OpenFoodFacts.",
                fontSize = 14.sp,
                color = colors.textSecondary,
                fontStyle = FontStyle.Italic
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Afișează scorurile din nou pentru claritate
            if (!product.nutriscoreGrade.isNullOrEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Nutri-Score: ",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .background(
                                getNutriscoreColor(product.nutriscoreGrade),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = product.nutriscoreGrade.uppercase(),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String, colors: AppColors, warning: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.Medium,
            color = colors.textPrimary,
            modifier = Modifier.width(120.dp)
        )
        Text(
            text = value,
            color = if (warning) Color(0xFFEF4444) else colors.textSecondary,
            modifier = Modifier.weight(1f)
        )
    }
}

fun getNutriscoreColor(grade: String): Color {
    return when (grade.lowercase()) {
        "a" -> Color(0xFF22c55e)
        "b" -> Color(0xFF84cc16)
        "c" -> Color(0xFFeab308)
        "d" -> Color(0xFFf97316)
        "e" -> Color(0xFFef4444)
        else -> Color.Gray
    }
}

fun getNovaColor(novaGroup: Int): Color {
    return when (novaGroup) {
        1 -> Color(0xFF22C55E)  // Minim procesat
        2 -> Color(0xFF84CC16)  // Procesat culinar
        3 -> Color(0xFFEAB308)  // Procesat
        4 -> Color(0xFFEF4444)  // Ultra-procesat
        else -> Color.Gray
    }
}