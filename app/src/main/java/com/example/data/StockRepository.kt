package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StockRepository(private val productDao: ProductDao) {

    val allProducts: Flow<List<ProductEntity>> = productDao.getAllProductsFlow()
    val allTransactions: Flow<List<TransactionEntity>> = productDao.getAllTransactionsFlow()

    fun getProductsByOwner(email: String): Flow<List<ProductEntity>> =
        productDao.getProductsByOwnerFlow(email)

    fun getTransactionsByOwner(email: String): Flow<List<TransactionEntity>> =
        productDao.getTransactionsByOwnerFlow(email)

    suspend fun getProductById(id: Int): ProductEntity? = withContext(Dispatchers.IO) {
        productDao.getProductById(id)
    }

    suspend fun getProductBySku(sku: String): ProductEntity? = withContext(Dispatchers.IO) {
        productDao.getProductBySku(sku)
    }

    suspend fun getProductBySkuAndOwner(sku: String, email: String): ProductEntity? = withContext(Dispatchers.IO) {
        productDao.getProductBySkuAndOwner(sku, email)
    }

    // Insert new product with transaction log
    suspend fun addProduct(product: ProductEntity, operatorName: String) = withContext(Dispatchers.IO) {
        val id = productDao.insertProduct(product)
        if (product.quantity > 0) {
            val transaction = TransactionEntity(
                productId = id.toInt(),
                productName = product.name,
                type = "ENTRÉE",
                quantity = product.quantity,
                unitPrice = product.price,
                category = product.category,
                operatorName = operatorName,
                ownerEmail = product.ownerEmail
            )
            productDao.insertTransaction(transaction)
        }
        id
    }

    // Update existing product and optionally log restock / exit transactions
    suspend fun updateProductWithTransaction(
        product: ProductEntity,
        quantityDelta: Int, // Positif si ajout (ENTRÉE), Négatif si retrait (SORTIE)
        operatorName: String
    ) = withContext(Dispatchers.IO) {
        productDao.updateProduct(product)
        if (quantityDelta != 0) {
            val type = if (quantityDelta > 0) "ENTRÉE" else "SORTIE"
            val transaction = TransactionEntity(
                productId = product.id,
                productName = product.name,
                type = type,
                quantity = kotlin.math.abs(quantityDelta),
                unitPrice = product.price,
                category = product.category,
                operatorName = operatorName,
                ownerEmail = product.ownerEmail
            )
            productDao.insertTransaction(transaction)
        }
    }

    // Delete product and log withdrawal (optional, but let's delete catalog item)
    suspend fun deleteProduct(product: ProductEntity) = withContext(Dispatchers.IO) {
        productDao.deleteProduct(product)
    }

    // Clear entire catalog and transactions for a specific owner
    suspend fun clearStoreByOwner(email: String) = withContext(Dispatchers.IO) {
        productDao.clearProductsByOwner(email)
        productDao.clearTransactionsByOwner(email)
    }

    // Helper to log standalone transaction
    suspend fun logTransaction(transaction: TransactionEntity) = withContext(Dispatchers.IO) {
        productDao.insertTransaction(transaction)
    }

    // Reset database with demo items for a specific owner
    suspend fun resetDatabaseToDemo(context: Context, ownerEmail: String) = withContext(Dispatchers.IO) {
        productDao.clearProductsByOwner(ownerEmail)
        productDao.clearTransactionsByOwner(ownerEmail)
        
        val p1 = ProductEntity(
            name = "iPhone 15 Pro",
            sku = "8898990124",
            quantity = 14,
            price = 850000.0,
            category = "Électronique",
            locationShelf = 1,
            locationColumn = 1,
            locationLevel = 3,
            minThreshold = 5,
            description = "Smartphone haut de gamme Apple avec châssis en titane.",
            ownerEmail = ownerEmail
        )
        val p2 = ProductEntity(
            name = "Café robusta d'Afrique",
            sku = "CAF89112",
            quantity = 45,
            price = 3500.0,
            category = "Alimentaire",
            locationShelf = 1,
            locationColumn = 3,
            locationLevel = 1,
            minThreshold = 10,
            description = "Pur café robusta en grains, torréfié localement.",
            ownerEmail = ownerEmail
        )
        val p3 = ProductEntity(
            name = "Chocolat Noir Premium 80%",
            sku = "7613035824",
            quantity = 3,
            price = 1500.0,
            category = "Alimentaire",
            locationShelf = 2,
            locationColumn = 2,
            locationLevel = 2,
            minThreshold = 8,
            description = "Chocolat de couverture premium et bio.",
            ownerEmail = ownerEmail
        )
        val p4 = ProductEntity(
            name = "Enceinte Connectée JBL",
            sku = "6925281987",
            quantity = 0,
            price = 75000.0,
            category = "Électronique",
            locationShelf = 2,
            locationColumn = 4,
            locationLevel = 3,
            minThreshold = 3,
            description = "Enceinte étanche Bluetooth à son puissant.",
            ownerEmail = ownerEmail
        )
        val p5 = ProductEntity(
            name = "Sneakers Air Premium",
            sku = "1931548174",
            quantity = 22,
            price = 65000.0,
            category = "Vêtements",
            locationShelf = 3,
            locationColumn = 2,
            locationLevel = 1,
            minThreshold = 4,
            description = "Paire de chaussures de sport stylées et confortables.",
            ownerEmail = ownerEmail
        )
        val p6 = ProductEntity(
            name = "Huile d'Argan Bio 100ml",
            sku = "HUI54129",
            quantity = 18,
            price = 12000.0,
            category = "Cosmétiques",
            locationShelf = 3,
            locationColumn = 3,
            locationLevel = 2,
            minThreshold = 6,
            description = "Élixir pur d'huile d'argan pour nutrition visage et cheveux.",
            ownerEmail = ownerEmail
        )

        val id1 = productDao.insertProduct(p1)
        val id2 = productDao.insertProduct(p2)
        val id3 = productDao.insertProduct(p3)
        val id4 = productDao.insertProduct(p4)
        val id5 = productDao.insertProduct(p5)
        val id6 = productDao.insertProduct(p6)

        productDao.insertTransaction(
            TransactionEntity(
                productId = id1.toInt(),
                productName = p1.name,
                type = "ENTRÉE",
                quantity = 20,
                unitPrice = p1.price,
                category = p1.category,
                ownerEmail = ownerEmail
            )
        )
        productDao.insertTransaction(
            TransactionEntity(
                productId = id1.toInt(),
                productName = p1.name,
                type = "SORTIE",
                quantity = 6,
                unitPrice = p1.price,
                category = p1.category,
                operatorName = "Opérateur Jean",
                ownerEmail = ownerEmail
            )
        )
    }
}
