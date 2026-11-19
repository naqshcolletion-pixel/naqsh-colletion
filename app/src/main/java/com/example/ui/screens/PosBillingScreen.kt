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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.*
import com.example.data.repository.ActiveSessionRole
import com.example.data.repository.CartLine
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.PhonePeQrImage
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.LossCrimson
import com.example.ui.theme.ProfitGreen
import com.example.util.Formatters
import com.example.util.pdf.PrintAndShareHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosBillingScreen(
    settings: BusinessSettingsEntity,
    activeRole: ActiveSessionRole,
    categories: List<CategoryEntity>,
    products: List<ProductEntity>,
    variants: List<ProductVariantEntity>,
    customers: List<CustomerEntity>,
    cartLines: List<CartLine>,
    selectedCustomer: CustomerEntity?,
    onSelectCustomer: (CustomerEntity?) -> Unit,
    onAddToCart: (ProductEntity, ProductVariantEntity?) -> Unit,
    onUpdateQuantity: (Int, Int) -> Unit,
    onUpdateLinePricing: (Int, Double, Double) -> Unit,
    onClearCart: () -> Unit,
    onCheckout: (walkInName: String, walkInMobile: String, billDiscount: Double, gstPercent: Double, paidAmount: Double, paymentMethod: String, notes: String) -> Unit,
    onQuickCreateCustomer: (name: String, mobile: String) -> Unit
) {
    val context = LocalContext.current
    val sym = settings.currencySymbol
    var searchQuery by remember { mutableStateOf("") }
    var selectedCat by remember { mutableStateOf("ALL") }
    var showCheckoutSheet by remember { mutableStateOf(false) }
    var showPhonePeQrModal by remember { mutableStateOf(false) }
    var variantPickProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var editLineIndex by remember { mutableStateOf<Int?>(null) }

    val filteredProducts = remember(products, searchQuery, selectedCat) {
        products.filter { p ->
            val catOk = selectedCat == "ALL" || p.categoryName.equals(selectedCat, true)
            val q = searchQuery.trim()
            val qOk = q.isEmpty() || p.name.contains(q, true) || p.sku.contains(q, true) || p.barcode.contains(q, true)
            catOk && qOk
        }
    }

    val cartRawTotal = cartLines.sumOf { it.unitPrice * it.quantity }
    val cartItemDisc = cartLines.sumOf { it.itemDiscount }
    val cartNet = (cartRawTotal - cartItemDisc).coerceAtLeast(0.0)
    val cartQtyCount = cartLines.sumOf { it.quantity }

    if (!activeRole.canMakeSales) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            EmptyStateCard(
                icon = Icons.Default.Lock,
                title = "Billing Restricted",
                subtitle = "Your staff profile does not have POS Billing permission. Contact ${settings.ownerName}."
            )
        }
        return
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (cartLines.isNotEmpty()) {
                Surface(
                    tonalElevation = 8.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "$cartQtyCount items • ${Formatters.currency(cartNet, sym)}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Text(
                                text = selectedCustomer?.name ?: "Walk-in Customer",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { showPhonePeQrModal = true },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ChampagneGold),
                                border = BorderStroke(1.dp, ChampagneGold)
                            ) {
                                Icon(Icons.Default.QrCode2, contentDescription = "PhonePe QR")
                            }
                            Button(
                                onClick = { showCheckoutSheet = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ChampagneGold,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier.testTag("open_checkout_button")
                            ) {
                                Text("Checkout", fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
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
                    Text("POS & Fast Billing", style = MaterialTheme.typography.headlineMedium)
                    Text("Tap products to add • Instant PDF & WhatsApp billing", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row {
                    IconButton(onClick = { showPhonePeQrModal = true }, modifier = Modifier.testTag("pos_phonepe_qr_btn")) {
                        Icon(Icons.Default.QrCode2, contentDescription = "Show PhonePe QR")
                    }
                    if (cartLines.isNotEmpty()) {
                        IconButton(onClick = onClearCart) {
                            Icon(Icons.Default.RemoveShoppingCart, contentDescription = "Clear Cart")
                        }
                    }
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search product, SKU, barcode...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("pos_search_input")
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(selected = selectedCat == "ALL", onClick = { selectedCat = "ALL" }, label = { Text("ALL") })
                categories.forEach { cat ->
                    FilterChip(
                        selected = selectedCat.equals(cat.name, true),
                        onClick = { selectedCat = cat.name },
                        label = { Text(cat.name) }
                    )
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (cartLines.isNotEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, ChampagneGold),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Current Bill Items (${cartLines.size})", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                                cartLines.forEachIndexed { idx, line ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { editLineIndex = idx },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text(line.product.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            val lbl = "${line.variant?.size ?: line.product.size}/${line.variant?.colour ?: line.product.colour} • ${Formatters.currency(line.unitPrice, sym)}" +
                                                if (line.itemDiscount > 0) " (-${Formatters.currencyCompact(line.itemDiscount, sym)})" else ""
                                            Text(lbl, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(onClick = { onUpdateQuantity(idx, line.quantity - 1) }, modifier = Modifier.size(36.dp)) {
                                                Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Decrease")
                                            }
                                            Text("${line.quantity}", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = 4.dp))
                                            IconButton(onClick = { onUpdateQuantity(idx, line.quantity + 1) }, modifier = Modifier.size(36.dp)) {
                                                Icon(Icons.Default.AddCircleOutline, contentDescription = "Increase")
                                            }
                                        }
                                        Text(
                                            text = Formatters.currencyCompact(line.lineSubtotal, sym),
                                            style = MaterialTheme.typography.titleSmall,
                                            modifier = Modifier.padding(start = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                items(filteredProducts, key = { it.id }) { prod ->
                    val prodVariants = variants.filter { it.productId == prod.id }
                    OutlinedCard(
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (prodVariants.isNotEmpty()) {
                                    variantPickProduct = prod
                                } else {
                                    onAddToCart(prod, null)
                                }
                            }
                            .testTag("pos_product_${prod.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(prod.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "${prod.categoryName} • ${prod.size}/${prod.colour} • Stock: ${prod.quantity}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (prod.quantity <= 0) LossCrimson else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    Formatters.currency(prod.sellingPrice, sym),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                FilledTonalIconButton(
                                    onClick = {
                                        if (prodVariants.isNotEmpty()) variantPickProduct = prod else onAddToCart(prod, null)
                                    }
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add ${prod.name} to bill")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Variant Selector Dialog
    if (variantPickProduct != null) {
        val prod = variantPickProduct!!
        val prodVariants = variants.filter { it.productId == prod.id }
        AlertDialog(
            onDismissRequest = { variantPickProduct = null },
            title = { Text("Select Size & Colour") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    prodVariants.forEach { v ->
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onAddToCart(prod, v)
                                    variantPickProduct = null
                                }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${v.size} • ${v.colour}", style = MaterialTheme.typography.titleSmall)
                                Text("Stock: ${v.quantity}", color = if (v.quantity > 0) ProfitGreen else LossCrimson)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onAddToCart(prod, null)
                    variantPickProduct = null
                }) { Text("Use Default") }
            },
            dismissButton = {
                TextButton(onClick = { variantPickProduct = null }) { Text("Cancel") }
            }
        )
    }

    // Item-wise discount / price edit
    if (editLineIndex != null && editLineIndex!! in cartLines.indices) {
        val idx = editLineIndex!!
        val line = cartLines[idx]
        var priceText by remember { mutableStateOf(line.unitPrice.toString()) }
        var discText by remember { mutableStateOf(if (line.itemDiscount > 0) line.itemDiscount.toString() else "") }
        AlertDialog(
            onDismissRequest = { editLineIndex = null },
            title = { Text(line.product.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("Unit Price ($sym)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = discText,
                        onValueChange = { discText = it },
                        label = { Text("Item Discount ($sym)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onUpdateLinePricing(idx, discText.toDoubleOrNull() ?: 0.0, priceText.toDoubleOrNull() ?: line.unitPrice)
                    editLineIndex = null
                }) { Text("Apply") }
            },
            dismissButton = {
                TextButton(onClick = { editLineIndex = null }) { Text("Cancel") }
            }
        )
    }

    // Checkout Sheet
    if (showCheckoutSheet) {
        CheckoutModal(
            settings = settings,
            customers = customers,
            selectedCustomer = selectedCustomer,
            cartRawTotal = cartRawTotal,
            cartItemDiscount = cartItemDisc,
            onSelectCustomer = onSelectCustomer,
            onQuickCreateCustomer = onQuickCreateCustomer,
            onShowQr = { showPhonePeQrModal = true },
            onDismiss = { showCheckoutSheet = false },
            onCompleteSale = { walkName, walkMob, billDisc, gstPct, paidAmt, method, notes ->
                showCheckoutSheet = false
                onCheckout(walkName, walkMob, billDisc, gstPct, paidAmt, method, notes)
            }
        )
    }

    // PhonePe QR Payment Screen Modal
    if (showPhonePeQrModal) {
        AlertDialog(
            onDismissRequest = { showPhonePeQrModal = false },
            title = { Text("PhonePe / UPI Payment") },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Payable: ${Formatters.currency(cartNet, sym)}",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    PhonePeQrImage(customQrUri = settings.customQrUri, size = 220.dp)
                    if (settings.upiId.isNotBlank()) {
                        Text("UPI ID: ${settings.upiId}", style = MaterialTheme.typography.titleSmall)
                        Button(
                            onClick = {
                                PrintAndShareHelper.launchUpiLink(
                                    context,
                                    settings.upiId,
                                    settings.businessName,
                                    cartNet,
                                    "Boutique Bill Payment"
                                )
                            }
                        ) {
                            Text("Open PhonePe / UPI App")
                        }
                    }
                    Text(
                        text = settings.paymentInstructions,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showPhonePeQrModal = false }) { Text("Done") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CheckoutModal(
    settings: BusinessSettingsEntity,
    customers: List<CustomerEntity>,
    selectedCustomer: CustomerEntity?,
    cartRawTotal: Double,
    cartItemDiscount: Double,
    onSelectCustomer: (CustomerEntity?) -> Unit,
    onQuickCreateCustomer: (String, String) -> Unit,
    onShowQr: () -> Unit,
    onDismiss: () -> Unit,
    onCompleteSale: (String, String, Double, Double, Double, String, String) -> Unit
) {
    val sym = settings.currencySymbol
    var walkInName by remember { mutableStateOf(selectedCustomer?.name ?: "") }
    var walkInMobile by remember { mutableStateOf(selectedCustomer?.mobile ?: "") }
    var billDiscountText by remember { mutableStateOf("") }
    var gstText by remember { mutableStateOf(if (settings.defaultGstPercent > 0) settings.defaultGstPercent.toString() else "0") }
    var paymentMethod by remember { mutableStateOf("Cash") }
    var notes by remember { mutableStateOf("") }
    var custDropdown by remember { mutableStateOf(false) }

    val billDisc = billDiscountText.toDoubleOrNull() ?: 0.0
    val gstPct = gstText.toDoubleOrNull() ?: 0.0
    val taxable = (cartRawTotal - cartItemDiscount - billDisc).coerceAtLeast(0.0)
    val taxAmt = if (gstPct > 0) taxable * (gstPct / 100.0) else 0.0
    val grandTotal = taxable + taxAmt

    var paidText by remember(grandTotal) { mutableStateOf("%.2f".format(grandTotal)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Complete Boutique Bill") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExposedDropdownMenuBox(expanded = custDropdown, onExpandedChange = { custDropdown = !custDropdown }) {
                    OutlinedTextField(
                        value = selectedCustomer?.let { "${it.name} (${it.mobile})" } ?: "Walk-in / New Customer",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select Customer") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = custDropdown) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = custDropdown, onDismissRequest = { custDropdown = false }) {
                        DropdownMenuItem(
                            text = { Text("Walk-in / New Customer") },
                            onClick = { onSelectCustomer(null); custDropdown = false }
                        )
                        customers.forEach { c ->
                            DropdownMenuItem(
                                text = { Text("${c.name} (${c.mobile})") },
                                onClick = {
                                    onSelectCustomer(c)
                                    walkInName = c.name
                                    walkInMobile = c.mobile
                                    custDropdown = false
                                }
                            )
                        }
                    }
                }

                if (selectedCustomer == null) {
                    OutlinedTextField(
                        value = walkInName,
                        onValueChange = { walkInName = it },
                        label = { Text("Customer Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("checkout_customer_name")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = walkInMobile,
                            onValueChange = { walkInMobile = it },
                            label = { Text("Mobile (WhatsApp)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("checkout_customer_mobile")
                        )
                        if (walkInName.isNotBlank() && walkInMobile.isNotBlank()) {
                            TextButton(onClick = { onQuickCreateCustomer(walkInName.trim(), walkInMobile.trim()) }) {
                                Text("Save Cust")
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = billDiscountText,
                        onValueChange = { billDiscountText = it },
                        label = { Text("Bill Disc ($sym)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = gstText,
                        onValueChange = { gstText = it },
                        label = { Text("GST %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("Payment Mode", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Cash", "PhonePe", "UPI", "Bank").forEach { mode ->
                        FilterChip(
                            selected = paymentMethod == mode,
                            onClick = { paymentMethod = mode },
                            label = { Text(mode) }
                        )
                    }
                }

                if (paymentMethod == "PhonePe" || paymentMethod == "UPI") {
                    OutlinedButton(onClick = onShowQr, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.QrCode2, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Show Scannable PhonePe QR")
                    }
                }

                OutlinedTextField(
                    value = paidText,
                    onValueChange = { paidText = it },
                    label = { Text("Amount Paid ($sym) • Partial allowed") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("checkout_paid_amount")
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Bill Note (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Subtotal: ${Formatters.currency(cartRawTotal, sym)}", style = MaterialTheme.typography.bodySmall)
                        if (taxAmt > 0) Text("GST: ${Formatters.currency(taxAmt, sym)}", style = MaterialTheme.typography.bodySmall)
                        Text("Grand Total: ${Formatters.currency(grandTotal, sym)}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        val paidVal = paidText.toDoubleOrNull() ?: grandTotal
                        val bal = (grandTotal - paidVal).coerceAtLeast(0.0)
                        if (bal > 0) {
                            Text("Remaining Due: ${Formatters.currency(bal, sym)}", style = MaterialTheme.typography.labelMedium, color = LossCrimson)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val paidVal = paidText.toDoubleOrNull() ?: grandTotal
                    onCompleteSale(walkInName, walkInMobile, billDisc, gstPct, paidVal, paymentMethod, notes)
                },
                modifier = Modifier.testTag("confirm_sale_button")
            ) {
                Text("Generate Bill")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
