package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        BusinessSettingsEntity::class,
        CategoryEntity::class,
        ProductEntity::class,
        ProductVariantEntity::class,
        CustomerEntity::class,
        CustomerPaymentEntity::class,
        SupplierEntity::class,
        SupplierPaymentEntity::class,
        PurchaseEntity::class,
        PurchaseItemEntity::class,
        SaleInvoiceEntity::class,
        SaleItemEntity::class,
        ReturnExchangeEntity::class,
        ExpenseEntity::class,
        OtherIncomeEntity::class,
        StockMovementEntity::class,
        StaffEntity::class,
        AuditLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun boutiqueDao(): BoutiqueDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "naqsh_collection_boutique.db"
                )
                    .addCallback(SeedDatabaseCallback(context.applicationContext))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class SeedDatabaseCallback(private val appContext: Context) : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            CoroutineScope(Dispatchers.IO).launch {
                val database = getInstance(appContext)
                seedInitialBoutiqueCatalog(database.boutiqueDao())
            }
        }
    }
}

suspend fun seedInitialBoutiqueCatalog(dao: BoutiqueDao) {
    if (dao.getSettingsOnce() == null) {
        dao.upsertSettings(BusinessSettingsEntity())
    }
    if (dao.getCategoriesOnce().isEmpty()) {
        val defaultCategories = listOf(
            "ABAYA",
            "HIJAB",
            "NAQAB AND BURQA",
            "CO-ORD SET",
            "PAKISTANI SUITS",
            "OTHER PRODUCTS"
        )
        defaultCategories.forEach { name ->
            dao.insertCategory(CategoryEntity(name = name, isDefault = true))
        }
    }
    if (dao.getAllProductsIncludingDeletedOnce().isEmpty()) {
        val sampleCatalog = listOf(
            // ABAYA
            Triple("ABAYA", "Saudi Abaya", "Classic Saudi flowing Nida abaya"),
            Triple("ABAYA", "Dubai Abaya", "Luxury Dubai embroidered front-pleated abaya"),
            Triple("ABAYA", "Turkish Abaya", "Modest Turkish cut belted abaya"),
            Triple("ABAYA", "Butterfly Abaya", "Wide sleeve flared butterfly abaya"),
            Triple("ABAYA", "Open Abaya", "Front-open shrug style abaya with matching belt"),
            Triple("ABAYA", "Closed Abaya", "Full-coverage closed A-line modest abaya"),
            Triple("ABAYA", "Nida Abaya", "Breathable Korean Nida fabric abaya"),
            // HIJAB
            Triple("HIJAB", "Georgette Hijab", "Lightweight matte georgette wrap hijab"),
            Triple("HIJAB", "Jersey Hijab", "Stretchable everyday soft jersey hijab"),
            Triple("HIJAB", "Modal Hijab", "Silky soft breathable modal hijab"),
            Triple("HIJAB", "Chiffon Hijab", "Elegant drape crinkle chiffon hijab"),
            Triple("HIJAB", "Premium Hijab", "Luxury shimmer & pearl border occasion hijab"),
            Triple("HIJAB", "Instant Hijab", "Ready-to-wear stitched instant hijab"),
            // NAQAB AND BURQA
            Triple("NAQAB AND BURQA", "One Layer Naqab", "Single layer breathable tie-back naqab"),
            Triple("NAQAB AND BURQA", "Two Layer Naqab", "Double layer modest georgette naqab"),
            Triple("NAQAB AND BURQA", "Three Layer Naqab", "Full three layer Saudi style naqab"),
            Triple("NAQAB AND BURQA", "Butterfly Naqab", "Long flared butterfly cape naqab"),
            Triple("NAQAB AND BURQA", "Burqa", "Traditional full-length modest burqa"),
            // CO-ORD SET
            Triple("CO-ORD SET", "Two Piece Co-ord Set", "Modest tunic & wide-leg trouser two piece set"),
            Triple("CO-ORD SET", "Three Piece Co-ord Set", "Tunic, trouser & matching stole three piece set"),
            Triple("CO-ORD SET", "Printed Co-ord Set", "Botanical & geometric modest printed co-ord"),
            Triple("CO-ORD SET", "Premium Co-ord Set", "Luxury crepe festive modest co-ord set"),
            // PAKISTANI SUITS
            Triple("PAKISTANI SUITS", "Lawn Pakistani Suit", "Original summer breathable lawn printed suit"),
            Triple("PAKISTANI SUITS", "Cotton Pakistani Suit", "Soft pure cotton embroidered Pakistani suit"),
            Triple("PAKISTANI SUITS", "Chiffon Pakistani Suit", "Festive embroidered chiffon dupatta Pakistani suit"),
            Triple("PAKISTANI SUITS", "Embroidered Pakistani Suit", "Heavy thread & zari embroidered Pakistani suit"),
            Triple("PAKISTANI SUITS", "Three Piece Pakistani Suit", "Complete unstitched/stitched 3-piece Pakistani suit"),
            // OTHER PRODUCTS
            Triple("OTHER PRODUCTS", "Inner Caps", "Stretchable cotton anti-slip under-hijab cap"),
            Triple("OTHER PRODUCTS", "Dupatta", "Modest full-width chiffon & lawn dupatta"),
            Triple("OTHER PRODUCTS", "Stoles", "Everyday casual & formal modest stole"),
            Triple("OTHER PRODUCTS", "Sleeves", "Full-arm breathable cotton modest sleeves"),
            Triple("OTHER PRODUCTS", "Scrunchies", "Voluminous silk & cotton hijab bun scrunchie"),
            Triple("OTHER PRODUCTS", "Accessories", "Magnetic hijab pins, brooches & garment care")
        )
        sampleCatalog.forEachIndexed { idx, (cat, name, desc) ->
            val skuCode = "NQ-${cat.take(3).uppercase()}-${101 + idx}"
            dao.insertProduct(
                ProductEntity(
                    name = name,
                    categoryName = cat,
                    sku = skuCode,
                    purchasePrice = 0.0,
                    sellingPrice = 0.0,
                    size = if (cat == "HIJAB" || cat == "OTHER PRODUCTS") "Standard" else "54 / M",
                    colour = "Black / Emerald",
                    quantity = 0,
                    minStock = 3,
                    description = desc
                )
            )
        }
    }
}
