package com.example.util.pdf

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object PrintAndShareHelper {

    fun getFileUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    fun sharePdfOnWhatsApp(
        context: Context,
        pdfFile: File,
        customerPhone: String,
        messageText: String
    ) {
        val uri = getFileUri(context, pdfFile)
        val pm = context.packageManager

        val whatsappPackage = listOf("com.whatsapp", "com.whatsapp.w4b").firstOrNull { pkg ->
            try {
                pm.getPackageInfo(pkg, 0)
                true
            } catch (_: PackageManager.NameNotFoundException) {
                false
            }
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, messageText)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (whatsappPackage != null) {
                setPackage(whatsappPackage)
            }
            val cleanDigits = customerPhone.filter { it.isDigit() }
            if (cleanDigits.length >= 10 && whatsappPackage != null) {
                val withCountry = if (cleanDigits.length == 10) "91$cleanDigits" else cleanDigits
                putExtra("jid", "$withCountry@s.whatsapp.net")
            }
        }

        try {
            val chooser = Intent.createChooser(shareIntent, "Share Invoice PDF via WhatsApp / Apps")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (_: Exception) {
            // Fallback to generic chooser without package lock
            val fallback = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, messageText)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(fallback, "Share Invoice PDF"))
        }
    }

    fun shareFileGeneric(context: Context, file: File, mimeType: String, title: String) {
        val uri = getFileUri(context, file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, title))
    }

    fun printPdfFile(context: Context, pdfFile: File, jobName: String) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (printManager == null) {
            Toast.makeText(context, "Print service unavailable on this device.", Toast.LENGTH_SHORT).show()
            return
        }

        val adapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder(pdfFile.name)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                    .build()
                callback.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback
            ) {
                try {
                    FileInputStream(pdfFile).use { input ->
                        FileOutputStream(destination.fileDescriptor).use { output ->
                            input.copyTo(output)
                        }
                    }
                    if (cancellationSignal?.isCanceled == true) {
                        callback.onWriteCancelled()
                    } else {
                        callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                    }
                } catch (e: Exception) {
                    callback.onWriteFailed(e.message)
                }
            }
        }
        printManager.print(jobName, adapter, PrintAttributes.Builder().build())
    }

    fun launchUpiLink(context: Context, upiId: String, payeeName: String, amount: Double, note: String) {
        if (upiId.isBlank()) {
            Toast.makeText(context, "Please configure your UPI ID in Payment Settings first.", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = Uri.Builder()
            .scheme("upi")
            .authority("pay")
            .appendQueryParameter("pa", upiId.trim())
            .appendQueryParameter("pn", payeeName)
            .appendQueryParameter("am", "%.2f".format(amount))
            .appendQueryParameter("cu", "INR")
            .appendQueryParameter("tn", note)
            .build()
        val intent = Intent(Intent.ACTION_VIEW, uri)
        try {
            context.startActivity(Intent.createChooser(intent, "Pay with PhonePe / UPI"))
        } catch (_: Exception) {
            Toast.makeText(context, "No UPI app installed to open payment link.", Toast.LENGTH_SHORT).show()
        }
    }
}
