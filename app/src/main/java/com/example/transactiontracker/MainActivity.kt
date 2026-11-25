package com.example.transactiontracker

import android.app.Activity
import android.os.Bundle
import android.provider.Settings
import android.content.Intent
import android.text.InputType
import android.widget.*

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("tracker", MODE_PRIVATE) }
    private lateinit var endpoint: EditText
    private lateinit var token: EditText
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 48, 36, 36)
        }
        layout.addView(TextView(this).apply { text = "Transaction Tracker"; textSize = 24f })
        layout.addView(TextView(this).apply {
            text = "Enter your Google Apps Script Web App URL and secret token. The app checks transaction notifications and sends only date, amount and debit/credit to your sheet."
            textSize = 14f
        })
        endpoint = EditText(this).apply {
            hint = "Google Apps Script Web App URL"
            setText(prefs.getString("endpoint", ""))
            inputType = InputType.TYPE_TEXT_VARIATION_URI
        }
        token = EditText(this).apply {
            hint = "Secret token"
            setText(prefs.getString("token", ""))
        }
        layout.addView(endpoint)
        layout.addView(token)
        layout.addView(Button(this).apply {
            text = "Save settings"
            setOnClickListener {
                prefs.edit().putString("endpoint", endpoint.text.toString().trim())
                    .putString("token", token.text.toString().trim()).apply()
                Toast.makeText(this@MainActivity, "Settings saved", Toast.LENGTH_SHORT).show()
            }
        })
        layout.addView(Button(this).apply {
            text = "Enable notification access"
            setOnClickListener { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
        })
        status = TextView(this).apply { textSize = 14f }
        layout.addView(status)
        layout.addView(TextView(this).apply {
            text = "Privacy: notification text is processed temporarily on this device. Only transaction date, amount and debit/credit are sent to Google Sheets. Notification access is broad Android access; the app filters transaction alerts and does not save notification text."
            textSize = 12f
        })
        setContentView(ScrollView(this).apply { addView(layout) })
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        val enabled = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
            ?.contains(packageName) == true
        status.text = if (enabled) "Notification access: enabled" else "Notification access: not enabled"
    }
}
