package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.local.*
import com.example.data.repository.ActiveSessionRole
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.theme.LossCrimson
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.WarningAmber
import com.example.util.Formatters

@Composable
fun InventoryScreen(
    settings: BusinessSettingsEntity,
    activeRole: ActiveSessionRole,
    categories: List<CategoryEntity>,
    products: List<ProductEntity>,
    variants: List<ProductVariantEntity>,
    suppliers: List<SupplierEntity>,
    stockMovements: List<StockMovementEntity>,
    openAddProductOnStart: Boolean = false,
    onConsumedOpenAdd: () -> Unit = {},
    onSaveProduct: (ProductEntity, List<ProductVariantEntity>) -> Unit,
    onDeleteProduct: (ProductEntity, Boolean) -> Unit,
    onAddCategory: (String) -> Unit,
    onDeleteCategory: (CategoryEntity) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("ALL") }
    var showMovements by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var showProductEditor by remember { mutableStateOf(false) }
    var productToDelete by remember { mutableStateOf<ProductEntity?>(null) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }

    LaunchedEffect(openAddProductOnStart) {
        if (openAddProductOnStart && activeRole.canManageInventory) {
            editingProduct = null
            showProductEditor = true
            onConsumedOpenAdd()
        }
    }

    val filteredProducts = remember(products, searchQuery, selectedCategory) {
        products.filter { p ->
            val catMatch = selectedCategory == "ALL" || p.categoryName.equals(selectedCategory, ignoreCase = true)
            val q = searchQuery.trim()
            val qMatch = q.isEmpty() ||
                p.name.contains(q, ignoreCase = true) ||
                p.sku.contains(q, ignoreCase = true) ||
                p.barcode.contains(q, ignoreCase = true) ||
                p.colour.contains(q, ignoreCase = true) ||
                p.size.contains(q, ignoreCase = true)
            catMatch && qMatch
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            if (activeRole.canManageInventory && !showMovements) {
                ExtendedFloatingActionButton(
                    onClick = {
                        editingProduct = null
                        showProductEditor = true
                    },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Add Product") },
                    text = { Text("Add Product") },
                    modifier = Modifier.testTag("fab_add_product")
                )
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
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Boutique Inventory", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        text = "${products.size} products • ${products.sumOf { it.quantity }} total units",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = showMovements,
                        onClick = { showMovements = !showMovements },
                        leadingIcon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text(if (showMovements) "Catalog" else "History") }
                    )
                    if (activeRole.canManageInventory) {
                        IconButton(onClick = { showAddCategoryDialog = true }, modifier = Modifier.testTag("add_category_btn")) {
                            Icon(Icons.Default.CreateNewFolder, contentDescription = "Add Category")
                        }
                    }
                }
            }

            if (showMovements) {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (stockMovements.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Default.History,
                                title = "No Stock Movements Yet",
                                subtitle = "Purchases, sales, returns, and stock adjustments are automatically tracked here."
                            )
                        }
                    } else {
                        items(stockMovements, key = { it.id }) { mv ->
                            OutlinedCard(shape = RoundedCornerShape(12.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(mv.productName, style = MaterialTheme.typography.titleSmall)
                                        Text("${mv.reason} • ${mv.variantLabel} • ${mv.referenceNumber}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(Formatters.dateTime(mv.timestamp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text(
                                        text = if (mv.changeQty >= 0) "+${mv.changeQty}" else "${mv.changeQty}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (mv.changeQty >= 0) ProfitGreen else LossCrimson
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search name, SKU, barcode, size, colour...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear Search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("inventory_search_input")
                )

                Spacer(Modifier.height(8.dp))

                // Categories strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedCategory == "ALL",
                        onClick = { selectedCategory = "ALL" },
                        label = { Text("ALL (${products.size})") }
                    )
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory.equals(cat.name, ignoreCase = true),
                            onClick = { selectedCategory = cat.name },
                            label = { Text(cat.name) },
                            trailingIcon = if (activeRole.isOwner && !cat.isDefault) {
                                {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Delete Category ${cat.name}",
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable {
                                                if (selectedCategory == cat.name) selectedCategory = "ALL"
                                                onDeleteCategory(cat)
                                            }
                                    )
                                }
                            } else null
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                if (filteredProducts.isEmpty()) {
                    Box(Modifier.padding(16.dp)) {
                        EmptyStateCard(
                            icon = Icons.Default.Checkroom,
                            title = "No Matching Products",
                            subtitle = "Tap + Add Product to add new Abayas, Hijabs, Pakistani Suits, or Co-ord Sets."
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 92.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredProducts, key = { it.id }) { prod ->
                            val prodVars = variants.filter { it.productId == prod.id }
                            val margin = Formatters.profitMarginPercent(prod.purchasePrice, prod.sellingPrice)
                            val isLow = prod.quantity <= prod.minStock

                            ElevatedCard(
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = activeRole.canManageInventory) {
                                        editingProduct = prod
                                        showProductEditor = true
                                    }
                                    .testTag("product_card_${prod.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Product Photo
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (prod.imageUri.isNotBlank()) {
                                            AsyncImage(
                                                model = Uri.parse(prod.imageUri),
                                                contentDescription = prod.name,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Icon(
                                                Icons.Default.Checkroom,
                                                contentDescription = prod.name,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    Spacer(Modifier.width(12.dp))

                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            text = prod.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${prod.categoryName} • SKU: ${prod.sku.ifBlank { "-" }}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        val variantSummary = if (prodVars.isNotEmpty()) {
                                            "${prodVars.size} variants (${prodVars.joinToString(", ") { "${it.size}/${it.colour}:${it.quantity}" }.take(34)})"
                                        } else {
                                            "Size: ${prod.size} • ${prod.colour}"
                                        }
                                        Text(
                                            text = variantSummary,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "Sell: ${Formatters.currencyCompact(prod.sellingPrice, settings.currencySymbol)}",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            if (activeRole.isOwner) {
                                                Text(
                                                    text = "Cost: ${Formatters.currencyCompact(prod.purchasePrice, settings.currencySymbol)} (${"%.0f".format(margin)}%)",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = (if (isLow) WarningAmber else ProfitGreen).copy(alpha = 0.14f)
                                        ) {
                                            Text(
                                                text = "Qty ${prod.quantity}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (isLow) WarningAmber else ProfitGreen,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                        if (activeRole.canManageInventory) {
                                            IconButton(onClick = { productToDelete = prod }) {
                                                Icon(
                                                    Icons.Default.DeleteOutline,
                                                    contentDescription = "Delete ${prod.name}",
                                                    tint = MaterialTheme.colorScheme.error
                                                )
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

    if (showProductEditor) {
        ProductEditorDialog(
            initial = editingProduct,
            initialVariants = editingProduct?.let { p -> variants.filter { it.productId == p.id } } ?: emptyList(),
            categories = categories.map { it.name },
            suppliers = suppliers,
            currencySymbol = settings.currencySymbol,
            onDismiss = { showProductEditor = false },
            onSave = { prod, vars ->
                onSaveProduct(prod, vars)
                showProductEditor = false
            }
        )
    }

    if (productToDelete != null) {
        val target = productToDelete!!
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title = { Text("Remove ${target.name}?") },
            text = { Text("Move to Trash (recoverable in Settings) or permanently remove this sample/product item?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteProduct(target, false)
                        productToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Move to Trash")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(
                        onClick = {
                            onDeleteProduct(target, true)
                            productToDelete = null
                        }
                    ) {
                        Text("Remove Permanently", color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = { productToDelete = null }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    if (showAddCategoryDialog) {
        var newCat by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text("New Boutique Category") },
            text = {
                OutlinedTextField(
                    value = newCat,
                    onValueChange = { newCat = it },
                    label = { Text("Category Name (e.g. BRIDAL ABAYA)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newCat.isNotBlank()) {
                        onAddCategory(newCat)
                        showAddCategoryDialog = false
                    }
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductEditorDialog(
    initial: ProductEntity?,
    initialVariants: List<ProductVariantEntity>,
    categories: List<String>,
    suppliers: List<SupplierEntity>,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (ProductEntity, List<ProductVariantEntity>) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var category by remember { mutableStateOf(initial?.categoryName ?: categories.firstOrNull() ?: "ABAYA") }
    var imageUri by remember { mutableStateOf(initial?.imageUri ?: "") }
    var sku by remember { mutableStateOf(initial?.sku ?: "NQ-${(1000..9999).random()}") }
    var barcode by remember { mutableStateOf(initial?.barcode ?: "") }
    var purchasePrice by remember { mutableStateOf(if ((initial?.purchasePrice ?: 0.0) > 0) initial!!.purchasePrice.toString() else "") }
    var sellingPrice by remember { mutableStateOf(if ((initial?.sellingPrice ?: 0.0) > 0) initial!!.sellingPrice.toString() else "") }
    var size by remember { mutableStateOf(initial?.size ?: "54 / M") }
    var colour by remember { mutableStateOf(initial?.colour ?: "Emerald / Black") }
    var quantity by remember { mutableStateOf((initial?.quantity ?: 0).toString()) }
    var minStock by remember { mutableStateOf((initial?.minStock ?: 3).toString()) }
    var supplierName by remember { mutableStateOf(initial?.supplierName ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var variantRows by remember { mutableStateOf(initialVariants) }
    var catExpanded by remember { mutableStateOf(false) }

    // Add variant draft inputs
    var vSize by remember { mutableStateOf("") }
    var vColour by remember { mutableStateOf("") }
    var vQty by remember { mutableStateOf("1") }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {
            }
            imageUri = uri.toString()
        }
    }

    val pPriceNum = purchasePrice.toDoubleOrNull() ?: 0.0
    val sPriceNum = sellingPrice.toDoubleOrNull() ?: 0.0
    val marginPct = Formatters.profitMarginPercent(pPriceNum, sPriceNum)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text(if (initial == null) "Add Boutique Product" else "Edit Product") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = {
                                if (name.isBlank()) return@TextButton
                                val finalQty = if (variantRows.isNotEmpty()) variantRows.sumOf { it.quantity } else (quantity.toIntOrNull() ?: 0)
                                val matchedSupplier = suppliers.firstOrNull { it.name.equals(supplierName.trim(), ignoreCase = true) }
                                onSave(
                                    ProductEntity(
                                        id = initial?.id ?: 0L,
                                        name = name.trim(),
                                        categoryName = category,
                                        imageUri = imageUri,
                                        sku = sku.trim(),
                                        barcode = barcode.trim(),
                                        purchasePrice = pPriceNum,
                                        sellingPrice = sPriceNum,
                                        size = size.trim().ifBlank { "Standard" },
                                        colour = colour.trim().ifBlank { "Classic" },
                                        quantity = finalQty,
                                        supplierId = matchedSupplier?.id ?: initial?.supplierId,
                                        supplierName = supplierName.trim(),
                                        minStock = minStock.toIntOrNull() ?: 3,
                                        description = description.trim(),
                                        createdAt = initial?.createdAt ?: System.currentTimeMillis()
                                    ),
                                    variantRows
                                )
                            },
                            modifier = Modifier.testTag("save_product_button")
                        ) {
                            Text("SAVE", fontWeight = FontWeight.Bold)
                        }
                    }
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Product Photo Picker
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (imageUri.isNotBlank()) {
                                AsyncImage(
                                    model = Uri.parse(imageUri),
                                    contentDescription = "Product Photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(Icons.Default.AddAPhoto, contentDescription = "Pick Photo", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Column {
                            OutlinedButton(
                                onClick = {
                                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                }
                            ) {
                                Text(if (imageUri.isBlank()) "Choose Product Photo" else "Change Photo")
                            }
                            Text("Zero-permission Android Photo Picker", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Product Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("product_name_input")
                    )

                    ExposedDropdownMenuBox(expanded = catExpanded, onExpandedChange = { catExpanded = !catExpanded }) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = { category = cat; catExpanded = false }
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = purchasePrice,
                            onValueChange = { purchasePrice = it },
                            label = { Text("Purchase ($currencySymbol)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("purchase_price_input")
                        )
                        OutlinedTextField(
                            value = sellingPrice,
                            onValueChange = { sellingPrice = it },
                            label = { Text("Selling ($currencySymbol)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("selling_price_input")
                        )
                    }

                    Text(
                        text = "Profit Margin: ${"%.1f".format(marginPct)}% (${Formatters.currency(sPriceNum - pPriceNum, currencySymbol)} per unit)",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (sPriceNum >= pPriceNum) ProfitGreen else LossCrimson
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = sku,
                            onValueChange = { sku = it },
                            label = { Text("SKU") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = barcode,
                            onValueChange = { barcode = it },
                            label = { Text("Barcode") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = size,
                            onValueChange = { size = it },
                            label = { Text("Default Size") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = colour,
                            onValueChange = { colour = it },
                            label = { Text("Default Colour") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = if (variantRows.isNotEmpty()) variantRows.sumOf { it.quantity }.toString() else quantity,
                            onValueChange = { quantity = it },
                            enabled = variantRows.isEmpty(),
                            label = { Text("Stock Qty") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("product_qty_input")
                        )
                        OutlinedTextField(
                            value = minStock,
                            onValueChange = { minStock = it },
                            label = { Text("Min Stock Alert") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = supplierName,
                        onValueChange = { supplierName = it },
                        label = { Text("Supplier Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Fabric / Design Description") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider(Modifier.padding(vertical = 6.dp))
                    Text("Size & Colour Stock Variants (Optional)", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Add multiple sizes & colours with separately tracked stock.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    variantRows.forEachIndexed { idx, v ->
                        OutlinedCard(shape = RoundedCornerShape(10.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${v.size} • ${v.colour} — Qty: ${v.quantity}", style = MaterialTheme.typography.bodyMedium)
                                IconButton(onClick = {
                                    variantRows = variantRows.toMutableList().apply { removeAt(idx) }
                                }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Remove Variant")
                                }
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = vSize,
                            onValueChange = { vSize = it },
                            label = { Text("Size") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = vColour,
                            onValueChange = { vColour = it },
                            label = { Text("Colour") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = vQty,
                            onValueChange = { vQty = it },
                            label = { Text("Qty") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.width(68.dp)
                        )
                        FilledTonalIconButton(
                            onClick = {
                                if (vSize.isNotBlank() || vColour.isNotBlank()) {
                                    variantRows = variantRows + ProductVariantEntity(
                                        productId = initial?.id ?: 0L,
                                        size = vSize.trim().ifBlank { "Std" },
                                        colour = vColour.trim().ifBlank { "Classic" },
                                        quantity = vQty.toIntOrNull() ?: 0
                                    )
                                    vSize = ""
                                    vColour = ""
                                    vQty = "1"
                                }
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Variant")
                        }
                    }
                }
            }
        }
    }
}
