package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.local.*
import com.example.data.repository.ActiveSessionRole
import com.example.data.repository.AuthManager
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.NaqshLogoImage
import com.example.ui.components.PhonePeQrImage
import com.example.ui.theme.LossCrimson
import com.example.ui.theme.ProfitGreen
import com.example.util.AppLanguage
import com.example.util.Formatters
import com.example.util.pdf.InvoiceFormat
import com.example.util.pdf.PrintAndShareHelper
import kotlinx.coroutines.launch

@Composable
fun MoreHubScreen(
    settings: BusinessSettingsEntity,
    activeRole: ActiveSessionRole,
    onNavigateCustomers: () -> Unit,
    onNavigateSuppliers: () -> Unit,
    onNavigatePurchases: () -> Unit,
    onNavigateReports: () -> Unit,
    onNavigateStaff: () -> Unit,
    onNavigateSettings: () -> Unit,
    onNavigateBackup: () -> Unit,
    onSwitchRoleClick: () -> Unit,
    onSignOutClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .widthIn(max = 820.dp)
            .padding(16.dp)
    ) {
        Text("Boutique Modules & Controls", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = if (activeRole.isOwner) "Logged in as Owner: ${settings.ownerName}" else "Staff Mode: ${activeRole.actorName}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(14.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                MoreMenuTile(Icons.Default.People, "Customers", "Customer records, ledgers, dues & invoices", activeRole.canManageCustomers, onNavigateCustomers, "more_customers")
            }
            item {
                MoreMenuTile(Icons.Default.Store, "Suppliers", "Supplier directory, payments & balances", activeRole.isOwner || activeRole.canManagePurchases, onNavigateSuppliers, "more_suppliers")
            }
            item {
                MoreMenuTile(Icons.Default.LocalShipping, "Purchases", "Supplier stock purchases & auto inventory restock", activeRole.canManagePurchases, onNavigatePurchases, "more_purchases")
            }
            item {
                MoreMenuTile(Icons.Default.Assessment, "Reports", "Daily/Monthly/Yearly sales, P&L, stock, PDF & Excel exports", activeRole.canViewBasicReports, onNavigateReports, "more_reports")
            }
            item {
                MoreMenuTile(Icons.Default.Badge, "Staff Management", "Create staff accounts, assign permissions & PINs", activeRole.isOwner, onNavigateStaff, "more_staff")
            }
            item {
                MoreMenuTile(Icons.Default.Settings, "Settings", "Business info, Logo, PhonePe QR, GST, OTP Mobile, Language", activeRole.isOwner, onNavigateSettings, "more_settings")
            }
            item {
                MoreMenuTile(Icons.Default.Backup, "Backup & Restore", "Manual JSON backup, restore, export & Firebase Cloud sync", activeRole.isOwner, onNavigateBackup, "more_backup")
            }
            item {
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
            }
            item {
                OutlinedButton(
                    onClick = onSwitchRoleClick,
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("switch_role_btn")
                ) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (activeRole.isOwner) "Switch to Staff Shift Mode" else "Switch Back to Owner")
                }
            }
            item {
                OutlinedButton(
                    onClick = onSignOutClick,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LossCrimson),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("sign_out_btn")
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Sign Out")
                }
            }
        }
    }
}

@Composable
private fun MoreMenuTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    ElevatedCard(
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                Text(if (enabled) subtitle else "Owner permission required", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(if (enabled) Icons.Default.ChevronRight else Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffManagementScreen(
    staffList: List<StaffEntity>,
    onBack: () -> Unit,
    onSaveStaff: (StaffEntity) -> Unit
) {
    var editingStaff by remember { mutableStateOf<StaffEntity?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Staff & Permissions") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editingStaff = null; showEditor = true },
                icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                text = { Text("Add Staff") },
                modifier = Modifier.testTag("fab_add_staff")
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).widthIn(max = 820.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (staffList.isEmpty()) {
                item { EmptyStateCard(Icons.Default.Badge, "No Staff Accounts", "Add sales or inventory staff with PIN login and permission controls.") }
            } else {
                items(staffList, key = { it.id }) { st ->
                    ElevatedCard(shape = RoundedCornerShape(14.dp)) {
                        Column(Modifier.padding(14.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("${st.name} (${st.roleTitle})", style = MaterialTheme.typography.titleMedium)
                                    Text(st.mobile, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Switch(checked = st.isActive, onCheckedChange = { onSaveStaff(st.copy(isActive = it)) })
                                    IconButton(onClick = { editingStaff = st; showEditor = true }) { Icon(Icons.Default.Edit, contentDescription = "Edit Staff") }
                                }
                            }
                            val perms = buildList {
                                if (st.canMakeSales) add("POS")
                                if (st.canManageInventory) add("Inventory")
                                if (st.canManagePurchases) add("Purchases")
                                if (st.canManageCustomers) add("Customers")
                                if (st.canViewBasicReports) add("Basic Reports")
                                if (st.canProcessReturns) add("Returns")
                            }
                            Text("Permissions: ${perms.joinToString(", ").ifBlank { "None" }}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }

    if (showEditor) {
        var name by remember { mutableStateOf(editingStaff?.name ?: "") }
        var mobile by remember { mutableStateOf(editingStaff?.mobile ?: "") }
        var roleTitle by remember { mutableStateOf(editingStaff?.roleTitle ?: "Sales Executive") }
        var pin by remember { mutableStateOf("") }
        var canSales by remember { mutableStateOf(editingStaff?.canMakeSales ?: true) }
        var canInv by remember { mutableStateOf(editingStaff?.canManageInventory ?: true) }
        var canPur by remember { mutableStateOf(editingStaff?.canManagePurchases ?: false) }
        var canCust by remember { mutableStateOf(editingStaff?.canManageCustomers ?: true) }
        var canRep by remember { mutableStateOf(editingStaff?.canViewBasicReports ?: false) }
        var canRet by remember { mutableStateOf(editingStaff?.canProcessReturns ?: false) }

        AlertDialog(
            onDismissRequest = { showEditor = false },
            title = { Text(if (editingStaff == null) "Create Staff Account" else "Edit Staff Account") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Staff Name *") }, singleLine = true)
                    OutlinedTextField(value = mobile, onValueChange = { mobile = it }, label = { Text("Mobile *") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true)
                    OutlinedTextField(value = roleTitle, onValueChange = { roleTitle = it }, label = { Text("Role Title") }, singleLine = true)
                    OutlinedTextField(value = pin, onValueChange = { pin = it }, label = { Text(if (editingStaff == null) "4-6 Digit Shift PIN *" else "New PIN (leave blank to keep)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), visualTransformation = PasswordVisualTransformation(), singleLine = true)
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    Text("Module Permissions", style = MaterialTheme.typography.labelMedium)
                    PermToggle("POS & Sales Billing", canSales) { canSales = it }
                    PermToggle("Manage Inventory", canInv) { canInv = it }
                    PermToggle("Manage Purchases", canPur) { canPur = it }
                    PermToggle("Manage Customers", canCust) { canCust = it }
                    PermToggle("View Basic Sales Reports", canRep) { canRep = it }
                    PermToggle("Process Returns / Exchanges", canRet) { canRet = it }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val hash = if (pin.isNotBlank()) Formatters.hashPin(pin.trim()) else editingStaff?.pinHash ?: ""
                    if (name.isNotBlank() && mobile.isNotBlank() && hash.isNotBlank()) {
                        onSaveStaff(
                            (editingStaff ?: StaffEntity(name = "", mobile = "", pinHash = hash)).copy(
                                name = name.trim(),
                                mobile = mobile.trim(),
                                roleTitle = roleTitle.trim(),
                                pinHash = hash,
                                canMakeSales = canSales,
                                canManageInventory = canInv,
                                canManagePurchases = canPur,
                                canManageCustomers = canCust,
                                canViewBasicReports = canRep,
                                canProcessReturns = canRet
                            )
                        )
                        showEditor = false
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showEditor = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun PermToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: BusinessSettingsEntity,
    authManager: AuthManager,
    deletedProducts: List<ProductEntity>,
    deletedCustomers: List<CustomerEntity>,
    deletedSuppliers: List<SupplierEntity>,
    auditLogs: List<AuditLogEntity>,
    onBack: () -> Unit,
    onSaveSettings: (BusinessSettingsEntity) -> Unit,
    onRestoreProduct: (ProductEntity) -> Unit,
    onRestoreCustomer: (CustomerEntity) -> Unit,
    onRestoreSupplier: (SupplierEntity) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var bizName by remember(settings) { mutableStateOf(settings.businessName) }
    var ownerName by remember(settings) { mutableStateOf(settings.ownerName) }
    var category by remember(settings) { mutableStateOf(settings.businessCategory) }
    var tagline by remember(settings) { mutableStateOf(settings.tagline) }
    var mobile by remember(settings) { mutableStateOf(settings.ownerMobile) }
    var address by remember(settings) { mutableStateOf(settings.address) }
    var gstin by remember(settings) { mutableStateOf(settings.gstin) }
    var gstPct by remember(settings) { mutableStateOf(settings.defaultGstPercent.toString()) }
    var upiId by remember(settings) { mutableStateOf(settings.upiId) }
    var payInstr by remember(settings) { mutableStateOf(settings.paymentInstructions) }
    var footerNote by remember(settings) { mutableStateOf(settings.invoiceFooterNote) }
    var currency by remember(settings) { mutableStateOf(settings.currencySymbol) }
    var language by remember(settings) { mutableStateOf(settings.language) }
    var paperSize by remember(settings) { mutableStateOf(settings.printerPaperSize) }
    var printerName by remember(settings) { mutableStateOf(settings.printerName) }
    var darkTheme by remember(settings) { mutableStateOf(settings.isDarkTheme) }
    var lowStockNotif by remember(settings) { mutableStateOf(settings.lowStockNotifications) }
    var logoUri by remember(settings) { mutableStateOf(settings.customLogoUri) }
    var qrUri by remember(settings) { mutableStateOf(settings.customQrUri) }

    // Mobile Change via Firebase Phone OTP
    var showPhoneChangeDialog by remember { mutableStateOf(false) }
    var showAuditLogs by remember { mutableStateOf(false) }
    var showTrashDialog by remember { mutableStateOf(false) }

    val logoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
            logoUri = uri.toString()
        }
    }
    val qrPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
            qrUri = uri.toString()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Boutique Settings") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
                actions = {
                    TextButton(
                        onClick = {
                            onSaveSettings(
                                settings.copy(
                                    businessName = bizName.trim().ifBlank { "NAQSH COLLECTION" },
                                    ownerName = ownerName.trim().ifBlank { "NASIR ALI" },
                                    businessCategory = category.trim(),
                                    tagline = tagline.trim(),
                                    ownerMobile = mobile.trim(),
                                    address = address.trim(),
                                    gstin = gstin.trim(),
                                    defaultGstPercent = gstPct.toDoubleOrNull() ?: 0.0,
                                    upiId = upiId.trim(),
                                    paymentInstructions = payInstr.trim(),
                                    invoiceFooterNote = footerNote.trim(),
                                    currencySymbol = currency.trim().ifBlank { "₹" },
                                    language = language,
                                    printerPaperSize = paperSize,
                                    printerName = printerName.trim(),
                                    isDarkTheme = darkTheme,
                                    lowStockNotifications = lowStockNotif,
                                    customLogoUri = logoUri,
                                    customQrUri = qrUri
                                )
                            )
                        },
                        modifier = Modifier.testTag("save_settings_button")
                    ) { Text("SAVE") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .widthIn(max = 820.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Owner & Business Details", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            OutlinedTextField(value = ownerName, onValueChange = { ownerName = it }, label = { Text("Owner Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = bizName, onValueChange = { bizName = it }, label = { Text("Business Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Business Category") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = tagline, onValueChange = { tagline = it }, label = { Text("Official Tagline") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = mobile, onValueChange = { mobile = it }, label = { Text("Owner Mobile") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedButton(onClick = { showPhoneChangeDialog = true }) { Text("Verify OTP") }
            }
            OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Official Address") }, minLines = 3, modifier = Modifier.fillMaxWidth())

            HorizontalDivider()
            Text("Official Logo & PhonePe QR Code", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                NaqshLogoImage(customLogoUri = logoUri, size = 72.dp)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = { logoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                        Text("Replace Logo")
                    }
                    if (logoUri.isNotBlank()) {
                        TextButton(onClick = { logoUri = "" }) { Text("Reset Official Logo") }
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                PhonePeQrImage(customQrUri = qrUri, size = 110.dp)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = { qrPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                        Text("Replace PhonePe QR")
                    }
                    if (qrUri.isNotBlank()) {
                        TextButton(onClick = { qrUri = "" }) { Text("Reset Original QR") }
                    }
                }
            }

            OutlinedTextField(value = upiId, onValueChange = { upiId = it }, label = { Text("Merchant UPI ID (Optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = payInstr, onValueChange = { payInstr = it }, label = { Text("Payment Instructions") }, minLines = 2, modifier = Modifier.fillMaxWidth())

            HorizontalDivider()
            Text("GST, Invoice & Printer Settings", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = gstin, onValueChange = { gstin = it }, label = { Text("GSTIN (Shown only if entered)") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = gstPct, onValueChange = { gstPct = it }, label = { Text("Default GST %") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.width(120.dp))
            }
            OutlinedTextField(value = footerNote, onValueChange = { footerNote = it }, label = { Text("Invoice Thank-You Note") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InvoiceFormat.entries.forEach { fmt ->
                    FilterChip(selected = paperSize == fmt.code, onClick = { paperSize = fmt.code }, label = { Text(fmt.code) })
                }
            }
            OutlinedTextField(value = printerName, onValueChange = { printerName = it }, label = { Text("Preferred Printer Label") }, singleLine = true, modifier = Modifier.fillMaxWidth())

            HorizontalDivider()
            Text("Language, Currency & Theme", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppLanguage.entries.forEach { l ->
                    FilterChip(selected = language == l.code, onClick = { language = l.code }, label = { Text(l.displayName) })
                }
            }
            OutlinedTextField(value = currency, onValueChange = { currency = it }, label = { Text("Currency Symbol") }, singleLine = true, modifier = Modifier.width(140.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Dark Luxury Boutique Theme")
                Switch(checked = darkTheme, onCheckedChange = { darkTheme = it })
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Low Stock Alerts")
                Switch(checked = lowStockNotif, onCheckedChange = { lowStockNotif = it })
            }

            HorizontalDivider()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showTrashDialog = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.RestoreFromTrash, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Trash (${deletedProducts.size + deletedCustomers.size + deletedSuppliers.size})")
                }
                OutlinedButton(onClick = { showAuditLogs = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Security, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Audit Log (${auditLogs.size})")
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showPhoneChangeDialog) {
        var newPhone by remember { mutableStateOf(mobile) }
        var otp by remember { mutableStateOf("") }
        var verId by remember { mutableStateOf<String?>(null) }
        var status by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showPhoneChangeDialog = false },
            title = { Text("Secure Mobile Number Change") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = newPhone, onValueChange = { newPhone = it }, label = { Text("New Mobile (+91...)") }, singleLine = true)
                    if (verId != null) {
                        OutlinedTextField(value = otp, onValueChange = { otp = it }, label = { Text("6-Digit OTP") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                    }
                    if (status != null) Text(status!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val activity = context as? Activity ?: return@Button
                    if (verId == null) {
                        authManager.sendPhoneOtp(
                            activity = activity,
                            phoneNumberE164 = newPhone.replace(" ", ""),
                            onCodeSent = { vid, _ -> verId = vid; status = "OTP sent to $newPhone" },
                            onAutoVerified = { _ -> status = "Verified! Save settings to update." },
                            onError = { err -> status = err }
                        )
                    } else {
                        scope.launch {
                            val res = authManager.updateOwnerPhoneWithCredential(verId!!, otp)
                            if (res.isSuccess) {
                                mobile = newPhone
                                showPhoneChangeDialog = false
                                Toast.makeText(context, "Verified & updated owner mobile!", Toast.LENGTH_SHORT).show()
                            } else {
                                status = res.exceptionOrNull()?.localizedMessage ?: "OTP verification failed."
                            }
                        }
                    }
                }) { Text(if (verId == null) "Send OTP" else "Verify & Update") }
            },
            dismissButton = { TextButton(onClick = { showPhoneChangeDialog = false }) { Text("Close") } }
        )
    }

    if (showTrashDialog) {
        AlertDialog(
            onDismissRequest = { showTrashDialog = false },
            title = { Text("Recover Deleted Non-Financial Records") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (deletedProducts.isEmpty() && deletedCustomers.isEmpty() && deletedSuppliers.isEmpty()) {
                        Text("Trash is empty.")
                    }
                    deletedProducts.forEach { p ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Product: ${p.name}", modifier = Modifier.weight(1f))
                            TextButton(onClick = { onRestoreProduct(p) }) { Text("Restore") }
                        }
                    }
                    deletedCustomers.forEach { c ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Customer: ${c.name}", modifier = Modifier.weight(1f))
                            TextButton(onClick = { onRestoreCustomer(c) }) { Text("Restore") }
                        }
                    }
                    deletedSuppliers.forEach { s ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Supplier: ${s.name}", modifier = Modifier.weight(1f))
                            TextButton(onClick = { onRestoreSupplier(s) }) { Text("Restore") }
                        }
                    }
                }
            },
            confirmButton = { Button(onClick = { showTrashDialog = false }) { Text("Done") } }
        )
    }

    if (showAuditLogs) {
        AlertDialog(
            onDismissRequest = { showAuditLogs = false },
            title = { Text("Business Audit History") },
            text = {
                Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    auditLogs.forEach { log ->
                        Text("${Formatters.dateTime(log.timestamp)} • ${log.actorName} (${log.actorRole})\n${log.action}: ${log.details}", style = MaterialTheme.typography.bodySmall)
                        HorizontalDivider()
                    }
                }
            },
            confirmButton = { Button(onClick = { showAuditLogs = false }) { Text("Close") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(
    onBack: () -> Unit,
    onExportJsonToUri: (android.net.Uri) -> Unit,
    onRestoreJsonFromUri: (android.net.Uri) -> Unit,
    onShareLocalBackup: () -> Unit,
    onSyncToCloud: () -> Unit
) {
    val createDocLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) onExportJsonToUri(uri)
    }
    val openDocLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onRestoreJsonFromUri(uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backup & Restore") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .widthIn(max = 720.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Offline Device Backup (JSON)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text("Save a complete copy of all products, customers, suppliers, invoices, expenses, and settings to your phone or Google Drive.", style = MaterialTheme.typography.bodySmall)
                    Button(
                        onClick = { createDocLauncher.launch("Naqsh_Backup_${Formatters.fileTimestamp()}.json") },
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Default.SaveAlt, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Save Backup File to Phone")
                    }
                    OutlinedButton(
                        onClick = onShareLocalBackup,
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Share Backup via WhatsApp / Drive")
                    }
                    OutlinedButton(
                        onClick = { openDocLauncher.launch(arrayOf("application/json", "*/*")) },
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Restore Backup From File")
                    }
                }
            }

            ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Firebase Cloud Snapshot Sync", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text("Securely sync your owner profile and boutique ledger summary to your authenticated Firebase Firestore database when internet is available.", style = MaterialTheme.typography.bodySmall)
                    Button(
                        onClick = onSyncToCloud,
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("cloud_sync_button")
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Sync Snapshot to Cloud")
                    }
                }
            }
        }
    }
}
