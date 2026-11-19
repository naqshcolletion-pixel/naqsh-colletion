package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.*
import com.example.data.repository.AuthManager
import com.example.data.repository.authStateFlow
import com.example.ui.screens.*
import com.example.ui.theme.NaqshCollectionTheme
import com.example.ui.viewmodel.BoutiqueViewModel
import com.example.util.AppStrings
import com.example.util.pdf.PrintAndShareHelper
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

enum class MainTab(
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("home", Icons.Filled.Home, Icons.Outlined.Home),
    INVENTORY("inventory", Icons.Filled.Inventory2, Icons.Outlined.Inventory2),
    POS("pos", Icons.Filled.PointOfSale, Icons.Outlined.PointOfSale),
    ACCOUNTS("accounts", Icons.Filled.AccountBalance, Icons.Outlined.AccountBalance),
    MORE("more", Icons.Filled.GridView, Icons.Outlined.GridView)
}

enum class MoreSubScreen {
    HUB,
    CUSTOMERS,
    SUPPLIERS,
    PURCHASES,
    REPORTS,
    STAFF,
    SETTINGS,
    BACKUP
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NaqshBoutiqueRoot()
        }
    }
}

@Composable
fun NaqshBoutiqueRoot() {
    val auth = remember { Firebase.auth }
    val authManager = remember { AuthManager(auth) }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val context = LocalContext.current
    val db = remember(context) { AppDatabase.getInstance(context) }
    val settingsPreview by db.boutiqueDao().observeSettings().collectAsStateWithLifecycle(initialValue = BusinessSettingsEntity())
    val safeSettings = settingsPreview ?: BusinessSettingsEntity()

    LaunchedEffect(Unit) {
        seedInitialBoutiqueCatalog(db.boutiqueDao())
    }

    NaqshCollectionTheme(darkTheme = safeSettings.isDarkTheme) {
        val user = currentUser
        if (user == null) {
            AuthScreen(
                settings = safeSettings,
                authManager = authManager,
                onAuthSuccess = {}
            )
        } else {
            key(user.uid) {
                BoutiqueMainApp(currentUserId = user.uid, authManager = authManager)
            }
        }
    }
}

@Composable
fun BoutiqueMainApp(
    currentUserId: String,
    authManager: AuthManager
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val vm: BoutiqueViewModel = viewModel(key = currentUserId)

    val settings by vm.settings.collectAsStateWithLifecycle()
    val activeRole by vm.activeRole.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val products by vm.products.collectAsStateWithLifecycle()
    val deletedProducts by vm.deletedProducts.collectAsStateWithLifecycle()
    val variants by vm.variants.collectAsStateWithLifecycle()
    val customers by vm.customers.collectAsStateWithLifecycle()
    val deletedCustomers by vm.deletedCustomers.collectAsStateWithLifecycle()
    val customerPayments by vm.customerPayments.collectAsStateWithLifecycle()
    val suppliers by vm.suppliers.collectAsStateWithLifecycle()
    val deletedSuppliers by vm.deletedSuppliers.collectAsStateWithLifecycle()
    val supplierPayments by vm.supplierPayments.collectAsStateWithLifecycle()
    val purchases by vm.purchases.collectAsStateWithLifecycle()
    val sales by vm.sales.collectAsStateWithLifecycle()
    val saleItems by vm.saleItems.collectAsStateWithLifecycle()
    val returns by vm.returns.collectAsStateWithLifecycle()
    val expenses by vm.expenses.collectAsStateWithLifecycle()
    val otherIncomes by vm.otherIncomes.collectAsStateWithLifecycle()
    val stockMovements by vm.stockMovements.collectAsStateWithLifecycle()
    val staffList by vm.staffList.collectAsStateWithLifecycle()
    val auditLogs by vm.auditLogs.collectAsStateWithLifecycle()
    val summary by vm.accountingSummary.collectAsStateWithLifecycle()
    val cartLines by vm.cartLines.collectAsStateWithLifecycle()
    val posCustomer by vm.posCustomer.collectAsStateWithLifecycle()
    val lastInvoicePreview by vm.lastInvoicePreview.collectAsStateWithLifecycle()
    val toastMsg by vm.toastMessage.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf(MainTab.HOME) }
    var moreSubScreen by remember { mutableStateOf(MoreSubScreen.HUB) }

    var openAddProductTrigger by remember { mutableStateOf(false) }
    var openAddPurchaseTrigger by remember { mutableStateOf(false) }
    var openAddExpenseTrigger by remember { mutableStateOf(false) }
    var openAddCustomerTrigger by remember { mutableStateOf(false) }
    var showRoleSwitchDialog by remember { mutableStateOf(false) }

    LaunchedEffect(toastMsg) {
        val m = toastMsg
        if (m != null) {
            Toast.makeText(context, m, Toast.LENGTH_SHORT).show()
            vm.clearToast()
        }
    }

    if (currentTab == MainTab.MORE && moreSubScreen != MoreSubScreen.HUB) {
        BackHandler { moreSubScreen = MoreSubScreen.HUB }
    } else if (currentTab != MainTab.HOME) {
        BackHandler { currentTab = MainTab.HOME }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 700.dp

        val tabLabel: (MainTab) -> String = { t ->
            when (t) {
                MainTab.HOME -> AppStrings.navHome(settings.language)
                MainTab.INVENTORY -> AppStrings.navInventory(settings.language)
                MainTab.POS -> AppStrings.navPos(settings.language)
                MainTab.ACCOUNTS -> AppStrings.navAccounts(settings.language)
                MainTab.MORE -> AppStrings.navMore(settings.language)
            }
        }

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (!useNavRail && !(currentTab == MainTab.MORE && moreSubScreen != MoreSubScreen.HUB)) {
                    NavigationBar(
                        windowInsets = WindowInsets.navigationBars,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        MainTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    currentTab = tab
                                    if (tab == MainTab.MORE) moreSubScreen = MoreSubScreen.HUB
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tabLabel(tab)
                                    )
                                },
                                label = {
                                    Text(
                                        text = tabLabel(tab),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                modifier = Modifier.testTag("nav_${tab.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.statusBarsPadding().navigationBarsPadding()
                    ) {
                        MainTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    currentTab = tab
                                    if (tab == MainTab.MORE) moreSubScreen = MoreSubScreen.HUB
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tabLabel(tab)
                                    )
                                },
                                label = { Text(tabLabel(tab)) }
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    when (currentTab) {
                        MainTab.HOME -> HomeDashboardScreen(
                            settings = settings,
                            activeRole = activeRole,
                            summary = summary,
                            products = products,
                            recentSales = sales,
                            onQuickNewSale = { currentTab = MainTab.POS },
                            onQuickAddProduct = {
                                openAddProductTrigger = true
                                currentTab = MainTab.INVENTORY
                            },
                            onQuickAddPurchase = {
                                openAddPurchaseTrigger = true
                                currentTab = MainTab.MORE
                                moreSubScreen = MoreSubScreen.PURCHASES
                            },
                            onQuickAddExpense = {
                                openAddExpenseTrigger = true
                                currentTab = MainTab.ACCOUNTS
                            },
                            onQuickAddCustomer = {
                                openAddCustomerTrigger = true
                                currentTab = MainTab.MORE
                                moreSubScreen = MoreSubScreen.CUSTOMERS
                            },
                            onQuickReports = {
                                currentTab = MainTab.MORE
                                moreSubScreen = MoreSubScreen.REPORTS
                            },
                            onInvoiceClick = { inv -> vm.setInvoicePreview(inv) },
                            onSwitchRoleClick = { showRoleSwitchDialog = true }
                        )

                        MainTab.INVENTORY -> InventoryScreen(
                            settings = settings,
                            activeRole = activeRole,
                            categories = categories,
                            products = products,
                            variants = variants,
                            suppliers = suppliers,
                            stockMovements = stockMovements,
                            openAddProductOnStart = openAddProductTrigger,
                            onConsumedOpenAdd = { openAddProductTrigger = false },
                            onSaveProduct = { prod, vars ->
                                scope.launch {
                                    vm.repo.saveProduct(prod, vars, activeRole.actorName, activeRole.actorRole)
                                    vm.showToast("Product saved!")
                                }
                            },
                            onDeleteProduct = { prod, permanent ->
                                scope.launch {
                                    vm.repo.deleteProduct(prod, permanent, activeRole.actorName, activeRole.actorRole)
                                    vm.showToast(if (permanent) "Product removed permanently." else "Moved to Trash.")
                                }
                            },
                            onAddCategory = { name ->
                                scope.launch { vm.repo.addCategory(name, activeRole.actorName, activeRole.actorRole) }
                            },
                            onDeleteCategory = { cat ->
                                scope.launch { vm.repo.deleteCategory(cat, activeRole.actorName, activeRole.actorRole) }
                            }
                        )

                        MainTab.POS -> PosBillingScreen(
                            settings = settings,
                            activeRole = activeRole,
                            categories = categories,
                            products = products,
                            variants = variants,
                            customers = customers,
                            cartLines = cartLines,
                            selectedCustomer = posCustomer,
                            onSelectCustomer = { vm.selectPosCustomer(it) },
                            onAddToCart = { prod, v -> vm.addToCart(prod, v) },
                            onUpdateQuantity = { idx, q -> vm.updateCartQuantity(idx, q) },
                            onUpdateLinePricing = { idx, disc, price -> vm.updateCartItemDiscount(idx, disc, price) },
                            onClearCart = { vm.clearCart() },
                            onCheckout = { wName, wMob, bDisc, gst, paid, mode, notes ->
                                vm.checkoutSale(wName, wMob, bDisc, gst, paid, mode, notes)
                            },
                            onQuickCreateCustomer = { name, mob ->
                                scope.launch {
                                    val id = vm.repo.saveCustomer(CustomerEntity(name = name, mobile = mob), activeRole.actorName, activeRole.actorRole)
                                    vm.selectPosCustomer(CustomerEntity(id = id, name = name, mobile = mob))
                                    vm.showToast("Customer $name saved!")
                                }
                            }
                        )

                        MainTab.ACCOUNTS -> AccountsScreen(
                            settings = settings,
                            activeRole = activeRole,
                            summary = summary,
                            expenses = expenses,
                            otherIncomes = otherIncomes,
                            sales = sales,
                            saleItems = saleItems,
                            returns = returns,
                            products = products,
                            openAddExpenseOnStart = openAddExpenseTrigger,
                            onConsumedOpenExpense = { openAddExpenseTrigger = false },
                            onAddExpense = { exp ->
                                scope.launch { vm.repo.addExpense(exp, activeRole.actorName, activeRole.actorRole) }
                            },
                            onArchiveExpense = { exp ->
                                scope.launch { vm.repo.deleteExpense(exp, activeRole.actorName, activeRole.actorRole) }
                            },
                            onAddOtherIncome = { inc ->
                                scope.launch { vm.repo.addOtherIncome(inc, activeRole.actorName, activeRole.actorRole) }
                            },
                            onOpenInvoice = { inv -> vm.setInvoicePreview(inv) },
                            onProcessReturnOrExchange = { inv, item, qty, isFull, exProd, refund, reason ->
                                scope.launch {
                                    vm.repo.processReturnOrExchange(inv, item, qty, isFull, exProd, refund, reason, activeRole.actorName, activeRole.actorRole)
                                    vm.showToast("Return / Exchange processed.")
                                }
                            },
                            onCancelInvoice = { inv, reason ->
                                scope.launch {
                                    vm.repo.cancelInvoice(inv, reason, activeRole.actorName, activeRole.actorRole)
                                    vm.showToast("Invoice cancelled & stock restored.")
                                }
                            }
                        )

                        MainTab.MORE -> when (moreSubScreen) {
                            MoreSubScreen.HUB -> MoreHubScreen(
                                settings = settings,
                                activeRole = activeRole,
                                onNavigateCustomers = { moreSubScreen = MoreSubScreen.CUSTOMERS },
                                onNavigateSuppliers = { moreSubScreen = MoreSubScreen.SUPPLIERS },
                                onNavigatePurchases = { moreSubScreen = MoreSubScreen.PURCHASES },
                                onNavigateReports = { moreSubScreen = MoreSubScreen.REPORTS },
                                onNavigateStaff = { moreSubScreen = MoreSubScreen.STAFF },
                                onNavigateSettings = { moreSubScreen = MoreSubScreen.SETTINGS },
                                onNavigateBackup = { moreSubScreen = MoreSubScreen.BACKUP },
                                onSwitchRoleClick = { showRoleSwitchDialog = true },
                                onSignOutClick = {
                                    authManager.signOut(credentialManager, scope)
                                }
                            )

                            MoreSubScreen.CUSTOMERS -> CustomersScreen(
                                settings = settings,
                                customers = customers,
                                customerPayments = customerPayments,
                                sales = sales,
                                openAddOnStart = openAddCustomerTrigger,
                                onConsumedOpenAdd = { openAddCustomerTrigger = false },
                                onBack = { moreSubScreen = MoreSubScreen.HUB },
                                onSaveCustomer = { cust ->
                                    scope.launch { vm.repo.saveCustomer(cust, activeRole.actorName, activeRole.actorRole) }
                                },
                                onDeleteCustomer = { cust ->
                                    scope.launch { vm.repo.deleteCustomer(cust, activeRole.actorName, activeRole.actorRole) }
                                },
                                onRecordPayment = { cust, amt, mode, note ->
                                    scope.launch { vm.repo.recordCustomerPayment(cust, amt, mode, note, activeRole.actorName, activeRole.actorRole) }
                                },
                                onOpenInvoice = { inv -> vm.setInvoicePreview(inv) }
                            )

                            MoreSubScreen.SUPPLIERS, MoreSubScreen.PURCHASES -> SuppliersAndPurchasesScreen(
                                settings = settings,
                                suppliers = suppliers,
                                supplierPayments = supplierPayments,
                                purchases = purchases,
                                products = products,
                                initialTab = if (moreSubScreen == MoreSubScreen.PURCHASES) 1 else 0,
                                openNewPurchaseOnStart = openAddPurchaseTrigger,
                                onConsumedOpenPurchase = { openAddPurchaseTrigger = false },
                                onBack = { moreSubScreen = MoreSubScreen.HUB },
                                onSaveSupplier = { sup ->
                                    scope.launch { vm.repo.saveSupplier(sup, activeRole.actorName, activeRole.actorRole) }
                                },
                                onDeleteSupplier = { sup ->
                                    scope.launch { vm.repo.deleteSupplier(sup, activeRole.actorName, activeRole.actorRole) }
                                },
                                onRecordSupplierPayment = { sup, amt, mode, note ->
                                    scope.launch { vm.repo.recordSupplierPayment(sup, amt, mode, note, activeRole.actorName, activeRole.actorRole) }
                                },
                                onRecordPurchase = { sup, fallback, lines, mode, paid, notes ->
                                    scope.launch {
                                        vm.repo.recordPurchase(sup, fallback, lines, mode, paid, notes, activeRole.actorName, activeRole.actorRole)
                                        vm.showToast("Purchase saved & stock increased!")
                                    }
                                }
                            )

                            MoreSubScreen.REPORTS -> ReportsScreen(
                                settings = settings,
                                activeRole = activeRole,
                                backupManager = vm.backupManager,
                                products = products,
                                sales = sales,
                                saleItems = saleItems,
                                purchases = purchases,
                                expenses = expenses,
                                customers = customers,
                                suppliers = suppliers,
                                onBack = { moreSubScreen = MoreSubScreen.HUB }
                            )

                            MoreSubScreen.STAFF -> StaffManagementScreen(
                                staffList = staffList,
                                onBack = { moreSubScreen = MoreSubScreen.HUB },
                                onSaveStaff = { st ->
                                    scope.launch { vm.repo.saveStaff(st, activeRole.actorName, activeRole.actorRole) }
                                }
                            )

                            MoreSubScreen.SETTINGS -> SettingsScreen(
                                settings = settings,
                                authManager = authManager,
                                deletedProducts = deletedProducts,
                                deletedCustomers = deletedCustomers,
                                deletedSuppliers = deletedSuppliers,
                                auditLogs = auditLogs,
                                onBack = { moreSubScreen = MoreSubScreen.HUB },
                                onSaveSettings = { vm.saveSettings(it) },
                                onRestoreProduct = { p ->
                                    scope.launch { vm.repo.restoreProduct(p.id, p.name, activeRole.actorName, activeRole.actorRole) }
                                },
                                onRestoreCustomer = { c ->
                                    scope.launch { vm.repo.restoreCustomer(c.id, c.name, activeRole.actorName, activeRole.actorRole) }
                                },
                                onRestoreSupplier = { s ->
                                    scope.launch { vm.repo.restoreSupplier(s.id, s.name, activeRole.actorName, activeRole.actorRole) }
                                }
                            )

                            MoreSubScreen.BACKUP -> BackupRestoreScreen(
                                onBack = { moreSubScreen = MoreSubScreen.HUB },
                                onExportJsonToUri = { uri -> vm.exportBackupToUri(uri) },
                                onRestoreJsonFromUri = { uri -> vm.restoreBackupFromUri(uri) },
                                onShareLocalBackup = {
                                    scope.launch {
                                        val file = vm.createLocalBackupFile()
                                        PrintAndShareHelper.shareFileGeneric(context, file, "application/json", "Share Naqsh Backup")
                                    }
                                },
                                onSyncToCloud = { vm.syncToCloud() }
                            )
                        }
                    }
                }
            }
        }
    }

    if (lastInvoicePreview != null) {
        val inv = lastInvoicePreview!!
        val items = saleItems.filter { it.invoiceId == inv.id }
        InvoicePreviewDialog(
            settings = settings,
            invoice = inv,
            items = items,
            onDismiss = { vm.setInvoicePreview(null) }
        )
    }

    if (showRoleSwitchDialog) {
        RoleSwitchDialog(
            ownerName = settings.ownerName,
            activeIsOwner = activeRole.isOwner,
            staffList = staffList.filter { it.isActive },
            onSwitchOwner = {
                vm.switchToOwner()
                showRoleSwitchDialog = false
            },
            onSwitchStaff = { st, pin ->
                val ok = vm.switchToStaff(st, pin)
                if (ok) {
                    showRoleSwitchDialog = false
                } else {
                    Toast.makeText(context, "Incorrect Staff PIN.", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { showRoleSwitchDialog = false }
        )
    }
}

@Composable
private fun RoleSwitchDialog(
    ownerName: String,
    activeIsOwner: Boolean,
    staffList: List<StaffEntity>,
    onSwitchOwner: () -> Unit,
    onSwitchStaff: (StaffEntity, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedStaff by remember { mutableStateOf(staffList.firstOrNull()) }
    var pin by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Owner / Staff Mode") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!activeIsOwner) {
                    Button(onClick = onSwitchOwner, modifier = Modifier.fillMaxWidth()) {
                        Text("Return to Owner ($ownerName)")
                    }
                    HorizontalDivider()
                }
                if (staffList.isEmpty()) {
                    Text("No active staff accounts configured. Owner can create staff accounts in MORE -> Staff Management.")
                } else {
                    Text("Switch to Staff Shift:", style = MaterialTheme.typography.labelMedium)
                    staffList.forEach { st ->
                        OutlinedCard(
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().clickable { selectedStaff = st }
                        ) {
                            Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${st.name} (${st.roleTitle})")
                                if (selectedStaff?.id == st.id) Icon(Icons.Default.Check, contentDescription = null)
                            }
                        }
                    }
                    if (selectedStaff != null) {
                        OutlinedTextField(
                            value = pin,
                            onValueChange = { pin = it },
                            label = { Text("Enter Staff PIN") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (selectedStaff != null) {
                Button(onClick = { onSwitchStaff(selectedStaff!!, pin) }) {
                    Text("Enter Staff Mode")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
