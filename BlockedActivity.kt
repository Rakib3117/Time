package com.kazi.timegate

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class BlockedActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var targetPackage: String
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var countdownText: TextView
    private lateinit var progressBar: ProgressBar

    private val updateRunnable = object : Runnable {
        override fun run() {
            val remaining = AppBlockService.getCooldownRemainingMs(prefs, targetPackage)
            if (remaining <= 0) {
                // Cooldown done
                countdownText.text = "✅ Cooldown শেষ!"
                progressBar.progress = 100
                handler.postDelayed({ finish() }, 1000)
            } else {
                val totalMs = AppBlockService.COOLDOWN_DURATION_MS
                val elapsed = totalMs - remaining
                val progress = ((elapsed.toFloat() / totalMs) * 100).toInt()
                progressBar.progress = progress

                val minutes = (remaining / 60000).toInt()
                val seconds = ((remaining % 60000) / 1000).toInt()
                countdownText.text = String.format("%02d:%02d", minutes, seconds)

                handler.postDelayed(this, 500)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blocked)

        prefs = getSharedPreferences(AppBlockService.PREFS_NAME, Context.MODE_PRIVATE)
        targetPackage = intent.getStringExtra("target_package") ?: run {
            finish()
            return
        }

        val appNameText = findViewById<TextView>(R.id.blockedAppName)
        appNameText.text = getAppName(targetPackage)

        countdownText = findViewById(R.id.countdownText)
        progressBar = findViewById(R.id.cooldownProgress)

        val homeBtn = findViewById<Button>(R.id.goHomeBtn)
        homeBtn.setOnClickListener {
            goHome()
        }

        handler.post(updateRunnable)
    }

    override fun onBackPressed() {
        goHome()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateRunnable)
    }

    private fun goHome() {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
        finish()
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
