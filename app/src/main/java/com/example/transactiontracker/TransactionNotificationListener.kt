package com.example.transactiontracker

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.work.*

class TransactionNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence("android.title")?.toString().orEmpty()
        val text = extras.getCharSequence("android.text")?.toString().orEmpty()
        val bigText = extras.getCharSequence("android.bigText")?.toString().orEmpty()
        val lines = extras.getCharSequenceArray("android.textLines")?.joinToString(" ").orEmpty()
        val notificationText = listOf(title, text, bigText, lines)
            .filter { it.isNotBlank() }.joinToString(" ")
        if (notificationText.isBlank()) return

        // Parse in memory only. Never persist or upload notification text or app identity.
        if (!TransactionParser.looksLikeTransaction(notificationText)) return
        val parsed = TransactionParser.parse(notificationText)
        if (parsed.amount.isBlank() || parsed.direction !in listOf("CREDIT", "DEBIT")) return

        val data = workDataOf(
            "amount" to parsed.amount,
            "direction" to parsed.direction,
            "timestamp" to sbn.postTime
        )
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setInputData(data)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        WorkManager.getInstance(applicationContext).enqueue(request)
    }
}
