package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val productId: Int,
    val productName: String,
    val type: String, // "ENTRÉE" ou "SORTIE"
    val quantity: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val operatorName: String = "Opérateur Jean",
    val unitPrice: Double, // Prix unitaire en FCFA lors de la transaction
    val category: String // Catégorie du produit lors de la transaction pour les stats
) {
    val totalAmount: Double get() = quantity * unitPrice
}
