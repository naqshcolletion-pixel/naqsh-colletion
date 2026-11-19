package com.example.util

enum class AppLanguage(val code: String, val displayName: String) {
    ENGLISH("EN", "Simple English"),
    ROMAN_URDU("UR", "Roman Urdu"),
    HINDI("HI", "हिन्दी (Hindi)")
}

object AppStrings {
    const val BISMILLAH = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ"

    fun navHome(lang: String) = when (lang) {
        "UR" -> "Home"
        "HI" -> "होम"
        else -> "HOME"
    }

    fun navInventory(lang: String) = when (lang) {
        "UR" -> "Maal / Stock"
        "HI" -> "स्टॉक"
        else -> "INVENTORY"
    }

    fun navPos(lang: String) = when (lang) {
        "UR" -> "Bill / POS"
        "HI" -> "बिलिंग"
        else -> "SALES / POS"
    }

    fun navAccounts(lang: String) = when (lang) {
        "UR" -> "Hisab Kitab"
        "HI" -> "खाता-बही"
        else -> "ACCOUNTS"
    }

    fun navMore(lang: String) = when (lang) {
        "UR" -> "Mazeed"
        "HI" -> "अधिक"
        else -> "MORE"
    }

    fun quickNewSale(lang: String) = when (lang) {
        "UR" -> "Naya Bill"
        "HI" -> "नया बिल"
        else -> "New Sale"
    }

    fun quickAddProduct(lang: String) = when (lang) {
        "UR" -> "Naya Product"
        "HI" -> "नया उत्पाद"
        else -> "Add Product"
    }

    fun quickAddPurchase(lang: String) = when (lang) {
        "UR" -> "Kharidari"
        "HI" -> "खरीदारी"
        else -> "Add Purchase"
    }

    fun quickAddExpense(lang: String) = when (lang) {
        "UR" -> "Kharch Likhein"
        "HI" -> "खर्च जोड़ें"
        else -> "Add Expense"
    }

    fun quickAddCustomer(lang: String) = when (lang) {
        "UR" -> "Grahak Jodein"
        "HI" -> "ग्राहक जोड़ें"
        else -> "Add Customer"
    }

    fun quickReports(lang: String) = when (lang) {
        "UR" -> "Reports"
        "HI" -> "रिपोर्ट्स"
        else -> "Reports"
    }

    fun todaySales(lang: String) = when (lang) {
        "UR" -> "Aaj Ki Bikri"
        "HI" -> "आज की बिक्री"
        else -> "Today's Sales"
    }

    fun todayProfit(lang: String) = when (lang) {
        "UR" -> "Aaj Ka Munafa"
        "HI" -> "आज का लाभ"
        else -> "Today's Net"
    }

    fun lowStockAlert(lang: String) = when (lang) {
        "UR" -> "Kam Stock Alert"
        "HI" -> "कम स्टॉक चेतावनी"
        else -> "Low Stock"
    }

    fun customerDue(lang: String) = when (lang) {
        "UR" -> "Grahak Baqaya"
        "HI" -> "ग्राहक बकाया"
        else -> "Receivable"
    }

    fun shareOnWhatsApp(lang: String) = when (lang) {
        "UR" -> "WhatsApp Par PDF Bhejein"
        "HI" -> "WhatsApp पर PDF भेजें"
        else -> "SHARE ON WHATSAPP"
    }

    fun printReceipt(lang: String) = when (lang) {
        "UR" -> "Print Karein"
        "HI" -> "प्रिंट करें"
        else -> "PRINT INVOICE"
    }

    fun thankYouMessage(lang: String) = when (lang) {
        "UR" -> "NAQSH COLLECTION se kharidari ka shukriya. JazakAllahu Khayran!"
        "HI" -> "NAQSH COLLECTION से खरीदारी के लिए धन्यवाद। जज़ाकअल्लाहु खैरन!"
        else -> "Thank you for shopping with NAQSH COLLECTION. JazakAllahu Khayran!"
    }
}
