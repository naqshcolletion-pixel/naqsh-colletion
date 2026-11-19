package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.*
import com.example.data.repository.ActiveSessionRole
import com.example.data.repository.BackupManager
import com.example.ui.components.EmptyStateCard
import com.example.ui.theme.LossCrimson
import com.example.ui.theme.ProfitGreen
import com.example.util.Formatters
import com.example.util.pdf.InvoicePdfGenerator
import com.example.util.pdf.PrintAndShareHelper
import kotlinx.coroutines.launch
import java.util.Calendar

enum class ReportPeriod(val label: String) {
    DAILY("Today"),
    WEEKLY("7 Days"),
    MONTHLY("This Month"),
    YEARLY("This Year"),
    ALL("All Time")
}

enum class ReportType(val label: String, val ownerOnly: Boolean = false) {
    SALES("Sales"),
    PRODUCT_WISE("Product Sales"),
    CATEGORY_WISE("Category Sales"),
    PURCHASES("Purchases", ownerOnly = true),
    EXPENSES("Expenses", ownerOnly = true),
    PROFIT_LOSS("Profit & Loss", ownerOnly = true),
    CUSTOMER_DUE("Customer Due"),
    SUPPLIER_DUE("Supplier Due", ownerOnly = true),
    STOCK("Stock Report"),
    PAYMENTS("Payment Modes")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    settings: BusinessSettingsEntity,
    activeRole: ActiveSessionRole,
    backupManager: BackupManager,
    products: List<ProductEntity>,
    sales: List<SaleInvoiceEntity>,
    saleItems: List<SaleItemEntity>,
    purchases: List<PurchaseEntity>,
    expenses: List<ExpenseEntity>,
    customers: List<CustomerEntity>,
    suppliers: List<SupplierEntity>,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sym = settings.currencySymbol

    if (!activeRole.canViewBasicReports) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Reports") },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }
                )
            }
        ) { p ->
            Box(Modifier.fillMaxSize().padding(p).padding(24.dp)) {
                EmptyStateCard(Icons.Default.Lock, "Reports Restricted", "Your staff account does not have permission to view reports.")
            }
        }
        return
    }

    var period by remember { mutableStateOf(ReportPeriod.MONTHLY) }
    var reportType by remember { mutableStateOf(ReportType.SALES) }

    val startTs = remember(period) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        when (period) {
            ReportPeriod.DAILY -> cal.timeInMillis
            ReportPeriod.WEEKLY -> cal.apply { add(Calendar.DAY_OF_YEAR, -6) }.timeInMillis
            ReportPeriod.MONTHLY -> cal.apply { set(Calendar.DAY_OF_MONTH, 1) }.timeInMillis
            ReportPeriod.YEARLY -> cal.apply { set(Calendar.DAY_OF_YEAR, 1) }.timeInMillis
            ReportPeriod.ALL -> 0L
        }
    }

    val filteredSales = remember(sales, startTs) { sales.filter { it.timestamp >= startTs && it.status != "CANCELLED" } }
    val filteredSaleIds = remember(filteredSales) { filteredSales.map { it.id }.toSet() }
    val filteredItems = remember(saleItems, filteredSaleIds) { saleItems.filter { it.invoiceId in filteredSaleIds } }
    val filteredPurchases = remember(purchases, startTs) { purchases.filter { it.purchaseDate >= startTs } }
    val filteredExpenses = remember(expenses, startTs) { expenses.filter { it.timestamp >= startTs } }

    val availableReports = remember(activeRole.isOwner) {
        ReportType.entries.filter { activeRole.isOwner || !it.ownerOnly }
    }

    val summaryPairs: List<Pair<String, String>> = remember(reportType, filteredSales, filteredItems, filteredPurchases, filteredExpenses, customers, suppliers, products) {
        when (reportType) {
            ReportType.SALES -> listOf(
                "Total Invoices" to "${filteredSales.size}",
                "Gross Sales" to Formatters.currency(filteredSales.sumOf { it.grandTotal }, sym),
                "Collected" to Formatters.currency(filteredSales.sumOf { it.amountPaid }, sym),
                "Receivable" to Formatters.currency(filteredSales.sumOf { it.remainingBalance }, sym)
            )
            ReportType.PROFIT_LOSS -> {
                val rev = filteredSales.sumOf { it.grandTotal }
                val cogs = filteredItems.sumOf { (it.quantity - it.returnedQuantity).coerceAtLeast(0) * it.purchasePriceAtSale }
                val gp = rev - cogs
                val exp = filteredExpenses.sumOf { it.amount }
                listOf(
                    "Sales Revenue" to Formatters.currency(rev, sym),
                    "Cost of Goods Sold" to Formatters.currency(cogs, sym),
                    "Gross Profit" to Formatters.currency(gp, sym),
                    "Total Expenses" to Formatters.currency(exp, sym),
                    "Net Profit" to Formatters.currency(gp - exp, sym)
                )
            }
            ReportType.PURCHASES -> listOf(
                "Purchase Bills" to "${filteredPurchases.size}",
                "Total Purchased" to Formatters.currency(filteredPurchases.sumOf { it.totalAmount }, sym),
                "Total Paid" to Formatters.currency(filteredPurchases.sumOf { it.paidAmount }, sym)
            )
            ReportType.EXPENSES -> listOf(
                "Expense Entries" to "${filteredExpenses.size}",
                "Total Expenses" to Formatters.currency(filteredExpenses.sumOf { it.amount }, sym)
            )
            ReportType.STOCK -> listOf(
                "Active Products" to "${products.size}",
                "Total Stock Units" to "${products.sumOf { it.quantity }}",
                "Inventory Cost Value" to Formatters.currency(products.sumOf { it.quantity * it.purchasePrice }, sym)
            )
            ReportType.CUSTOMER_DUE -> listOf(
                "Customers with Due" to "${customers.count { it.outstandingBalance > 0 }}",
                "Total Receivable" to Formatters.currency(customers.sumOf { it.outstandingBalance }, sym)
            )
            ReportType.SUPPLIER_DUE -> listOf(
                "Suppliers with Due" to "${suppliers.count { it.remainingBalance > 0 }}",
                "Total Payable" to Formatters.currency(suppliers.sumOf { it.remainingBalance }, sym)
            )
            else -> listOf(
                "Total Invoices" to "${filteredSales.size}",
                "Total Revenue" to Formatters.currency(filteredSales.sumOf { it.grandTotal }, sym)
            )
        }
    }

    val rows: List<List<String>> = remember(reportType, filteredSales, filteredItems, filteredPurchases, filteredExpenses, customers, suppliers, products) {
        when (reportType) {
            ReportType.SALES -> filteredSales.map { listOf(it.invoiceNumber, it.customerName, Formatters.dateOnly(it.timestamp), Formatters.currency(it.grandTotal, sym)) }
            ReportType.PRODUCT_WISE -> filteredItems.groupBy { it.productName }.map { (name, list) ->
                listOf(name, "Qty: ${list.sumOf { it.quantity }}", Formatters.currency(list.sumOf { it.lineTotal }, sym))
            }
            ReportType.CATEGORY_WISE -> {
                val catMap = products.associate { it.id to it.categoryName }
                filteredItems.groupBy { catMap[it.productId] ?: "OTHER" }.map { (cat, list) ->
                    listOf(cat, "Qty: ${list.sumOf { it.quantity }}", Formatters.currency(list.sumOf { it.lineTotal }, sym))
                }
            }
            ReportType.PURCHASES -> filteredPurchases.map { listOf(it.purchaseInvoiceNumber, it.supplierName, Formatters.currency(it.totalAmount, sym)) }
            ReportType.EXPENSES -> filteredExpenses.map { listOf(it.title, it.category, Formatters.currency(it.amount, sym)) }
            ReportType.PROFIT_LOSS -> summaryPairs.map { listOf(it.first, it.second) }
            ReportType.CUSTOMER_DUE -> customers.filter { it.outstandingBalance > 0 }.map { listOf(it.name, it.mobile, Formatters.currency(it.outstandingBalance, sym)) }
            ReportType.SUPPLIER_DUE -> suppliers.filter { it.remainingBalance > 0 }.map { listOf(it.name, it.mobile, Formatters.currency(it.remainingBalance, sym)) }
            ReportType.STOCK -> products.map { listOf(it.name, it.categoryName, "Qty: ${it.quantity}", Formatters.currency(it.quantity * it.purchasePrice, sym)) }
            ReportType.PAYMENTS -> filteredSales.groupBy { it.paymentMethod }.map { (m, list) ->
                listOf(m, "${list.size} bills", Formatters.currency(list.sumOf { it.amountPaid }, sym))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Business Reports") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                val pdf = InvoicePdfGenerator.generateBusinessReportPdf(
                                    context, settings, reportType.label, period.label, summaryPairs, rows
                                )
                                PrintAndShareHelper.shareFileGeneric(context, pdf, "application/pdf", "Share Report PDF")
                            }
                        },
                        modifier = Modifier.testTag("export_report_pdf_btn")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF")
                    }
                    IconButton(
                        onClick = {
                            scope.launch {
                                val csv = if (reportType == ReportType.STOCK) {
                                    backupManager.exportInventoryCsv(products)
                                } else {
                                    backupManager.exportSalesCsv(filteredSales)
                                }
                                PrintAndShareHelper.shareFileGeneric(context, csv, "text/csv", "Export Excel CSV")
                                Toast.makeText(context, "CSV ready to open in Excel / Sheets!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.testTag("export_report_csv_btn")
                    ) {
                        Icon(Icons.Default.TableChart, contentDescription = "Export Excel CSV")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .widthIn(max = 820.dp)
        ) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReportPeriod.entries.forEach { p ->
                    FilterChip(selected = period == p, onClick = { period = p }, label = { Text(p.label) })
                }
            }

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableReports.forEach { r ->
                    FilterChip(selected = reportType == r, onClick = { reportType = r }, label = { Text(r.label) })
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("${reportType.label} Summary (${period.label})", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            HorizontalDivider()
                            summaryPairs.forEach { (k, v) ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(k, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(v, style = MaterialTheme.typography.titleSmall, color = if (k.contains("Net Profit") && v.startsWith("-")) LossCrimson else ProfitGreen)
                                }
                            }
                        }
                    }
                }

                if (rows.isEmpty()) {
                    item {
                        EmptyStateCard(Icons.Default.Assessment, "No Records in Selected Period", "Transactions in this period will appear here and export cleanly to PDF or Excel CSV.")
                    }
                } else {
                    items(rows) { cols ->
                        OutlinedCard(shape = RoundedCornerShape(12.dp)) {
                            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(cols.dropLast(1).joinToString(" • "), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                Text(cols.last(), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}
