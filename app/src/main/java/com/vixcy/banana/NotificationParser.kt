package com.vixcy.banana

data class Transaction(
    val amount: Double,
    val merchant: String,
    val date: String,
    val accountLast4: String,
    val rawSms: String,
    val upiRef: String = ""
)

object NotificationParser {

    private val UPI_APPS = listOf(
        "com.phonepe.app",
        "net.one97.paytm",
        "com.google.android.apps.nbu.paisa.user",
        "in.org.npci.upiapp",
        "com.amazon.mShop.android.shopping"
    )

    fun isUpiApp(packageName: String): Boolean {
        return UPI_APPS.any { packageName.contains(it) }
    }

    fun isBankSms(sender: String, body: String): Boolean {
        val bankKeywords = listOf(
            "HDFCBK", "SBIINB", "ICICIB", "AXISBK", "KOTAK", "PNBSMS",
            "YESBNK", "IDFCBK", "SBIUPI", "SBIPSG", "SBIBNK"
        )
        val txKeywords = listOf("debited", "credited", "spent", "payment", "transferred", "trf")
        val otpKeywords = listOf("otp", "one time", "password")
        val isBank = bankKeywords.any { sender.uppercase().contains(it) }
        val isTx = txKeywords.any { body.lowercase().contains(it) }
        val isOtp = otpKeywords.any { body.lowercase().contains(it) }
        return isBank && isTx && !isOtp
    }

    fun parse(title: String?, text: String?): Transaction? {
        val content = "${title ?: ""} ${text ?: ""}".trim()
        if (content.isBlank()) return null
        val amount = extractAmount(content) ?: return null
        val merchant = extractMerchant(content)
        val date = getCurrentDate()
        return Transaction(amount, merchant, date, "UPI", content, "")
    }

    fun parseFromSms(body: String): Transaction? {
        val amount = extractAmount(body) ?: return null
        val merchant = extractMerchant(body)
        val date = getCurrentDate()
        val ref = extractRef(body)
        return Transaction(amount, merchant, date, "SMS", body, ref)
    }

    private fun extractAmount(text: String): Double? {
        val prefixRegex = Regex("(?:Rs\\.?|INR|₹)\\s?([\\d,]+(?:\\.\\d{1,2})?)", RegexOption.IGNORE_CASE)
        prefixRegex.find(text)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()?.let { return it }

        val debitedRegex = Regex("debited\\s+(?:by|with)\\s+([\\d,]+(?:\\.\\d{1,2})?)", RegexOption.IGNORE_CASE)
        debitedRegex.find(text)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()?.let { return it }

        val forRegex = Regex("for\\s+([\\d,]+(?:\\.\\d{1,2})?)\\s", RegexOption.IGNORE_CASE)
        forRegex.find(text)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()?.let { return it }

        return null
    }

    private fun extractMerchant(text: String): String {
        val trfRegex = Regex("trf\\s+to\\s+([A-Za-z ]+?)(?:\\s+Ref|\\s+ref|\\s+on|\\.|$)", RegexOption.IGNORE_CASE)
        trfRegex.find(text)?.groupValues?.get(1)?.trim()?.let {
            if (it.isNotBlank() && !it.uppercase().contains("DEAR")) return it.uppercase()
        }

        val toRegex = Regex("(?:paid to|sent to|payment to|paying to)\\s+([A-Za-z0-9 _\\-\\.&]+?)(?:\\s+via|\\s+on|\\s+ref|\\.|$)", RegexOption.IGNORE_CASE)
        toRegex.find(text)?.groupValues?.get(1)?.trim()?.let {
            if (it.isNotBlank()) return it.uppercase()
        }

        val upiRegex = Regex("to\\s+([A-Za-z0-9]+?)(?:@|\\s+via|\\s+UPI)", RegexOption.IGNORE_CASE)
        upiRegex.find(text)?.groupValues?.get(1)?.trim()?.let {
            if (it.length > 2) return it.uppercase()
        }

        return "Unknown"
    }

    private fun extractRef(text: String): String {
        val regex = Regex("(?:Ref|Refno|ref no|UPI Ref)[\\s:no]*([0-9]+)", RegexOption.IGNORE_CASE)
        return regex.find(text)?.groupValues?.get(1) ?: ""
    }

    private fun getCurrentDate(): String {
        val sdf = java.text.SimpleDateFormat("dd-MM-yy", java.util.Locale.getDefault())
        return sdf.format(java.util.Date())
    }

    fun categorize(merchant: String): String {
        val m = merchant.uppercase()
        return when {
            m.containsAny("ZOMATO", "SWIGGY", "DOMINO", "MCDONALD", "FOOD", "RESTAURANT", "CAFE") -> "Food"
            m.containsAny("OLA", "UBER", "RAPIDO", "METRO", "IRCTC", "TRAIN", "BUS", "FLIGHT") -> "Transport"
            m.containsAny("AMAZON", "FLIPKART", "MYNTRA", "AJIO", "MEESHO", "SHOP") -> "Shopping"
            m.containsAny("NETFLIX", "SPOTIFY", "PRIME", "HOTSTAR", "YOUTUBE") -> "Entertainment"
            m.containsAny("ELECTRICITY", "WATER", "GAS", "BILL", "RECHARGE", "BROADBAND") -> "Utilities"
            m.containsAny("PHARMACY", "MEDIC", "HOSPITAL", "CLINIC", "HEALTH") -> "Health"
            else -> "Other"
        }
    }

    private fun String.containsAny(vararg keywords: String): Boolean {
        return keywords.any { this.contains(it) }
    }
}