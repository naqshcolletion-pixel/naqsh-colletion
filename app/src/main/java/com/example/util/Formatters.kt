package com.example.util

import java.security.MessageDigest
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Formatters {
    private val indianLocale = Locale("en", "IN")

    fun currency(amount: Double, symbol: String = "₹"): String {
        val nf = NumberFormat.getNumberInstance(indianLocale).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        val sign = if (amount < 0) "-" else ""
        return "$sign$symbol${nf.format(kotlin.math.abs(amount))}"
    }

    fun currencyCompact(amount: Double, symbol: String = "₹"): String {
        val nf = NumberFormat.getNumberInstance(indianLocale).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 0
        }
        val sign = if (amount < 0) "-" else ""
        return "$sign$symbol${nf.format(kotlin.math.abs(amount))}"
    }

    fun dateTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH)
        return sdf.format(Date(timestamp))
    }

    fun dateOnly(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
        return sdf.format(Date(timestamp))
    }

    fun fileTimestamp(timestamp: Long = System.currentTimeMillis()): String {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH)
        return sdf.format(Date(timestamp))
    }

    fun hashPin(pin: String): String {
        val salted = "naqsh_boutique_salt::$pin"
        val digest = MessageDigest.getInstance("SHA-256").digest(salted.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun profitMarginPercent(purchasePrice: Double, sellingPrice: Double): Double {
        if (sellingPrice <= 0.0) return 0.0
        return ((sellingPrice - purchasePrice) / sellingPrice) * 100.0
    }
}
