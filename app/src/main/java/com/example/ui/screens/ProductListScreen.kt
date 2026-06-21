package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import android.util.Log
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ProductEntity
import com.example.ui.StockViewModel
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
    viewModel: StockViewModel,
    preselectedCoordinates: Triple<Int, Int, Int>? = null, // shelf, col, level if adding from 3D zone
    preselectedSku: String? = null,
    onClearPreselection: () -> Unit = {}
) {
    val products by viewModel.filteredProducts.collectAsState()
    val rawProducts by viewModel.productsFlow.collectAsState()
    val activeCategoryFilter by viewModel.selectedCategoryFilter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editTargetProduct by remember { mutableStateOf<ProductEntity?>(null) }
    
    val context = LocalContext.current
    val categories = listOf("Tous", "🚨 Alertes", "Électronique", "Alimentaire", "Vêtements", "Cosmétiques", "Autre")

    val currencyFormatter = remember {
        NumberFormat.getNumberInstance(Locale.FRANCE).apply {
            maximumFractionDigits = 0
        }
    }

    // Trigger add dialog automatically if navigated with preselected coordinates or preselected SKU!
    LaunchedEffect(preselectedCoordinates, preselectedSku) {
        if (preselectedCoordinates != null || preselectedSku != null) {
            showAddDialog = true
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
                        "Catalogue & Stocks", 
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ) 
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = SlateDark
                )
            )
        },
        containerColor = SlateDark,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = CyanNeon,
                contentColor = SlateDark,
                shape = CircleShape,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Ajouter Produit", modifier = Modifier.size(28.dp))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Search Input Header
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Rechercher par nom, catégorie, code barre...", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Recherche", tint = CyanNeon) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Effacer", tint = Color.LightGray)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanNeon,
                    unfocusedBorderColor = SlateCardBorder,
                    focusedContainerColor = SlateMedium,
                    unfocusedContainerColor = SlateMedium,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Horizontal active category list filter
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(categories) { cat ->
                    val isSelected = activeCategoryFilter == cat
                    Box(
                        modifier = Modifier
                            .background(
                                if (isSelected) CyanNeon else SlateMedium,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .border(
                                1.dp,
                                if (isSelected) CyanNeon else SlateCardBorder,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { viewModel.setCategoryFilter(cat) }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = cat,
                            color = if (isSelected) SlateDark else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Mapped elements list
            if (products.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Inventory2, 
                            contentDescription = "Vide", 
                            tint = Color.Gray, 
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Aucun stock correspondant.",
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Essayez d'ajuster vos critères ou enregistrez de nouveaux articles.",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp).padding(top = 4.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp)
                ) {
                    items(products, key = { it.id }) { product ->
                        ProductItemRow(
                            product = product,
                            currencyFormatter = currencyFormatter,
                            onItemClick = { editTargetProduct = product },
                            onQtyAdjust = { delta ->
                                viewModel.updateProductQty(product, delta)
                            }
                        )
                    }
                }
            }
        }

        // --- Dialog Add Product ---
        if (showAddDialog) {
            ProductDetailsFormDialog(
                title = "Enregistrer un nouveau produit",
                initialShelf = preselectedCoordinates?.first ?: 1,
                initialCol = preselectedCoordinates?.second ?: 1,
                initialLevel = preselectedCoordinates?.third ?: 1,
                initialSku = preselectedSku ?: "",
                productsList = rawProducts,
                onDismiss = {
                    showAddDialog = false
                    onClearPreselection()
                },
                onSave = { sku, name, qty, price, category, shelf, col, lvl, threshold, desc ->
                    viewModel.addProduct(sku, name, qty, price, category, shelf, col, lvl, threshold, desc)
                    showAddDialog = false
                    onClearPreselection()
                }
            )
        }

        // --- Dialog Edit Product details ---
        if (editTargetProduct != null) {
            val prod = editTargetProduct!!
            ProductDetailsFormDialog(
                title = "Fiche Produit : ${prod.name}",
                productToEdit = prod,
                productsList = rawProducts,
                onDismiss = { editTargetProduct = null },
                onSave = { sku, name, qty, price, category, shelf, col, lvl, threshold, desc ->
                    val updated = prod.copy(
                        sku = sku,
                        name = name,
                        quantity = qty,
                        price = price,
                        category = category,
                        locationShelf = shelf,
                        locationColumn = col,
                        locationLevel = lvl,
                        minThreshold = threshold,
                        description = desc
                    )
                    viewModel.updateProductDetails(updated)
                    editTargetProduct = null
                },
                onDelete = {
                    viewModel.deleteProduct(prod)
                    editTargetProduct = null
                }
            )
        }
    }
    }
}

@Composable
fun ProductItemRow(
    product: ProductEntity,
    currencyFormatter: NumberFormat,
    onItemClick: () -> Unit,
    onQtyAdjust: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick() },
        colors = CardDefaults.cardColors(containerColor = SlateMedium),
        border = BorderStroke(1.dp, SlateCardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon with colored bullet
            val categoryColor = getCategoryColor(product.category.hashCode())
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(SlateDark, RoundedCornerShape(10.dp))
                    .border(1.dp, categoryColor, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        product.category.equals("Électronique", true) -> Icons.Default.Computer
                        product.category.equals("Alimentaire", true) -> Icons.Default.Restaurant
                        product.category.equals("Vêtements", true) -> Icons.Default.Checkroom
                        product.category.equals("Cosmétiques", true) -> Icons.Default.Spa
                        else -> Icons.Default.Category
                    },
                    contentDescription = product.category,
                    tint = categoryColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text elements (name, catalog, localization)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        text = "Rayon ${product.locationShelf}-${product.locationColumn}-${product.locationLevel}",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .background(Color.Gray, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = product.category,
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }

                Text(
                    text = "${currencyFormatter.format(product.price)} FCFA",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = CyanNeon,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Quick Qty adjust panel
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = { onQtyAdjust(-1) },
                    modifier = Modifier
                        .size(28.dp)
                        .background(SlateDark, CircleShape)
                        .border(0.5.dp, SlateCardBorder, CircleShape)
                ) {
                    Icon(Icons.Default.Remove, "Retirer Qty", tint = Color.White, modifier = Modifier.size(14.dp))
                }

                val stockColor = when {
                    product.isOutOfStock() -> CrimsonRed
                    product.isLowStock() -> AmberGlow
                    else -> EmeraldGlow
                }
                
                Text(
                    text = "${product.quantity}",
                    color = stockColor,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(horizontal = 10.dp),
                    textAlign = TextAlign.Center
                )

                IconButton(
                    onClick = { onQtyAdjust(1) },
                    modifier = Modifier
                        .size(28.dp)
                        .background(SlateDark, CircleShape)
                        .border(0.5.dp, SlateCardBorder, CircleShape)
                ) {
                    Icon(Icons.Default.Add, "Ajouter Qty", tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

// Dialog Form for adding and updating products
@Composable
fun ProductDetailsFormDialog(
    title: String,
    productToEdit: ProductEntity? = null,
    initialShelf: Int = 1,
    initialCol: Int = 1,
    initialLevel: Int = 1,
    initialSku: String = "",
    productsList: List<ProductEntity>,
    onDismiss: () -> Unit,
    onSave: (
        sku: String, name: String, qty: Int, price: Double, category: String, 
        shelf: Int, col: Int, level: Int, minThreshold: Int, description: String
    ) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(productToEdit?.name ?: "") }
    var sku by remember { mutableStateOf(productToEdit?.sku ?: initialSku) }
    var quantityText by remember { mutableStateOf(productToEdit?.quantity?.toString() ?: "1") }
    var priceText by remember { mutableStateOf(productToEdit?.price?.toInt()?.toString() ?: "0") }
    var category by remember { mutableStateOf(productToEdit?.category ?: "Électronique") }
    var shelfText by remember { mutableStateOf(productToEdit?.locationShelf?.toString() ?: initialShelf.toString()) }
    var colText by remember { mutableStateOf(productToEdit?.locationColumn?.toString() ?: initialCol.toString()) }
    var levelText by remember { mutableStateOf(productToEdit?.locationLevel?.toString() ?: initialLevel.toString()) }
    var minThresholdText by remember { mutableStateOf(productToEdit?.minThreshold?.toString() ?: "5") }
    var description by remember { mutableStateOf(productToEdit?.description ?: "") }

    var hasError by remember { mutableStateOf(false) }
    var showScannerOverlay by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isAiScanningLoading by remember { mutableStateOf(false) }

    LaunchedEffect(initialSku) {
        if (initialSku.isNotBlank() && productToEdit == null) {
            val matched = productsList.firstOrNull { it.sku.uppercase() == initialSku.uppercase() }
            if (matched != null) {
                name = matched.name
                category = matched.category
                priceText = matched.price.toInt().toString()
                description = matched.description
                shelfText = matched.locationShelf.toString()
                colText = matched.locationColumn.toString()
                levelText = matched.locationLevel.toString()
                minThresholdText = matched.minThreshold.toString()
                quantityText = matched.quantity.toString()
                Toast.makeText(context, "Produit '${matched.name}' détecté !", Toast.LENGTH_LONG).show()
            } else {
                coroutineScope.launch {
                    isAiScanningLoading = true
                    Toast.makeText(context, "Recherche internet mondiale pour '$initialSku'...", Toast.LENGTH_SHORT).show()
                    try {
                        val details = fetchProductDetailsFull(initialSku)
                        name = details.name
                        category = details.category
                        priceText = details.price.toString()
                        description = details.description
                        shelfText = details.shelf.toString()
                        colText = details.col.toString()
                        levelText = details.level.toString()
                        minThresholdText = details.threshold.toString()
                        quantityText = "1"
                        Toast.makeText(context, "Produit identifié avec succès ! ✨", Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Log.e("ProductListScreen", "Initial sku lookup failed: ${e.message}")
                    } finally {
                        isAiScanningLoading = false
                    }
                }
            }
        }
    }

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

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SlateMedium,
        title = {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            // LazyColumn to avoid keyboard cutting elements off
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isAiScanningLoading) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = CyanNeon.copy(alpha = 0.15f)),
                            border = BorderStroke(1.dp, CyanNeon)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = CyanNeon,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    "Recherche des informations du produit sur internet du monde entier...",
                                    color = CyanNeon,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                item {
                    Button(
                        onClick = { showScannerOverlay = true },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scanner",
                            tint = SlateDark,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Scanner Code-Barres / QR 📸",
                            color = SlateDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nom du Produit *", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = formInputColors(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = sku,
                        onValueChange = { sku = it },
                        label = { Text("Code Barre / SKU *", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = formInputColors(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Catégorie * (ex: Alimentaire, Électronique, Cosmétiques...)", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = formInputColors(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = quantityText,
                            onValueChange = { quantityText = it },
                            label = { Text("Quantité *", color = Color.Gray) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            colors = formInputColors(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = priceText,
                            onValueChange = { priceText = it },
                            label = { Text("Prix (FCFA) *", color = Color.Gray) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1.2f),
                            colors = formInputColors(),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                item {
                    Text(
                        "Localisation dans l'entrepôt 3D (Détermine la position graphique)",
                        fontSize = 11.sp,
                        color = CyanNeon,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = shelfText,
                            onValueChange = { shelfText = it },
                            label = { Text("Rayon (1-3)", color = Color.Gray) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            colors = formInputColors(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = colText,
                            onValueChange = { colText = it },
                            label = { Text("Col (1-4)", color = Color.Gray) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            colors = formInputColors(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = levelText,
                            onValueChange = { levelText = it },
                            label = { Text("Niv (1-3)", color = Color.Gray) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            colors = formInputColors(),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = minThresholdText,
                            onValueChange = { minThresholdText = it },
                            label = { Text("Seuil Critique d'Alerte *", color = Color.Gray) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            colors = formInputColors(),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = formInputColors(),
                        shape = RoundedCornerShape(8.dp),
                        maxLines = 3
                    )
                }

                if (hasError) {
                    item {
                        Text(
                            text = "⚠ Veuillez remplir correctement tous les champs obligatoires (*). Rayon (1-3), Col (1-4), Niv (1-3).",
                            color = CrimsonRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = quantityText.toIntOrNull()
                    val price = priceText.toDoubleOrNull()
                    val shelf = shelfText.toIntOrNull()
                    val col = colText.toIntOrNull()
                    val level = levelText.toIntOrNull()
                    val threshold = minThresholdText.toIntOrNull()

                    if (name.isNotBlank() && qty != null && price != null && 
                        shelf != null && shelf in 1..3 && 
                        col != null && col in 1..4 && 
                        level != null && level in 1..3 && 
                        threshold != null
                    ) {
                        onSave(sku, name, qty, price, category, shelf, col, level, threshold, description)
                    } else {
                        hasError = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanNeon)
            ) {
                Text("Enregistrer", color = SlateDark, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = CrimsonRed)
                    ) {
                        Text("Supprimer", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                TextButton(onClick = onDismiss) {
                    Text("Annuler", color = Color.White)
                }
            }
        }
    )

    if (showScannerOverlay) {
        val infiniteTransition = rememberInfiniteTransition(label = "line")
        val laserOffset by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "lineOffset"
        )

        AlertDialog(
            onDismissRequest = { showScannerOverlay = false },
            containerColor = SlateMedium,
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, CyanNeon, RoundedCornerShape(24.dp)),
            title = {
                Text(
                    "Scanner de Code-barres de Produit 📸",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (hasCameraPermission) {
                        Text(
                            "Positionnez le code-barres du produit dans le cadre ci-dessous pour lancer une recherche mondiale automatique :",
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(2.dp, CyanNeon, RoundedCornerShape(16.dp))
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            CameraPreviewView(
                                onBarcodeScanned = { rawCode ->
                                    sku = rawCode
                                    showScannerOverlay = false
                                    
                                    // Auto-populate when scanned
                                    val matched = productsList.firstOrNull { it.sku.uppercase() == rawCode.uppercase() }
                                    if (matched != null) {
                                        name = matched.name
                                        category = matched.category
                                        priceText = matched.price.toInt().toString()
                                        description = matched.description
                                        shelfText = matched.locationShelf.toString()
                                        colText = matched.locationColumn.toString()
                                        levelText = matched.locationLevel.toString()
                                        minThresholdText = matched.minThreshold.toString()
                                        quantityText = matched.quantity.toString()
                                        Toast.makeText(context, "Produit '${matched.name}' détecté et importé !", Toast.LENGTH_LONG).show()
                                    } else {
                                        coroutineScope.launch {
                                            isAiScanningLoading = true
                                            Toast.makeText(context, "Recherche internet mondiale pour '$rawCode'...", Toast.LENGTH_SHORT).show()
                                            try {
                                                val details = fetchProductDetailsFull(rawCode)
                                                name = details.name
                                                category = details.category
                                                priceText = details.price.toString()
                                                description = details.description
                                                shelfText = details.shelf.toString()
                                                colText = details.col.toString()
                                                levelText = details.level.toString()
                                                minThresholdText = details.threshold.toString()
                                                quantityText = "1"
                                                Toast.makeText(context, "Produit identifié avec succès ! ✨", Toast.LENGTH_LONG).show()
                                            } catch (e: Exception) {
                                                Log.e("ProductListScreen", "Network lookup error: ${e.message}")
                                                Toast.makeText(context, "Erreur réseau. Remplissez manuellement.", Toast.LENGTH_LONG).show()
                                            } finally {
                                                isAiScanningLoading = false
                                            }
                                        }
                                    }
                                }
                            )

                            // Viewfinder lines
                            Box(
                                modifier = Modifier
                                    .size(width = 180.dp, height = 120.dp)
                                    .border(1.5.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(2.dp)
                                        .background(Color.Red)
                                        .align(Alignment.TopCenter)
                                        .offset(y = (120.dp * laserOffset))
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .background(SlateDark, RoundedCornerShape(12.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "Accès caméra requis",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Button(
                                    onClick = { launcher.launch(Manifest.permission.CAMERA) },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Autoriser l'appareil photo", color = SlateDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showScannerOverlay = false }) {
                    Text("Annuler", color = Color.LightGray)
                }
            }
        )
    }
}

data class ScannedDemoPreset(
    val sku: String,
    val name: String,
    val category: String,
    val price: Double,
    val description: String,
    val shelf: Int,
    val col: Int,
    val level: Int,
    val threshold: Int
)

@Composable
fun formInputColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = CyanNeon,
    unfocusedBorderColor = SlateCardBorder,
    focusedContainerColor = SlateDark,
    unfocusedContainerColor = SlateDark,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White
)

suspend fun fetchProductDetailsFromInternet(barcode: String): Triple<String, String, String>? {
    return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        // 1. Try OpenFoodFacts API
        try {
            val url = java.net.URL("https://world.openfoodfacts.org/api/v0/product/$barcode.json")
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "AIStudioApp - Android - Version 1.0")
            connection.connectTimeout = 4000
            connection.readTimeout = 4000
            
            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = org.json.JSONObject(response)
                if (json.optInt("status", 0) == 1) {
                    val product = json.getJSONObject("product")
                    val name = product.optString("product_name", product.optString("generic_name", ""))
                    val brand = product.optString("brands", "")
                    val ingredients = product.optString("ingredients_text", "")
                    val desc = if (brand.isNotBlank()) "Marque: $brand. $ingredients" else ingredients
                    if (name.isNotBlank()) {
                        return@withContext Triple(name, "Alimentaire", desc.take(200))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // 2. Try UPCItemDB Free Public API for non-food / electronics / clothing
        try {
            val url = java.net.URL("https://api.upcitemdb.com/prod/trial/lookup?upc=$barcode")
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Mozilla/5.0")
            connection.connectTimeout = 4000
            connection.readTimeout = 4000
            
            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = org.json.JSONObject(response)
                val items = json.optJSONArray("items")
                if (items != null && items.length() > 0) {
                    val product = items.getJSONObject(0)
                    val title = product.optString("title", "")
                    val category = product.optString("category", "Autre")
                    val desc = product.optString("description", "")
                    
                    val finalCat = if(category.contains("Food", true) || category.contains("Grocery", true)) "Alimentaire" 
                                   else if(category.contains("Electronics", true)) "Électronique" 
                                   else if(category.contains("Clothing", true)) "Vêtements"
                                   else if(category.contains("Beauty", true)) "Cosmétiques"
                                   else "Autre"
                    
                    if (title.isNotBlank()) {
                        return@withContext Triple(title, finalCat, desc.take(200))
                    }
                }
            }
        } catch (e: Exception) {
             e.printStackTrace()
        }
        null
    }
}

data class OnlineFullDetails(
    val name: String,
    val category: String,
    val price: Int,
    val description: String,
    val shelf: Int,
    val col: Int,
    val level: Int,
    val threshold: Int
)

suspend fun fetchProductDetailsFull(barcode: String): OnlineFullDetails {
    return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        // 1. Direct query to worldwide internet directories (OpenFoodFacts & UPCItemDB APIs)
        try {
            val details = fetchProductDetailsFromInternet(barcode)
            if (details != null) {
                return@withContext OnlineFullDetails(
                    name = details.first,
                    category = details.second,
                    price = 1500,
                    description = details.third,
                    shelf = 1,
                    col = 1,
                    level = 1,
                    threshold = 5
                )
            }
        } catch (e: Exception) {
            Log.e("ProductListScreen", "Worldwide Internet Lookup failed: ${e.message}")
        }

        // 2. Exact fallback for localized items
        OnlineFullDetails(
            name = "Produit $barcode",
            category = "Alimentaire",
            price = 1500,
            description = "Produit scanné avec le code-barres $barcode.",
            shelf = 1,
            col = 1,
            level = 1,
            threshold = 5
        )
    }
}
