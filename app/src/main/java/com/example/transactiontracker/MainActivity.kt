package com.example.transactiontracker

import android.Manifest
import android.app.Activity
import android.os.Bundle
import android.content.pm.PackageManager
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

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
        layout.addView(TextView(this).apply {
            text = "Transaction Tracker"
            textSize = 24f
        })
        layout.addView(TextView(this).apply {
            text = "Paste your Google Apps Script Web App URL and secret token. Only messages that look like bank/UPI transaction alerts are processed."
            textSize = 14f
        })
        endpoint = EditText(this).apply {
            hint = "Google Apps Script Web App URL"
            setText(prefs.getString("endpoint", ""))
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_URI
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
            text = "Enable SMS access"
            setOnClickListener { requestSmsPermissions() }
        })
        status = TextView(this).apply { textSize = 14f }
        layout.addView(status)
        layout.addView(TextView(this).apply {
            text = "Privacy: SMS is filtered on this phone. Matching transaction alerts are sent to the Google Sheet endpoint you configure. No SMS inbox upload or background reading of old messages is performed."
            textSize = 12f
        })
        setContentView(ScrollView(this).apply { addView(layout) })
        updateStatus()
    }

    private fun requestSmsPermissions() {
        val missing = arrayOf(Manifest.permission.RECEIVE_SMS)
            .filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) ActivityCompat.requestPermissions(this, missing.toTypedArray(), 42)
        else updateStatus()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        updateStatus()
        if (grantResults.any { it != PackageManager.PERMISSION_GRANTED }) {
            Toast.makeText(this, "SMS permission is needed to detect incoming transaction alerts.", Toast.LENGTH_LONG).show()
        }
    }

    private fun updateStatus() {
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        status.text = if (granted) "SMS detection permission: enabled" else "SMS detection permission: not enabled"
    }
}
