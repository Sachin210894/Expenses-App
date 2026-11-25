package com.example.transactiontracker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val prefs = applicationContext.getSharedPreferences("tracker", Context.MODE_PRIVATE)
        val endpoint = prefs.getString("endpoint", "").orEmpty()
        val token = prefs.getString("token", "").orEmpty()
        if (endpoint.isBlank() || token.isBlank()) return@withContext Result.failure()

        val amount = inputData.getString("amount").orEmpty()
        val direction = inputData.getString("direction").orEmpty()
        val timestamp = inputData.getLong("timestamp", System.currentTimeMillis())
        if (amount.isBlank() || direction !in listOf("CREDIT", "DEBIT")) return@withContext Result.success()

        val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).format(Date(timestamp))
        val id = sha256("$timestamp|$amount|$direction")
        try {
            val params = linkedMapOf(
                "token" to token,
                "id" to id,
                "date" to date,
                "amount" to amount,
                "direction" to direction
            )
            val postData = params.entries.joinToString("&") {
                URLEncoder.encode(it.key, "UTF-8") + "=" + URLEncoder.encode(it.value, "UTF-8")
            }
            val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 15000
                readTimeout = 15000
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            }
            conn.outputStream.use { it.write(postData.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            conn.disconnect()
            if (code in 200..299) Result.success() else Result.retry()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
}
