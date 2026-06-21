package com.example.ui

import android.content.Context
import android.graphics.pdf.PdfDocument
import android.graphics.Paint
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class StockViewModel(
    private val repository: StockRepository,
    private val context: Context
) : ViewModel() {

    // --- Authentication ---
    val authService = AuthService(context)
    val isLoggedIn = authService.isLoggedIn
    val userEmail = authService.userEmail

    // --- Helper for Operator Names ---
    fun getCurrentOperator(): String {
        val email = userEmail.value ?: return "Opérateur Jean"
        val part = email.substringBefore("@")
        return "Opérateur " + part.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }

    // --- Flows from Room DB ---
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val productsFlow: StateFlow<List<ProductEntity>> = userEmail
        .flatMapLatest { email ->
            val activeEmail = email ?: "operator.jean@stock3d.com"
            repository.getProductsByOwner(activeEmail)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val transactionsFlow: StateFlow<List<TransactionEntity>> = userEmail
        .flatMapLatest { email ->
            val activeEmail = email ?: "operator.jean@stock3d.com"
            repository.getTransactionsByOwner(activeEmail)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Active Visual Alerts for Low Stock/Rupture ---
    private val _activeAlerts = MutableStateFlow<List<ProductEntity>>(emptyList())
    val activeAlerts: StateFlow<List<ProductEntity>> = _activeAlerts.asStateFlow()
    private val dismissedAlertIds = Collections.synchronizedSet(mutableSetOf<Int>())

    init {
        viewModelScope.launch {
            userEmail.collect { email ->
                val activeEmail = email ?: "operator.jean@stock3d.com"
                if (activeEmail == "operator.jean@stock3d.com") {
                    // Check if default user has products, if not seed it so the app is instantly testable
                    repository.getProductsByOwner(activeEmail).firstOrNull()?.let { list ->
                        if (list.isEmpty()) {
                            repository.resetDatabaseToDemo(context, activeEmail)
                            pushNotification("Bienvenue ! Votre boutique de démonstration a été initialisée avec des produits modèles.")
                        }
                    }
                } else {
                    // For brand-new custom stores, we leave the catalog fully EMPTY (zero articles)
                    // so the user can insert their own articles.
                    repository.getProductsByOwner(activeEmail).firstOrNull()?.let { list ->
                        if (list.isEmpty()) {
                            pushNotification("Félicitations pour votre nouvelle boutique ! Votre catalogue est vide pour commencer à insérer vos propres articles.")
                        }
                    }
                }
            }
        }

        // Monitoring of stock levels for real-time visual alerts
        viewModelScope.launch {
            productsFlow.collect { list ->
                list.forEach { prod ->
                    if (prod.isOutOfStock() || prod.isLowStock()) {
                        val existingAlert = _activeAlerts.value.firstOrNull { it.id == prod.id }
                        val qtyChanged = existingAlert != null && existingAlert.quantity != prod.quantity
                        
                        if (qtyChanged) {
                            dismissedAlertIds.remove(prod.id)
                        }

                        if (!dismissedAlertIds.contains(prod.id)) {
                            _activeAlerts.update { current ->
                                if (current.any { it.id == prod.id }) {
                                    current.map { if (it.id == prod.id) prod else it }
                                } else {
                                    current + prod
                                }
                            }
                        }
                    } else {
                        dismissedAlertIds.remove(prod.id)
                        _activeAlerts.update { current -> current.filter { it.id != prod.id } }
                    }
                }
                // Handle deletion
                val currentIds = list.map { it.id }.toSet()
                _activeAlerts.update { current -> current.filter { it.id in currentIds } }
            }
        }
    }

    fun dismissAlert(productId: Int) {
        dismissedAlertIds.add(productId)
        _activeAlerts.update { list -> list.filter { it.id != productId } }
    }

    // --- UI Filters and Statuses ---
    private val _selectedCategoryFilter = MutableStateFlow("Tous")
    val selectedCategoryFilter: StateFlow<String> = _selectedCategoryFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // --- Notifications Feed ---
    private val _notifications = MutableStateFlow<List<String>>(
        listOf(
            "Système Stock3D initialisé avec succès.",
            "Visualisation 3D en temps réel maintenant connectée.",
            "Base de données locale synchronisée et sécurisée."
        )
    )
    val notifications: StateFlow<List<String>> = _notifications.asStateFlow()

    fun pushNotification(message: String) {
        val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        _notifications.update { listOf("[$timeStr] $message") + it }
    }

    // --- Search & Category Mapped Product List ---
    val filteredProducts: StateFlow<List<ProductEntity>> = combine(
        productsFlow,
        _selectedCategoryFilter,
        _searchQuery
    ) { products, category, query ->
        products.filter { product ->
            val matchesCategory = when {
                category == "Tous" -> true
                category.contains("alert", ignoreCase = true) -> product.isLowStock() || product.isOutOfStock()
                else -> product.category.equals(category, ignoreCase = true)
            }
            val matchesQuery = query.isEmpty() || 
                    product.name.contains(query, ignoreCase = true) || 
                    product.sku.contains(query, ignoreCase = true) || 
                    product.category.contains(query, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Low Stock Alert List ---
    val lowStockCount: StateFlow<Int> = productsFlow.map { list ->
        list.count { it.isLowStock() || it.isOutOfStock() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // --- AI Prediction State ---
    private val _aiPredictionResult = MutableStateFlow<String>("")
    val aiPredictionResult: StateFlow<String> = _aiPredictionResult.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    // --- Selected Shelf/Slot in 3D Warehouse ---
    private val _selectedShelf = MutableStateFlow<Int?>(null) // Rayon 1 à 3
    val selectedShelf: StateFlow<Int?> = _selectedShelf.asStateFlow()

    fun selectShelf(shelf: Int?) {
        _selectedShelf.value = shelf
    }

    // --- CRUD Actions ---
    
    fun addProduct(
        sku: String,
        name: String,
        quantity: Int,
        price: Double,
        category: String,
        locationShelf: Int,
        locationColumn: Int,
        locationLevel: Int,
        minThreshold: Int,
        description: String,
        operator: String = "Opérateur Jean"
    ) {
        viewModelScope.launch {
            val formattedSku = sku.ifBlank { "STK-" + (10000..99999).random() }
            val activeEmail = userEmail.value ?: "operator.jean@stock3d.com"
            val activeOperator = if (operator == "Opérateur Jean") getCurrentOperator() else operator
            val newProduct = ProductEntity(
                sku = formattedSku,
                name = name,
                quantity = quantity,
                price = price,
                category = category,
                locationShelf = locationShelf,
                locationColumn = locationColumn,
                locationLevel = locationLevel,
                minThreshold = minThreshold,
                description = description,
                ownerEmail = activeEmail
            )
            repository.addProduct(newProduct, activeOperator)
            pushNotification("Nouveau produit d'article ajouté : $name ($formattedSku) au Rayon $locationShelf")
            checkThresholds()
        }
    }

    fun updateProductQty(product: ProductEntity, delta: Int, operator: String = "Opérateur Jean") {
        viewModelScope.launch {
            val activeOperator = if (operator == "Opérateur Jean") getCurrentOperator() else operator
            val newQty = (product.quantity + delta).coerceAtLeast(0)
            val updated = product.copy(
                quantity = newQty,
                exitDate = if (delta < 0) System.currentTimeMillis() else product.exitDate
            )
            repository.updateProductWithTransaction(updated, delta, activeOperator)
            
            val action = if (delta > 0) "approvisionné de +$delta unités" else "déstocké de $delta unités"
            pushNotification("Produit ${product.name} $action. Stock actuel : $newQty")
            checkThresholds()
        }
    }

    fun registerSale(items: List<Pair<ProductEntity, Int>>, operator: String = "Opérateur Jean") {
        viewModelScope.launch {
            val activeOperator = if (operator == "Opérateur Jean") getCurrentOperator() else operator
            var totalAmount = 0.0
            items.forEach { (prod, qtyToSell) ->
                val newQty = (prod.quantity - qtyToSell).coerceAtLeast(0)
                val updated = prod.copy(
                    quantity = newQty,
                    exitDate = System.currentTimeMillis()
                )
                // qtyDelta is negative for stock exits
                repository.updateProductWithTransaction(updated, -qtyToSell, activeOperator)
                totalAmount += qtyToSell * prod.price
            }
            val totalFormatted = String.format(Locale.FRANCE, "%,.0f", totalAmount).replace(",", " ")
            pushNotification("Vente validée par $activeOperator : $totalFormatted FCFA encaissés. Stock mis à jour.")
            checkThresholds()
        }
    }

    fun updateProductDetails(product: ProductEntity, operator: String = "Opérateur Jean") {
        viewModelScope.launch {
            val activeOperator = if (operator == "Opérateur Jean") getCurrentOperator() else operator
            // Check delta
            val oldProduct = repository.getProductById(product.id)
            if (oldProduct != null) {
                val delta = product.quantity - oldProduct.quantity
                repository.updateProductWithTransaction(product, delta, activeOperator)
                pushNotification("Détails mis à jour pour : ${product.name}")
                checkThresholds()
            }
        }
    }

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.deleteProduct(product)
            pushNotification("Produit supprimé du catalogue : ${product.name}")
        }
    }

    fun setCategoryFilter(category: String) {
        _selectedCategoryFilter.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Reset database
    fun resetDatabase() {
        viewModelScope.launch {
            val activeEmail = userEmail.value ?: "operator.jean@stock3d.com"
            if (activeEmail == "operator.jean@stock3d.com") {
                repository.resetDatabaseToDemo(context, activeEmail)
                pushNotification("Données et historique de stock réinitialisés à l'état de démonstration.")
            } else {
                repository.clearStoreByOwner(activeEmail)
                pushNotification("Toutes les données de votre boutique ont été effacées. Stock réinitialisé à zéro.")
            }
        }
    }

    private fun checkThresholds() {
        viewModelScope.launch {
            val currentProducts = productsFlow.value
            currentProducts.forEach { prod ->
                if (prod.isOutOfStock()) {
                    pushNotification("Alerte CRITIQUE : ${prod.name} est en RUPTURE DE STOCK ! (Rayon ${prod.locationShelf})")
                } else if (prod.isLowStock()) {
                    pushNotification("Alerte Stock Bas : ${prod.name} approche du seuil critique (Qté: ${prod.quantity}/${prod.minThreshold})")
                }
            }
        }
    }

    // --- Barcode simulator ---
    fun simulateBarcodeScan(barcode: String, onMatched: (ProductEntity) -> Unit, onNotFound: () -> Unit) {
        viewModelScope.launch {
            val activeEmail = userEmail.value ?: "operator.jean@stock3d.com"
            val found = repository.getProductBySkuAndOwner(barcode, activeEmail)
            if (found != null) {
                onMatched(found)
            } else {
                onNotFound()
            }
        }
    }

    // --- Excel / CSV Exportation ---
    fun exportStockToCSV(): String? {
        val products = productsFlow.value
        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
        val timestamp = dateFormat.format(Date())
        val filename = "Rapport_Stock3D_$timestamp.csv"
        
        return try {
            val path = context.getExternalFilesDir(null)
            val file = File(path, filename)
            val writer = FileWriter(file)
            
            // CSV Header
            writer.append("ID,SKU,Nom,Categorie,Quantite,Prix_Unit_FCFA,Valeur_FCFA,Etagere,Colonne,Niveau,Statut,Description\n")
            
            products.forEach { prod ->
                val status = when {
                    prod.isOutOfStock() -> "RUPTURE"
                    prod.isLowStock() -> "ALERTE"
                    else -> "REMPLI"
                }
                writer.append("${prod.id},\"${prod.sku}\",\"${prod.name}\",\"${prod.category}\",${prod.quantity},${prod.price},${prod.quantity * prod.price},${prod.locationShelf},${prod.locationColumn},${prod.locationLevel},\"$status\",\"${prod.description.replace("\n", " ")}\"\n")
            }
            
            writer.flush()
            writer.close()
            pushNotification("Rapport de stock CSV exporté : $filename")
            file.absolutePath
        } catch (e: Exception) {
            pushNotification("Échec de l'exportation du rapport CSV: ${e.message}")
            null
        }
    }

    // --- High-Fidelity PDF Exportation ---
    fun exportStockToPDF(): String? {
        val products = productsFlow.value
        val alerts = products.filter { it.isOutOfStock() || it.isLowStock() }
        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
        val timestamp = dateFormat.format(Date())
        val filename = "Rapport_Stock3D_$timestamp.pdf"
        
        return try {
            val path = context.getExternalFilesDir(null)
            val file = File(path, filename)
            val pdfDocument = PdfDocument()
            
            // A4 size: 595 x 842 points (at 72 dpi)
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas
            
            val paint = Paint()
            val textPaint = Paint()
            val boldPaint = Paint()
            
            // Palette Colors
            val primaryColor = 0xFF151922.toInt() // Deep Slate Dark
            val accentColor = 0xFF00E5FF.toInt() // CyanNeon
            val alertColor = 0xFFFF5252.toInt() // Crimson/Red
            val warningColor = 0xFFFFAB40.toInt() // Orange
            val darkGray = 0xFF444444.toInt()
            val lightGray = 0xFFF0F2F5.toInt()
            
            fun drawHeader(pageNumber: Int) {
                // Header Banner Box
                paint.color = primaryColor
                canvas.drawRect(0f, 0f, 595f, 85f, paint)
                
                // Accent Bar
                paint.color = accentColor
                canvas.drawRect(0f, 81f, 595f, 85f, paint)
                
                // Title
                boldPaint.color = AndroidColor.WHITE
                boldPaint.textSize = 16f
                boldPaint.isFakeBoldText = true
                canvas.drawText("RAPPORT D'INVENTAIRE - STOCK3D", 30f, 40f, boldPaint)
                
                // Subtitle / Date / User Info
                textPaint.color = 0xFFB0BEC5.toInt()
                textPaint.textSize = 9f
                val activeEmail = userEmail.value ?: "operator.jean@stock3d.com"
                canvas.drawText("Généré le: ${SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())}  |  Utilisateur: $activeEmail", 30f, 58f, textPaint)
                canvas.drawText("Position d'entrepôt virtuelle 3D synchronisée en temps réel", 30f, 72f, textPaint)
                
                // Page Tag
                textPaint.color = AndroidColor.WHITE
                canvas.drawText("PAGE $pageNumber", 515f, 40f, textPaint)
            }
            
            var currentPageNum = 1
            drawHeader(currentPageNum)
            var yPosition = 120f
            
            // SECTION 1: ALERTES CRITIQUES
            boldPaint.color = primaryColor
            boldPaint.textSize = 12f
            boldPaint.isFakeBoldText = true
            canvas.drawText("1. SITUATION DES ALERTES DE STOCK", 30f, yPosition, boldPaint)
            yPosition += 15f
            
            paint.color = primaryColor
            paint.strokeWidth = 1f
            canvas.drawLine(30f, yPosition - 5f, 565f, yPosition - 5f, paint)
            
            textPaint.textSize = 9.5f
            textPaint.color = AndroidColor.BLACK
            
            if (alerts.isEmpty()) {
                textPaint.color = 0xFF2E7D32.toInt() // Green Dark
                boldPaint.color = 0xFF2E7D32.toInt()
                boldPaint.textSize = 10f
                canvas.drawText("✓ Aucune alerte de stock en cours. Tous les articles sont opérationnels.", 30f, yPosition, boldPaint)
                yPosition += 30f
            } else {
                // Draw alerts table header background
                paint.color = lightGray
                canvas.drawRect(30f, yPosition - 10f, 565f, yPosition + 10f, paint)
                
                // Draw alerts table header text
                boldPaint.textSize = 8.5f
                boldPaint.color = primaryColor
                canvas.drawText("SKU", 35f, yPosition + 3f, boldPaint)
                canvas.drawText("NOM DE L'ARTICLE", 100f, yPosition + 3f, boldPaint)
                canvas.drawText("CATÉGORIE", 250f, yPosition + 3f, boldPaint)
                canvas.drawText("QTY / SEUIL", 350f, yPosition + 3f, boldPaint)
                canvas.drawText("STATUT", 440f, yPosition + 3f, boldPaint)
                canvas.drawText("LOCALISATION", 500f, yPosition + 3f, boldPaint)
                
                yPosition += 25f
                
                alerts.forEach { prod ->
                    val isRupture = prod.quantity <= 0
                    
                    textPaint.color = AndroidColor.BLACK
                    canvas.drawText(prod.sku, 35f, yPosition, textPaint)
                    
                    val dispName = if (prod.name.length > 21) prod.name.take(19) + "..." else prod.name
                    canvas.drawText(dispName, 100f, yPosition, textPaint)
                    canvas.drawText(prod.category, 250f, yPosition, textPaint)
                    
                    canvas.drawText("${prod.quantity} / ${prod.minThreshold}", 350f, yPosition, textPaint)
                    
                    // Style the Status Text based on stock state
                    boldPaint.textSize = 8.5f
                    if (isRupture) {
                        boldPaint.color = alertColor
                        canvas.drawText("RUPTURE", 440f, yPosition, boldPaint)
                    } else {
                        boldPaint.color = warningColor
                        canvas.drawText("STOCK BAS", 440f, yPosition, boldPaint)
                    }
                    
                    textPaint.color = darkGray
                    canvas.drawText("R:${prod.locationShelf} C:${prod.locationColumn} N:${prod.locationLevel}", 500f, yPosition, textPaint)
                    
                    // Row separator
                    paint.color = 0xFFE0E0E0.toInt()
                    canvas.drawLine(30f, yPosition + 5f, 565f, yPosition + 5f, paint)
                    
                    yPosition += 18f
                    
                    // Multi-page layout protection
                    if (yPosition > 770f) {
                        pdfDocument.finishPage(page)
                        currentPageNum++
                        page = pdfDocument.startPage(pageInfo)
                        canvas = page.canvas
                        drawHeader(currentPageNum)
                        yPosition = 120f
                    }
                }
                yPosition += 15f
            }
            
            // SECTION 2: ÉTAT GLOBAL DE L'INVENTAIRE
            if (yPosition > 700f) {
                pdfDocument.finishPage(page)
                currentPageNum++
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                drawHeader(currentPageNum)
                yPosition = 120f
            }
            
            boldPaint.color = primaryColor
            boldPaint.textSize = 12f
            boldPaint.isFakeBoldText = true
            canvas.drawText("2. ÉTAT DÉTAILLÉ DE L'INVENTAIRE GLOBAL", 30f, yPosition, boldPaint)
            yPosition += 15f
            
            paint.color = primaryColor
            paint.strokeWidth = 1f
            canvas.drawLine(30f, yPosition - 5f, 565f, yPosition - 5f, paint)
            
            // Inventory table header background
            paint.color = lightGray
            canvas.drawRect(30f, yPosition - 10f, 565f, yPosition + 10f, paint)
            
            // Inventory table header text
            boldPaint.textSize = 8.5f
            boldPaint.color = primaryColor
            canvas.drawText("SKU", 35f, yPosition + 3f, boldPaint)
            canvas.drawText("PRODUIT", 100f, yPosition + 3f, boldPaint)
            canvas.drawText("CATÉGORIE", 250f, yPosition + 3f, boldPaint)
            canvas.drawText("QUANTITÉ", 350f, yPosition + 3f, boldPaint)
            canvas.drawText("PRIX UNITAIRE", 430f, yPosition + 3f, boldPaint)
            canvas.drawText("VALEUR (FCFA)", 500f, yPosition + 3f, boldPaint)
            
            yPosition += 25f
            
            val doubleFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            
            products.forEach { prod ->
                textPaint.color = AndroidColor.BLACK
                textPaint.textSize = 9f
                canvas.drawText(prod.sku, 35f, yPosition, textPaint)
                
                val dispName = if (prod.name.length > 21) prod.name.take(19) + "..." else prod.name
                canvas.drawText(dispName, 100f, yPosition, textPaint)
                canvas.drawText(prod.category, 250f, yPosition, textPaint)
                
                canvas.drawText(prod.quantity.toString(), 350f, yPosition, textPaint)
                canvas.drawText("${prod.price.toLong()} F", 430f, yPosition, textPaint)
                
                val totalValue = (prod.quantity * prod.price).toLong()
                canvas.drawText("$totalValue F", 500f, yPosition, textPaint)
                
                // Row separator
                paint.color = 0xFFE0E0E0.toInt()
                canvas.drawLine(30f, yPosition + 5f, 565f, yPosition + 5f, paint)
                
                yPosition += 18f
                
                // Pagination check
                if (yPosition > 770f) {
                    pdfDocument.finishPage(page)
                    currentPageNum++
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    drawHeader(currentPageNum)
                    yPosition = 120f
                }
            }
            
            // Draw summary metrics box at the very end
            if (yPosition > 700f) {
                pdfDocument.finishPage(page)
                currentPageNum++
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                drawHeader(currentPageNum)
                yPosition = 120f
            }
            
            yPosition += 15f
            paint.color = lightGray
            canvas.drawRect(30f, yPosition, 565f, yPosition + 45f, paint)
            
            boldPaint.color = primaryColor
            boldPaint.textSize = 9.5f
            boldPaint.isFakeBoldText = true
            
            val totalQty = products.sumOf { it.quantity }
            val totalValue = products.sumOf { it.quantity * it.price }.toLong()
            
            canvas.drawText("SYMTHÈSE DU RAPPORT : ", 40f, yPosition + 18f, boldPaint)
            
            textPaint.color = AndroidColor.BLACK
            textPaint.textSize = 9.5f
            canvas.drawText("Nombre d'articles : $totalQty unités  |  Valeur totale active du stock : $totalValue FCFA  |  Alertes actives : ${alerts.size}", 40f, yPosition + 33f, textPaint)
            
            pdfDocument.finishPage(page)
            
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            outputStream.flush()
            outputStream.close()
            
            pushNotification("Rapport de stock PDF exporté : $filename")
            file.absolutePath
        } catch (e: Exception) {
            pushNotification("Échec de l'exportation du rapport PDF: ${e.message}")
            null
        }
    }

    // --- Refresh Application State ---
    fun refreshApp() {
        pushNotification("Application de stock actualisée avec succès. Visualisations 3D en temps réel synchronisées.")
    }

    // --- Gemini Predictive AI Stock Analysis ---
    fun requestAiStockPrediction() {
        viewModelScope.launch {
            _isAiLoading.value = true
            _aiPredictionResult.value = "Analyse prédictive de votre stock en cours par l'IA..."
            
            val productsList = productsFlow.value
            val transactionsList = transactionsFlow.value

            if (productsList.isEmpty()) {
                _aiPredictionResult.value = "Le stock est actuellement vide. Veuillez ajouter ou réinitialiser les produits de démonstration pour effectuer une analyse prédictive."
                _isAiLoading.value = false
                return@launch
            }

            // Construire un prompt riche en français décrivant l'inventaire actuel
            val promptBuilder = StringBuilder()
            promptBuilder.append("Tu es un consultant expert IA en logistique pour les PME en Afrique de l'Ouest (Sénégal, Côte d'Ivoire, Cameroun, etc.). Alloue tes réponses dans la monnaie locale : le Franc CFA (FCFA) uniquement. ")
            promptBuilder.append("Analyse le stock actuel suivant de l'entrepôt et donne des prédictions intelligentes de réapprovisionnement, des alertes de rupture, des recommandations catégorielles et des conseils d'optimisation financière.\n\n")
            promptBuilder.append("Données du Stock Actuel (FCFA):\n")
            
            var totalValue = 0.0
            productsList.forEach { p ->
                val warningStatus = when {
                    p.isOutOfStock() -> "[RUPTURE]"
                    p.isLowStock() -> "[STOCK BAS]"
                    else -> "[OK]"
                }
                val value = p.quantity * p.price
                totalValue += value
                promptBuilder.append("- SKU: ${p.sku} | Nom: ${p.name} | Catégorie: ${p.category} | Qté Actuelle: ${p.quantity} | Prix Unitaire: ${p.price} FCFA | Valeur: $value FCFA | Seuil critique: ${p.minThreshold} | Statut: $warningStatus | Localisation Rayon: ${p.locationShelf} Col: ${p.locationColumn} Niv: ${p.locationLevel}\n")
            }
            promptBuilder.append("\nValeur totale actuelle en stock: $totalValue FCFA\n\n")

            if (transactionsList.isNotEmpty()) {
                promptBuilder.append("Historique des dernières transactions logistiques:\n")
                transactionsList.take(10).forEach { t ->
                    promptBuilder.append("- Type: ${t.type} | Produit: ${t.productName} | Qté: ${t.quantity} | Prix unitaire: ${t.unitPrice} FCFA | Opérateur: ${t.operatorName} | Date: ${SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(t.timestamp))}\n")
                }
                promptBuilder.append("\n")
            }

            promptBuilder.append("Rédige un rapport d'analyse très structuré, moderne et professionnel en français avec les sections suivantes :\n")
            promptBuilder.append("1. 🚨 **Ruptures Actuelles et Menaces Imminentes** (Identifie les produits en alerte ou rupture et l'urgence de passer commande)\n")
            promptBuilder.append("2. 📊 **Analyse d'Activité et Rotation** (Quelles catégories bougent le plus ou mobilisent trop de capital en FCFA)\n")
            promptBuilder.append("3. 💡 **Prédictions d'IA et Seuils de Sécurité conseillés** (Modifications suggérées de seuils basés sur tes prévisions)\n")
            promptBuilder.append("4. 🌍 **Conseil Logistique Local en Afrique** (Par exemple, gestion de chaîne d'approvisionnement, gestion saisonnière ou logistique de transport)\n")
            promptBuilder.append("\nFormatte le texte de manière élégante en utilisant des points clés clairs et gras.")

            val result = GeminiClient.generateStockPrediction(promptBuilder.toString())
            _aiPredictionResult.value = result
            _isAiLoading.value = false
            pushNotification("Analyse prédictive IA générée avec succès.")
        }
    }
}

// --- Factory Factory for ViewModel to provide context & custom constructor arguments ---

class StockViewModelFactory(
    private val repository: StockRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StockViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StockViewModel(repository, context) as T
        }
        throw IllegalArgumentException("ViewModel Class Inconnue")
    }
}
