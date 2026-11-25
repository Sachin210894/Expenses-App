package com.example.transactiontracker

import java.util.Locale

data class ParsedTransaction(
    val amount: String,
    val direction: String,
    val merchant: String,
    val reference: String,
    val currency: String = "INR"
)

object TransactionParser {
    private val amountRegex = Regex("""(?:₹|Rs\.?|INR)\s*([0-9,]+(?:\.[0-9]{1,2})?)|(?:debited|credited|paid|received|spent|sent|transferred)[^0-9]{0,35}([0-9,]+(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE)
    private val refRegex = Regex("""(?:UPI Ref(?:erence)?|UTR|RRN|Txn(?:\s*ID)?|Transaction ID)\s*[:#-]?\s*([A-Za-z0-9-]{6,})""", RegexOption.IGNORE_CASE)
    private val merchantRegex = Regex("""(?:to|at|from|for)\s+([A-Za-z0-9 ._&@'-]{2,45})""", RegexOption.IGNORE_CASE)

    fun looksLikeTransaction(raw: String): Boolean {
        val s = raw.lowercase(Locale.ROOT)
        val hasMoney = amountRegex.containsMatchIn(raw) || Regex("""\b(?:debited|credited|withdrawn|deposited)\b""", RegexOption.IGNORE_CASE).containsMatchIn(raw)
        val hasSignal = listOf("upi", "a/c", "acct", "account", "debited", "credited", "received", "sent", "paid", "spent", "withdrawn", "deposited", "txn", "transaction", "neft", "imps", "rtgs", "pos").any { s.contains(it) }
        val marketing = listOf("offer", "cashback offer", "loan offer", "apply now", "sale ends").any { s.contains(it) }
        return hasMoney && hasSignal && !marketing
    }

    fun parse(raw: String): ParsedTransaction {
        val amountMatch = amountRegex.find(raw)
        val amount = (amountMatch?.groups?.get(1)?.value ?: amountMatch?.groups?.get(2)?.value ?: "").replace(",", "")
        val lower = raw.lowercase(Locale.ROOT)
        val direction = when {
            listOf("credited", "received", "deposited").any { lower.contains(it) } -> "CREDIT"
            listOf("debited", "paid", "spent", "sent", "withdrawn").any { lower.contains(it) } -> "DEBIT"
            else -> "UNKNOWN"
        }
        val ref = refRegex.find(raw)?.groupValues?.get(1).orEmpty()
        val merchant = merchantRegex.find(raw)?.groupValues?.get(1)?.trim()?.take(45).orEmpty()
        return ParsedTransaction(amount, direction, merchant, ref)
    }
}
