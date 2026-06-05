package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class StockViewModel(
    private val repository: StockRepository,
    private val context: Context
) : ViewModel() {

    // --- Flows from Room DB ---
    val productsFlow: StateFlow<List<ProductEntity>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactionsFlow: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
            val matchesCategory = category == "Tous" || product.category.equals(category, ignoreCase = true)
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
                description = description
            )
            repository.addProduct(newProduct, operator)
            pushNotification("Nouveau produit ajouté : $name ($formattedSku) au Rayon $locationShelf")
            checkThresholds()
        }
    }

    fun updateProductQty(product: ProductEntity, delta: Int, operator: String = "Opérateur Jean") {
        viewModelScope.launch {
            val newQty = (product.quantity + delta).coerceAtLeast(0)
            val updated = product.copy(
                quantity = newQty,
                exitDate = if (delta < 0) System.currentTimeMillis() else product.exitDate
            )
            repository.updateProductWithTransaction(updated, delta, operator)
            
            val action = if (delta > 0) "approvisionné de +$delta unités" else "déstocké de $delta unités"
            pushNotification("Produit ${product.name} $action. Stock actuel : $newQty")
            checkThresholds()
        }
    }

    fun registerSale(items: List<Pair<ProductEntity, Int>>, operator: String = "Opérateur Jean") {
        viewModelScope.launch {
            var totalAmount = 0.0
            items.forEach { (prod, qtyToSell) ->
                val newQty = (prod.quantity - qtyToSell).coerceAtLeast(0)
                val updated = prod.copy(
                    quantity = newQty,
                    exitDate = System.currentTimeMillis()
                )
                // qtyDelta is negative for stock exits
                repository.updateProductWithTransaction(updated, -qtyToSell, operator)
                totalAmount += qtyToSell * prod.price
            }
            val totalFormatted = String.format(Locale.FRANCE, "%,.0f", totalAmount).replace(",", " ")
            pushNotification("Vente validée : $totalFormatted FCFA encaissés. Stock mis à jour.")
            checkThresholds()
        }
    }

    fun updateProductDetails(product: ProductEntity, operator: String = "Opérateur Jean") {
        viewModelScope.launch {
            // Check delta
            val oldProduct = repository.getProductById(product.id)
            if (oldProduct != null) {
                val delta = product.quantity - oldProduct.quantity
                repository.updateProductWithTransaction(product, delta, operator)
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
            repository.resetDatabaseToDemo(context)
            pushNotification("Données et historique de stock réinitialisés à l'état de démonstration.")
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
            val found = repository.getProductBySku(barcode)
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
