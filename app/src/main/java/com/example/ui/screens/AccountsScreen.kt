package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.*
import com.example.data.repository.ActiveSessionRole
import com.example.ui.components.BoutiqueKpiCard
import com.example.ui.components.EmptyStateCard
import com.example.ui.theme.LossCrimson
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.AccountingSummary
import com.example.util.Formatters

@Composable
fun AccountsScreen(
    settings: BusinessSettingsEntity,
    activeRole: ActiveSessionRole,
    summary: AccountingSummary,
    expenses: List<ExpenseEntity>,
    otherIncomes: List<OtherIncomeEntity>,
    sales: List<SaleInvoiceEntity>,
    saleItems: List<SaleItemEntity>,
    returns: List<ReturnExchangeEntity>,
    products: List<ProductEntity>,
    openAddExpenseOnStart: Boolean = false,
    onConsumedOpenExpense: () -> Unit = {},
    onAddExpense: (ExpenseEntity) -> Unit,
    onArchiveExpense: (ExpenseEntity) -> Unit,
    onAddOtherIncome: (OtherIncomeEntity) -> Unit,
    onOpenInvoice: (SaleInvoiceEntity) -> Unit,
    onProcessReturnOrExchange: (SaleInvoiceEntity, SaleItemEntity?, Int, Boolean, ProductEntity?, Double, String) -> Unit,
    onCancelInvoice: (SaleInvoiceEntity, String) -> Unit
) {
    if (!activeRole.isOwner) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            EmptyStateCard(
                icon = Icons.Default.Lock,
                title = "Owner Financial Access Only",
                subtitle = "Accounting, Profit & Loss, Expenses, and Cash Flow reports are restricted to Owner (${settings.ownerName})."
            )
        }
        return
    }

    val sym = settings.currencySymbol
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showAddOtherIncomeDialog by remember { mutableStateOf(false) }
    var returnTargetInvoice by remember { mutableStateOf<SaleInvoiceEntity?>(null) }

    LaunchedEffect(openAddExpenseOnStart) {
        if (openAddExpenseOnStart) {
            selectedTab = 1
            showAddExpenseDialog = true
            onConsumedOpenExpense()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .widthIn(max = 820.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Accounting & Finance", style = MaterialTheme.typography.headlineMedium)
                Text("Real-time P&L, Cash Flow, Expenses & Returns", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (selectedTab == 1) {
                FilledTonalButton(onClick = { showAddExpenseDialog = true }, modifier = Modifier.testTag("add_expense_btn")) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Expense")
                }
            } else if (selectedTab == 2) {
                FilledTonalButton(onClick = { showAddOtherIncomeDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Income")
                }
            }
        }

        ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 16.dp) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Overview") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Expenses (${expenses.size})") })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Other Income") })
            Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Invoices & Returns") })
        }

        when (selectedTab) {
            0 -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            BoutiqueKpiCard("Daily Income", Formatters.currency(summary.dailyIncome, sym), icon = Icons.Default.ArrowDownward, accentColor = ProfitGreen, modifier = Modifier.weight(1f))
                            BoutiqueKpiCard("Daily Expenses", Formatters.currency(summary.dailyExpenses, sym), icon = Icons.Default.ArrowUpward, accentColor = LossCrimson, modifier = Modifier.weight(1f))
                        }
                    }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            BoutiqueKpiCard("Monthly Income", Formatters.currency(summary.monthlyIncome, sym), icon = Icons.Default.CalendarMonth, accentColor = ProfitGreen, modifier = Modifier.weight(1f))
                            BoutiqueKpiCard("Monthly Expenses", Formatters.currency(summary.monthlyExpenses, sym), icon = Icons.Default.ReceiptLong, accentColor = LossCrimson, modifier = Modifier.weight(1f))
                        }
                    }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            BoutiqueKpiCard("Gross Profit", Formatters.currency(summary.grossProfit, sym), subtitle = "Sales minus COGS", icon = Icons.Default.ShowChart, accentColor = ProfitGreen, modifier = Modifier.weight(1f))
                            BoutiqueKpiCard("Net Profit", Formatters.currency(summary.netProfit, sym), subtitle = "After all expenses", icon = Icons.Default.AccountBalance, accentColor = if (summary.netProfit >= 0) ProfitGreen else LossCrimson, modifier = Modifier.weight(1f))
                        }
                    }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            BoutiqueKpiCard("Cash in Hand", Formatters.currency(summary.cashInHand, sym), icon = Icons.Default.Payments, accentColor = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                            BoutiqueKpiCard("UPI / PhonePe", Formatters.currency(summary.upiCollections, sym), icon = Icons.Default.QrCode2, accentColor = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                        }
                    }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            BoutiqueKpiCard("Bank Balance (Est.)", Formatters.currency(summary.bankBalance, sym), icon = Icons.Default.AccountBalanceWallet, accentColor = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                            BoutiqueKpiCard("Inventory Value", Formatters.currency(summary.inventoryValue, sym), subtitle = "At purchase cost", icon = Icons.Default.Inventory, accentColor = MaterialTheme.colorScheme.secondary, modifier = Modifier.weight(1f))
                        }
                    }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            BoutiqueKpiCard("Customer Due", Formatters.currency(summary.customerOutstanding, sym), icon = Icons.Default.PersonOutline, accentColor = WarningAmber, modifier = Modifier.weight(1f))
                            BoutiqueKpiCard("Supplier Payable", Formatters.currency(summary.supplierOutstanding, sym), icon = Icons.Default.LocalShipping, accentColor = LossCrimson, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            1 -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (expenses.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Default.ReceiptLong,
                                title = "No Expenses Logged",
                                subtitle = "Track Shop Rent, Electricity, Transport, Packaging, Salary, and Marketing."
                            )
                        }
                    } else {
                        items(expenses, key = { it.id }) { exp ->
                            ElevatedCard(shape = RoundedCornerShape(14.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(exp.title, style = MaterialTheme.typography.titleSmall)
                                        Text("${exp.category} • ${exp.paymentMode} • ${Formatters.dateTime(exp.timestamp)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text(Formatters.currency(exp.amount, sym), style = MaterialTheme.typography.titleSmall, color = LossCrimson)
                                    IconButton(onClick = { onArchiveExpense(exp) }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Archive Expense")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (otherIncomes.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Default.AddCard,
                                title = "No Other Income Entries",
                                subtitle = "Record alteration fees, custom stitching charges, or miscellaneous income."
                            )
                        }
                    } else {
                        items(otherIncomes, key = { it.id }) { inc ->
                            ElevatedCard(shape = RoundedCornerShape(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(inc.title, style = MaterialTheme.typography.titleSmall)
                                        Text("${inc.source} • ${inc.paymentMode} • ${Formatters.dateTime(inc.timestamp)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text(Formatters.currency(inc.amount, sym), style = MaterialTheme.typography.titleSmall, color = ProfitGreen)
                                }
                            }
                        }
                    }
                }
            }

            3 -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (returns.isNotEmpty()) {
                        item {
                            Text("Returns & Exchanges History (${returns.size})", style = MaterialTheme.typography.titleSmall)
                        }
                        items(returns.take(10), key = { "ret_${it.id}" }) { r ->
                            OutlinedCard(shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.4f))) {
                                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                                    Text("${r.type} • ${r.invoiceNumber}", style = MaterialTheme.typography.titleSmall, color = WarningAmber)
                                    Text("Returned: ${r.returnedProductName} (x${r.returnedQty})" + if (r.exchangeProductName.isNotBlank()) " → Exchanged: ${r.exchangeProductName}" else "", style = MaterialTheme.typography.bodySmall)
                                    Text("Refund: ${Formatters.currency(r.refundAmount, sym)} • Reason: ${r.reason}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    item {
                        Text("All Sales Invoices (Permanent Traceable Ledger)", style = MaterialTheme.typography.titleSmall)
                    }

                    if (sales.isEmpty()) {
                        item {
                            EmptyStateCard(Icons.Default.Receipt, "No Invoices Yet", "Completed bills appear here permanently.")
                        }
                    } else {
                        items(sales, key = { "inv_${it.id}" }) { inv ->
                            ElevatedCard(
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().clickable { onOpenInvoice(inv) }
                            ) {
                                Column(Modifier.padding(14.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("${inv.invoiceNumber} • ${inv.customerName}", style = MaterialTheme.typography.titleSmall)
                                        Text(Formatters.currency(inv.grandTotal, sym), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Text("${Formatters.dateTime(inv.timestamp)} • ${inv.paymentMethod} • Status: ${inv.status}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (inv.status != "CANCELLED" && inv.status != "RETURNED") {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                                            OutlinedButton(onClick = { onOpenInvoice(inv) }) {
                                                Text("PDF / Share")
                                            }
                                            OutlinedButton(onClick = { returnTargetInvoice = inv }) {
                                                Text("Return / Exchange")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddExpenseDialog) {
        AddExpenseDialog(
            sym = sym,
            onDismiss = { showAddExpenseDialog = false },
            onSave = { exp ->
                onAddExpense(exp)
                showAddExpenseDialog = false
            }
        )
    }

    if (showAddOtherIncomeDialog) {
        AddOtherIncomeDialog(
            sym = sym,
            onDismiss = { showAddOtherIncomeDialog = false },
            onSave = { inc ->
                onAddOtherIncome(inc)
                showAddOtherIncomeDialog = false
            }
        )
    }

    if (returnTargetInvoice != null) {
        val inv = returnTargetInvoice!!
        val items = saleItems.filter { it.invoiceId == inv.id }
        ReturnExchangeDialog(
            sym = sym,
            invoice = inv,
            items = items,
            products = products,
            onDismiss = { returnTargetInvoice = null },
            onConfirmReturn = { item, qty, isFull, exchangeProd, refundAmt, reason ->
                onProcessReturnOrExchange(inv, item, qty, isFull, exchangeProd, refundAmt, reason)
                returnTargetInvoice = null
            },
            onCancelInvoice = { reason ->
                onCancelInvoice(inv, reason)
                returnTargetInvoice = null
            }
        )
    }
}

@Composable
private fun AddExpenseDialog(
    sym: String,
    onDismiss: () -> Unit,
    onSave: (ExpenseEntity) -> Unit
) {
    val cats = listOf("Shop Rent", "Electricity", "Transport", "Packaging", "Salary", "Marketing", "Miscellaneous")
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(cats.first()) }
    var amountText by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf("Cash") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Business Expense") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Expense Title *") }, singleLine = true, modifier = Modifier.testTag("expense_title_input"))
                Text("Category", style = MaterialTheme.typography.labelMedium)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    cats.forEach { c ->
                        FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c) })
                    }
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount ($sym) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.testTag("expense_amount_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Cash", "UPI", "Bank").forEach { m ->
                        FilterChip(selected = mode == m, onClick = { mode = m }, label = { Text(m) })
                    }
                }
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Note (Optional)") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && amt > 0) {
                        onSave(ExpenseEntity(title = title.trim(), category = category, amount = amt, paymentMode = mode, note = note.trim()))
                    }
                },
                modifier = Modifier.testTag("save_expense_button")
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun AddOtherIncomeDialog(
    sym: String,
    onDismiss: () -> Unit,
    onSave: (OtherIncomeEntity) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var source by remember { mutableStateOf("Alteration / Tailoring") }
    var amountText by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf("Cash") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Other Income") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Income Title *") }, singleLine = true)
                OutlinedTextField(value = source, onValueChange = { source = it }, label = { Text("Source") }, singleLine = true)
                OutlinedTextField(value = amountText, onValueChange = { amountText = it }, label = { Text("Amount ($sym) *") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Cash", "UPI", "Bank").forEach { m ->
                        FilterChip(selected = mode == m, onClick = { mode = m }, label = { Text(m) })
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val amt = amountText.toDoubleOrNull() ?: 0.0
                if (title.isNotBlank() && amt > 0) {
                    onSave(OtherIncomeEntity(title = title.trim(), source = source.trim(), amount = amt, paymentMode = mode))
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReturnExchangeDialog(
    sym: String,
    invoice: SaleInvoiceEntity,
    items: List<SaleItemEntity>,
    products: List<ProductEntity>,
    onDismiss: () -> Unit,
    onConfirmReturn: (SaleItemEntity?, Int, Boolean, ProductEntity?, Double, String) -> Unit,
    onCancelInvoice: (String) -> Unit
) {
    var mode by remember { mutableStateOf("FULL") } // FULL, PARTIAL, EXCHANGE, CANCEL
    var selectedItem by remember { mutableStateOf(items.firstOrNull { it.quantity > it.returnedQuantity }) }
    var returnQtyText by remember { mutableStateOf("1") }
    var exchangeProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var refundText by remember { mutableStateOf("%.2f".format(invoice.amountPaid)) }
    var reason by remember { mutableStateOf("") }
    var prodMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Return / Exchange • ${invoice.invoiceNumber}") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = mode == "FULL", onClick = { mode = "FULL"; refundText = "%.2f".format(invoice.amountPaid) }, label = { Text("Full Return") })
                    FilterChip(selected = mode == "PARTIAL", onClick = { mode = "PARTIAL" }, label = { Text("Partial") })
                    FilterChip(selected = mode == "EXCHANGE", onClick = { mode = "EXCHANGE"; refundText = "0.00" }, label = { Text("Exchange") })
                    FilterChip(selected = mode == "CANCEL", onClick = { mode = "CANCEL" }, label = { Text("Cancel Bill") })
                }

                if (mode == "PARTIAL" || mode == "EXCHANGE") {
                    Text("Select Item to Return:", style = MaterialTheme.typography.labelMedium)
                    items.filter { it.quantity > it.returnedQuantity }.forEach { si ->
                        OutlinedCard(
                            border = BorderStroke(1.dp, if (selectedItem?.id == si.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth().clickable { selectedItem = si }
                        ) {
                            Text("${si.productName} (Returnable: ${si.quantity - si.returnedQuantity})", modifier = Modifier.padding(10.dp), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    OutlinedTextField(
                        value = returnQtyText,
                        onValueChange = { returnQtyText = it },
                        label = { Text("Return Quantity") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }

                if (mode == "EXCHANGE") {
                    ExposedDropdownMenuBox(expanded = prodMenu, onExpandedChange = { prodMenu = !prodMenu }) {
                        OutlinedTextField(
                            value = exchangeProduct?.name ?: "Select Replacement Product",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Exchange With") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = prodMenu) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = prodMenu, onDismissRequest = { prodMenu = false }) {
                            products.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text("${p.name} (Stock: ${p.quantity})") },
                                    onClick = { exchangeProduct = p; prodMenu = false }
                                )
                            }
                        }
                    }
                }

                if (mode != "CANCEL") {
                    OutlinedTextField(
                        value = refundText,
                        onValueChange = { refundText = it },
                        label = { Text("Refund Amount ($sym)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Return / Cancel Reason *") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val safeReason = reason.ifBlank { "Customer request" }
                if (mode == "CANCEL") {
                    onCancelInvoice(safeReason)
                } else {
                    val qty = returnQtyText.toIntOrNull() ?: 1
                    val refund = refundText.toDoubleOrNull() ?: 0.0
                    onConfirmReturn(selectedItem, qty, mode == "FULL", if (mode == "EXCHANGE") exchangeProduct else null, refund, safeReason)
                }
            }) { Text("Confirm") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
