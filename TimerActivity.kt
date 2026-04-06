package com.kazi.timegate

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class TimerActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var targetPackage: String
    private var selectedMinutes = 15 // default

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_timer)

        prefs = getSharedPreferences(AppBlockService.PREFS_NAME, Context.MODE_PRIVATE)
        targetPackage = intent.getStringExtra("target_package") ?: run {
            finish()
            return
        }

        // Show app name
        val appNameText = findViewById<TextView>(R.id.appNameText)
        appNameText.text = getAppName(targetPackage)

        // Time selector buttons
        val btn5 = findViewById<Button>(R.id.btn5min)
        val btn10 = findViewById<Button>(R.id.btn10min)
        val btn15 = findViewById<Button>(R.id.btn15min)
        val btn30 = findViewById<Button>(R.id.btn30min)
        val btn60 = findViewById<Button>(R.id.btn60min)
        val selectedTimeText = findViewById<TextView>(R.id.selectedTimeText)

        fun selectTime(minutes: Int) {
            selectedMinutes = minutes
            selectedTimeText.text = "$minutes মিনিট"
            listOf(btn5, btn10, btn15, btn30, btn60).forEach { it.isSelected = false }
            when (minutes) {
                5 -> btn5.isSelected = true
                10 -> btn10.isSelected = true
                15 -> btn15.isSelected = true
                30 -> btn30.isSelected = true
                60 -> btn60.isSelected = true
            }
        }

        // Default
        selectTime(15)

        btn5.setOnClickListener { selectTime(5) }
        btn10.setOnClickListener { selectTime(10) }
        btn15.setOnClickListener { selectTime(15) }
        btn30.setOnClickListener { selectTime(30) }
        btn60.setOnClickListener { selectTime(60) }

        // Start button
        val startBtn = findViewById<Button>(R.id.startBtn)
        startBtn.setOnClickListener {
            val durationMs = selectedMinutes * 60 * 1000L
            AppBlockService.setSessionTime(prefs, targetPackage, durationMs)

            Toast.makeText(this, "$selectedMinutes মিনিট শুরু হলো!", Toast.LENGTH_SHORT).show()

            // Launch the target app
            val launchIntent = packageManager.getLaunchIntentForPackage(targetPackage)
            if (launchIntent != null) {
                launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(launchIntent)
            }
            finish()
        }

        // Cancel button — go home
        val cancelBtn = findViewById<Button>(R.id.cancelBtn)
        cancelBtn.setOnClickListener {
            goHome()
            finish()
        }
    }

    override fun onBackPressed() {
        goHome()
        super.onBackPressed()
    }

    private fun goHome() {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
    }

    private fun getAppName(packageName: String): String {
        return try {
            val info = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(info).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }
}
