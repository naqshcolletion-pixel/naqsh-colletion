package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.*
import com.example.data.repository.*
import com.example.util.Formatters
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar

data class AccountingSummary(
    val dailyIncome: Double = 0.0,
    val dailyExpenses: Double = 0.0,
    val monthlyIncome: Double = 0.0,
    val monthlyExpenses: Double = 0.0,
    val grossProfit: Double = 0.0,
    val netProfit: Double = 0.0,
    val cashInHand: Double = 0.0,
    val upiCollections: Double = 0.0,
    val bankBalance: Double = 0.0,
    val customerOutstanding: Double = 0.0,
    val supplierOutstanding: Double = 0.0,
    val inventoryValue: Double = 0.0,
    val lowStockCount: Int = 0,
    val totalSalesCount: Int = 0
)

class BoutiqueViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    val repo = BoutiqueRepository(db)
    val backupManager = BackupManager(application, db)
    val cloudRepo = CloudSyncRepository(application)
    val authManager = AuthManager()

    init {
        viewModelScope.launch {
            repo.ensureInitialized()
        }
    }

    val settings: StateFlow<BusinessSettingsEntity> = repo.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BusinessSettingsEntity())

    val categories: StateFlow<List<CategoryEntity>> = repo.categoriesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val products: StateFlow<List<ProductEntity>> = repo.productsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deletedProducts: StateFlow<List<ProductEntity>> = repo.deletedProductsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val variants: StateFlow<List<ProductVariantEntity>> = repo.variantsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customers: StateFlow<List<CustomerEntity>> = repo.customersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deletedCustomers: StateFlow<List<CustomerEntity>> = repo.deletedCustomersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customerPayments: StateFlow<List<CustomerPaymentEntity>> = repo.customerPaymentsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val suppliers: StateFlow<List<SupplierEntity>> = repo.suppliersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deletedSuppliers: StateFlow<List<SupplierEntity>> = repo.deletedSuppliersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val supplierPayments: StateFlow<List<SupplierPaymentEntity>> = repo.supplierPaymentsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val purchases: StateFlow<List<PurchaseEntity>> = repo.purchasesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val purchaseItems: StateFlow<List<PurchaseItemEntity>> = repo.purchaseItemsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sales: StateFlow<List<SaleInvoiceEntity>> = repo.salesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val saleItems: StateFlow<List<SaleItemEntity>> = repo.saleItemsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val returns: StateFlow<List<ReturnExchangeEntity>> = repo.returnsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expenses: StateFlow<List<ExpenseEntity>> = repo.expensesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val otherIncomes: StateFlow<List<OtherIncomeEntity>> = repo.otherIncomesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stockMovements: StateFlow<List<StockMovementEntity>> = repo.stockMovementsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val staffList: StateFlow<List<StaffEntity>> = repo.staffFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val auditLogs: StateFlow<List<AuditLogEntity>> = repo.auditLogsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeRole = MutableStateFlow(ActiveSessionRole(isOwner = true, staff = null))
    val activeRole: StateFlow<ActiveSessionRole> = _activeRole.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // POS Cart State
    private val _cartLines = MutableStateFlow<List<CartLine>>(emptyList())
    val cartLines: StateFlow<List<CartLine>> = _cartLines.asStateFlow()

    private val _posCustomer = MutableStateFlow<CustomerEntity?>(null)
    val posCustomer: StateFlow<CustomerEntity?> = _posCustomer.asStateFlow()

    private val _lastInvoicePreview = MutableStateFlow<SaleInvoiceEntity?>(null)
    val lastInvoicePreview: StateFlow<SaleInvoiceEntity?> = _lastInvoicePreview.asStateFlow()

    val accountingSummary: StateFlow<AccountingSummary> = combine(
        sales,
        saleItems,
        expenses,
        otherIncomes,
        returns,
        products,
        customers,
        suppliers,
        customerPayments,
        supplierPayments
    ) { arr ->
        @Suppress("UNCHECKED_CAST")
        val saleList = arr[0] as List<SaleInvoiceEntity>
        @Suppress("UNCHECKED_CAST")
        val sItems = arr[1] as List<SaleItemEntity>
        @Suppress("UNCHECKED_CAST")
        val expList = arr[2] as List<ExpenseEntity>
        @Suppress("UNCHECKED_CAST")
        val otherList = arr[3] as List<OtherIncomeEntity>
        @Suppress("UNCHECKED_CAST")
        val retList = arr[4] as List<ReturnExchangeEntity>
        @Suppress("UNCHECKED_CAST")
        val prodList = arr[5] as List<ProductEntity>
        @Suppress("UNCHECKED_CAST")
        val custList = arr[6] as List<CustomerEntity>
        @Suppress("UNCHECKED_CAST")
        val supList = arr[7] as List<SupplierEntity>
        @Suppress("UNCHECKED_CAST")
        val custPays = arr[8] as List<CustomerPaymentEntity>
        @Suppress("UNCHECKED_CAST")
        val supPays = arr[9] as List<SupplierPaymentEntity>

        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val dayStart = cal.timeInMillis
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val monthStart = cal.timeInMillis

        val activeSales = saleList.filter { it.status != "CANCELLED" }
        val dailySales = activeSales.filter { it.timestamp >= dayStart }.sumOf { it.grandTotal }
        val dailyOther = otherList.filter { it.timestamp >= dayStart }.sumOf { it.amount }
        val dailyExp = expList.filter { it.timestamp >= dayStart }.sumOf { it.amount }

        val monthlySales = activeSales.filter { it.timestamp >= monthStart }.sumOf { it.grandTotal }
        val monthlyOther = otherList.filter { it.timestamp >= monthStart }.sumOf { it.amount }
        val monthlyExp = expList.filter { it.timestamp >= monthStart }.sumOf { it.amount }

        val totalRefunds = retList.sumOf { it.refundAmount }
        val validInvoiceIds = activeSales.map { it.id }.toSet()
        val totalCogs = sItems.filter { it.invoiceId in validInvoiceIds }.sumOf { item ->
            val netQty = (item.quantity - item.returnedQuantity).coerceAtLeast(0)
            netQty * item.purchasePriceAtSale
        }
        val totalRevenue = activeSales.sumOf { it.grandTotal } - totalRefunds + otherList.sumOf { it.amount }
        val grossProfit = (activeSales.sumOf { it.grandTotal } - totalRefunds) - totalCogs
        val totalExpenses = expList.sumOf { it.amount }
        val netProfit = grossProfit + otherList.sumOf { it.amount } - totalExpenses

        // Cash flow by payment mode
        val cashIn = activeSales.filter { it.paymentMethod.equals("Cash", true) }.sumOf { it.amountPaid } +
            otherList.filter { it.paymentMode.equals("Cash", true) }.sumOf { it.amount } +
            custPays.filter { it.paymentMode.equals("Cash", true) && !it.note.startsWith("Payment against") }.sumOf { it.amount }
        val cashOut = expList.filter { it.paymentMode.equals("Cash", true) }.sumOf { it.amount } +
            supPays.filter { it.paymentMode.equals("Cash", true) }.sumOf { it.amount } + totalRefunds

        val upiIn = activeSales.filter {
            it.paymentMethod.equals("UPI", true) || it.paymentMethod.equals("PhonePe", true)
        }.sumOf { it.amountPaid } +
            otherList.filter { it.paymentMode.equals("UPI", true) || it.paymentMode.equals("PhonePe", true) }.sumOf { it.amount } +
            custPays.filter { (it.paymentMode.equals("UPI", true) || it.paymentMode.equals("PhonePe", true)) && !it.note.startsWith("Payment against") }.sumOf { it.amount }
        val upiOut = expList.filter { it.paymentMode.equals("UPI", true) || it.paymentMode.equals("PhonePe", true) }.sumOf { it.amount } +
            supPays.filter { it.paymentMode.equals("UPI", true) || it.paymentMode.equals("PhonePe", true) }.sumOf { it.amount }

        val bankIn = activeSales.filter { it.paymentMethod.equals("Bank", true) || it.paymentMethod.equals("Card", true) }.sumOf { it.amountPaid } +
            otherList.filter { it.paymentMode.equals("Bank", true) }.sumOf { it.amount }
        val bankOut = expList.filter { it.paymentMode.equals("Bank", true) }.sumOf { it.amount } +
            supPays.filter { it.paymentMode.equals("Bank", true) }.sumOf { it.amount }

        AccountingSummary(
            dailyIncome = dailySales + dailyOther,
            dailyExpenses = dailyExp,
            monthlyIncome = monthlySales + monthlyOther,
            monthlyExpenses = monthlyExp,
            grossProfit = grossProfit,
            netProfit = netProfit,
            cashInHand = cashIn - cashOut,
            upiCollections = upiIn - upiOut,
            bankBalance = (upiIn - upiOut) + (bankIn - bankOut),
            customerOutstanding = custList.sumOf { it.outstandingBalance },
            supplierOutstanding = supList.sumOf { it.remainingBalance },
            inventoryValue = prodList.sumOf { it.quantity * it.purchasePrice },
            lowStockCount = prodList.count { it.quantity <= it.minStock },
            totalSalesCount = activeSales.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AccountingSummary())

    fun clearToast() {
        _toastMessage.value = null
    }

    fun showToast(msg: String) {
        _toastMessage.value = msg
    }

    fun switchToOwner() {
        _activeRole.value = ActiveSessionRole(isOwner = true, staff = null)
    }

    fun switchToStaff(staff: StaffEntity, pin: String): Boolean {
        if (!staff.isActive) return false
        if (Formatters.hashPin(pin) != staff.pinHash) return false
        _activeRole.value = ActiveSessionRole(isOwner = false, staff = staff)
        return true
    }

    // Cart Operations
    fun addToCart(product: ProductEntity, variant: ProductVariantEntity? = null) {
        val current = _cartLines.value.toMutableList()
        val idx = current.indexOfFirst { it.product.id == product.id && it.variant?.id == variant?.id }
        if (idx >= 0) {
            val line = current[idx]
            current[idx] = line.copy(quantity = line.quantity + 1)
        } else {
            current.add(CartLine(product = product, variant = variant, quantity = 1, unitPrice = product.sellingPrice))
        }
        _cartLines.value = current
    }

    fun updateCartQuantity(index: Int, newQty: Int) {
        val current = _cartLines.value.toMutableList()
        if (index !in current.indices) return
        if (newQty <= 0) {
            current.removeAt(index)
        } else {
            current[index] = current[index].copy(quantity = newQty)
        }
        _cartLines.value = current
    }

    fun updateCartItemDiscount(index: Int, discount: Double, customPrice: Double) {
        val current = _cartLines.value.toMutableList()
        if (index !in current.indices) return
        current[index] = current[index].copy(
            itemDiscount = discount.coerceAtLeast(0.0),
            unitPrice = customPrice.coerceAtLeast(0.0)
        )
        _cartLines.value = current
    }

    fun clearCart() {
        _cartLines.value = emptyList()
        _posCustomer.value = null
    }

    fun selectPosCustomer(customer: CustomerEntity?) {
        _posCustomer.value = customer
    }

    fun setInvoicePreview(invoice: SaleInvoiceEntity?) {
        _lastInvoicePreview.value = invoice
    }

    fun checkoutSale(
        walkInName: String,
        walkInMobile: String,
        billDiscount: Double,
        gstPercent: Double,
        amountPaid: Double,
        paymentMethod: String,
        notes: String
    ) {
        val cart = _cartLines.value
        if (cart.isEmpty()) {
            _toastMessage.value = "Cart is empty. Add products before billing."
            return
        }
        viewModelScope.launch {
            val role = _activeRole.value
            val invoice = repo.completeSale(
                cart = cart,
                customer = _posCustomer.value,
                walkInName = walkInName,
                walkInMobile = walkInMobile,
                billDiscount = billDiscount,
                gstPercent = gstPercent,
                amountPaid = amountPaid,
                paymentMethod = paymentMethod,
                notes = notes,
                actorName = role.actorName,
                actorRole = role.actorRole
            )
            clearCart()
            _lastInvoicePreview.value = invoice
            _toastMessage.value = "Invoice ${invoice.invoiceNumber} saved!"
        }
    }

    fun saveSettings(updated: BusinessSettingsEntity) {
        viewModelScope.launch {
            val role = _activeRole.value
            repo.updateSettings(updated, role.actorName, role.actorRole)
            _toastMessage.value = "Settings saved."
        }
    }

    fun syncToCloud() {
        viewModelScope.launch {
            val s = settings.value
            val summary = accountingSummary.value
            val res1 = cloudRepo.syncOwnerProfile(s.ownerName, s.businessName, s.ownerMobile, s.gstin, s.upiId)
            val res2 = cloudRepo.pushCloudSnapshot(
                label = "Backup ${Formatters.dateTime(System.currentTimeMillis())}",
                productCount = products.value.size,
                customerCount = customers.value.size,
                invoiceCount = sales.value.size,
                totalRevenue = sales.value.filter { it.status != "CANCELLED" }.sumOf { it.grandTotal }
            )
            _toastMessage.value = if (res1.isSuccess && res2.isSuccess) {
                "Synced profile & snapshot (${summary.totalSalesCount} bills) to Firebase Cloud!"
            } else {
                res1.exceptionOrNull()?.localizedMessage
                    ?: res2.exceptionOrNull()?.localizedMessage
                    ?: "Cloud sync requires active internet & Firebase sign-in."
            }
        }
    }

    fun exportBackupToUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val json = backupManager.exportFullBackupJson()
                getApplication<Application>().contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray())
                }
                _toastMessage.value = "Backup saved successfully!"
            } catch (e: Exception) {
                _toastMessage.value = "Backup export failed: ${e.localizedMessage}"
            }
        }
    }

    fun restoreBackupFromUri(uri: Uri) {
        viewModelScope.launch {
            val res = backupManager.restoreFromUri(uri)
            _toastMessage.value = res.getOrElse { "Restore failed: ${it.localizedMessage}" }
        }
    }

    suspend fun createLocalBackupFile(): File = backupManager.saveLocalBackupFile()
}
