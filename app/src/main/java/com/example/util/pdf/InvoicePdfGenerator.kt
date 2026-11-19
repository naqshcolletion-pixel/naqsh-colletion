package com.example.util.pdf

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.R
import com.example.data.local.BusinessSettingsEntity
import com.example.data.local.SaleInvoiceEntity
import com.example.data.local.SaleItemEntity
import com.example.util.AppStrings
import com.example.util.Formatters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

enum class InvoiceFormat(val code: String, val label: String) {
    A4("A4", "A4 Luxury Invoice"),
    THERMAL_80("80MM", "80mm Thermal Receipt"),
    THERMAL_58("58MM", "58mm Thermal Receipt")
}

object InvoicePdfGenerator {

    private fun loadBitmapSafely(context: Context, uriString: String, fallbackResId: Int): Bitmap? {
        if (uriString.isNotBlank()) {
            try {
                val uri = Uri.parse(uriString)
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val decoded = BitmapFactory.decodeStream(stream)
                    if (decoded != null) return decoded
                }
            } catch (_: Exception) {
            }
        }
        return BitmapFactory.decodeResource(context.resources, fallbackResId)
    }

    suspend fun generateInvoicePdf(
        context: Context,
        settings: BusinessSettingsEntity,
        invoice: SaleInvoiceEntity,
        items: List<SaleItemEntity>,
        format: InvoiceFormat = InvoiceFormat.A4
    ): File = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "invoices").apply { mkdirs() }
        val safeInv = invoice.invoiceNumber.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val outFile = File(dir, "Invoice_${safeInv}_${format.code}.pdf")

        val logoBmp = loadBitmapSafely(context, settings.customLogoUri, R.drawable.naqsh_logo)
        val qrBmp = loadBitmapSafely(context, settings.customQrUri, R.drawable.phonepe_qr)

        val document = PdfDocument()
        try {
            when (format) {
                InvoiceFormat.A4 -> drawA4Invoice(document, settings, invoice, items, logoBmp, qrBmp)
                InvoiceFormat.THERMAL_80 -> drawThermalReceipt(document, settings, invoice, items, logoBmp, qrBmp, 226)
                InvoiceFormat.THERMAL_58 -> drawThermalReceipt(document, settings, invoice, items, logoBmp, qrBmp, 164)
            }

            FileOutputStream(outFile).use { fos ->
                document.writeTo(fos)
            }
        } finally {
            try { document.close() } catch (_: Exception) {}
        }
        outFile
    }

    private fun drawA4Invoice(
        document: PdfDocument,
        settings: BusinessSettingsEntity,
        invoice: SaleInvoiceEntity,
        items: List<SaleItemEntity>,
        logoBmp: Bitmap?,
        qrBmp: Bitmap?
    ) {
        val pageWidth = 595
        val pageHeight = 842
        val margin = 28f
        val contentWidth = pageWidth - margin * 2

        val emeraldDeep = Color.parseColor("#021F1B")
        val emeraldPrimary = Color.parseColor("#084C3E")
        val champagneGold = Color.parseColor("#D4AF37")
        val warmIvory = Color.parseColor("#FAF6EE")
        val ivoryAlt = Color.parseColor("#F3ECDD")
        val inkDark = Color.parseColor("#13221E")
        val inkMuted = Color.parseColor("#536661")

        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        // Background
        canvas.drawColor(warmIvory)

        // Outer gold frame
        val borderPaint = Paint().apply {
            color = champagneGold
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRect(14f, 14f, pageWidth - 14f, pageHeight - 14f, borderPaint)

        // Emerald Header Banner
        val headerHeight = 148f
        val headerPaint = Paint().apply { color = emeraldDeep }
        canvas.drawRect(15f, 15f, pageWidth - 15f, 15f + headerHeight, headerPaint)

        val goldPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = champagneGold
            textAlign = Paint.Align.CENTER
            textSize = 13f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        canvas.drawText(AppStrings.BISMILLAH, pageWidth / 2f, 36f, goldPaint)

        // Exact Original Logo on left
        if (logoBmp != null) {
            val logoRect = RectF(margin, 44f, margin + 88f, 44f + 88f)
            canvas.drawBitmap(logoBmp, null, logoRect, Paint(Paint.FILTER_BITMAP_FLAG))
        }

        // Brand Info on right of logo
        val brandTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = champagneGold
            textSize = 22f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        canvas.drawText(settings.businessName, margin + 102f, 68f, brandTitlePaint)

        val catPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 10f
            letterSpacing = 0.12f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText(settings.businessCategory, margin + 102f, 84f, catPaint)

        val tagPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E8D08B")
            textSize = 9.5f
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
        }
        canvas.drawText(settings.tagline, margin + 102f, 99f, tagPaint)

        val addrPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E0EAE7")
            textSize = 8.5f
        }
        val cleanAddr = settings.address.replace("\n", ", ")
        val addrLayout = StaticLayout.Builder.obtain(cleanAddr, 0, cleanAddr.length, addrPaint, (contentWidth - 108f).toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .build()
        canvas.save()
        canvas.translate(margin + 102f, 108f)
        addrLayout.draw(canvas)
        canvas.restore()

        val mobPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = champagneGold
            textSize = 9f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val gstSuffix = if (settings.gstin.isNotBlank()) "  |  GSTIN: ${settings.gstin}" else ""
        canvas.drawText("Mob: ${settings.ownerMobile}$gstSuffix", margin + 102f, 152f, mobPaint)

        var y = 180f

        // Invoice & Customer Meta Card
        val cardPaint = Paint().apply { color = Color.WHITE }
        canvas.drawRoundRect(margin, y, pageWidth - margin, y + 66f, 8f, 8f, cardPaint)
        val cardStroke = Paint().apply {
            color = champagneGold
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRoundRect(margin, y, pageWidth - margin, y + 66f, 8f, 8f, cardStroke)

        val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkMuted
            textSize = 8.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val valPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkDark
            textSize = 10.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        canvas.drawText("TAX / BOUTIQUE INVOICE", margin + 12f, y + 18f, labelPaint)
        canvas.drawText("Invoice #: ${invoice.invoiceNumber}", margin + 12f, y + 34f, valPaint)
        canvas.drawText("Date: ${Formatters.dateTime(invoice.timestamp)}", margin + 12f, y + 50f, labelPaint)

        val rightColX = pageWidth / 2f + 12f
        canvas.drawText("BILLED TO", rightColX, y + 18f, labelPaint)
        canvas.drawText(invoice.customerName, rightColX, y + 34f, valPaint)
        val custPhone = if (invoice.customerMobile.isNotBlank()) "Mob: ${invoice.customerMobile}" else "Walk-in Boutique Customer"
        canvas.drawText(custPhone, rightColX, y + 50f, labelPaint)

        y += 80f

        // Table Header
        val tblHdrPaint = Paint().apply { color = emeraldPrimary }
        canvas.drawRect(margin, y, pageWidth - margin, y + 24f, tblHdrPaint)

        val thPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 9f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val thRight = TextPaint(thPaint).apply { textAlign = Paint.Align.RIGHT }

        canvas.drawText("#", margin + 8f, y + 16f, thPaint)
        canvas.drawText("PRODUCT & VARIANT", margin + 26f, y + 16f, thPaint)
        canvas.drawText("QTY", margin + 290f, y + 16f, thRight)
        canvas.drawText("RATE", margin + 365f, y + 16f, thRight)
        canvas.drawText("DISC", margin + 435f, y + 16f, thRight)
        canvas.drawText("TOTAL", pageWidth - margin - 8f, y + 16f, thRight)

        y += 24f

        val rowTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkDark
            textSize = 9.5f
        }
        val rowSubPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkMuted
            textSize = 8f
        }
        val rowRightPaint = TextPaint(rowTextPaint).apply { textAlign = Paint.Align.RIGHT }

        items.forEachIndexed { idx, item ->
            val rowHeight = 28f
            if (idx % 2 == 1) {
                canvas.drawRect(margin, y, pageWidth - margin, y + rowHeight, Paint().apply { color = ivoryAlt })
            } else {
                canvas.drawRect(margin, y, pageWidth - margin, y + rowHeight, Paint().apply { color = Color.WHITE })
            }
            canvas.drawText("${idx + 1}", margin + 8f, y + 13f, rowTextPaint)
            canvas.drawText(item.productName.take(36), margin + 26f, y + 13f, rowTextPaint)
            canvas.drawText("${item.size} • ${item.colour}", margin + 26f, y + 23f, rowSubPaint)
            canvas.drawText("${item.quantity}", margin + 290f, y + 16f, rowRightPaint)
            canvas.drawText(Formatters.currency(item.unitPrice, settings.currencySymbol), margin + 365f, y + 16f, rowRightPaint)
            canvas.drawText(
                if (item.itemDiscount > 0) Formatters.currency(item.itemDiscount, settings.currencySymbol) else "-",
                margin + 435f,
                y + 16f,
                rowRightPaint
            )
            canvas.drawText(Formatters.currency(item.lineTotal, settings.currencySymbol), pageWidth - margin - 8f, y + 16f, rowRightPaint)
            y += rowHeight
        }

        // Gold line below items
        canvas.drawLine(margin, y, pageWidth - margin, y, borderPaint)
        y += 14f

        // Bottom Section: Left = PhonePe QR (when configured/available) + Payment Info, Right = Financial Summary
        val summaryX = pageWidth / 2f + 20f
        val summaryRight = pageWidth - margin - 10f

        // Left: QR Box
        if (qrBmp != null) {
            val qrCardRect = RectF(margin, y, margin + 150f, y + 178f)
            canvas.drawRoundRect(qrCardRect, 8f, 8f, cardPaint)
            canvas.drawRoundRect(qrCardRect, 8f, 8f, cardStroke)

            canvas.drawText("SCAN & PAY (PHONEPE / UPI)", margin + 10f, y + 16f, labelPaint)
            // Keep exact aspect ratio 1:1 without distortion!
            val qrRect = RectF(margin + 15f, y + 22f, margin + 135f, y + 142f)
            canvas.drawBitmap(qrBmp, null, qrRect, Paint(Paint.FILTER_BITMAP_FLAG))
            val upiLine = if (settings.upiId.isNotBlank()) "UPI: ${settings.upiId}" else "PhonePe Merchant QR"
            canvas.drawText(upiLine.take(26), margin + 12f, y + 158f, rowSubPaint)
            canvas.drawText("Method: ${invoice.paymentMethod}", margin + 12f, y + 170f, rowSubPaint)
        }

        // Right: Totals Card
        val totalCardTop = y
        val totalRows = mutableListOf(
            "Subtotal" to Formatters.currency(invoice.subtotal, settings.currencySymbol)
        )
        val totalDisc = invoice.itemDiscountTotal + invoice.billDiscount
        if (totalDisc > 0.0) {
            totalRows.add("Discount" to "-${Formatters.currency(totalDisc, settings.currencySymbol)}")
        }
        if (invoice.taxAmount > 0.0) {
            totalRows.add("GST (${"%.1f".format(invoice.gstPercent)}%)" to Formatters.currency(invoice.taxAmount, settings.currencySymbol))
        }

        var sy = totalCardTop + 14f
        val sumLabelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = inkMuted; textSize = 10f }
        val sumValPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = inkDark; textSize = 10f; textAlign = Paint.Align.RIGHT }

        totalRows.forEach { (lbl, v) ->
            canvas.drawText(lbl, summaryX, sy, sumLabelPaint)
            canvas.drawText(v, summaryRight, sy, sumValPaint)
            sy += 18f
        }

        // Grand Total Pill
        canvas.drawRoundRect(summaryX - 6f, sy - 2f, summaryRight + 6f, sy + 24f, 6f, 6f, Paint().apply { color = emeraldPrimary })
        val grandLabelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = champagneGold
            textSize = 11.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val grandValPaint = TextPaint(grandLabelPaint).apply { textAlign = Paint.Align.RIGHT; color = Color.WHITE }
        canvas.drawText("GRAND TOTAL", summaryX + 4f, sy + 15f, grandLabelPaint)
        canvas.drawText(Formatters.currency(invoice.grandTotal, settings.currencySymbol), summaryRight - 4f, sy + 15f, grandValPaint)
        sy += 38f

        canvas.drawText("Amount Paid (${invoice.paymentMethod})", summaryX, sy, sumLabelPaint)
        canvas.drawText(Formatters.currency(invoice.amountPaid, settings.currencySymbol), summaryRight, sy, sumValPaint)
        sy += 18f

        val balPaint = TextPaint(sumValPaint).apply {
            color = if (invoice.remainingBalance > 0) Color.parseColor("#B3261E") else Color.parseColor("#147D4F")
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText("Remaining Balance", summaryX, sy, sumLabelPaint)
        canvas.drawText(Formatters.currency(invoice.remainingBalance, settings.currencySymbol), summaryRight, sy, balPaint)

        // Footer Thank You Banner
        val footerY = pageHeight - 54f
        canvas.drawLine(margin, footerY - 12f, pageWidth - margin, footerY - 12f, borderPaint)
        val thankPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = emeraldPrimary
            textAlign = Paint.Align.CENTER
            textSize = 10.5f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC)
        }
        val thankText = settings.invoiceFooterNote.ifBlank { AppStrings.thankYouMessage(settings.language) }
        canvas.drawText(thankText, pageWidth / 2f, footerY + 4f, thankPaint)
        val statusFooter = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkMuted
            textAlign = Paint.Align.CENTER
            textSize = 8f
        }
        canvas.drawText("Status: ${invoice.status}  •  Billed by: ${invoice.staffName}  •  ${settings.businessName}", pageWidth / 2f, footerY + 18f, statusFooter)

        document.finishPage(page)
    }

    private fun drawThermalReceipt(
        document: PdfDocument,
        settings: BusinessSettingsEntity,
        invoice: SaleInvoiceEntity,
        items: List<SaleItemEntity>,
        logoBmp: Bitmap?,
        qrBmp: Bitmap?,
        widthPt: Int
    ) {
        val margin = 8f
        val contentWidth = widthPt - margin * 2
        val is58 = widthPt < 200
        val titleSize = if (is58) 11f else 13f
        val bodySize = if (is58) 7.5f else 8.5f
        val smallSize = if (is58) 6.5f else 7.5f

        val qrSize = if (qrBmp != null) (contentWidth * 0.62f) else 0f
        val estimatedHeight = (250 + items.size * 26 + qrSize.toInt()).coerceAtLeast(420)

        val pageInfo = PdfDocument.PageInfo.Builder(widthPt, estimatedHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        canvas.drawColor(Color.WHITE)

        val centerBold = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textAlign = Paint.Align.CENTER
            textSize = titleSize
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val centerNorm = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textAlign = Paint.Align.CENTER
            textSize = smallSize
        }
        val leftPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = bodySize
        }
        val rightPaint = TextPaint(leftPaint).apply { textAlign = Paint.Align.RIGHT }
        val boldLeft = TextPaint(leftPaint).apply { typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) }
        val boldRight = TextPaint(boldLeft).apply { textAlign = Paint.Align.RIGHT }

        var y = 14f
        canvas.drawText(AppStrings.BISMILLAH, widthPt / 2f, y, centerNorm)
        y += 12f

        if (logoBmp != null) {
            val logoDim = if (is58) 38f else 48f
            val lx = (widthPt - logoDim) / 2f
            canvas.drawBitmap(logoBmp, null, RectF(lx, y, lx + logoDim, y + logoDim), Paint(Paint.FILTER_BITMAP_FLAG))
            y += logoDim + 10f
        }

        canvas.drawText(settings.businessName, widthPt / 2f, y, centerBold)
        y += 10f
        canvas.drawText(settings.businessCategory, widthPt / 2f, y, centerNorm)
        y += 9f
        canvas.drawText(settings.tagline, widthPt / 2f, y, centerNorm)
        y += 10f

        val addrClean = settings.address.replace("\n", ", ")
        val addrLayout = StaticLayout.Builder.obtain(addrClean, 0, addrClean.length, centerNorm, contentWidth.toInt()).build()
        canvas.save()
        canvas.translate(margin, y)
        addrLayout.draw(canvas)
        canvas.restore()
        y += addrLayout.height + 6f

        canvas.drawText("Mob: ${settings.ownerMobile}", widthPt / 2f, y, centerNorm)
        y += 9f
        if (settings.gstin.isNotBlank()) {
            canvas.drawText("GSTIN: ${settings.gstin}", widthPt / 2f, y, centerNorm)
            y += 9f
        }

        val dashPaint = Paint().apply { color = Color.BLACK; strokeWidth = 0.8f }
        canvas.drawLine(margin, y, widthPt - margin, y, dashPaint)
        y += 11f

        canvas.drawText("Inv: ${invoice.invoiceNumber}", margin, y, boldLeft)
        y += 10f
        canvas.drawText("Date: ${Formatters.dateTime(invoice.timestamp)}", margin, y, leftPaint)
        y += 10f
        canvas.drawText("Cust: ${invoice.customerName}", margin, y, leftPaint)
        y += 10f
        if (invoice.customerMobile.isNotBlank()) {
            canvas.drawText("Mob: ${invoice.customerMobile}", margin, y, leftPaint)
            y += 10f
        }
        canvas.drawLine(margin, y, widthPt - margin, y, dashPaint)
        y += 10f

        canvas.drawText("ITEM", margin, y, boldLeft)
        canvas.drawText("AMT", widthPt - margin, y, boldRight)
        y += 10f

        items.forEach { item ->
            canvas.drawText(item.productName.take(if (is58) 20 else 28), margin, y, boldLeft)
            y += 9f
            canvas.drawText("${item.size}/${item.colour} x${item.quantity} @ ${Formatters.currencyCompact(item.unitPrice, settings.currencySymbol)}", margin, y, leftPaint)
            canvas.drawText(Formatters.currency(item.lineTotal, settings.currencySymbol), widthPt - margin, y, rightPaint)
            y += 11f
        }

        canvas.drawLine(margin, y, widthPt - margin, y, dashPaint)
        y += 11f

        canvas.drawText("Subtotal", margin, y, leftPaint)
        canvas.drawText(Formatters.currency(invoice.subtotal, settings.currencySymbol), widthPt - margin, y, rightPaint)
        y += 10f

        val disc = invoice.itemDiscountTotal + invoice.billDiscount
        if (disc > 0) {
            canvas.drawText("Discount", margin, y, leftPaint)
            canvas.drawText("-${Formatters.currency(disc, settings.currencySymbol)}", widthPt - margin, y, rightPaint)
            y += 10f
        }
        if (invoice.taxAmount > 0) {
            canvas.drawText("Tax (${"%.1f".format(invoice.gstPercent)}%)", margin, y, leftPaint)
            canvas.drawText(Formatters.currency(invoice.taxAmount, settings.currencySymbol), widthPt - margin, y, rightPaint)
            y += 10f
        }

        canvas.drawText("GRAND TOTAL", margin, y, boldLeft)
        canvas.drawText(Formatters.currency(invoice.grandTotal, settings.currencySymbol), widthPt - margin, y, boldRight)
        y += 11f
        canvas.drawText("Paid (${invoice.paymentMethod})", margin, y, leftPaint)
        canvas.drawText(Formatters.currency(invoice.amountPaid, settings.currencySymbol), widthPt - margin, y, rightPaint)
        y += 10f
        if (invoice.remainingBalance > 0) {
            canvas.drawText("Balance Due", margin, y, boldLeft)
            canvas.drawText(Formatters.currency(invoice.remainingBalance, settings.currencySymbol), widthPt - margin, y, boldRight)
            y += 11f
        }

        if (qrBmp != null) {
            y += 6f
            canvas.drawText("Scan PhonePe / UPI QR", widthPt / 2f, y, centerNorm)
            y += 6f
            val qx = (widthPt - qrSize) / 2f
            canvas.drawBitmap(qrBmp, null, RectF(qx, y, qx + qrSize, y + qrSize), Paint(Paint.FILTER_BITMAP_FLAG))
            y += qrSize + 10f
        }

        canvas.drawText(settings.invoiceFooterNote.take(48), widthPt / 2f, y, centerNorm)
        document.finishPage(page)
    }

    suspend fun generateBusinessReportPdf(
        context: Context,
        settings: BusinessSettingsEntity,
        title: String,
        periodLabel: String,
        summaryLines: List<Pair<String, String>>,
        tableRows: List<List<String>>
    ): File = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "reports").apply { mkdirs() }
        val outFile = File(dir, "Naqsh_Report_${Formatters.fileTimestamp()}.pdf")
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas

        val emeraldDeep = Color.parseColor("#021F1B")
        val champagneGold = Color.parseColor("#D4AF37")
        val warmIvory = Color.parseColor("#FAF6EE")
        canvas.drawColor(warmIvory)

        canvas.drawRect(0f, 0f, 595f, 88f, Paint().apply { color = emeraldDeep })
        val hdrGold = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = champagneGold
            textSize = 18f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        val hdrSub = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 10f
        }
        canvas.drawText("${settings.businessName} — $title", 28f, 36f, hdrGold)
        canvas.drawText("Period: $periodLabel  |  Generated: ${Formatters.dateTime(System.currentTimeMillis())}", 28f, 56f, hdrSub)
        canvas.drawText(AppStrings.BISMILLAH, 28f, 74f, hdrSub)

        var y = 114f
        val kPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#536661"); textSize = 10.5f }
        val vPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#13221E")
            textSize = 11f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        summaryLines.forEach { (k, v) ->
            canvas.drawText(k, 28f, y, kPaint)
            canvas.drawText(v, 567f, y, vPaint)
            y += 20f
        }

        y += 10f
        canvas.drawLine(28f, y, 567f, y, Paint().apply { color = champagneGold; strokeWidth = 1.5f })
        y += 18f

        val rowPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 9f }
        tableRows.take(32).forEach { cols ->
            canvas.drawText(cols.joinToString("  |  ").take(92), 28f, y, rowPaint)
            y += 16f
        }

        doc.finishPage(page)
        FileOutputStream(outFile).use { doc.writeTo(it) }
        doc.close()
        outFile
    }
}
