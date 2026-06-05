package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [ProductEntity::class, TransactionEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract val productDao: ProductDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "stock3d_database"
                )
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database.productDao)
                }
            }
        }

        suspend fun populateDatabase(productDao: ProductDao) {
            // Insérer des produits de démonstration distribués dans l'entrepôt 3D
            val p1 = ProductEntity(
                name = "iPhone 15 Pro",
                sku = "8898990124",
                quantity = 14,
                price = 850000.0, // En FCFA
                category = "Électronique",
                locationShelf = 1,
                locationColumn = 1,
                locationLevel = 3,
                minThreshold = 5,
                description = "Smartphone haut de gamme Apple avec châssis en titane."
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
                description = "Pur café robusta en grains, torréfié localement."
            )
            val p3 = ProductEntity(
                name = "Chocolat Noir Premium 80%",
                sku = "7613035824",
                quantity = 3, // Low stock!
                price = 1500.0,
                category = "Alimentaire",
                locationShelf = 2,
                locationColumn = 2,
                locationLevel = 2,
                minThreshold = 8,
                description = "Chocolat de couverture premium et bio."
            )
            val p4 = ProductEntity(
                name = "Enceinte Connectée JBL",
                sku = "6925281987",
                quantity = 0, // Out of stock!
                price = 75000.0,
                category = "Électronique",
                locationShelf = 2,
                locationColumn = 4,
                locationLevel = 3,
                minThreshold = 3,
                description = "Enceinte étanche Bluetooth à son puissant."
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
                description = "Paire de chaussures de sport stylées et confortables."
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
                description = "Élixir pur d'huile d'argan pour nutrition visage et cheveux."
            )

            // Insérer les produits
            val id1 = productDao.insertProduct(p1)
            val id2 = productDao.insertProduct(p2)
            val id3 = productDao.insertProduct(p3)
            val id4 = productDao.insertProduct(p4)
            val id5 = productDao.insertProduct(p5)
            val id6 = productDao.insertProduct(p6)

            // Insérer quelques transactions historiques
            productDao.insertTransaction(
                TransactionEntity(
                    productId = id1.toInt(),
                    productName = p1.name,
                    type = "ENTRÉE",
                    quantity = 20,
                    unitPrice = p1.price,
                    category = p1.category
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
                    operatorName = "Opérateur Jean"
                )
            )
            productDao.insertTransaction(
                TransactionEntity(
                    productId = id3.toInt(),
                    productName = p3.name,
                    type = "ENTRÉE",
                    quantity = 3,
                    unitPrice = p3.price,
                    category = p3.category
                )
            )
            productDao.insertTransaction(
                TransactionEntity(
                    productId = id4.toInt(),
                    productName = p4.name,
                    type = "ENTRÉE",
                    quantity = 5,
                    unitPrice = p4.price,
                    category = p4.category
                )
            )
            productDao.insertTransaction(
                TransactionEntity(
                    productId = id4.toInt(),
                    productName = p4.name,
                    type = "SORTIE",
                    quantity = 5, // All sold out
                    unitPrice = p4.price,
                    category = p4.category,
                    operatorName = "Opératrice Marie"
                )
            )
        }
    }
}
