package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sku: String, // Code Barre / QR ou SKU généré
    val name: String,
    val quantity: Int,
    val price: Double, // En FCFA
    val category: String, // Électronique, Alimentaire, etc.
    val locationShelf: Int, // 1 à 3 (Rayon)
    val locationColumn: Int, // 1 à 4 (Colonne du meuble)
    val locationLevel: Int, // 1 à 3 (Étage de l'étagère)
    val minThreshold: Int, // Seuil d'alerte pour les alertes de stock
    val entryDate: Long = System.currentTimeMillis(),
    val exitDate: Long? = null,
    val description: String = "",
    val ownerEmail: String = "operator.jean@stock3d.com"
) {
    // Helper to check if stock is in warning state
    fun isLowStock(): Boolean = quantity > 0 && quantity <= minThreshold
    fun isOutOfStock(): Boolean = quantity <= 0
}
