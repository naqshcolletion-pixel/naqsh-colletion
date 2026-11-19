package com.example.data.repository

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.example.data.local.*
import com.example.util.Formatters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class BackupManager(private val context: Context, private val db: AppDatabase) {
    private val dao = db.boutiqueDao()

    suspend fun exportFullBackupJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("app", "NAQSH COLLECTION")
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())

        val settings = dao.getSettingsOnce() ?: BusinessSettingsEntity()
        root.put("settings", JSONObject().apply {
            put("businessName", settings.businessName)
            put("ownerName", settings.ownerName)
            put("businessCategory", settings.businessCategory)
            put("tagline", settings.tagline)
            put("ownerMobile", settings.ownerMobile)
            put("address", settings.address)
            put("gstin", settings.gstin)
            put("defaultGstPercent", settings.defaultGstPercent)
            put("upiId", settings.upiId)
            put("paymentInstructions", settings.paymentInstructions)
            put("invoiceFooterNote", settings.invoiceFooterNote)
            put("currencySymbol", settings.currencySymbol)
            put("language", settings.language)
            put("printerPaperSize", settings.printerPaperSize)
            put("nextInvoiceSeq", settings.nextInvoiceSeq)
            put("nextPurchaseSeq", settings.nextPurchaseSeq)
        })

        root.put("categories", JSONArray().apply {
            dao.getCategoriesOnce().forEach { c ->
                put(JSONObject().apply {
                    put("id", c.id)
                    put("name", c.name)
                    put("isDefault", c.isDefault)
                    put("createdAt", c.createdAt)
                })
            }
        })

        root.put("products", JSONArray().apply {
            dao.getAllProductsIncludingDeletedOnce().forEach { p ->
                put(JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("categoryName", p.categoryName)
                    put("sku", p.sku)
                    put("barcode", p.barcode)
                    put("purchasePrice", p.purchasePrice)
                    put("sellingPrice", p.sellingPrice)
                    put("size", p.size)
                    put("colour", p.colour)
                    put("quantity", p.quantity)
                    put("supplierName", p.supplierName)
                    put("minStock", p.minStock)
                    put("description", p.description)
                    put("isDeleted", p.isDeleted)
                    put("createdAt", p.createdAt)
                    put("updatedAt", p.updatedAt)
                })
            }
        })

        root.put("customers", JSONArray().apply {
            dao.getAllCustomersOnce().forEach { c ->
                put(JSONObject().apply {
                    put("id", c.id)
                    put("name", c.name)
                    put("mobile", c.mobile)
                    put("address", c.address)
                    put("totalPurchases", c.totalPurchases)
                    put("totalPaid", c.totalPaid)
                    put("outstandingBalance", c.outstandingBalance)
                    put("isDeleted", c.isDeleted)
                    put("createdAt", c.createdAt)
                    put("updatedAt", c.updatedAt)
                })
            }
        })

        root.put("suppliers", JSONArray().apply {
            dao.getAllSuppliersOnce().forEach { s ->
                put(JSONObject().apply {
                    put("id", s.id)
                    put("name", s.name)
                    put("mobile", s.mobile)
                    put("address", s.address)
                    put("totalPurchased", s.totalPurchased)
                    put("totalPaid", s.totalPaid)
                    put("remainingBalance", s.remainingBalance)
                    put("isDeleted", s.isDeleted)
                    put("createdAt", s.createdAt)
                    put("updatedAt", s.updatedAt)
                })
            }
        })

        root.put("sales", JSONArray().apply {
            dao.getAllSalesOnce().forEach { inv ->
                val items = dao.getSaleItemsForInvoice(inv.id)
                put(JSONObject().apply {
                    put("id", inv.id)
                    put("invoiceNumber", inv.invoiceNumber)
                    put("timestamp", inv.timestamp)
                    put("customerId", inv.customerId ?: JSONObject.NULL)
                    put("customerName", inv.customerName)
                    put("customerMobile", inv.customerMobile)
                    put("subtotal", inv.subtotal)
                    put("itemDiscountTotal", inv.itemDiscountTotal)
                    put("billDiscount", inv.billDiscount)
                    put("gstPercent", inv.gstPercent)
                    put("taxAmount", inv.taxAmount)
                    put("grandTotal", inv.grandTotal)
                    put("amountPaid", inv.amountPaid)
                    put("remainingBalance", inv.remainingBalance)
                    put("paymentMethod", inv.paymentMethod)
                    put("status", inv.status)
                    put("staffName", inv.staffName)
                    put("notes", inv.notes)
                    put("items", JSONArray().apply {
                        items.forEach { si ->
                            put(JSONObject().apply {
                                put("productId", si.productId)
                                put("productName", si.productName)
                                put("size", si.size)
                                put("colour", si.colour)
                                put("quantity", si.quantity)
                                put("returnedQuantity", si.returnedQuantity)
                                put("unitPrice", si.unitPrice)
                                put("purchasePriceAtSale", si.purchasePriceAtSale)
                                put("itemDiscount", si.itemDiscount)
                                put("lineTotal", si.lineTotal)
                            })
                        }
                    })
                })
            }
        })

        root.put("expenses", JSONArray().apply {
            dao.getAllExpensesOnce().forEach { e ->
                put(JSONObject().apply {
                    put("id", e.id)
                    put("title", e.title)
                    put("category", e.category)
                    put("amount", e.amount)
                    put("paymentMode", e.paymentMode)
                    put("note", e.note)
                    put("timestamp", e.timestamp)
                    put("isDeleted", e.isDeleted)
                })
            }
        })

        root.put("otherIncomes", JSONArray().apply {
            dao.getAllOtherIncomesOnce().forEach { inc ->
                put(JSONObject().apply {
                    put("id", inc.id)
                    put("title", inc.title)
                    put("source", inc.source)
                    put("amount", inc.amount)
                    put("paymentMode", inc.paymentMode)
                    put("note", inc.note)
                    put("timestamp", inc.timestamp)
                })
            }
        })

        root.toString(2)
    }

    suspend fun saveLocalBackupFile(): File = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "backups").apply { mkdirs() }
        val file = File(dir, "naqsh_backup_${Formatters.fileTimestamp()}.json")
        file.writeText(exportFullBackupJson())
        file
    }

    suspend fun restoreFromJson(jsonText: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonText)
            if (root.optString("app") != "NAQSH COLLECTION") {
                return@withContext Result.failure(IllegalArgumentException("Invalid Naqsh Collection backup file."))
            }
            db.withTransaction {
                val sObj = root.optJSONObject("settings")
                if (sObj != null) {
                    val current = dao.getSettingsOnce() ?: BusinessSettingsEntity()
                    dao.upsertSettings(
                        current.copy(
                            businessName = sObj.optString("businessName", current.businessName),
                            ownerName = sObj.optString("ownerName", current.ownerName),
                            businessCategory = sObj.optString("businessCategory", current.businessCategory),
                            tagline = sObj.optString("tagline", current.tagline),
                            ownerMobile = sObj.optString("ownerMobile", current.ownerMobile),
                            address = sObj.optString("address", current.address),
                            gstin = sObj.optString("gstin", current.gstin),
                            defaultGstPercent = sObj.optDouble("defaultGstPercent", current.defaultGstPercent),
                            upiId = sObj.optString("upiId", current.upiId),
                            paymentInstructions = sObj.optString("paymentInstructions", current.paymentInstructions),
                            invoiceFooterNote = sObj.optString("invoiceFooterNote", current.invoiceFooterNote),
                            currencySymbol = sObj.optString("currencySymbol", current.currencySymbol),
                            language = sObj.optString("language", current.language),
                            printerPaperSize = sObj.optString("printerPaperSize", current.printerPaperSize),
                            nextInvoiceSeq = sObj.optInt("nextInvoiceSeq", current.nextInvoiceSeq),
                            nextPurchaseSeq = sObj.optInt("nextPurchaseSeq", current.nextPurchaseSeq)
                        )
                    )
                }

                val cats = root.optJSONArray("categories")
                if (cats != null) {
                    for (i in 0 until cats.length()) {
                        val c = cats.getJSONObject(i)
                        dao.insertCategory(
                            CategoryEntity(
                                id = c.optLong("id"),
                                name = c.getString("name"),
                                isDefault = c.optBoolean("isDefault", false),
                                createdAt = c.optLong("createdAt", System.currentTimeMillis())
                            )
                        )
                    }
                }

                val prods = root.optJSONArray("products")
                if (prods != null) {
                    for (i in 0 until prods.length()) {
                        val p = prods.getJSONObject(i)
                        dao.insertProduct(
                            ProductEntity(
                                id = p.optLong("id"),
                                name = p.getString("name"),
                                categoryName = p.getString("categoryName"),
                                sku = p.optString("sku"),
                                barcode = p.optString("barcode"),
                                purchasePrice = p.optDouble("purchasePrice", 0.0),
                                sellingPrice = p.optDouble("sellingPrice", 0.0),
                                size = p.optString("size", "Standard"),
                                colour = p.optString("colour", "Classic"),
                                quantity = p.optInt("quantity", 0),
                                supplierName = p.optString("supplierName"),
                                minStock = p.optInt("minStock", 3),
                                description = p.optString("description"),
                                isDeleted = p.optBoolean("isDeleted", false),
                                createdAt = p.optLong("createdAt", System.currentTimeMillis()),
                                updatedAt = p.optLong("updatedAt", System.currentTimeMillis())
                            )
                        )
                    }
                }

                val custs = root.optJSONArray("customers")
                if (custs != null) {
                    for (i in 0 until custs.length()) {
                        val c = custs.getJSONObject(i)
                        dao.insertCustomer(
                            CustomerEntity(
                                id = c.optLong("id"),
                                name = c.getString("name"),
                                mobile = c.optString("mobile"),
                                address = c.optString("address"),
                                totalPurchases = c.optDouble("totalPurchases", 0.0),
                                totalPaid = c.optDouble("totalPaid", 0.0),
                                outstandingBalance = c.optDouble("outstandingBalance", 0.0),
                                isDeleted = c.optBoolean("isDeleted", false),
                                createdAt = c.optLong("createdAt", System.currentTimeMillis()),
                                updatedAt = c.optLong("updatedAt", System.currentTimeMillis())
                            )
                        )
                    }
                }

                val sups = root.optJSONArray("suppliers")
                if (sups != null) {
                    for (i in 0 until sups.length()) {
                        val s = sups.getJSONObject(i)
                        dao.insertSupplier(
                            SupplierEntity(
                                id = s.optLong("id"),
                                name = s.getString("name"),
                                mobile = s.optString("mobile"),
                                address = s.optString("address"),
                                totalPurchased = s.optDouble("totalPurchased", 0.0),
                                totalPaid = s.optDouble("totalPaid", 0.0),
                                remainingBalance = s.optDouble("remainingBalance", 0.0),
                                isDeleted = s.optBoolean("isDeleted", false),
                                createdAt = s.optLong("createdAt", System.currentTimeMillis()),
                                updatedAt = s.optLong("updatedAt", System.currentTimeMillis())
                            )
                        )
                    }
                }

                val exps = root.optJSONArray("expenses")
                if (exps != null) {
                    for (i in 0 until exps.length()) {
                        val e = exps.getJSONObject(i)
                        dao.insertExpense(
                            ExpenseEntity(
                                id = e.optLong("id"),
                                title = e.getString("title"),
                                category = e.getString("category"),
                                amount = e.getDouble("amount"),
                                paymentMode = e.optString("paymentMode", "Cash"),
                                note = e.optString("note"),
                                timestamp = e.optLong("timestamp", System.currentTimeMillis()),
                                isDeleted = e.optBoolean("isDeleted", false)
                            )
                        )
                    }
                }

                val sales = root.optJSONArray("sales")
                if (sales != null) {
                    for (i in 0 until sales.length()) {
                        val inv = sales.getJSONObject(i)
                        val savedInvId = dao.insertSaleInvoice(
                            SaleInvoiceEntity(
                                id = inv.optLong("id"),
                                invoiceNumber = inv.getString("invoiceNumber"),
                                timestamp = inv.getLong("timestamp"),
                                customerId = if (inv.isNull("customerId")) null else inv.optLong("customerId"),
                                customerName = inv.optString("customerName", "Walk-in Customer"),
                                customerMobile = inv.optString("customerMobile"),
                                subtotal = inv.optDouble("subtotal", 0.0),
                                itemDiscountTotal = inv.optDouble("itemDiscountTotal", 0.0),
                                billDiscount = inv.optDouble("billDiscount", 0.0),
                                gstPercent = inv.optDouble("gstPercent", 0.0),
                                taxAmount = inv.optDouble("taxAmount", 0.0),
                                grandTotal = inv.optDouble("grandTotal", 0.0),
                                amountPaid = inv.optDouble("amountPaid", 0.0),
                                remainingBalance = inv.optDouble("remainingBalance", 0.0),
                                paymentMethod = inv.optString("paymentMethod", "Cash"),
                                status = inv.optString("status", "COMPLETED"),
                                staffName = inv.optString("staffName", "Owner"),
                                notes = inv.optString("notes")
                            )
                        )
                        val items = inv.optJSONArray("items")
                        if (items != null && dao.getSaleItemsForInvoice(savedInvId).isEmpty()) {
                            for (j in 0 until items.length()) {
                                val it = items.getJSONObject(j)
                                dao.insertSaleItem(
                                    SaleItemEntity(
                                        invoiceId = savedInvId,
                                        productId = it.optLong("productId"),
                                        productName = it.optString("productName"),
                                        size = it.optString("size"),
                                        colour = it.optString("colour"),
                                        quantity = it.optInt("quantity", 1),
                                        returnedQuantity = it.optInt("returnedQuantity", 0),
                                        unitPrice = it.optDouble("unitPrice", 0.0),
                                        purchasePriceAtSale = it.optDouble("purchasePriceAtSale", 0.0),
                                        itemDiscount = it.optDouble("itemDiscount", 0.0),
                                        lineTotal = it.optDouble("lineTotal", 0.0)
                                    )
                                )
                            }
                        }
                    }
                }
                dao.insertAuditLog(
                    AuditLogEntity(
                        actorName = "NASIR ALI",
                        actorRole = "OWNER",
                        action = "BACKUP_RESTORED",
                        details = "Restored boutique database backup"
                    )
                )
            }
            Result.success("Backup restored successfully.")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreFromUri(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: return@withContext Result.failure(IllegalStateException("Could not read selected file."))
            restoreFromJson(content)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun exportSalesCsv(sales: List<SaleInvoiceEntity>): File = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "reports").apply { mkdirs() }
        val file = File(dir, "naqsh_sales_report_${Formatters.fileTimestamp()}.csv")
        val sb = StringBuilder()
        sb.appendLine("Invoice No,Date,Customer,Mobile,Subtotal,Discount,Tax,Grand Total,Paid,Balance,Payment Method,Status,Billed By")
        sales.forEach { s ->
            sb.appendLine(
                listOf(
                    s.invoiceNumber,
                    "\"${Formatters.dateTime(s.timestamp)}\"",
                    "\"${s.customerName.replace("\"", "'")}\"",
                    "\"${s.customerMobile}\"",
                    "%.2f".format(s.subtotal),
                    "%.2f".format(s.itemDiscountTotal + s.billDiscount),
                    "%.2f".format(s.taxAmount),
                    "%.2f".format(s.grandTotal),
                    "%.2f".format(s.amountPaid),
                    "%.2f".format(s.remainingBalance),
                    s.paymentMethod,
                    s.status,
                    "\"${s.staffName}\""
                ).joinToString(",")
            )
        }
        file.writeText(sb.toString())
        file
    }

    suspend fun exportInventoryCsv(products: List<ProductEntity>): File = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "reports").apply { mkdirs() }
        val file = File(dir, "naqsh_stock_report_${Formatters.fileTimestamp()}.csv")
        val sb = StringBuilder()
        sb.appendLine("SKU,Product Name,Category,Size,Colour,Stock Qty,Min Stock,Purchase Price,Selling Price,Stock Value,Supplier")
        products.forEach { p ->
            sb.appendLine(
                listOf(
                    "\"${p.sku}\"",
                    "\"${p.name.replace("\"", "'")}\"",
                    "\"${p.categoryName}\"",
                    "\"${p.size}\"",
                    "\"${p.colour}\"",
                    p.quantity.toString(),
                    p.minStock.toString(),
                    "%.2f".format(p.purchasePrice),
                    "%.2f".format(p.sellingPrice),
                    "%.2f".format(p.quantity * p.purchasePrice),
                    "\"${p.supplierName}\""
                ).joinToString(",")
            )
        }
        file.writeText(sb.toString())
        file
    }
}
