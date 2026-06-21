package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    // --- Products ---
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProductsFlow(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE ownerEmail = :email ORDER BY name ASC")
    fun getProductsByOwnerFlow(email: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products ORDER BY name ASC")
    suspend fun getAllProductsList(): List<ProductEntity>

    @Query("SELECT * FROM products WHERE ownerEmail = :email ORDER BY name ASC")
    suspend fun getProductsByOwnerList(email: String): List<ProductEntity>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: Int): ProductEntity?

    @Query("SELECT * FROM products WHERE sku = :sku")
    suspend fun getProductBySku(sku: String): ProductEntity?

    @Query("SELECT * FROM products WHERE sku = :sku AND ownerEmail = :email")
    suspend fun getProductBySkuAndOwner(sku: String, email: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity): Long

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Delete
    suspend fun deleteProduct(product: ProductEntity)

    // --- Transactions ---
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactionsFlow(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE ownerEmail = :email ORDER BY timestamp DESC")
    fun getTransactionsByOwnerFlow(email: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    suspend fun getAllTransactionsList(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE ownerEmail = :email ORDER BY timestamp DESC")
    suspend fun getTransactionsByOwnerList(email: String): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    // Clear all tables (for demo reset)
    @Query("DELETE FROM products")
    suspend fun clearProducts()

    @Query("DELETE FROM transactions")
    suspend fun clearTransactions()

    @Query("DELETE FROM products WHERE ownerEmail = :email")
    suspend fun clearProductsByOwner(email: String)

    @Query("DELETE FROM transactions WHERE ownerEmail = :email")
    suspend fun clearTransactionsByOwner(email: String)
}
