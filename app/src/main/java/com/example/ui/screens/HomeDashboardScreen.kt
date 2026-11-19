package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.*
import com.example.data.repository.ActiveSessionRole
import com.example.ui.components.BismillahBrandBanner
import com.example.ui.components.BoutiqueKpiCard
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.LuxurySectionHeader
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.LossCrimson
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.AccountingSummary
import com.example.util.AppStrings
import com.example.util.Formatters

@Composable
fun HomeDashboardScreen(
    settings: BusinessSettingsEntity,
    activeRole: ActiveSessionRole,
    summary: AccountingSummary,
    products: List<ProductEntity>,
    recentSales: List<SaleInvoiceEntity>,
    onQuickNewSale: () -> Unit,
    onQuickAddProduct: () -> Unit,
    onQuickAddPurchase: () -> Unit,
    onQuickAddExpense: () -> Unit,
    onQuickAddCustomer: () -> Unit,
    onQuickReports: () -> Unit,
    onInvoiceClick: (SaleInvoiceEntity) -> Unit,
    onSwitchRoleClick: () -> Unit
) {
    val lang = settings.language
    val sym = settings.currencySymbol
    val lowStockItems = remember(products) { products.filter { it.quantity <= it.minStock }.take(6) }

    Column(modifier = Modifier.fillMaxSize()) {
        BismillahBrandBanner(
            businessName = settings.businessName,
            tagline = settings.tagline,
            customLogoUri = settings.customLogoUri,
            roleBadge = if (activeRole.isOwner) "Owner: ${settings.ownerName.split(" ").firstOrNull() ?: "Owner"}" else "Staff: ${activeRole.actorName}",
            onRoleBadgeClick = onSwitchRoleClick
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .align(Alignment.CenterHorizontally),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // KPI Grid (2 rows x 2 columns)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BoutiqueKpiCard(
                        title = AppStrings.todaySales(lang),
                        value = Formatters.currencyCompact(summary.dailyIncome, sym),
                        subtitle = "${summary.totalSalesCount} total bills",
                        icon = Icons.Default.PointOfSale,
                        accentColor = ProfitGreen,
                        modifier = Modifier.weight(1f)
                    )
                    if (activeRole.isOwner) {
                        val todayNet = summary.dailyIncome - summary.dailyExpenses
                        BoutiqueKpiCard(
                            title = AppStrings.todayProfit(lang),
                            value = Formatters.currencyCompact(todayNet, sym),
                            subtitle = "Exp: ${Formatters.currencyCompact(summary.dailyExpenses, sym)}",
                            icon = Icons.Default.TrendingUp,
                            accentColor = if (todayNet >= 0) ProfitGreen else LossCrimson,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        BoutiqueKpiCard(
                            title = "Products",
                            value = "${products.size}",
                            subtitle = "In boutique catalog",
                            icon = Icons.Default.Inventory2,
                            accentColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BoutiqueKpiCard(
                        title = AppStrings.lowStockAlert(lang),
                        value = "${summary.lowStockCount} items",
                        subtitle = if (summary.lowStockCount > 0) "Needs restock" else "Stock healthy",
                        icon = Icons.Default.WarningAmber,
                        accentColor = if (summary.lowStockCount > 0) WarningAmber else ProfitGreen,
                        modifier = Modifier.weight(1f)
                    )
                    BoutiqueKpiCard(
                        title = AppStrings.customerDue(lang),
                        value = Formatters.currencyCompact(summary.customerOutstanding, sym),
                        subtitle = if (activeRole.isOwner) "Payable: ${Formatters.currencyCompact(summary.supplierOutstanding, sym)}" else "Pending dues",
                        icon = Icons.Default.AccountBalanceWallet,
                        accentColor = if (summary.customerOutstanding > 0) LossCrimson else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Quick Actions (6 actions)
            item {
                LuxurySectionHeader(title = "Quick Actions")
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        QuickActionButton(
                            label = AppStrings.quickNewSale(lang),
                            icon = Icons.Default.AddShoppingCart,
                            enabled = activeRole.canMakeSales,
                            onClick = onQuickNewSale,
                            modifier = Modifier.weight(1f).testTag("qa_new_sale")
                        )
                        QuickActionButton(
                            label = AppStrings.quickAddProduct(lang),
                            icon = Icons.Default.Checkroom,
                            enabled = activeRole.canManageInventory,
                            onClick = onQuickAddProduct,
                            modifier = Modifier.weight(1f).testTag("qa_add_product")
                        )
                        QuickActionButton(
                            label = AppStrings.quickAddPurchase(lang),
                            icon = Icons.Default.LocalShipping,
                            enabled = activeRole.canManagePurchases,
                            onClick = onQuickAddPurchase,
                            modifier = Modifier.weight(1f).testTag("qa_add_purchase")
                        )
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        QuickActionButton(
                            label = AppStrings.quickAddExpense(lang),
                            icon = Icons.Default.ReceiptLong,
                            enabled = activeRole.isOwner,
                            onClick = onQuickAddExpense,
                            modifier = Modifier.weight(1f).testTag("qa_add_expense")
                        )
                        QuickActionButton(
                            label = AppStrings.quickAddCustomer(lang),
                            icon = Icons.Default.PersonAdd,
                            enabled = activeRole.canManageCustomers,
                            onClick = onQuickAddCustomer,
                            modifier = Modifier.weight(1f).testTag("qa_add_customer")
                        )
                        QuickActionButton(
                            label = AppStrings.quickReports(lang),
                            icon = Icons.Default.Assessment,
                            enabled = activeRole.canViewBasicReports,
                            onClick = onQuickReports,
                            modifier = Modifier.weight(1f).testTag("qa_reports")
                        )
                    }
                }
            }

            // Recent Invoices
            item {
                LuxurySectionHeader(title = "Recent Invoices")
            }

            if (recentSales.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Default.Receipt,
                        title = "No Sales Recorded Yet",
                        subtitle = "Tap 'New Sale' above or open the SALES / POS tab to generate your first luxury boutique invoice."
                    )
                }
            } else {
                items(recentSales.take(5), key = { it.id }) { sale ->
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onInvoiceClick(sale) },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = "${sale.invoiceNumber} • ${sale.customerName}",
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${Formatters.dateTime(sale.timestamp)} • ${sale.paymentMethod} • ${sale.status}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = Formatters.currency(sale.grandTotal, sym),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (sale.remainingBalance > 0) {
                                    Text(
                                        text = "Due ${Formatters.currencyCompact(sale.remainingBalance, sym)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = LossCrimson
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Low Stock Watchlist
            if (lowStockItems.isNotEmpty()) {
                item {
                    LuxurySectionHeader(title = "Low Stock Watchlist")
                }
                items(lowStockItems, key = { "low_${it.id}" }) { prod ->
                    OutlinedCard(
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(prod.name, style = MaterialTheme.typography.titleSmall)
                                Text("${prod.categoryName} • ${prod.size} / ${prod.colour}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            AssistChip(
                                onClick = onQuickAddPurchase,
                                label = { Text("Qty: ${prod.quantity}") }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    label: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .heightIn(min = 82.dp)
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (enabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
