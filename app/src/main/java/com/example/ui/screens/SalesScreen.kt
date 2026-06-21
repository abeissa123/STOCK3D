package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.testTag
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
import java.text.SimpleDateFormat
import java.util.*

data class CartItem(val product: ProductEntity, val quantity: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesScreen(
    viewModel: StockViewModel,
    onNavigateToCatalog: () -> Unit
) {
    val allProducts by viewModel.productsFlow.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var cartItems by remember { mutableStateOf<List<CartItem>>(emptyList()) }
    var cashReceivedText by remember { mutableStateOf("") }
    
    // State for receipt dialog
    var showReceiptDialog by remember { mutableStateOf(false) }
    var lastReceiptData by remember { mutableStateOf<ReceiptData?>(null) }
    
    val context = LocalContext.current
    
    val currencyFormatter = remember {
        NumberFormat.getNumberInstance(Locale.FRANCE).apply {
            maximumFractionDigits = 0
        }
    }
    
    fun formatCurrency(amount: Double): String {
        return "${currencyFormatter.format(amount)} FCFA"
    }
    
    // Filtered available products with stock remaining
    val availableProducts = remember(allProducts, searchQuery) {
        if (searchQuery.isBlank()) {
            allProducts.filter { it.quantity > 0 }
        } else {
            allProducts.filter { prod ->
                (prod.name.contains(searchQuery, ignoreCase = true) ||
                 prod.sku.contains(searchQuery, ignoreCase = true) ||
                 prod.category.contains(searchQuery, ignoreCase = true)) &&
                prod.quantity > 0
            }
        }
    }
    
    val totalAmount = cartItems.sumOf { it.product.price * it.quantity }
    val cashReceived = cashReceivedText.toDoubleOrNull() ?: 0.0
    val changeToReturn = if (cashReceived >= totalAmount) cashReceived - totalAmount else 0.0
    
    // Quick add / edit cart handlers
    fun addToCart(product: ProductEntity) {
        val inCart = cartItems.find { it.product.id == product.id }
        if (inCart != null) {
            if (inCart.quantity < product.quantity) {
                cartItems = cartItems.map {
                    if (it.product.id == product.id) it.copy(quantity = it.quantity + 1) else it
                }
            } else {
                Toast.makeText(context, "Stock maximal atteint (${product.quantity} disponibles)", Toast.LENGTH_SHORT).show()
            }
        } else {
            cartItems = cartItems + CartItem(product, 1)
        }
    }
    
    fun decreaseQty(product: ProductEntity) {
        val inCart = cartItems.find { it.product.id == product.id }
        if (inCart != null) {
            if (inCart.quantity > 1) {
                cartItems = cartItems.map {
                    if (it.product.id == product.id) it.copy(quantity = it.quantity - 1) else it
                }
            } else {
                cartItems = cartItems.filter { it.product.id != product.id }
            }
        }
    }
    
    fun increaseQty(product: ProductEntity) {
        val inCart = cartItems.find { it.product.id == product.id }
        if (inCart != null) {
            if (inCart.quantity < product.quantity) {
                cartItems = cartItems.map {
                    if (it.product.id == product.id) it.copy(quantity = it.quantity + 1) else it
                }
            } else {
                Toast.makeText(context, "Pas assez de stock (${product.quantity} disponibles)", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    fun removeFromCart(product: ProductEntity) {
        cartItems = cartItems.filter { it.product.id != product.id }
    }
    
    fun finalizeSale() {
        if (cartItems.isEmpty()) return
        if (cashReceived < totalAmount) {
            Toast.makeText(context, "Le montant reçu est insuffisant !", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Pass to ViewModel
        val listToRegister = cartItems.map { Pair(it.product, it.quantity) }
        viewModel.registerSale(listToRegister)
        
        val rawEmail = viewModel.userEmail.value ?: "operator.jean@stock3d.com"
        val shopPrefix = rawEmail.substringBefore("@").replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        val finalStoreName = "BOUTIQUE $shopPrefix".uppercase(Locale.getDefault())

        // Save current receipt data before clearing
        val currentOperator = viewModel.getCurrentOperator()
        val hashValue = currentOperator.hashCode().let { if (it < 0) -it else it }
        val operatorPhonePart = 650000000 + (hashValue % 49000000)
        val finalOperatorPhone = "+237 $operatorPhonePart"

        lastReceiptData = ReceiptData(
            id = "REC-" + (1000..9999).random() + "-" + (10..99).random(),
            date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date()),
            items = cartItems,
            total = totalAmount,
            cashReceived = cashReceived,
            change = changeToReturn,
            operatorName = currentOperator,
            storeName = finalStoreName,
            operatorPhone = finalOperatorPhone
        )
        
        // Reset states
        cartItems = emptyList()
        cashReceivedText = ""
        showReceiptDialog = true
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
                        "Caisse & Terminal de Vente",
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
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isWideScreen = maxWidth >= 720.dp
            
            if (isWideScreen) {
                // Horizontal split for wide devices (tablets/landscape)
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Catalog Area (Left)
                    Box(modifier = Modifier.weight(1.1f)) {
                        ProductSelectionPane(
                            availableProducts = availableProducts,
                            searchQuery = searchQuery,
                            onQueryChange = { searchQuery = it },
                            onProductSelected = { addToCart(it) },
                            formatCurrency = ::formatCurrency
                        )
                    }
                    
                    // Cart & Checkout Area (Right)
                    Box(modifier = Modifier.weight(0.9f)) {
                        CheckoutPane(
                            cartItems = cartItems,
                            totalAmount = totalAmount,
                            cashReceivedText = cashReceivedText,
                            onCashReceivedChange = { cashReceivedText = it },
                            onDecreaseQty = { decreaseQty(it.product) },
                            onIncreaseQty = { increaseQty(it.product) },
                            onRemoveItem = { removeFromCart(it.product) },
                            onValidateSale = { finalizeSale() },
                            formatCurrency = ::formatCurrency
                        )
                    }
                }
            } else {
                // Vertical scrolling list with collapsed tabs / areas
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Product Selection
                    Box(modifier = Modifier.weight(1f)) {
                        ProductSelectionPane(
                            availableProducts = availableProducts,
                            searchQuery = searchQuery,
                            onQueryChange = { searchQuery = it },
                            onProductSelected = { addToCart(it) },
                            formatCurrency = ::formatCurrency
                        )
                    }
                    
                    Divider(color = SlateCardBorder.copy(alpha = 0.5f), thickness = 1.dp)
                    
                    // Cart & Checkout View
                    Box(modifier = Modifier.weight(1.2f)) {
                        CheckoutPane(
                            cartItems = cartItems,
                            totalAmount = totalAmount,
                            cashReceivedText = cashReceivedText,
                            onCashReceivedChange = { cashReceivedText = it },
                            onDecreaseQty = { decreaseQty(it.product) },
                            onIncreaseQty = { increaseQty(it.product) },
                            onRemoveItem = { removeFromCart(it.product) },
                            onValidateSale = { finalizeSale() },
                            formatCurrency = ::formatCurrency
                        )
                    }
                }
            }
        }
    }
    
    // Digital Receipt dialog
    if (showReceiptDialog && lastReceiptData != null) {
        val receipt = lastReceiptData!!
        AlertDialog(
            onDismissRequest = { showReceiptDialog = false },
            containerColor = SlateMedium,
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .border(1.dp, SlateCardBorder, RoundedCornerShape(24.dp)),
            title = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(EmeraldGlow.copy(alpha = 0.15f), CircleShape)
                            .border(1.dp, EmeraldGlow, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Succès", tint = EmeraldGlow, modifier = Modifier.size(32.dp))
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        "Encaissé avec Succès !",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            },
            text = {
                // Receipt style content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SlateDark, RoundedCornerShape(12.dp))
                        .border(1.dp, SlateCardBorder, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        receipt.storeName,
                        fontWeight = FontWeight.Black,
                        color = CyanNeon,
                        fontSize = 14.sp
                    )
                    Text(
                        "FIGUIL-cameroun",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                    Text(
                        receipt.operatorName,
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Réf: ${receipt.id}",
                        color = Color.LightGray,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                    Text(
                        "Date: ${receipt.date}",
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                    
                    Divider(
                        color = SlateCardBorder,
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                    
                    // Listed items
                    receipt.items.forEach { cartItem ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    cartItem.product.name,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "${cartItem.quantity} x ${formatCurrency(cartItem.product.price)}",
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                            }
                            Text(
                                formatCurrency(cartItem.product.price * cartItem.quantity),
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    
                    Divider(
                        color = SlateCardBorder,
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                    
                    // Summary values
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("TOTAL NET", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(formatCurrency(receipt.total), color = CyanNeon, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Espèces Reçues", color = Color.Gray, fontSize = 12.sp)
                        Text(formatCurrency(receipt.cashReceived), color = Color.White, fontSize = 12.sp)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Monnaie Rendue", color = Color.Gray, fontSize = 12.sp)
                        Text(formatCurrency(receipt.change), color = EmeraldGlow, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        "*** Merci pour votre confiance ! ***",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { printReceipt(context, receipt) },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGlow),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.1f)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = "Imprimer", tint = SlateDark, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Imprimer / PDF", color = SlateDark, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                        }

                        Button(
                            onClick = { shareReceiptText(context, receipt) },
                            colors = ButtonDefaults.buttonColors(containerColor = SlateDark),
                            border = BorderStroke(1.dp, SlateCardBorder),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(0.9f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Partager", tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Partager", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                        }
                    }

                    Button(
                        onClick = { showReceiptDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Nouvelle Vente", tint = SlateDark, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Nouvelle Vente", color = SlateDark, fontWeight = FontWeight.Bold)
                    }
                }
            }
        )
    }
    }
}

// Sub-compositions for cleaner architecture and premium responsiveness
@Composable
fun ProductSelectionPane(
    availableProducts: List<ProductEntity>,
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onProductSelected: (ProductEntity) -> Unit,
    formatCurrency: (Double) -> String
) {
    Card(
        modifier = Modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(containerColor = SlateMedium),
        border = BorderStroke(1.dp, SlateCardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                "Sélection des Produits",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onQueryChange,
                placeholder = { Text("Rechercher un produit ou scanner...", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray)
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = CyanNeon,
                    unfocusedBorderColor = SlateCardBorder,
                    focusedContainerColor = SlateDark,
                    unfocusedContainerColor = SlateDark
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (availableProducts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.ShoppingBag, contentDescription = "Empty", tint = Color.Gray, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Aucun produit disponible.",
                            color = Color.LightGray,
                            fontSize = 14.sp
                        )
                        Text(
                            "Tous en rupture ou ne correspondent pas.",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableProducts, key = { it.id }) { product ->
                        ProductCartSelectorRow(
                            product = product,
                            onAdd = { onProductSelected(product) },
                            formatCurrency = formatCurrency
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProductCartSelectorRow(
    product: ProductEntity,
    onAdd: () -> Unit,
    formatCurrency: (Double) -> String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAdd() },
        colors = CardDefaults.cardColors(containerColor = SlateDark),
        border = BorderStroke(1.dp, SlateCardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    product.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(CyanNeon.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            product.category,
                            color = CyanNeon,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Dispo: ${product.quantity}",
                        color = if (product.isLowStock()) AmberGlow else Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    formatCurrency(product.price),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(end = 12.dp)
                )
                
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(CyanNeon, CircleShape)
                        .clickable { onAdd() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add to cart", tint = SlateDark, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun CheckoutPane(
    cartItems: List<CartItem>,
    totalAmount: Double,
    cashReceivedText: String,
    onCashReceivedChange: (String) -> Unit,
    onDecreaseQty: (CartItem) -> Unit,
    onIncreaseQty: (CartItem) -> Unit,
    onRemoveItem: (CartItem) -> Unit,
    onValidateSale: () -> Unit,
    formatCurrency: (Double) -> String
) {
    var showConfirmDialog by remember { mutableStateOf(false) }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            containerColor = SlateMedium,
            modifier = Modifier
                .border(2.dp, EmeraldGlow, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline, 
                        contentDescription = null, 
                        tint = EmeraldGlow, 
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Confirmer la vente", 
                        color = Color.White, 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column {
                    Text(
                        "Voulez-vous valider et enregistrer cette transaction de vente ?",
                        color = Color.LightGray,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SlateDark, RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Montant Total :", color = Color.Gray, fontSize = 13.sp)
                        Text(
                            formatCurrency(totalAmount), 
                            color = CyanNeon, 
                            fontWeight = FontWeight.Bold, 
                            fontSize = 15.sp
                        )
                    }
                    val cashRec = cashReceivedText.toDoubleOrNull() ?: 0.0
                    if (cashRec > 0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SlateDark, RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Argent Reçu :", color = Color.Gray, fontSize = 13.sp)
                            Text(
                                formatCurrency(cashRec), 
                                color = Color.White, 
                                fontWeight = FontWeight.Bold, 
                                fontSize = 15.sp
                            )
                        }
                        if (cashRec >= totalAmount) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SlateDark, RoundedCornerShape(8.dp))
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Monnaie à Rendre :", color = Color.Gray, fontSize = 13.sp)
                                Text(
                                    formatCurrency(cashRec - totalAmount), 
                                    color = EmeraldGlow, 
                                    fontWeight = FontWeight.Black, 
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        onValidateSale()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGlow)
                ) {
                    Text("Oui, finaliser", color = SlateDark, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Annuler", color = Color.LightGray)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // --- 1. CADRE PANIER ACTUEL ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SlateMedium),
            border = BorderStroke(1.dp, SlateCardBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Panier Actuel",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Badge(
                        containerColor = CyanNeon,
                        contentColor = SlateDark
                    ) {
                        val totalQty = cartItems.sumOf { it.quantity }
                        Text(
                            "$totalQty " + if (totalQty > 1) "articles" else "article", 
                            fontWeight = FontWeight.Black, 
                            modifier = Modifier.padding(2.dp)
                        )
                    }
                }
                
                // Cart Listed Items
                if (cartItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = "Cart Empty", tint = Color.Gray, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                "Le panier est vide.",
                                color = Color.LightGray,
                                fontSize = 13.sp
                            )
                            Text(
                                "Sélectionnez des produits à gauche.",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        cartItems.forEach { item ->
                            CartListItem(
                                item = item,
                                onDecrease = { onDecreaseQty(item) },
                                onIncrease = { onIncreaseQty(item) },
                                onDelete = { onRemoveItem(item) },
                                formatCurrency = formatCurrency
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- 2. CADRE REGLEMENT & FACTURATION ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SlateMedium),
            border = BorderStroke(1.dp, SlateCardBorder)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    "Note de Règlement",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Total & Calculation
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SlateDark, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("TOTAL À ENCAISSER", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(
                            formatCurrency(totalAmount),
                            color = CyanNeon,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Keyboard and client collection
                    OutlinedTextField(
                        value = cashReceivedText,
                        onValueChange = onCashReceivedChange,
                        placeholder = { Text("Argent reçu (FCFA)...", color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Default.Payments, contentDescription = "Encaisser", tint = Color.Gray) },
                        trailingIcon = {
                            TextButton(
                                onClick = { onCashReceivedChange(totalAmount.toInt().toString()) },
                                enabled = totalAmount > 0
                            ) {
                                Text("MONTANT EXACT", color = CyanNeon, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = CyanNeon,
                            unfocusedBorderColor = SlateCardBorder,
                            focusedContainerColor = SlateMedium,
                            unfocusedContainerColor = SlateMedium
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    
                    // Add Quick Sum buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(1000, 5000, 10000, 20000).forEach { sum ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(SlateMedium, RoundedCornerShape(6.dp))
                                    .border(0.5.dp, SlateCardBorder, RoundedCornerShape(6.dp))
                                    .clickable {
                                        val currentVal = cashReceivedText.toIntOrNull() ?: 0
                                        onCashReceivedChange((currentVal + sum).toString())
                                    }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "+${sum / 1000}k",
                                    color = Color.LightGray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    
                    // Return calculation display
                    val cashReceivedNum = cashReceivedText.toDoubleOrNull() ?: 0.0
                    if (totalAmount > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Monnaie à rendre", color = Color.LightGray, fontSize = 12.sp)
                            val isEnough = cashReceivedNum >= totalAmount
                            val valColor = if (isEnough) EmeraldGlow else CrimsonRed
                            Text(
                                if (isEnough) formatCurrency(cashReceivedNum - totalAmount) else "Montant insuffisant",
                                color = valColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- 3. CADRE INDÉPENDANT D'ACTION : ENCAISSER & VALIDER ---
        val isReady = cartItems.isNotEmpty() && (cashReceivedText.toDoubleOrNull() ?: 0.0) >= totalAmount
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 2.5.dp,
                    color = if (isReady) EmeraldGlow else SlateCardBorder.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp)
                ),
            colors = CardDefaults.cardColors(containerColor = SlateDark),
            shape = RoundedCornerShape(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = { showConfirmDialog = true },
                    enabled = isReady,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldGlow,
                        disabledContainerColor = SlateCardBorder.copy(alpha = 0.25f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .testTag("validate_sale_button"),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Encaisser",
                        tint = if (isReady) SlateDark else Color.LightGray,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "ENCAISSER & VALIDER",
                        color = if (isReady) SlateDark else Color.LightGray,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.5.sp,
                        fontSize = 17.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }
        }
    }
}

@Composable
fun CartListItem(
    item: CartItem,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    onDelete: () -> Unit,
    formatCurrency: (Double) -> String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SlateDark),
        border = BorderStroke(0.5.dp, SlateCardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.product.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "Total: " + formatCurrency(item.product.price * item.quantity),
                    color = CyanNeon,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                // Quantity controls
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(SlateMedium, CircleShape)
                        .clickable { onDecrease() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (item.quantity == 1) Icons.Default.Delete else Icons.Default.Remove,
                        contentDescription = "Decrease",
                        tint = if (item.quantity == 1) CrimsonRed else Color.LightGray,
                        modifier = Modifier.size(16.dp)
                    )
                }
                
                Text(
                    "${item.quantity}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 10.dp)
                )
                
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(SlateMedium, CircleShape)
                        .clickable { onIncrease() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Increase", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

data class ReceiptData(
    val id: String,
    val date: String,
    val items: List<CartItem>,
    val total: Double,
    val cashReceived: Double,
    val change: Double,
    val operatorName: String = "Opérateur",
    val storeName: String = "STOCK3D BOUTIQUE",
    val operatorPhone: String = "+237 680 12 34 56"
)

fun printReceipt(context: Context, receipt: ReceiptData) {
    try {
        val webView = WebView(context)
        val itemsHtml = receipt.items.joinToString("") { item ->
            val totalItem = item.product.price * item.quantity
            """
            <tr>
                <td style="padding: 6px 0; color: #1e293b;">${item.product.name}</td>
                <td style="padding: 6px 0; text-align: center; color: #475569;">${item.quantity}</td>
                <td style="padding: 6px 0; text-align: right; font-weight: bold; color: #0f172a;">${String.format(Locale.FRANCE, "%,.0f", totalItem)} FCFA</td>
            </tr>
            """
        }
        
        val htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <style>
                    body {
                        font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;
                        margin: 40px;
                        color: #1e293b;
                        background: #ffffff;
                    }
                    .receipt-card {
                        max-width: 600px;
                        margin: auto;
                        padding: 30px;
                        border: 2px solid #e2e8f0;
                        border-radius: 12px;
                        box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05);
                    }
                    .header {
                        text-align: center;
                        margin-bottom: 25px;
                        border-bottom: 2px dashed #cbd5e1;
                        padding-bottom: 20px;
                    }
                    .header h1 {
                        font-size: 24px;
                        margin: 0;
                        color: #0f172a;
                        letter-spacing: 1px;
                        font-weight: 800;
                    }
                    .header p {
                        margin: 5px 0 0;
                        color: #64748b;
                        font-size: 14px;
                    }
                    .info-grid {
                        display: grid;
                        grid-template-columns: 1f 1f;
                        gap: 10px;
                        font-size: 13px;
                        margin-bottom: 25px;
                        background: #f8fafc;
                        padding: 15px;
                        border-radius: 8px;
                    }
                    .info-item {
                        display: flex;
                        justify-content: space-between;
                        padding: 2px 0;
                    }
                    .info-label {
                        color: #64748b;
                        font-weight: 500;
                    }
                    .info-value {
                        color: #0f172a;
                        font-weight: bold;
                    }
                    .items-table {
                        width: 100%;
                        border-collapse: collapse;
                        font-size: 14px;
                        margin-bottom: 25px;
                    }
                    .items-table th {
                        border-bottom: 2px solid #e2e8f0;
                        text-align: left;
                        padding-bottom: 8px;
                        color: #64748b;
                        font-weight: 600;
                        font-size: 12px;
                        text-transform: uppercase;
                    }
                    .totals-box {
                        border-top: 2px dashed #cbd5e1;
                        padding-top: 15px;
                    }
                    .total-row {
                        display: flex;
                        justify-content: space-between;
                        font-size: 14px;
                        margin-bottom: 8px;
                    }
                    .total-row.grand-total {
                        font-size: 18px;
                        font-weight: 800;
                        color: #0f172a;
                        border-top: 1px solid #e2e8f0;
                        padding-top: 12px;
                        margin-top: 8px;
                    }
                    .footer {
                        text-align: center;
                        margin-top: 35px;
                        font-size: 13px;
                        color: #64748b;
                        border-top: 1px solid #e2e8f0;
                        padding-top: 15px;
                    }
                </style>
            </head>
            <body>
                <div class="receipt-card">
                    <div class="header">
                        <h1>${receipt.storeName}</h1>
                        <p>FIGUIL-cameroun</p>
                    </div>
                    
                    <div class="info-grid">
                        <div class="info-item">
                            <span class="info-label">Facture N°:</span>
                            <span class="info-value" style="font-family: monospace;">${receipt.id}</span>
                        </div>
                        <div class="info-item">
                            <span class="info-label">Date:</span>
                            <span class="info-value">${receipt.date}</span>
                        </div>
                        <div class="info-item">
                            <span class="info-label">Opérateur:</span>
                            <span class="info-value">${receipt.operatorName}</span>
                        </div>
                    </div>
                    
                    <table class="items-table">
                        <thead>
                            <tr>
                                <th style="width: 50%;">Désignation</th>
                                <th style="width: 20%; text-align: center;">Qté</th>
                                <th style="width: 30%; text-align: right;">Total</th>
                            </tr>
                        </thead>
                        <tbody>
                            $itemsHtml
                        </tbody>
                    </table>
                    
                    <div class="totals-box">
                        <div class="total-row">
                            <span style="color: #64748b;">Sous-total HT :</span>
                            <span style="font-weight: 500;">${String.format(Locale.FRANCE, "%,.0f", receipt.total * 0.82)} FCFA</span>
                        </div>
                        <div class="total-row">
                            <span style="color: #64748b;">TVA (18%) :</span>
                            <span style="font-weight: 500;">${String.format(Locale.FRANCE, "%,.0f", receipt.total * 0.18)} FCFA</span>
                        </div>
                        <div class="total-row grand-total">
                            <span>TOTAL NET À PAYER</span>
                            <span style="color: #0ea5e9;">${String.format(Locale.FRANCE, "%,.0f", receipt.total)} FCFA</span>
                        </div>
                        <div class="total-row" style="margin-top: 12px;">
                            <span style="color: #64748b;">Montant Reçu :</span>
                            <span>${String.format(Locale.FRANCE, "%,.0f", receipt.cashReceived)} FCFA</span>
                        </div>
                        <div class="total-row" style="font-weight: bold; color: #10b981;">
                            <span>Monnaie Rendue :</span>
                            <span>${String.format(Locale.FRANCE, "%,.0f", receipt.change)} FCFA</span>
                        </div>
                    </div>
                    
                    <div class="footer">
                        <p>Merci pour votre achat !<br><b>STOCK3D - Gestion de Stock intelligente & 3D</b></p>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()
        
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                val jobName = "Facture-${receipt.id}"
                val printAdapter = webView.createPrintDocumentAdapter(jobName)
                printManager?.print(jobName, printAdapter, PrintAttributes.Builder().build())
            }
        }
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
    } catch (e: Exception) {
         Toast.makeText(context, "Erreur lors de l'impression : ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}

fun shareReceiptText(context: Context, receipt: ReceiptData) {
    try {
        val shareText = StringBuilder().apply {
            append("⚡ ${receipt.storeName} - FACTURE ⚡\n")
            append("FIGUIL-cameroun\n")
            append("------------------------------------\n")
            append("Réf Facture : ${receipt.id}\n")
            append("Date        : ${receipt.date}\n")
            append("Opérateur   : ${receipt.operatorName}\n")
            append("------------------------------------\n")
            receipt.items.forEach { item ->
                append("${item.product.name}\n")
                append("  ${item.quantity} x ${String.format(Locale.FRANCE, "%,.0f", item.product.price)} = ${String.format(Locale.FRANCE, "%,.0f", item.product.price * item.quantity)} FCFA\n")
            }
            append("------------------------------------\n")
            append("TOTAL NET À PAYER : ${String.format(Locale.FRANCE, "%,.0f", receipt.total)} FCFA\n")
            append("Montant Encaissé  : ${String.format(Locale.FRANCE, "%,.0f", receipt.cashReceived)} FCFA\n")
            append("Monnaie Rendue    : ${String.format(Locale.FRANCE, "%,.0f", receipt.change)} FCFA\n")
            append("====================================\n")
            append("Merci pour votre confiance & à bientôt !")
        }.toString()

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Facture ${receipt.id}")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        context.startActivity(Intent.createChooser(intent, "Partager la facture via"))
    } catch (e: Exception) {
        Toast.makeText(context, "Erreur de partage : ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}
