package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.data.ProductEntity
import com.example.ui.StockViewModel
import com.example.ui.theme.*
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanBarcodeScreen(
    viewModel: StockViewModel,
    onNavigateToProductDetail: (ProductEntity) -> Unit,
    onNavigateToProductAddWithSku: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0 = Appareil Photo Réel, 1 = Simulateur Démo

    // Track camera scanning state
    var scannedSkuResult by remember { mutableStateOf("") }
    var matchStatusText by remember { mutableStateOf("") }
    var matchedProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var scanSuccess by remember { mutableStateOf(false) }

    // Floating laser height animation for viewfinder
    val infiniteTransition = rememberInfiniteTransition(label = "LaserPreview")
    val laserYOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 220f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LaserOffsetY"
    )

    // Camera permission check
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, "L'appareil photo est requis pour scanner en magasin.", Toast.LENGTH_LONG).show()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 1200.dp),
            topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "Scanner Code-Barres / QR", 
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ) 
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = SlateDark
                )
            )
        },
        containerColor = SlateDark
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // Premium styled Mode Selector Segmented Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SlateMedium)
                    .border(1.dp, SlateCardBorder, RoundedCornerShape(14.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selectedTab == 0) CyanNeon else Color.Transparent)
                        .clickable { selectedTab = 0 }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = if (selectedTab == 0) SlateDark else Color.LightGray,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "CAMÉRA LIVE",
                            color = if (selectedTab == 0) SlateDark else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selectedTab == 1) CyanNeon else Color.Transparent)
                        .clickable { selectedTab = 1 }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = null,
                            tint = if (selectedTab == 1) SlateDark else Color.LightGray,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "DÉMO / CLOUD",
                            color = if (selectedTab == 1) SlateDark else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            if (selectedTab == 0) {
                // REAL CAMERA SCAN MODE
                if (hasCameraPermission) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .border(3.dp, CyanNeon, RoundedCornerShape(24.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        // Live Camera feed
                        CameraPreviewView(
                            onBarcodeScanned = { rawCode ->
                                if (!scanSuccess) {
                                    scanSuccess = true
                                    scannedSkuResult = rawCode
                                    viewModel.pushNotification("Code-barres détecté par caméra : $rawCode")
                                    
                                    viewModel.simulateBarcodeScan(
                                        barcode = rawCode,
                                        onMatched = { prod ->
                                            matchedProduct = prod
                                            matchStatusText = "Produit trouvé dans votre inventaire !"
                                        },
                                        onNotFound = {
                                            matchedProduct = null
                                            matchStatusText = "Nouveau produit ! Enregistrez-le pour le stocker."
                                        }
                                    )
                                }
                            }
                        )

                        // Holo-Viewfinder laser effect overlay on top of camera
                        Box(
                            modifier = Modifier
                                .size(240.dp)
                                .border(2.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                        ) {
                            // Moving neon laser sweeper line
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .offset(y = laserYOffset.dp)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color.Transparent, CyanNeon, Color.Transparent)
                                        )
                                    )
                            )
                        }

                        // Status notification overlay
                        Text(
                            "Cadrez le code-barres dans le viseur",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 16.dp)
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )

                        // If successful scan overlay
                        if (scanSuccess) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.85f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Success",
                                        tint = EmeraldGlow,
                                        modifier = Modifier.size(60.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        "Code Identifié ! 🎉",
                                        color = EmeraldGlow,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp
                                    )
                                    Text(
                                        "SKU : $scannedSkuResult",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                    Text(
                                        matchStatusText,
                                        color = Color.LightGray,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    
                                    Spacer(modifier = Modifier.height(24.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                scanSuccess = false
                                                scannedSkuResult = ""
                                                matchedProduct = null
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = SlateDark),
                                            border = BorderStroke(1.dp, SlateCardBorder),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Scanner autre", color = Color.White)
                                        }

                                        if (matchedProduct != null) {
                                            Button(
                                                onClick = {
                                                    val product = matchedProduct!!
                                                    scanSuccess = false
                                                    scannedSkuResult = ""
                                                    matchedProduct = null
                                                    onNavigateToProductDetail(product)
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
                                                modifier = Modifier.weight(1.2f)
                                            ) {
                                                Text("Fiche Stock", color = SlateDark, fontWeight = FontWeight.Bold)
                                            }
                                        } else {
                                            Button(
                                                onClick = {
                                                    val code = scannedSkuResult
                                                    scanSuccess = false
                                                    scannedSkuResult = ""
                                                    matchedProduct = null
                                                    onNavigateToProductAddWithSku(code)
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
                                                modifier = Modifier.weight(1.2f)
                                            ) {
                                                Text("Créer Produit", color = SlateDark, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Camera permission display button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .border(1.dp, SlateCardBorder, RoundedCornerShape(24.dp))
                            .background(SlateMedium),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = CyanNeon,
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                "Autoriser la caméra du téléphone",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "L'autorisation d'accès à l'appareil photo est requise pour utiliser le scanner intelligent sur les articles réels.",
                                color = Color.LightGray,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = { launcher.launch(Manifest.permission.CAMERA) },
                                colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Activer la caméra", color = SlateDark, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                // SIMULATOR MODE
                val testScans = listOf(
                    Pair("8898990124", "iPhone 15 Pro (Déjà en stock)"),
                    Pair("CAF89112", "Café Robusta (Déjà en stock)"),
                    Pair("HUI54129", "Huile d'Argan (Déjà en stock)"),
                    Pair("NEW99411", "Nouveau Code Barre (Inconnu)")
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Test de détection en un clic",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CyanNeon
                    )

                    Text(
                        "Particulièrement utile lors des tests sur simulateur web cloud (sans caméra physique disponible) :",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                    )

                    // Dummy Viewfinder Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(SlateMedium)
                            .border(2.dp, CyanNeon.copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scope",
                            tint = Color.White.copy(alpha = 0.08f),
                            modifier = Modifier.size(100.dp)
                        )
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .offset(y = (-65).dp + (laserYOffset * 0.6f).dp)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color.Transparent, CyanNeon, Color.Transparent)
                                    )
                                )
                        )
                        
                        if (scanSuccess) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.8f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, "OK", tint = EmeraldGlow)
                                    Text("Produit scanné : $scannedSkuResult", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Test triggers list
                    testScans.forEach { (skuCode, label) ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scanSuccess = true
                                    scannedSkuResult = skuCode
                                    viewModel.simulateBarcodeScan(
                                        barcode = skuCode,
                                        onMatched = { prod ->
                                            matchedProduct = prod
                                            matchStatusText = "Produit identifié !"
                                        },
                                        onNotFound = {
                                            matchedProduct = null
                                            matchStatusText = "Code inconnu. Vous pouvez l'ajouter."
                                        }
                                    )
                                },
                            colors = CardDefaults.cardColors(containerColor = SlateMedium),
                            border = BorderStroke(1.dp, SlateCardBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.QrCode, null, tint = CyanNeon, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text("SKU: $skuCode", fontSize = 10.sp, color = Color.Gray)
                                    }
                                }
                                Icon(Icons.Default.ArrowForwardIos, null, tint = Color.LightGray, modifier = Modifier.size(12.dp))
                            }
                        }
                    }

                    // Results block
                    if (scanSuccess) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SlateDark),
                            border = BorderStroke(1.dp, CyanNeon)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    matchStatusText,
                                    color = if (matchedProduct != null) EmeraldGlow else AmberGlow,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                                
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            scanSuccess = false
                                            matchedProduct = null
                                            scannedSkuResult = ""
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SlateMedium),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Réinitialiser", color = Color.White)
                                    }

                                    if (matchedProduct != null) {
                                        Button(
                                            onClick = {
                                                val prod = matchedProduct!!
                                                scanSuccess = false
                                                matchedProduct = null
                                                scannedSkuResult = ""
                                                onNavigateToProductDetail(prod)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
                                            modifier = Modifier.weight(1.2f)
                                        ) {
                                            Text("Fiche Stock", color = SlateDark, fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        Button(
                                            onClick = {
                                                val sku = scannedSkuResult
                                                scanSuccess = false
                                                matchedProduct = null
                                                scannedSkuResult = ""
                                                onNavigateToProductAddWithSku(sku)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
                                            modifier = Modifier.weight(1.2f)
                                        ) {
                                            Text("Créer l'article", color = SlateDark, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    }
}

@SuppressLint("UnrememberedMutableState")
@Composable
fun CameraPreviewView(
    onBarcodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val executor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            executor.shutdown()
            try {
                if (cameraProviderFuture.isDone) {
                    val cameraProvider = cameraProviderFuture.get()
                    cameraProvider.unbindAll()
                }
            } catch (e: Exception) {
                Log.e("CameraScanner", "Error unbinding camera on dispose: ${e.localizedMessage}")
            }
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().apply {
                    surfaceProvider = previewView.surfaceProvider
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                val analyzer = BarcodeAnalyzer { barcode ->
                    if (barcode.isNotBlank()) {
                        // Deliver results on the UI Thread
                        previewView.post {
                            onBarcodeScanned(barcode)
                        }
                    }
                }

                imageAnalysis.setAnalyzer(executor, analyzer)

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    Log.e("CameraScanner", "Camera loading failed : ${e.localizedMessage}")
                }
            }, ContextCompat.getMainExecutor(ctx))
            
            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

class BarcodeAnalyzer(
    private val onBarcodeScanned: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_EAN_13,
            Barcode.FORMAT_EAN_8,
            Barcode.FORMAT_UPC_A,
            Barcode.FORMAT_UPC_E,
            Barcode.FORMAT_CODE_128,
            Barcode.FORMAT_CODE_39,
            Barcode.FORMAT_QR_CODE
        )
        .build()

    private val scanner = BarcodeScanning.getClient(options)
    private var isScanningActive = true

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null && isScanningActive) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty() && isScanningActive) {
                        barcodes.firstOrNull()?.rawValue?.let { codeValue ->
                            isScanningActive = false
                            onBarcodeScanned(codeValue)
                        }
                    }
                }
                .addOnFailureListener {
                    // Silence errors for standard sweep scanning
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}
