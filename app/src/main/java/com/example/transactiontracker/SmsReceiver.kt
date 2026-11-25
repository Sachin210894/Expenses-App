package com.example.transactiontracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.work.*

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val grouped = messages.groupBy { it.originatingAddress ?: "unknown" }
        grouped.forEach { (sender, parts) ->
            val body = parts.joinToString("") { it.messageBody ?: "" }
            if (TransactionParser.looksLikeTransaction(body)) {
                val data = workDataOf(
                    "sender" to sender,
                    "body" to body,
                    "timestamp" to (parts.firstOrNull()?.timestampMillis ?: System.currentTimeMillis())
                )
                val request = OneTimeWorkRequestBuilder<SyncWorker>()
                    .setInputData(data)
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .build()
                WorkManager.getInstance(context).enqueue(request)
            }
        }
    }
}
