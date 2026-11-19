package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.BusinessSettingsEntity
import com.example.data.local.SaleInvoiceEntity
import com.example.data.local.SaleItemEntity
import com.example.ui.components.NaqshLogoImage
import com.example.ui.components.PhonePeQrImage
import com.example.ui.theme.*
import com.example.util.AppStrings
import com.example.util.Formatters
import com.example.util.pdf.InvoiceFormat
import com.example.util.pdf.InvoicePdfGenerator
import com.example.util.pdf.PrintAndShareHelper
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun InvoicePreviewDialog(
    settings: BusinessSettingsEntity,
    invoice: SaleInvoiceEntity,
    items: List<SaleItemEntity>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedFormat by remember {
        mutableStateOf(
            InvoiceFormat.entries.firstOrNull { it.code == settings.printerPaperSize } ?: InvoiceFormat.A4
        )
    }
    var pendingSaveFile by remember { mutableStateOf<File?>(null) }
    var isGenerating by remember { mutableStateOf(false) }

    val savePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val file = pendingSaveFile
        if (uri != null && file != null) {
            scope.launch {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        file.inputStream().use { inp -> inp.copyTo(out) }
                    }
                    Toast.makeText(context, "PDF saved to Downloads / chosen folder!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Save failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DeepEmerald)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Invoice ${invoice.invoiceNumber}",
                            color = ChampagneGold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "${invoice.status} • ${Formatters.dateTime(invoice.timestamp)}",
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close Invoice Preview", tint = Color.White)
                    }
                }

                // Format Switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    InvoiceFormat.entries.forEach { fmt ->
                        FilterChip(
                            selected = selectedFormat == fmt,
                            onClick = { selectedFormat = fmt },
                            label = { Text(fmt.code, style = MaterialTheme.typography.labelMedium) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Visual Invoice Preview
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.5.dp, ChampagneGold),
                        colors = CardDefaults.cardColors(containerColor = WarmIvory),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = AppStrings.BISMILLAH,
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(8.dp))
                            NaqshLogoImage(customLogoUri = settings.customLogoUri, size = 72.dp)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = settings.businessName,
                                style = MaterialTheme.typography.headlineMedium,
                                color = DeepEmerald,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = settings.businessCategory,
                                style = MaterialTheme.typography.labelMedium,
                                color = EmeraldPrimary,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = settings.tagline,
                                style = MaterialTheme.typography.bodySmall,
                                color = ChampagneGoldDark,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = settings.address.replace("\n", ", "),
                                style = MaterialTheme.typography.bodySmall,
                                color = InkMuted,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Mob: ${settings.ownerMobile}" + if (settings.gstin.isNotBlank()) " • GSTIN: ${settings.gstin}" else "",
                                style = MaterialTheme.typography.labelMedium,
                                color = DeepEmerald,
                                textAlign = TextAlign.Center
                            )

                            HorizontalDivider(Modifier.padding(vertical = 10.dp), color = ChampagneGold)

                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Invoice #: ${invoice.invoiceNumber}", fontWeight = FontWeight.Bold, color = InkDark, fontSize = 12.sp)
                                    Text(Formatters.dateTime(invoice.timestamp), color = InkMuted, fontSize = 11.sp)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(invoice.customerName, fontWeight = FontWeight.Bold, color = InkDark, fontSize = 12.sp)
                                    if (invoice.customerMobile.isNotBlank()) {
                                        Text(invoice.customerMobile, color = InkMuted, fontSize = 11.sp)
                                    }
                                }
                            }

                            HorizontalDivider(Modifier.padding(vertical = 10.dp), color = OutlineIvory)

                            items.forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(item.productName, fontWeight = FontWeight.SemiBold, color = InkDark, fontSize = 13.sp)
                                        Text("${item.size} • ${item.colour} • ${item.quantity} x ${Formatters.currency(item.unitPrice, settings.currencySymbol)}", color = InkMuted, fontSize = 11.sp)
                                    }
                                    Text(
                                        text = Formatters.currency(item.lineTotal, settings.currencySymbol),
                                        fontWeight = FontWeight.SemiBold,
                                        color = InkDark,
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            HorizontalDivider(Modifier.padding(vertical = 10.dp), color = ChampagneGold)

                            SummaryLine("Subtotal", Formatters.currency(invoice.subtotal, settings.currencySymbol))
                            val disc = invoice.itemDiscountTotal + invoice.billDiscount
                            if (disc > 0) {
                                SummaryLine("Discount", "-${Formatters.currency(disc, settings.currencySymbol)}")
                            }
                            if (invoice.taxAmount > 0) {
                                SummaryLine("GST (${"%.1f".format(invoice.gstPercent)}%)", Formatters.currency(invoice.taxAmount, settings.currencySymbol))
                            }
                            SummaryLine("Grand Total", Formatters.currency(invoice.grandTotal, settings.currencySymbol), bold = true)
                            SummaryLine("Paid (${invoice.paymentMethod})", Formatters.currency(invoice.amountPaid, settings.currencySymbol))
                            SummaryLine(
                                "Balance Due",
                                Formatters.currency(invoice.remainingBalance, settings.currencySymbol),
                                valueColor = if (invoice.remainingBalance > 0) LossCrimson else ProfitGreen
                            )

                            Spacer(Modifier.height(12.dp))
                            Text("Scan & Pay via PhonePe / UPI", style = MaterialTheme.typography.labelMedium, color = EmeraldPrimary)
                            Spacer(Modifier.height(6.dp))
                            PhonePeQrImage(customQrUri = settings.customQrUri, size = 150.dp)
                            if (settings.upiId.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text("UPI ID: ${settings.upiId}", style = MaterialTheme.typography.labelSmall, color = InkMuted)
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = settings.invoiceFooterNote.ifBlank { AppStrings.thankYouMessage(settings.language) },
                                style = MaterialTheme.typography.bodySmall,
                                color = EmeraldPrimary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // Action Footer
                Surface(
                    tonalElevation = 4.dp,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                isGenerating = true
                                scope.launch {
                                    val pdf = InvoicePdfGenerator.generateInvoicePdf(context, settings, invoice, items, selectedFormat)
                                    isGenerating = false
                                    val msg = "Assalamu Alaikum ${invoice.customerName},\n" +
                                        "Thank you for shopping at *${settings.businessName}*.\n" +
                                        "Invoice: ${invoice.invoiceNumber}\n" +
                                        "Total: ${Formatters.currency(invoice.grandTotal, settings.currencySymbol)}\n" +
                                        "Paid: ${Formatters.currency(invoice.amountPaid, settings.currencySymbol)}\n" +
                                        (if (invoice.remainingBalance > 0) "Balance Due: ${Formatters.currency(invoice.remainingBalance, settings.currencySymbol)}\n" else "") +
                                        "Please find your official invoice PDF attached. JazakAllahu Khayran!"
                                    PrintAndShareHelper.sharePdfOnWhatsApp(context, pdf, invoice.customerMobile, msg)
                                }
                            },
                            enabled = !isGenerating,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF128C7E)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("share_whatsapp_pdf_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text(AppStrings.shareOnWhatsApp(settings.language), color = Color.White)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    isGenerating = true
                                    scope.launch {
                                        val pdf = InvoicePdfGenerator.generateInvoicePdf(context, settings, invoice, items, selectedFormat)
                                        isGenerating = false
                                        PrintAndShareHelper.printPdfFile(context, pdf, "Invoice_${invoice.invoiceNumber}")
                                    }
                                },
                                enabled = !isGenerating,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("print_invoice_button")
                            ) {
                                Icon(Icons.Default.Print, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Print")
                            }

                            OutlinedButton(
                                onClick = {
                                    isGenerating = true
                                    scope.launch {
                                        val pdf = InvoicePdfGenerator.generateInvoicePdf(context, settings, invoice, items, selectedFormat)
                                        pendingSaveFile = pdf
                                        isGenerating = false
                                        savePdfLauncher.launch("${invoice.invoiceNumber}_${selectedFormat.code}.pdf")
                                    }
                                },
                                enabled = !isGenerating,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("download_pdf_button")
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Save PDF")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String, bold: Boolean = false, valueColor: Color = InkDark) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color = if (bold) DeepEmerald else InkMuted,
            fontSize = if (bold) 14.sp else 12.sp
        )
        Text(
            text = value,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
            color = valueColor,
            fontSize = if (bold) 14.sp else 12.sp
        )
    }
}
