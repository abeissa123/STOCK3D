package com.example.ui.screens

import android.widget.Toast
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
    onClearPreselection: () -> Unit = {}
) {
    val products by viewModel.filteredProducts.collectAsState()
    val rawProducts by viewModel.productsFlow.collectAsState()
    val activeCategoryFilter by viewModel.selectedCategoryFilter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editTargetProduct by remember { mutableStateOf<ProductEntity?>(null) }
    
    val context = LocalContext.current
    val categories = listOf("Tous", "Électronique", "Alimentaire", "Vêtements", "Cosmétiques", "Autre")

    val currencyFormatter = remember {
        NumberFormat.getNumberInstance(Locale.FRANCE).apply {
            maximumFractionDigits = 0
        }
    }

    // Trigger add dialog automatically if navigated with preselected coordinates from 3D warehouse!
    LaunchedEffect(preselectedCoordinates) {
        if (preselectedCoordinates != null) {
            showAddDialog = true
        }
    }

    Scaffold(
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
    productsList: List<ProductEntity>,
    onDismiss: () -> Unit,
    onSave: (
        sku: String, name: String, qty: Int, price: Double, category: String, 
        shelf: Int, col: Int, level: Int, minThreshold: Int, description: String
    ) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(productToEdit?.name ?: "") }
    var sku by remember { mutableStateOf(productToEdit?.sku ?: "") }
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
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Scanner",
                            tint = SlateDark,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Scanner Code-Barres de Démo 📸",
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
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = sku,
                            onValueChange = { sku = it },
                            label = { Text("SKU / Code Barre", color = Color.Gray) },
                            modifier = Modifier.weight(1f),
                            colors = formInputColors(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            label = { Text("Catégorie *", color = Color.Gray) },
                            modifier = Modifier.weight(1f),
                            colors = formInputColors(),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
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
                    "Scanner Intelligent AI 📸",
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
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Passez le laser sur le code-barres pour lire l'article",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 14.dp)
                    )

                    // Viewfinder box
                    Box(
                        modifier = Modifier
                            .size(width = 240.dp, height = 130.dp)
                            .background(Color.Black, RoundedCornerShape(12.dp))
                            .border(1.5.dp, Color.Gray, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Viseur",
                                tint = Color.DarkGray,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            // Barcode pattern
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf(3, 7, 2, 5, 8, 4, 2, 8, 3, 5, 2, 7, 3).forEach { heightMultiplier ->
                                    Box(
                                        modifier = Modifier
                                            .width(if (heightMultiplier % 2 == 0) 3.dp else 1.dp)
                                            .height(30.dp)
                                            .background(Color.White)
                                    )
                                }
                            }
                        }

                        // Moving red laser line
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(Color.Red)
                                .align(Alignment.TopCenter)
                                .offset(y = (130.dp * laserOffset))
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "Sélectionnez un produit réel à simuler :",
                        color = CyanNeon,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Presets
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val presets = listOf(
                            ScannedDemoPreset("7701234567890", "Café Touba Royal", "Alimentation", 1500.0, "Café traditionnel sénégalais de qualité supérieure, parfumé au piment de Selim.", 1, 2, 1, 10),
                            ScannedDemoPreset("7709876543210", "Jus de Bissap Bio", "Alimentation", 1000.0, "Jus traditionnel de fleurs d'hibiscus biologique, rafraîchissant et artisanal.", 1, 4, 3, 15),
                            ScannedDemoPreset("1901980001234", "iPhone 15 Pro Max", "Électronique", 850000.0, "Smartphone premium Apple, châssis en titane, zoom optique x5, 256 Go.", 3, 1, 2, 2),
                            ScannedDemoPreset("8806090005678", "Sèche-cheveux Pro", "Vêtements", 25000.0, "Sèche-cheveux ionique professionnel avec buse concentratrice, réglages précis.", 2, 3, 1, 5),
                            ScannedDemoPreset("8410137001421", "Riz Corbeille Parfumé", "Alimentation", 18500.0, "Sac de riz de luxe parfumé de qualité supérieure.", 1, 1, 1, 8),
                            ScannedDemoPreset("1234567890123", "Casque Réduction de Bruit", "Électronique", 65000.0, "Casque circum-auriculaire sans fil avec réduction de bruit active d'excellence.", 3, 2, 3, 3)
                        )

                        items(presets) { preset ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        name = preset.name
                                        sku = preset.sku
                                        category = if (preset.category == "Alimentation") "Alimentaire" else preset.category
                                        priceText = preset.price.toInt().toString()
                                        description = preset.description
                                        shelfText = preset.shelf.toString()
                                        colText = preset.col.toString()
                                        levelText = preset.level.toString()
                                        minThresholdText = preset.threshold.toString()

                                        Toast.makeText(context, "Code ${preset.sku} scanné ! '${preset.name}' importé.", Toast.LENGTH_SHORT).show()
                                        showScannerOverlay = false
                                    },
                                colors = CardDefaults.cardColors(containerColor = SlateDark),
                                border = BorderStroke(1.dp, SlateCardBorder)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("${preset.name} (${preset.price.toInt()} FCFA)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text("Code: ${preset.sku}", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                    }
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Simuler",
                                        tint = CyanNeon,
                                        modifier = Modifier.size(16.dp)
                                    )
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
