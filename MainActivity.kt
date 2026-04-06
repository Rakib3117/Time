package com.kazi.timegate

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var statusText: TextView
    private lateinit var enableBtn: Button
    private lateinit var addAppBtn: Button
    private lateinit var appListView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("timegate_prefs", Context.MODE_PRIVATE)

        statusText = findViewById(R.id.statusText)
        enableBtn = findViewById(R.id.enableAccessibilityBtn)
        addAppBtn = findViewById(R.id.addAppBtn)
        appListView = findViewById(R.id.appListView)

        appListView.layoutManager = LinearLayoutManager(this)

        enableBtn.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        addAppBtn.setOnClickListener {
            startActivity(Intent(this, AppSelectActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun updateUI() {
        val isEnabled = isAccessibilityServiceEnabled()
        if (isEnabled) {
            statusText.text = "✅ TimeGate সক্রিয়"
            statusText.setTextColor(getColor(R.color.green))
            enableBtn.text = "Accessibility চালু আছে"
            enableBtn.isEnabled = false
        } else {
            statusText.text = "⚠️ Accessibility Service বন্ধ"
            statusText.setTextColor(getColor(R.color.red))
            enableBtn.text = "Accessibility চালু করুন"
            enableBtn.isEnabled = true
        }

        refreshAppList()
    }

    private fun refreshAppList() {
        val monitoredApps = AppBlockService.getMonitoredApps(prefs)
        val adapter = MonitoredAppAdapter(monitoredApps, prefs) {
            refreshAppList()
        }
        appListView.adapter = adapter
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        return enabledServices.any {
            it.resolveInfo.serviceInfo.packageName == packageName &&
            it.resolveInfo.serviceInfo.name == AppBlockService::class.java.name
        }
    }
}
