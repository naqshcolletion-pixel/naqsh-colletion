package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.*
import com.example.data.repository.PurchaseCartLine
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.theme.LossCrimson
import com.example.ui.theme.ProfitGreen
import com.example.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersScreen(
    settings: BusinessSettingsEntity,
    customers: List<CustomerEntity>,
    customerPayments: List<CustomerPaymentEntity>,
    sales: List<SaleInvoiceEntity>,
    openAddOnStart: Boolean = false,
    onConsumedOpenAdd: () -> Unit = {},
    onBack: () -> Unit,
    onSaveCustomer: (CustomerEntity) -> Unit,
    onDeleteCustomer: (CustomerEntity) -> Unit,
    onRecordPayment: (CustomerEntity, Double, String, String) -> Unit,
    onOpenInvoice: (SaleInvoiceEntity) -> Unit
) {
    val sym = settings.currencySymbol
    var search by remember { mutableStateOf("") }
    var editingCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var ledgerCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var paymentCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var deleteCustomer by remember { mutableStateOf<CustomerEntity?>(null) }

    LaunchedEffect(openAddOnStart) {
        if (openAddOnStart) {
            editingCustomer = null
            showEditor = true
            onConsumedOpenAdd()
        }
    }

    val filtered = remember(customers, search) {
        customers.filter {
            search.isBlank() || it.name.contains(search, true) || it.mobile.contains(search, true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Customers (${customers.size})") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editingCustomer = null; showEditor = true },
                icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                text = { Text("Add Customer") },
                modifier = Modifier.testTag("fab_add_customer")
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .widthIn(max = 820.dp)
        ) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Search customer name or mobile...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (filtered.isEmpty()) {
                    item {
                        EmptyStateCard(Icons.Default.PeopleOutline, "No Customers Found", "Add boutique customers to track their purchase history and outstanding dues.")
                    }
                } else {
                    items(filtered, key = { it.id }) { cust ->
                        ElevatedCard(
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().clickable { ledgerCustomer = cust }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(cust.name, style = MaterialTheme.typography.titleMedium)
                                    Text("${cust.mobile} • ${cust.address.ifBlank { "No address" }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Purchases: ${Formatters.currencyCompact(cust.totalPurchases, sym)} • Paid: ${Formatters.currencyCompact(cust.totalPaid, sym)}", style = MaterialTheme.typography.labelSmall)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = if (cust.outstandingBalance > 0) "Due ${Formatters.currencyCompact(cust.outstandingBalance, sym)}" else "Settled",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (cust.outstandingBalance > 0) LossCrimson else ProfitGreen
                                    )
                                    Row {
                                        IconButton(onClick = { paymentCustomer = cust }) {
                                            Icon(Icons.Default.Payments, contentDescription = "Receive Payment")
                                        }
                                        IconButton(onClick = { editingCustomer = cust; showEditor = true }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                                        }
                                        IconButton(onClick = { deleteCustomer = cust }) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
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

    if (showEditor) {
        var name by remember { mutableStateOf(editingCustomer?.name ?: "") }
        var mobile by remember { mutableStateOf(editingCustomer?.mobile ?: "") }
        var address by remember { mutableStateOf(editingCustomer?.address ?: "") }
        AlertDialog(
            onDismissRequest = { showEditor = false },
            title = { Text(if (editingCustomer == null) "Add Customer" else "Edit Customer") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Customer Name *") }, singleLine = true, modifier = Modifier.testTag("customer_name_input"))
                    OutlinedTextField(value = mobile, onValueChange = { mobile = it }, label = { Text("Mobile Number *") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true, modifier = Modifier.testTag("customer_mobile_input"))
                    OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, minLines = 2)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank() && mobile.isNotBlank()) {
                            onSaveCustomer(
                                (editingCustomer ?: CustomerEntity(name = "", mobile = "")).copy(
                                    name = name.trim(),
                                    mobile = mobile.trim(),
                                    address = address.trim()
                                )
                            )
                            showEditor = false
                        }
                    },
                    modifier = Modifier.testTag("save_customer_button")
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showEditor = false }) { Text("Cancel") } }
        )
    }

    if (paymentCustomer != null) {
        val c = paymentCustomer!!
        var amtText by remember { mutableStateOf("%.2f".format(c.outstandingBalance)) }
        var mode by remember { mutableStateOf("Cash") }
        var note by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { paymentCustomer = null },
            title = { Text("Receive Payment • ${c.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Outstanding: ${Formatters.currency(c.outstandingBalance, sym)}", color = LossCrimson)
                    OutlinedTextField(value = amtText, onValueChange = { amtText = it }, label = { Text("Amount ($sym)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Cash", "PhonePe", "UPI", "Bank").forEach { m ->
                            FilterChip(selected = mode == m, onClick = { mode = m }, label = { Text(m) })
                        }
                    }
                    OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Note") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amt = amtText.toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        onRecordPayment(c, amt, mode, note.trim())
                        paymentCustomer = null
                    }
                }) { Text("Record") }
            },
            dismissButton = { TextButton(onClick = { paymentCustomer = null }) { Text("Cancel") } }
        )
    }

    if (ledgerCustomer != null) {
        val c = ledgerCustomer!!
        val custInvoices = sales.filter { it.customerId == c.id }
        val custPays = customerPayments.filter { it.customerId == c.id }
        AlertDialog(
            onDismissRequest = { ledgerCustomer = null },
            title = { Text("${c.name} Ledger") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Total Purchases: ${Formatters.currency(c.totalPurchases, sym)}")
                    Text("Total Paid: ${Formatters.currency(c.totalPaid, sym)}", color = ProfitGreen)
                    Text("Outstanding: ${Formatters.currency(c.outstandingBalance, sym)}", color = LossCrimson)
                    HorizontalDivider()
                    Text("Previous Invoices (${custInvoices.size})", style = MaterialTheme.typography.titleSmall)
                    custInvoices.forEach { inv ->
                        Row(Modifier.fillMaxWidth().clickable { ledgerCustomer = null; onOpenInvoice(inv) }.padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${inv.invoiceNumber} • ${Formatters.dateOnly(inv.timestamp)}", style = MaterialTheme.typography.bodySmall)
                            Text(Formatters.currency(inv.grandTotal, sym), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    HorizontalDivider()
                    Text("Payment History (${custPays.size})", style = MaterialTheme.typography.titleSmall)
                    custPays.forEach { p ->
                        Text("${Formatters.dateOnly(p.timestamp)} • ${Formatters.currency(p.amount, sym)} (${p.paymentMode})", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = { Button(onClick = { ledgerCustomer = null }) { Text("Close") } }
        )
    }

    if (deleteCustomer != null) {
        ConfirmDeleteDialog(
            title = "Move ${deleteCustomer!!.name} to Trash?",
            message = "Their financial invoices remain intact and you can restore the customer profile anytime from Settings.",
            onConfirm = {
                onDeleteCustomer(deleteCustomer!!)
                deleteCustomer = null
            },
            onDismiss = { deleteCustomer = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuppliersAndPurchasesScreen(
    settings: BusinessSettingsEntity,
    suppliers: List<SupplierEntity>,
    supplierPayments: List<SupplierPaymentEntity>,
    purchases: List<PurchaseEntity>,
    products: List<ProductEntity>,
    initialTab: Int = 0,
    openNewPurchaseOnStart: Boolean = false,
    onConsumedOpenPurchase: () -> Unit = {},
    onBack: () -> Unit,
    onSaveSupplier: (SupplierEntity) -> Unit,
    onDeleteSupplier: (SupplierEntity) -> Unit,
    onRecordSupplierPayment: (SupplierEntity, Double, String, String) -> Unit,
    onRecordPurchase: (SupplierEntity?, String, List<PurchaseCartLine>, String, Double, String) -> Unit
) {
    val sym = settings.currencySymbol
    var tab by remember { mutableIntStateOf(initialTab) }
    var showSupplierEditor by remember { mutableStateOf(false) }
    var editingSupplier by remember { mutableStateOf<SupplierEntity?>(null) }
    var paymentSupplier by remember { mutableStateOf<SupplierEntity?>(null) }
    var deleteSupplier by remember { mutableStateOf<SupplierEntity?>(null) }
    var showNewPurchaseDialog by remember { mutableStateOf(false) }

    LaunchedEffect(openNewPurchaseOnStart) {
        if (openNewPurchaseOnStart) {
            tab = 1
            showNewPurchaseDialog = true
            onConsumedOpenPurchase()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Suppliers & Purchases") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (tab == 0) {
                        editingSupplier = null; showSupplierEditor = true
                    } else {
                        showNewPurchaseDialog = true
                    }
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(if (tab == 0) "Add Supplier" else "New Purchase") },
                modifier = Modifier.testTag("fab_supplier_purchase")
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .widthIn(max = 820.dp)
        ) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Suppliers (${suppliers.size})") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Purchases (${purchases.size})") })
            }

            if (tab == 0) {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (suppliers.isEmpty()) {
                        item { EmptyStateCard(Icons.Default.LocalShipping, "No Suppliers Yet", "Add wholesale Abaya/Suit suppliers to track purchases and balances.") }
                    } else {
                        items(suppliers, key = { it.id }) { sup ->
                            ElevatedCard(shape = RoundedCornerShape(14.dp)) {
                                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(sup.name, style = MaterialTheme.typography.titleMedium)
                                        Text("${sup.mobile} • ${sup.address}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("Purchased: ${Formatters.currencyCompact(sup.totalPurchased, sym)} • Paid: ${Formatters.currencyCompact(sup.totalPaid, sym)}", style = MaterialTheme.typography.labelSmall)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = if (sup.remainingBalance > 0) "Payable ${Formatters.currencyCompact(sup.remainingBalance, sym)}" else "Settled",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = if (sup.remainingBalance > 0) LossCrimson else ProfitGreen
                                        )
                                        Row {
                                            IconButton(onClick = { paymentSupplier = sup }) { Icon(Icons.Default.Payments, contentDescription = "Pay Supplier") }
                                            IconButton(onClick = { editingSupplier = sup; showSupplierEditor = true }) { Icon(Icons.Default.Edit, contentDescription = "Edit") }
                                            IconButton(onClick = { deleteSupplier = sup }) { Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (purchases.isEmpty()) {
                        item { EmptyStateCard(Icons.Default.Inventory, "No Purchases Recorded", "Tap + New Purchase to restock inventory and update supplier ledger automatically.") }
                    } else {
                        items(purchases, key = { it.id }) { pur ->
                            ElevatedCard(shape = RoundedCornerShape(14.dp)) {
                                Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column(Modifier.weight(1f)) {
                                        Text("${pur.purchaseInvoiceNumber} • ${pur.supplierName}", style = MaterialTheme.typography.titleSmall)
                                        Text("${Formatters.dateTime(pur.purchaseDate)} • ${pur.paymentMode}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("Paid: ${Formatters.currency(pur.paidAmount, sym)} • Due: ${Formatters.currency(pur.outstandingAmount, sym)}", style = MaterialTheme.typography.labelSmall)
                                    }
                                    Text(Formatters.currency(pur.totalAmount, sym), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSupplierEditor) {
        var name by remember { mutableStateOf(editingSupplier?.name ?: "") }
        var mobile by remember { mutableStateOf(editingSupplier?.mobile ?: "") }
        var address by remember { mutableStateOf(editingSupplier?.address ?: "") }
        AlertDialog(
            onDismissRequest = { showSupplierEditor = false },
            title = { Text(if (editingSupplier == null) "Add Supplier" else "Edit Supplier") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Supplier Name *") }, singleLine = true)
                    OutlinedTextField(value = mobile, onValueChange = { mobile = it }, label = { Text("Mobile *") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true)
                    OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("City / Market Address") }, minLines = 2)
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (name.isNotBlank() && mobile.isNotBlank()) {
                        onSaveSupplier((editingSupplier ?: SupplierEntity(name = "", mobile = "")).copy(name = name.trim(), mobile = mobile.trim(), address = address.trim()))
                        showSupplierEditor = false
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showSupplierEditor = false }) { Text("Cancel") } }
        )
    }

    if (paymentSupplier != null) {
        val s = paymentSupplier!!
        var amtText by remember { mutableStateOf("%.2f".format(s.remainingBalance)) }
        var mode by remember { mutableStateOf("Cash") }
        AlertDialog(
            onDismissRequest = { paymentSupplier = null },
            title = { Text("Pay Supplier • ${s.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Remaining Balance: ${Formatters.currency(s.remainingBalance, sym)}", color = LossCrimson)
                    OutlinedTextField(value = amtText, onValueChange = { amtText = it }, label = { Text("Amount ($sym)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Cash", "UPI", "Bank").forEach { m ->
                            FilterChip(selected = mode == m, onClick = { mode = m }, label = { Text(m) })
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amt = amtText.toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        onRecordSupplierPayment(s, amt, mode, "Supplier Settlement")
                        paymentSupplier = null
                    }
                }) { Text("Pay") }
            },
            dismissButton = { TextButton(onClick = { paymentSupplier = null }) { Text("Cancel") } }
        )
    }

    if (deleteSupplier != null) {
        ConfirmDeleteDialog(
            title = "Move ${deleteSupplier!!.name} to Trash?",
            message = "Purchase history remains intact and you can restore the supplier later from Settings.",
            onConfirm = { onDeleteSupplier(deleteSupplier!!); deleteSupplier = null },
            onDismiss = { deleteSupplier = null }
        )
    }

    if (showNewPurchaseDialog) {
        NewPurchaseDialog(
            sym = sym,
            suppliers = suppliers,
            products = products,
            onDismiss = { showNewPurchaseDialog = false },
            onConfirm = { sup, fallbackName, lines, mode, paid, notes ->
                onRecordPurchase(sup, fallbackName, lines, mode, paid, notes)
                showNewPurchaseDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewPurchaseDialog(
    sym: String,
    suppliers: List<SupplierEntity>,
    products: List<ProductEntity>,
    onDismiss: () -> Unit,
    onConfirm: (SupplierEntity?, String, List<PurchaseCartLine>, String, Double, String) -> Unit
) {
    var selectedSupplier by remember { mutableStateOf<SupplierEntity?>(suppliers.firstOrNull()) }
    var fallbackSupplier by remember { mutableStateOf("") }
    var supExpanded by remember { mutableStateOf(false) }

    var selectedProd by remember { mutableStateOf(products.firstOrNull()) }
    var prodExpanded by remember { mutableStateOf(false) }
    var qtyText by remember { mutableStateOf("5") }
    var priceText by remember { mutableStateOf(selectedProd?.purchasePrice?.takeIf { it > 0 }?.toString() ?: "500") }

    var lines by remember { mutableStateOf<List<PurchaseCartLine>>(emptyList()) }
    var paymentMode by remember { mutableStateOf("Cash") }
    val total = lines.sumOf { it.lineTotal }
    var paidText by remember(total) { mutableStateOf("%.2f".format(total)) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Stock Purchase") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(expanded = supExpanded, onExpandedChange = { supExpanded = !supExpanded }) {
                    OutlinedTextField(
                        value = selectedSupplier?.name ?: "Direct / Custom Supplier",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Supplier") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = supExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = supExpanded, onDismissRequest = { supExpanded = false }) {
                        DropdownMenuItem(text = { Text("Direct / Custom Supplier") }, onClick = { selectedSupplier = null; supExpanded = false })
                        suppliers.forEach { s ->
                            DropdownMenuItem(text = { Text(s.name) }, onClick = { selectedSupplier = s; supExpanded = false })
                        }
                    }
                }
                if (selectedSupplier == null) {
                    OutlinedTextField(value = fallbackSupplier, onValueChange = { fallbackSupplier = it }, label = { Text("Supplier Name") }, singleLine = true)
                }

                HorizontalDivider()
                ExposedDropdownMenuBox(expanded = prodExpanded, onExpandedChange = { prodExpanded = !prodExpanded }) {
                    OutlinedTextField(
                        value = selectedProd?.name ?: "Select Product",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Product to Restock") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = prodExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = prodExpanded, onDismissRequest = { prodExpanded = false }) {
                        products.forEach { p ->
                            DropdownMenuItem(text = { Text("${p.name} (Cur: ${p.quantity})") }, onClick = {
                                selectedProd = p
                                if (p.purchasePrice > 0) priceText = p.purchasePrice.toString()
                                prodExpanded = false
                            })
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = qtyText, onValueChange = { qtyText = it }, label = { Text("Qty") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = priceText, onValueChange = { priceText = it }, label = { Text("Cost ($sym)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                    FilledTonalButton(onClick = {
                        val p = selectedProd ?: return@FilledTonalButton
                        val q = qtyText.toIntOrNull() ?: 0
                        val pr = priceText.toDoubleOrNull() ?: 0.0
                        if (q > 0 && pr >= 0) {
                            lines = lines + PurchaseCartLine(p, null, q, pr)
                        }
                    }) { Text("+ Add") }
                }

                lines.forEachIndexed { idx, l ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("${l.product.name} x${l.quantity} @ ${Formatters.currencyCompact(l.purchasePrice, sym)}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        Text(Formatters.currencyCompact(l.lineTotal, sym), style = MaterialTheme.typography.labelMedium)
                        IconButton(onClick = { lines = lines.toMutableList().apply { removeAt(idx) } }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                        }
                    }
                }

                if (lines.isNotEmpty()) {
                    HorizontalDivider()
                    Text("Total Purchase: ${Formatters.currency(total, sym)}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Cash", "UPI", "Bank", "Credit").forEach { m ->
                            FilterChip(selected = paymentMode == m, onClick = { paymentMode = m; if (m == "Credit") paidText = "0" }, label = { Text(m) })
                        }
                    }
                    OutlinedTextField(value = paidText, onValueChange = { paidText = it }, label = { Text("Paid Amount ($sym)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Purchase Note") }, singleLine = true)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = lines.isNotEmpty(),
                onClick = {
                    val paid = paidText.toDoubleOrNull() ?: 0.0
                    onConfirm(selectedSupplier, fallbackSupplier, lines, paymentMode, paid, notes)
                },
                modifier = Modifier.testTag("confirm_purchase_button")
            ) { Text("Save & Increase Stock") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
