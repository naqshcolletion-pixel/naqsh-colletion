package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BoutiqueDao {
    // Business Settings
    @Query("SELECT * FROM business_settings WHERE id = 1")
    fun observeSettings(): Flow<BusinessSettingsEntity?>

    @Query("SELECT * FROM business_settings WHERE id = 1")
    suspend fun getSettingsOnce(): BusinessSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: BusinessSettingsEntity)

    // Categories
    @Query("SELECT * FROM categories ORDER BY id ASC")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY id ASC")
    suspend fun getCategoriesOnce(): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategoryById(id: Long)

    // Products & Variants
    @Query("SELECT * FROM products WHERE isDeleted = 0 ORDER BY updatedAt DESC")
    fun observeProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE isDeleted = 1 ORDER BY updatedAt DESC")
    fun observeDeletedProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products ORDER BY id ASC")
    suspend fun getAllProductsIncludingDeletedOnce(): List<ProductEntity>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: Long): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity): Long

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Query("UPDATE products SET isDeleted = 1, updatedAt = :now WHERE id = :id")
    suspend fun softDeleteProduct(id: Long, now: Long = System.currentTimeMillis())

    @Query("UPDATE products SET isDeleted = 0, updatedAt = :now WHERE id = :id")
    suspend fun restoreProduct(id: Long, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun hardDeleteProduct(id: Long)

    @Query("SELECT * FROM product_variants ORDER BY id ASC")
    fun observeAllVariants(): Flow<List<ProductVariantEntity>>

    @Query("SELECT * FROM product_variants WHERE productId = :productId ORDER BY id ASC")
    suspend fun getVariantsForProduct(productId: Long): List<ProductVariantEntity>

    @Query("SELECT * FROM product_variants WHERE id = :variantId")
    suspend fun getVariantById(variantId: Long): ProductVariantEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVariant(variant: ProductVariantEntity): Long

    @Update
    suspend fun updateVariant(variant: ProductVariantEntity)

    @Query("DELETE FROM product_variants WHERE id = :variantId")
    suspend fun deleteVariantById(variantId: Long)

    @Query("DELETE FROM product_variants WHERE productId = :productId")
    suspend fun deleteVariantsForProduct(productId: Long)

    // Customers
    @Query("SELECT * FROM customers WHERE isDeleted = 0 ORDER BY updatedAt DESC")
    fun observeCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE isDeleted = 1 ORDER BY updatedAt DESC")
    fun observeDeletedCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers ORDER BY id ASC")
    suspend fun getAllCustomersOnce(): List<CustomerEntity>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getCustomerById(id: Long): CustomerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity): Long

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Query("UPDATE customers SET isDeleted = 1, updatedAt = :now WHERE id = :id")
    suspend fun softDeleteCustomer(id: Long, now: Long = System.currentTimeMillis())

    @Query("UPDATE customers SET isDeleted = 0, updatedAt = :now WHERE id = :id")
    suspend fun restoreCustomer(id: Long, now: Long = System.currentTimeMillis())

    @Query("SELECT * FROM customer_payments ORDER BY timestamp DESC")
    fun observeCustomerPayments(): Flow<List<CustomerPaymentEntity>>

    @Query("SELECT * FROM customer_payments ORDER BY timestamp DESC")
    suspend fun getAllCustomerPaymentsOnce(): List<CustomerPaymentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomerPayment(payment: CustomerPaymentEntity): Long

    // Suppliers
    @Query("SELECT * FROM suppliers WHERE isDeleted = 0 ORDER BY updatedAt DESC")
    fun observeSuppliers(): Flow<List<SupplierEntity>>

    @Query("SELECT * FROM suppliers WHERE isDeleted = 1 ORDER BY updatedAt DESC")
    fun observeDeletedSuppliers(): Flow<List<SupplierEntity>>

    @Query("SELECT * FROM suppliers ORDER BY id ASC")
    suspend fun getAllSuppliersOnce(): List<SupplierEntity>

    @Query("SELECT * FROM suppliers WHERE id = :id")
    suspend fun getSupplierById(id: Long): SupplierEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplier(supplier: SupplierEntity): Long

    @Update
    suspend fun updateSupplier(supplier: SupplierEntity)

    @Query("UPDATE suppliers SET isDeleted = 1, updatedAt = :now WHERE id = :id")
    suspend fun softDeleteSupplier(id: Long, now: Long = System.currentTimeMillis())

    @Query("UPDATE suppliers SET isDeleted = 0, updatedAt = :now WHERE id = :id")
    suspend fun restoreSupplier(id: Long, now: Long = System.currentTimeMillis())

    @Query("SELECT * FROM supplier_payments ORDER BY timestamp DESC")
    fun observeSupplierPayments(): Flow<List<SupplierPaymentEntity>>

    @Query("SELECT * FROM supplier_payments ORDER BY timestamp DESC")
    suspend fun getAllSupplierPaymentsOnce(): List<SupplierPaymentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplierPayment(payment: SupplierPaymentEntity): Long

    // Purchases
    @Query("SELECT * FROM purchases ORDER BY purchaseDate DESC")
    fun observePurchases(): Flow<List<PurchaseEntity>>

    @Query("SELECT * FROM purchases ORDER BY purchaseDate DESC")
    suspend fun getAllPurchasesOnce(): List<PurchaseEntity>

    @Query("SELECT * FROM purchase_items ORDER BY id ASC")
    fun observeAllPurchaseItems(): Flow<List<PurchaseItemEntity>>

    @Query("SELECT * FROM purchase_items WHERE purchaseId = :purchaseId")
    suspend fun getPurchaseItems(purchaseId: Long): List<PurchaseItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchase(purchase: PurchaseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchaseItem(item: PurchaseItemEntity): Long

    // Sales & Invoices
    @Query("SELECT * FROM sale_invoices ORDER BY timestamp DESC")
    fun observeSales(): Flow<List<SaleInvoiceEntity>>

    @Query("SELECT * FROM sale_invoices ORDER BY timestamp DESC")
    suspend fun getAllSalesOnce(): List<SaleInvoiceEntity>

    @Query("SELECT * FROM sale_invoices WHERE id = :id")
    suspend fun getSaleById(id: Long): SaleInvoiceEntity?

    @Query("SELECT * FROM sale_items ORDER BY id ASC")
    fun observeAllSaleItems(): Flow<List<SaleItemEntity>>

    @Query("SELECT * FROM sale_items WHERE invoiceId = :invoiceId")
    suspend fun getSaleItemsForInvoice(invoiceId: Long): List<SaleItemEntity>

    @Query("SELECT * FROM sale_items WHERE id = :itemId")
    suspend fun getSaleItemById(itemId: Long): SaleItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleInvoice(invoice: SaleInvoiceEntity): Long

    @Update
    suspend fun updateSaleInvoice(invoice: SaleInvoiceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleItem(item: SaleItemEntity): Long

    @Update
    suspend fun updateSaleItem(item: SaleItemEntity)

    // Returns & Exchanges
    @Query("SELECT * FROM returns_exchanges ORDER BY timestamp DESC")
    fun observeReturns(): Flow<List<ReturnExchangeEntity>>

    @Query("SELECT * FROM returns_exchanges ORDER BY timestamp DESC")
    suspend fun getAllReturnsOnce(): List<ReturnExchangeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReturnExchange(entry: ReturnExchangeEntity): Long

    // Expenses & Other Income
    @Query("SELECT * FROM expenses WHERE isDeleted = 0 ORDER BY timestamp DESC")
    fun observeExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    suspend fun getAllExpensesOnce(): List<ExpenseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Query("UPDATE expenses SET isDeleted = 1 WHERE id = :id")
    suspend fun softDeleteExpense(id: Long)

    @Query("SELECT * FROM other_incomes ORDER BY timestamp DESC")
    fun observeOtherIncomes(): Flow<List<OtherIncomeEntity>>

    @Query("SELECT * FROM other_incomes ORDER BY timestamp DESC")
    suspend fun getAllOtherIncomesOnce(): List<OtherIncomeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOtherIncome(income: OtherIncomeEntity): Long

    // Stock Movements
    @Query("SELECT * FROM stock_movements ORDER BY timestamp DESC LIMIT 500")
    fun observeStockMovements(): Flow<List<StockMovementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockMovement(movement: StockMovementEntity): Long

    // Staff
    @Query("SELECT * FROM staff_accounts ORDER BY createdAt DESC")
    fun observeStaff(): Flow<List<StaffEntity>>

    @Query("SELECT * FROM staff_accounts WHERE isActive = 1 ORDER BY name ASC")
    suspend fun getActiveStaffOnce(): List<StaffEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStaff(staff: StaffEntity): Long

    @Update
    suspend fun updateStaff(staff: StaffEntity)

    // Audit Logs
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 300")
    fun observeAuditLogs(): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLogEntity): Long
}
