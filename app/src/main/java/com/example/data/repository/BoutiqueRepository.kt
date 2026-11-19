package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

data class CartLine(
    val product: ProductEntity,
    val variant: ProductVariantEntity? = null,
    val quantity: Int = 1,
    val unitPrice: Double = product.sellingPrice,
    val itemDiscount: Double = 0.0
) {
    val lineSubtotal: Double get() = (unitPrice * quantity) - itemDiscount
}

data class PurchaseCartLine(
    val product: ProductEntity,
    val variant: ProductVariantEntity? = null,
    val quantity: Int = 1,
    val purchasePrice: Double = product.purchasePrice
) {
    val lineTotal: Double get() = purchasePrice * quantity
}

class BoutiqueRepository(private val db: AppDatabase) {
    private val dao = db.boutiqueDao()

    val settingsFlow: Flow<BusinessSettingsEntity> = dao.observeSettings().map { it ?: BusinessSettingsEntity() }
    val categoriesFlow: Flow<List<CategoryEntity>> = dao.observeCategories()
    val productsFlow: Flow<List<ProductEntity>> = dao.observeProducts()
    val deletedProductsFlow: Flow<List<ProductEntity>> = dao.observeDeletedProducts()
    val variantsFlow: Flow<List<ProductVariantEntity>> = dao.observeAllVariants()
    val customersFlow: Flow<List<CustomerEntity>> = dao.observeCustomers()
    val deletedCustomersFlow: Flow<List<CustomerEntity>> = dao.observeDeletedCustomers()
    val customerPaymentsFlow: Flow<List<CustomerPaymentEntity>> = dao.observeCustomerPayments()
    val suppliersFlow: Flow<List<SupplierEntity>> = dao.observeSuppliers()
    val deletedSuppliersFlow: Flow<List<SupplierEntity>> = dao.observeDeletedSuppliers()
    val supplierPaymentsFlow: Flow<List<SupplierPaymentEntity>> = dao.observeSupplierPayments()
    val purchasesFlow: Flow<List<PurchaseEntity>> = dao.observePurchases()
    val purchaseItemsFlow: Flow<List<PurchaseItemEntity>> = dao.observeAllPurchaseItems()
    val salesFlow: Flow<List<SaleInvoiceEntity>> = dao.observeSales()
    val saleItemsFlow: Flow<List<SaleItemEntity>> = dao.observeAllSaleItems()
    val returnsFlow: Flow<List<ReturnExchangeEntity>> = dao.observeReturns()
    val expensesFlow: Flow<List<ExpenseEntity>> = dao.observeExpenses()
    val otherIncomesFlow: Flow<List<OtherIncomeEntity>> = dao.observeOtherIncomes()
    val stockMovementsFlow: Flow<List<StockMovementEntity>> = dao.observeStockMovements()
    val staffFlow: Flow<List<StaffEntity>> = dao.observeStaff()
    val auditLogsFlow: Flow<List<AuditLogEntity>> = dao.observeAuditLogs()

    suspend fun ensureInitialized() {
        seedInitialBoutiqueCatalog(dao)
    }

    suspend fun updateSettings(settings: BusinessSettingsEntity, actorName: String, actorRole: String) {
        dao.upsertSettings(settings)
        dao.insertAuditLog(
            AuditLogEntity(
                actorName = actorName,
                actorRole = actorRole,
                action = "SETTINGS_UPDATED",
                details = "Updated business/system settings"
            )
        )
    }

    suspend fun addCategory(name: String, actorName: String, actorRole: String) {
        val clean = name.trim().uppercase()
        if (clean.isBlank()) return
        val existing = dao.getCategoriesOnce().any { it.name.equals(clean, ignoreCase = true) }
        if (!existing) {
            dao.insertCategory(CategoryEntity(name = clean, isDefault = false))
            dao.insertAuditLog(AuditLogEntity(actorName = actorName, actorRole = actorRole, action = "CATEGORY_ADDED", details = clean))
        }
    }

    suspend fun deleteCategory(category: CategoryEntity, actorName: String, actorRole: String) {
        dao.deleteCategoryById(category.id)
        dao.insertAuditLog(AuditLogEntity(actorName = actorName, actorRole = actorRole, action = "CATEGORY_DELETED", details = category.name))
    }

    suspend fun saveProduct(
        product: ProductEntity,
        variants: List<ProductVariantEntity>,
        actorName: String,
        actorRole: String
    ): Long {
        return db.withTransaction {
            val now = System.currentTimeMillis()
            val totalQty = if (variants.isNotEmpty()) variants.sumOf { it.quantity } else product.quantity
            val isNew = product.id == 0L
            val previousQty = if (!isNew) dao.getProductById(product.id)?.quantity ?: 0 else 0
            val savedId = dao.insertProduct(
                product.copy(
                    quantity = totalQty,
                    createdAt = if (isNew) now else product.createdAt,
                    updatedAt = now
                )
            )
            dao.deleteVariantsForProduct(savedId)
            variants.forEach { v ->
                dao.insertVariant(v.copy(id = 0, productId = savedId))
            }
            val diff = totalQty - previousQty
            if (isNew && totalQty > 0) {
                dao.insertStockMovement(
                    StockMovementEntity(
                        productId = savedId,
                        productName = product.name,
                        variantLabel = "${product.size} / ${product.colour}",
                        changeQty = totalQty,
                        reason = "OPENING",
                        referenceNumber = product.sku
                    )
                )
            } else if (!isNew && diff != 0) {
                dao.insertStockMovement(
                    StockMovementEntity(
                        productId = savedId,
                        productName = product.name,
                        variantLabel = "${product.size} / ${product.colour}",
                        changeQty = diff,
                        reason = "ADJUSTMENT",
                        referenceNumber = product.sku
                    )
                )
            }
            dao.insertAuditLog(
                AuditLogEntity(
                    actorName = actorName,
                    actorRole = actorRole,
                    action = if (isNew) "PRODUCT_CREATED" else "PRODUCT_UPDATED",
                    details = "${product.name} (${product.categoryName}) Qty: $totalQty"
                )
            )
            savedId
        }
    }

    suspend fun deleteProduct(product: ProductEntity, permanent: Boolean = false, actorName: String, actorRole: String) {
        if (permanent) {
            dao.deleteVariantsForProduct(product.id)
            dao.hardDeleteProduct(product.id)
        } else {
            dao.softDeleteProduct(product.id)
        }
        dao.insertAuditLog(
            AuditLogEntity(
                actorName = actorName,
                actorRole = actorRole,
                action = if (permanent) "PRODUCT_PERMANENTLY_DELETED" else "PRODUCT_MOVED_TO_TRASH",
                details = product.name
            )
        )
    }

    suspend fun restoreProduct(productId: Long, name: String, actorName: String, actorRole: String) {
        dao.restoreProduct(productId)
        dao.insertAuditLog(AuditLogEntity(actorName = actorName, actorRole = actorRole, action = "PRODUCT_RESTORED", details = name))
    }

    suspend fun completeSale(
        cart: List<CartLine>,
        customer: CustomerEntity?,
        walkInName: String,
        walkInMobile: String,
        billDiscount: Double,
        gstPercent: Double,
        amountPaid: Double,
        paymentMethod: String,
        notes: String,
        actorName: String,
        actorRole: String
    ): SaleInvoiceEntity {
        return db.withTransaction {
            val settings = dao.getSettingsOnce() ?: BusinessSettingsEntity()
            val seq = settings.nextInvoiceSeq
            val year = Calendar.getInstance().get(Calendar.YEAR)
            val invoiceNumber = "NC-$year-$seq"

            val rawSubtotal = cart.sumOf { it.unitPrice * it.quantity }
            val itemDiscTotal = cart.sumOf { it.itemDiscount }
            val netAfterDiscounts = (rawSubtotal - itemDiscTotal - billDiscount).coerceAtLeast(0.0)
            val taxAmount = if (gstPercent > 0.0) netAfterDiscounts * (gstPercent / 100.0) else 0.0
            val grandTotal = netAfterDiscounts + taxAmount
            val safePaid = amountPaid.coerceIn(0.0, grandTotal)
            val remaining = (grandTotal - safePaid).coerceAtLeast(0.0)

            val resolvedCustomerName = customer?.name ?: walkInName.ifBlank { "Walk-in Customer" }
            val resolvedCustomerMobile = customer?.mobile ?: walkInMobile

            val invoice = SaleInvoiceEntity(
                invoiceNumber = invoiceNumber,
                timestamp = System.currentTimeMillis(),
                customerId = customer?.id,
                customerName = resolvedCustomerName,
                customerMobile = resolvedCustomerMobile,
                subtotal = rawSubtotal,
                itemDiscountTotal = itemDiscTotal,
                billDiscount = billDiscount,
                gstPercent = gstPercent,
                taxAmount = taxAmount,
                grandTotal = grandTotal,
                amountPaid = safePaid,
                remainingBalance = remaining,
                paymentMethod = paymentMethod,
                status = "COMPLETED",
                staffName = "$actorName ($actorRole)",
                notes = notes
            )
            val invoiceId = dao.insertSaleInvoice(invoice)

            cart.forEach { line ->
                val sizeLabel = line.variant?.size ?: line.product.size
                val colourLabel = line.variant?.colour ?: line.product.colour
                dao.insertSaleItem(
                    SaleItemEntity(
                        invoiceId = invoiceId,
                        productId = line.product.id,
                        variantId = line.variant?.id,
                        productName = line.product.name,
                        size = sizeLabel,
                        colour = colourLabel,
                        quantity = line.quantity,
                        unitPrice = line.unitPrice,
                        purchasePriceAtSale = line.product.purchasePrice,
                        itemDiscount = line.itemDiscount,
                        lineTotal = line.lineSubtotal.coerceAtLeast(0.0)
                    )
                )

                // Deduct stock from variant if present, and update product total stock
                if (line.variant != null) {
                    val v = dao.getVariantById(line.variant.id)
                    if (v != null) {
                        dao.updateVariant(v.copy(quantity = (v.quantity - line.quantity).coerceAtLeast(0)))
                    }
                    val allVariants = dao.getVariantsForProduct(line.product.id)
                    val freshProd = dao.getProductById(line.product.id)
                    if (freshProd != null) {
                        dao.updateProduct(freshProd.copy(quantity = allVariants.sumOf { it.quantity }, updatedAt = System.currentTimeMillis()))
                    }
                } else {
                    val freshProd = dao.getProductById(line.product.id)
                    if (freshProd != null) {
                        dao.updateProduct(
                            freshProd.copy(
                                quantity = (freshProd.quantity - line.quantity).coerceAtLeast(0),
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                }

                dao.insertStockMovement(
                    StockMovementEntity(
                        productId = line.product.id,
                        productName = line.product.name,
                        variantLabel = "$sizeLabel / $colourLabel",
                        changeQty = -line.quantity,
                        reason = "SALE",
                        referenceNumber = invoiceNumber
                    )
                )
            }

            if (customer != null) {
                val freshCustomer = dao.getCustomerById(customer.id) ?: customer
                dao.updateCustomer(
                    freshCustomer.copy(
                        totalPurchases = freshCustomer.totalPurchases + grandTotal,
                        totalPaid = freshCustomer.totalPaid + safePaid,
                        outstandingBalance = (freshCustomer.outstandingBalance + remaining).coerceAtLeast(0.0),
                        updatedAt = System.currentTimeMillis()
                    )
                )
                if (safePaid > 0.0) {
                    dao.insertCustomerPayment(
                        CustomerPaymentEntity(
                            customerId = customer.id,
                            customerName = freshCustomer.name,
                            amount = safePaid,
                            paymentMode = paymentMethod,
                            note = "Payment against $invoiceNumber"
                        )
                    )
                }
            }

            dao.upsertSettings(settings.copy(nextInvoiceSeq = seq + 1))
            dao.insertAuditLog(
                AuditLogEntity(
                    actorName = actorName,
                    actorRole = actorRole,
                    action = "SALE_COMPLETED",
                    details = "$invoiceNumber • Total ₹${"%.2f".format(grandTotal)} • Paid ₹${"%.2f".format(safePaid)}"
                )
            )
            invoice.copy(id = invoiceId)
        }
    }

    suspend fun recordPurchase(
        supplier: SupplierEntity?,
        supplierNameFallback: String,
        lines: List<PurchaseCartLine>,
        paymentMode: String,
        paidAmount: Double,
        notes: String,
        actorName: String,
        actorRole: String
    ): PurchaseEntity {
        return db.withTransaction {
            val settings = dao.getSettingsOnce() ?: BusinessSettingsEntity()
            val seq = settings.nextPurchaseSeq
            val purchaseInvNo = "PUR-${Calendar.getInstance().get(Calendar.YEAR)}-$seq"
            val totalAmount = lines.sumOf { it.lineTotal }
            val safePaid = paidAmount.coerceIn(0.0, totalAmount)
            val outstanding = (totalAmount - safePaid).coerceAtLeast(0.0)
            val resolvedSupplier = supplier?.name ?: supplierNameFallback.ifBlank { "Direct Supplier" }

            val purchase = PurchaseEntity(
                purchaseInvoiceNumber = purchaseInvNo,
                supplierId = supplier?.id,
                supplierName = resolvedSupplier,
                purchaseDate = System.currentTimeMillis(),
                totalAmount = totalAmount,
                paymentMode = paymentMode,
                paidAmount = safePaid,
                outstandingAmount = outstanding,
                notes = notes
            )
            val purchaseId = dao.insertPurchase(purchase)

            lines.forEach { line ->
                val sizeLabel = line.variant?.size ?: line.product.size
                val colourLabel = line.variant?.colour ?: line.product.colour
                dao.insertPurchaseItem(
                    PurchaseItemEntity(
                        purchaseId = purchaseId,
                        productId = line.product.id,
                        variantId = line.variant?.id,
                        productName = line.product.name,
                        size = sizeLabel,
                        colour = colourLabel,
                        quantity = line.quantity,
                        purchasePrice = line.purchasePrice,
                        lineTotal = line.lineTotal
                    )
                )

                if (line.variant != null) {
                    val v = dao.getVariantById(line.variant.id)
                    if (v != null) {
                        dao.updateVariant(v.copy(quantity = v.quantity + line.quantity))
                    }
                    val allVariants = dao.getVariantsForProduct(line.product.id)
                    val freshProd = dao.getProductById(line.product.id)
                    if (freshProd != null) {
                        dao.updateProduct(
                            freshProd.copy(
                                quantity = allVariants.sumOf { it.quantity },
                                purchasePrice = line.purchasePrice,
                                supplierId = supplier?.id ?: freshProd.supplierId,
                                supplierName = resolvedSupplier,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                } else {
                    val freshProd = dao.getProductById(line.product.id)
                    if (freshProd != null) {
                        dao.updateProduct(
                            freshProd.copy(
                                quantity = freshProd.quantity + line.quantity,
                                purchasePrice = line.purchasePrice,
                                supplierId = supplier?.id ?: freshProd.supplierId,
                                supplierName = resolvedSupplier,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                }

                dao.insertStockMovement(
                    StockMovementEntity(
                        productId = line.product.id,
                        productName = line.product.name,
                        variantLabel = "$sizeLabel / $colourLabel",
                        changeQty = line.quantity,
                        reason = "PURCHASE",
                        referenceNumber = purchaseInvNo
                    )
                )
            }

            if (supplier != null) {
                val freshSupplier = dao.getSupplierById(supplier.id) ?: supplier
                dao.updateSupplier(
                    freshSupplier.copy(
                        totalPurchased = freshSupplier.totalPurchased + totalAmount,
                        totalPaid = freshSupplier.totalPaid + safePaid,
                        remainingBalance = (freshSupplier.remainingBalance + outstanding).coerceAtLeast(0.0),
                        updatedAt = System.currentTimeMillis()
                    )
                )
                if (safePaid > 0.0) {
                    dao.insertSupplierPayment(
                        SupplierPaymentEntity(
                            supplierId = supplier.id,
                            supplierName = freshSupplier.name,
                            amount = safePaid,
                            paymentMode = paymentMode,
                            note = "Payment against $purchaseInvNo"
                        )
                    )
                }
            }

            dao.upsertSettings(settings.copy(nextPurchaseSeq = seq + 1))
            dao.insertAuditLog(
                AuditLogEntity(
                    actorName = actorName,
                    actorRole = actorRole,
                    action = "PURCHASE_RECORDED",
                    details = "$purchaseInvNo • Supplier: $resolvedSupplier • Total ₹${"%.2f".format(totalAmount)}"
                )
            )
            purchase.copy(id = purchaseId)
        }
    }

    suspend fun processReturnOrExchange(
        invoice: SaleInvoiceEntity,
        item: SaleItemEntity?,
        returnQty: Int,
        isFullReturn: Boolean,
        exchangeProduct: ProductEntity?,
        refundAmount: Double,
        reason: String,
        actorName: String,
        actorRole: String
    ) {
        db.withTransaction {
            val invoiceItems = dao.getSaleItemsForInvoice(invoice.id)
            if (isFullReturn) {
                invoiceItems.forEach { si ->
                    val remainingReturnable = (si.quantity - si.returnedQuantity).coerceAtLeast(0)
                    if (remainingReturnable > 0) {
                        restoreStockForSaleItem(si, remainingReturnable, "RETURN", invoice.invoiceNumber)
                        dao.updateSaleItem(si.copy(returnedQuantity = si.quantity))
                    }
                }
                dao.updateSaleInvoice(invoice.copy(status = "RETURNED"))
                dao.insertReturnExchange(
                    ReturnExchangeEntity(
                        invoiceId = invoice.id,
                        invoiceNumber = invoice.invoiceNumber,
                        type = "FULL_RETURN",
                        returnedProductName = "All Bill Items (${invoiceItems.size})",
                        returnedQty = invoiceItems.sumOf { it.quantity },
                        refundAmount = refundAmount,
                        reason = reason,
                        processedBy = actorName
                    )
                )
            } else if (item != null && returnQty > 0) {
                val validQty = returnQty.coerceAtMost(item.quantity - item.returnedQuantity)
                if (validQty > 0) {
                    restoreStockForSaleItem(item, validQty, if (exchangeProduct != null) "EXCHANGE" else "RETURN", invoice.invoiceNumber)
                    dao.updateSaleItem(item.copy(returnedQuantity = item.returnedQuantity + validQty))

                    if (exchangeProduct != null) {
                        val freshEx = dao.getProductById(exchangeProduct.id)
                        if (freshEx != null) {
                            dao.updateProduct(freshEx.copy(quantity = (freshEx.quantity - validQty).coerceAtLeast(0), updatedAt = System.currentTimeMillis()))
                        }
                        dao.insertStockMovement(
                            StockMovementEntity(
                                productId = exchangeProduct.id,
                                productName = exchangeProduct.name,
                                variantLabel = "${exchangeProduct.size} / ${exchangeProduct.colour}",
                                changeQty = -validQty,
                                reason = "EXCHANGE",
                                referenceNumber = invoice.invoiceNumber
                            )
                        )
                    }

                    val updatedItems = dao.getSaleItemsForInvoice(invoice.id)
                    val allReturned = updatedItems.all { it.returnedQuantity >= it.quantity }
                    dao.updateSaleInvoice(invoice.copy(status = if (allReturned) "RETURNED" else "PARTIAL_RETURN"))
                    dao.insertReturnExchange(
                        ReturnExchangeEntity(
                            invoiceId = invoice.id,
                            invoiceNumber = invoice.invoiceNumber,
                            type = if (exchangeProduct != null) "EXCHANGE" else "PARTIAL_RETURN",
                            returnedItemId = item.id,
                            returnedProductName = item.productName,
                            returnedQty = validQty,
                            exchangeProductId = exchangeProduct?.id,
                            exchangeProductName = exchangeProduct?.name ?: "",
                            exchangeQty = if (exchangeProduct != null) validQty else 0,
                            refundAmount = refundAmount,
                            reason = reason,
                            processedBy = actorName
                        )
                    )
                }
            }

            if (invoice.customerId != null && refundAmount > 0.0) {
                val customer = dao.getCustomerById(invoice.customerId)
                if (customer != null) {
                    val newBalance = (customer.outstandingBalance - refundAmount).coerceAtLeast(0.0)
                    dao.updateCustomer(customer.copy(outstandingBalance = newBalance, updatedAt = System.currentTimeMillis()))
                }
            }

            dao.insertAuditLog(
                AuditLogEntity(
                    actorName = actorName,
                    actorRole = actorRole,
                    action = if (exchangeProduct != null) "PRODUCT_EXCHANGED" else "SALE_RETURN_PROCESSED",
                    details = "${invoice.invoiceNumber} • Refund ₹${"%.2f".format(refundAmount)} • $reason"
                )
            )
        }
    }

    private suspend fun restoreStockForSaleItem(item: SaleItemEntity, qty: Int, reason: String, refNo: String) {
        if (item.variantId != null) {
            val v = dao.getVariantById(item.variantId)
            if (v != null) dao.updateVariant(v.copy(quantity = v.quantity + qty))
            val allVars = dao.getVariantsForProduct(item.productId)
            val p = dao.getProductById(item.productId)
            if (p != null) dao.updateProduct(p.copy(quantity = allVars.sumOf { it.quantity }, updatedAt = System.currentTimeMillis()))
        } else {
            val p = dao.getProductById(item.productId)
            if (p != null) dao.updateProduct(p.copy(quantity = p.quantity + qty, updatedAt = System.currentTimeMillis()))
        }
        dao.insertStockMovement(
            StockMovementEntity(
                productId = item.productId,
                productName = item.productName,
                variantLabel = "${item.size} / ${item.colour}",
                changeQty = qty,
                reason = reason,
                referenceNumber = refNo
            )
        )
    }

    suspend fun cancelInvoice(invoice: SaleInvoiceEntity, reason: String, actorName: String, actorRole: String) {
        db.withTransaction {
            if (invoice.status == "CANCELLED") return@withTransaction
            val items = dao.getSaleItemsForInvoice(invoice.id)
            items.forEach { si ->
                val restorable = (si.quantity - si.returnedQuantity).coerceAtLeast(0)
                if (restorable > 0) {
                    restoreStockForSaleItem(si, restorable, "RETURN", invoice.invoiceNumber)
                    dao.updateSaleItem(si.copy(returnedQuantity = si.quantity))
                }
            }
            dao.updateSaleInvoice(invoice.copy(status = "CANCELLED", notes = "${invoice.notes} [Cancelled: $reason]".trim()))
            dao.insertAuditLog(
                AuditLogEntity(
                    actorName = actorName,
                    actorRole = actorRole,
                    action = "INVOICE_CANCELLED",
                    details = "${invoice.invoiceNumber} • $reason"
                )
            )
        }
    }

    suspend fun saveCustomer(customer: CustomerEntity, actorName: String, actorRole: String): Long {
        val id = dao.insertCustomer(customer.copy(updatedAt = System.currentTimeMillis()))
        dao.insertAuditLog(AuditLogEntity(actorName = actorName, actorRole = actorRole, action = "CUSTOMER_SAVED", details = customer.name))
        return id
    }

    suspend fun deleteCustomer(customer: CustomerEntity, actorName: String, actorRole: String) {
        dao.softDeleteCustomer(customer.id)
        dao.insertAuditLog(AuditLogEntity(actorName = actorName, actorRole = actorRole, action = "CUSTOMER_TRASHED", details = customer.name))
    }

    suspend fun restoreCustomer(customerId: Long, name: String, actorName: String, actorRole: String) {
        dao.restoreCustomer(customerId)
        dao.insertAuditLog(AuditLogEntity(actorName = actorName, actorRole = actorRole, action = "CUSTOMER_RESTORED", details = name))
    }

    suspend fun recordCustomerPayment(customer: CustomerEntity, amount: Double, mode: String, note: String, actorName: String, actorRole: String) {
        db.withTransaction {
            val fresh = dao.getCustomerById(customer.id) ?: customer
            dao.insertCustomerPayment(
                CustomerPaymentEntity(
                    customerId = fresh.id,
                    customerName = fresh.name,
                    amount = amount,
                    paymentMode = mode,
                    note = note
                )
            )
            dao.updateCustomer(
                fresh.copy(
                    totalPaid = fresh.totalPaid + amount,
                    outstandingBalance = (fresh.outstandingBalance - amount).coerceAtLeast(0.0),
                    updatedAt = System.currentTimeMillis()
                )
            )
            dao.insertAuditLog(
                AuditLogEntity(
                    actorName = actorName,
                    actorRole = actorRole,
                    action = "CUSTOMER_PAYMENT_RECEIVED",
                    details = "${fresh.name} • ₹${"%.2f".format(amount)} ($mode)"
                )
            )
        }
    }

    suspend fun saveSupplier(supplier: SupplierEntity, actorName: String, actorRole: String): Long {
        val id = dao.insertSupplier(supplier.copy(updatedAt = System.currentTimeMillis()))
        dao.insertAuditLog(AuditLogEntity(actorName = actorName, actorRole = actorRole, action = "SUPPLIER_SAVED", details = supplier.name))
        return id
    }

    suspend fun deleteSupplier(supplier: SupplierEntity, actorName: String, actorRole: String) {
        dao.softDeleteSupplier(supplier.id)
        dao.insertAuditLog(AuditLogEntity(actorName = actorName, actorRole = actorRole, action = "SUPPLIER_TRASHED", details = supplier.name))
    }

    suspend fun restoreSupplier(supplierId: Long, name: String, actorName: String, actorRole: String) {
        dao.restoreSupplier(supplierId)
        dao.insertAuditLog(AuditLogEntity(actorName = actorName, actorRole = actorRole, action = "SUPPLIER_RESTORED", details = name))
    }

    suspend fun recordSupplierPayment(supplier: SupplierEntity, amount: Double, mode: String, note: String, actorName: String, actorRole: String) {
        db.withTransaction {
            val fresh = dao.getSupplierById(supplier.id) ?: supplier
            dao.insertSupplierPayment(
                SupplierPaymentEntity(
                    supplierId = fresh.id,
                    supplierName = fresh.name,
                    amount = amount,
                    paymentMode = mode,
                    note = note
                )
            )
            dao.updateSupplier(
                fresh.copy(
                    totalPaid = fresh.totalPaid + amount,
                    remainingBalance = (fresh.remainingBalance - amount).coerceAtLeast(0.0),
                    updatedAt = System.currentTimeMillis()
                )
            )
            dao.insertAuditLog(
                AuditLogEntity(
                    actorName = actorName,
                    actorRole = actorRole,
                    action = "SUPPLIER_PAYMENT_MADE",
                    details = "${fresh.name} • ₹${"%.2f".format(amount)} ($mode)"
                )
            )
        }
    }

    suspend fun addExpense(expense: ExpenseEntity, actorName: String, actorRole: String) {
        dao.insertExpense(expense)
        dao.insertAuditLog(
            AuditLogEntity(
                actorName = actorName,
                actorRole = actorRole,
                action = "EXPENSE_ADDED",
                details = "${expense.title} (${expense.category}) ₹${"%.2f".format(expense.amount)}"
            )
        )
    }

    suspend fun deleteExpense(expense: ExpenseEntity, actorName: String, actorRole: String) {
        dao.softDeleteExpense(expense.id)
        dao.insertAuditLog(
            AuditLogEntity(
                actorName = actorName,
                actorRole = actorRole,
                action = "EXPENSE_ARCHIVED",
                details = "${expense.title} ₹${"%.2f".format(expense.amount)}"
            )
        )
    }

    suspend fun addOtherIncome(income: OtherIncomeEntity, actorName: String, actorRole: String) {
        dao.insertOtherIncome(income)
        dao.insertAuditLog(
            AuditLogEntity(
                actorName = actorName,
                actorRole = actorRole,
                action = "OTHER_INCOME_ADDED",
                details = "${income.title} ₹${"%.2f".format(income.amount)}"
            )
        )
    }

    suspend fun saveStaff(staff: StaffEntity, actorName: String, actorRole: String) {
        dao.insertStaff(staff)
        dao.insertAuditLog(
            AuditLogEntity(
                actorName = actorName,
                actorRole = actorRole,
                action = if (staff.id == 0L) "STAFF_CREATED" else "STAFF_UPDATED",
                details = "${staff.name} (${staff.roleTitle}) Active=${staff.isActive}"
            )
        )
    }
}
