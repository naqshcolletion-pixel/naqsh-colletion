package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "business_settings")
data class BusinessSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val businessName: String = "NAQSH COLLECTION",
    val ownerName: String = "NASIR ALI",
    val businessCategory: String = "MODEST & PAKISTANI WEAR",
    val tagline: String = "THE ART OF MODEST ELEGANCE",
    val ownerMobile: String = "+91 9471477887",
    val address: String = "NAQSH COLLECTION\nC/O Lete Ali Sahab\nNew Colony, Near Pizza Hut,\nMithanpura, Muzaffarpur,\nBihar, India – 842002.",
    val gstin: String = "",
    val defaultGstPercent: Double = 0.0,
    val upiId: String = "",
    val paymentInstructions: String = "Scan the PhonePe QR code or pay via UPI ID. Please verify payment before handover.",
    val customLogoUri: String = "",
    val customQrUri: String = "",
    val invoiceFooterNote: String = "Thank you for shopping with NAQSH COLLECTION. JazakAllahu Khayran!",
    val currencySymbol: String = "₹",
    val language: String = "EN",
    val printerPaperSize: String = "A4",
    val printerName: String = "System Print Service",
    val nextInvoiceSeq: Int = 1001,
    val nextPurchaseSeq: Int = 101,
    val isDarkTheme: Boolean = false,
    val lowStockNotifications: Boolean = true,
    val appLockPinHash: String = ""
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val categoryName: String,
    val imageUri: String = "",
    val sku: String = "",
    val barcode: String = "",
    val purchasePrice: Double = 0.0,
    val sellingPrice: Double = 0.0,
    val size: String = "Free Size",
    val colour: String = "Classic",
    val quantity: Int = 0,
    val supplierId: Long? = null,
    val supplierName: String = "",
    val minStock: Int = 3,
    val description: String = "",
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "product_variants")
data class ProductVariantEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val size: String,
    val colour: String,
    val quantity: Int = 0,
    val skuSuffix: String = ""
)

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val mobile: String,
    val address: String = "",
    val totalPurchases: Double = 0.0,
    val totalPaid: Double = 0.0,
    val outstandingBalance: Double = 0.0,
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "customer_payments")
data class CustomerPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long,
    val customerName: String,
    val amount: Double,
    val paymentMode: String,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "suppliers")
data class SupplierEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val mobile: String,
    val address: String = "",
    val totalPurchased: Double = 0.0,
    val totalPaid: Double = 0.0,
    val remainingBalance: Double = 0.0,
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "supplier_payments")
data class SupplierPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val supplierId: Long,
    val supplierName: String,
    val amount: Double,
    val paymentMode: String,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "purchases")
data class PurchaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val purchaseInvoiceNumber: String,
    val supplierId: Long?,
    val supplierName: String,
    val purchaseDate: Long = System.currentTimeMillis(),
    val totalAmount: Double,
    val paymentMode: String,
    val paidAmount: Double,
    val outstandingAmount: Double,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "purchase_items")
data class PurchaseItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val purchaseId: Long,
    val productId: Long,
    val variantId: Long? = null,
    val productName: String,
    val size: String,
    val colour: String,
    val quantity: Int,
    val purchasePrice: Double,
    val lineTotal: Double
)

@Entity(tableName = "sale_invoices")
data class SaleInvoiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceNumber: String,
    val timestamp: Long = System.currentTimeMillis(),
    val customerId: Long? = null,
    val customerName: String = "Walk-in Customer",
    val customerMobile: String = "",
    val subtotal: Double,
    val itemDiscountTotal: Double = 0.0,
    val billDiscount: Double = 0.0,
    val gstPercent: Double = 0.0,
    val taxAmount: Double = 0.0,
    val grandTotal: Double,
    val amountPaid: Double,
    val remainingBalance: Double,
    val paymentMethod: String,
    val status: String = "COMPLETED", // COMPLETED, PARTIAL_RETURN, RETURNED, CANCELLED
    val staffName: String = "NASIR ALI (Owner)",
    val notes: String = ""
)

@Entity(tableName = "sale_items")
data class SaleItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceId: Long,
    val productId: Long,
    val variantId: Long? = null,
    val productName: String,
    val size: String,
    val colour: String,
    val quantity: Int,
    val returnedQuantity: Int = 0,
    val unitPrice: Double,
    val purchasePriceAtSale: Double,
    val itemDiscount: Double = 0.0,
    val lineTotal: Double
)

@Entity(tableName = "returns_exchanges")
data class ReturnExchangeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceId: Long,
    val invoiceNumber: String,
    val type: String, // FULL_RETURN, PARTIAL_RETURN, EXCHANGE
    val returnedItemId: Long? = null,
    val returnedProductName: String,
    val returnedQty: Int,
    val exchangeProductId: Long? = null,
    val exchangeProductName: String = "",
    val exchangeQty: Int = 0,
    val refundAmount: Double = 0.0,
    val reason: String = "",
    val processedBy: String = "Owner",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String,
    val amount: Double,
    val paymentMode: String,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

@Entity(tableName = "other_incomes")
data class OtherIncomeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val source: String,
    val amount: Double,
    val paymentMode: String,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "stock_movements")
data class StockMovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val productName: String,
    val variantLabel: String = "",
    val changeQty: Int,
    val reason: String,
    val referenceNumber: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "staff_accounts")
data class StaffEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val mobile: String,
    val roleTitle: String = "Boutique Staff",
    val pinHash: String,
    val isActive: Boolean = true,
    val canMakeSales: Boolean = true,
    val canManageInventory: Boolean = true,
    val canManagePurchases: Boolean = false,
    val canManageCustomers: Boolean = true,
    val canViewBasicReports: Boolean = false,
    val canProcessReturns: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val actorName: String,
    val actorRole: String,
    val action: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)
