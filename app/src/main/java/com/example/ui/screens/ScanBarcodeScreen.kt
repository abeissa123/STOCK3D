package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ProductEntity
import com.example.ui.StockViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanBarcodeScreen(
    viewModel: StockViewModel,
    onNavigateToProductDetail: (ProductEntity) -> Unit,
    onNavigateToProductAddWithSku: (String) -> Unit
) {
    var scannedSkuResult by remember { mutableStateOf("") }
    var matchStatusText by remember { mutableStateOf("") }
    var matchedProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var scanSuccess by remember { mutableStateOf(false) }

    // Floating line laser animation loop
    val infiniteTransition = rememberInfiniteTransition(label = "Laser")
    val laserYOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 220f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LaserOffsetY"
    )

    // Test QR / Barcodes available for simulation
    val testScans = listOf(
        Pair("8898990124", "iPhone 15 Pro (Déjà en stock)"),
        Pair("CAF89112", "Café Robusta (Déjà en stock)"),
        Pair("HUI54129", "Huile d'Argan (Déjà en stock)"),
        Pair("NEW99411", "Nouveau Code Barre (Inconnu)")
    )

    Scaffold(
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
            Text(
                "Simulateur de Scan Intelligent",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CyanNeon
            )
            
            Text(
                "Puisqu'aucun capteur physique n'est disponible sur le simulateur cloud, sélectionnez un code test ci-dessous pour déclencher instantanément le moteur de détection de stock :",
                color = Color.LightGray,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // Sci-fi Viewfinder Container
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(SlateMedium)
                    .border(3.dp, CyanNeon, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Outer corners indicators
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    // Draw simulated QR scope
                    Icon(
                        Icons.Default.QrCodeScanner,
                        contentDescription = "Scope",
                        tint = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Pulsing laser sweep line
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .offset(y = (-110).dp + laserYOffset.dp)
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
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.CheckCircle, "Succès", tint = EmeraldGlow, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Scan Réussi", color = EmeraldGlow, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("SKU : $scannedSkuResult", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Test scan trigger buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Simuler un scan de code-barre :",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )

                testScans.forEach { (skuCode, label) ->
                    Button(
                        onClick = {
                            scanSuccess = true
                            scannedSkuResult = skuCode
                            // Query Database via ViewModel
                            viewModel.simulateBarcodeScan(
                                barcode = skuCode,
                                onMatched = { prod ->
                                    matchedProduct = prod
                                    matchStatusText = "Produit identifié dans le catalogue."
                                },
                                onNotFound = {
                                    matchedProduct = null
                                    matchStatusText = "Code inconnu. Enregistrez cet article pour l'ajouter au stock."
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SlateMedium,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SlateCardBorder, RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.QrCode, "Code", tint = CyanNeon, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("SKU: $skuCode", fontSize = 10.sp, color = Color.Gray)
                                }
                            }
                            Icon(Icons.Default.ArrowForwardIos, "Détail", tint = Color.LightGray, modifier = Modifier.size(12.dp))
                        }
                    }
                }
            }

            // Results Display block
            if (scanSuccess) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateMedium),
                    border = BorderStroke(1.dp, SlateCardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            matchStatusText,
                            color = if (matchedProduct != null) EmeraldGlow else AmberGlow,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Button(
                                onClick = {
                                    scanSuccess = false
                                    matchedProduct = null
                                    scannedSkuResult = ""
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SlateDark),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Réinitialiser", color = Color.White)
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))

                            if (matchedProduct != null) {
                                Button(
                                    onClick = {
                                        val target = matchedProduct!!
                                        scanSuccess = false
                                        matchedProduct = null
                                        scannedSkuResult = ""
                                        onNavigateToProductDetail(target)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
                                    modifier = Modifier.weight(1.3f)
                                ) {
                                    Text("Fiche Stock", color = SlateDark, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Button(
                                    onClick = {
                                        val skuToSave = scannedSkuResult
                                        scanSuccess = false
                                        matchedProduct = null
                                        scannedSkuResult = ""
                                        onNavigateToProductAddWithSku(skuToSave)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
                                    modifier = Modifier.weight(1.3f)
                                ) {
                                    Icon(Icons.Default.Add, "Ajouter", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Enregistrer", color = SlateDark, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
