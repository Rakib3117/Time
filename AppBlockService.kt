package com.kazi.timegate

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.view.accessibility.AccessibilityEvent
import java.util.concurrent.ConcurrentHashMap

class AppBlockService : AccessibilityService() {

    companion object {
        const val PREFS_NAME = "timegate_prefs"
        const val KEY_MONITORED_APPS = "monitored_apps"
        const val KEY_COOLDOWN_SUFFIX = "_cooldown_until"
        const val KEY_SESSION_END_SUFFIX = "_session_end"
        const val COOLDOWN_DURATION_MS = 5 * 60 * 1000L // 5 minutes

        // Track which apps we've already shown the timer for this session
        private val activeTimers = ConcurrentHashMap<String, Long>() // pkg -> session end time

        fun getMonitoredApps(prefs: SharedPreferences): List<String> {
            val json = prefs.getString(KEY_MONITORED_APPS, "[]") ?: "[]"
            val result = mutableListOf<String>()
            val arr = org.json.JSONArray(json)
            for (i in 0 until arr.length()) result.add(arr.getString(i))
            return result
        }

        fun addMonitoredApp(prefs: SharedPreferences, packageName: String) {
            val apps = getMonitoredApps(prefs).toMutableList()
            if (!apps.contains(packageName)) {
                apps.add(packageName)
                prefs.edit().putString(KEY_MONITORED_APPS, org.json.JSONArray(apps).toString()).apply()
            }
        }

        fun removeMonitoredApp(prefs: SharedPreferences, packageName: String) {
            val apps = getMonitoredApps(prefs).toMutableList()
            apps.remove(packageName)
            prefs.edit().putString(KEY_MONITORED_APPS, org.json.JSONArray(apps).toString()).apply()
            // Clear any timer state
            prefs.edit()
                .remove(packageName + KEY_COOLDOWN_SUFFIX)
                .remove(packageName + KEY_SESSION_END_SUFFIX)
                .apply()
            activeTimers.remove(packageName)
        }

        fun setSessionTime(prefs: SharedPreferences, packageName: String, durationMs: Long) {
            val sessionEndTime = System.currentTimeMillis() + durationMs
            prefs.edit().putLong(packageName + KEY_SESSION_END_SUFFIX, sessionEndTime).apply()
            activeTimers[packageName] = sessionEndTime
        }

        fun isInCooldown(prefs: SharedPreferences, packageName: String): Boolean {
            val cooldownUntil = prefs.getLong(packageName + KEY_COOLDOWN_SUFFIX, 0)
            return System.currentTimeMillis() < cooldownUntil
        }

        fun getCooldownRemainingMs(prefs: SharedPreferences, packageName: String): Long {
            val cooldownUntil = prefs.getLong(packageName + KEY_COOLDOWN_SUFFIX, 0)
            return maxOf(0, cooldownUntil - System.currentTimeMillis())
        }

        fun startCooldown(prefs: SharedPreferences, packageName: String) {
            val until = System.currentTimeMillis() + COOLDOWN_DURATION_MS
            prefs.edit().putLong(packageName + KEY_COOLDOWN_SUFFIX, until).apply()
            activeTimers.remove(packageName)
            // Also clear session
            prefs.edit().remove(packageName + KEY_SESSION_END_SUFFIX).apply()
        }

        fun hasActiveSession(prefs: SharedPreferences, packageName: String): Boolean {
            val sessionEnd = activeTimers[packageName]
                ?: prefs.getLong(packageName + KEY_SESSION_END_SUFFIX, 0L)
            if (sessionEnd == 0L) return false
            return System.currentTimeMillis() < sessionEnd
        }

        fun clearSession(packageName: String) {
            activeTimers.remove(packageName)
        }
    }

    private lateinit var prefs: SharedPreferences
    private var lastHandledPkg = ""
    private var lastHandledTime = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return

        // Ignore our own app
        if (pkg == packageName) return
        // Ignore system UI
        if (pkg == "com.android.systemui" || pkg == "android") return

        // Debounce — same app within 1 second
        val now = System.currentTimeMillis()
        if (pkg == lastHandledPkg && now - lastHandledTime < 1000) return

        val monitoredApps = getMonitoredApps(prefs)
        if (!monitoredApps.contains(pkg)) return

        lastHandledPkg = pkg
        lastHandledTime = now

        when {
            // 1. App is in cooldown — send to blocked screen
            isInCooldown(prefs, pkg) -> {
                showBlockedScreen(pkg)
            }

            // 2. App has an active session — let them in
            hasActiveSession(prefs, pkg) -> {
                // Valid session, do nothing
            }

            // 3. Session just expired (was tracked but now done) — start cooldown
            prefs.getLong(pkg + KEY_SESSION_END_SUFFIX, 0L) > 0L -> {
                // There was a session but it expired
                startCooldown(prefs, pkg)
                showBlockedScreen(pkg)
            }

            // 4. No session at all — show timer setup
            else -> {
                showTimerScreen(pkg)
            }
        }
    }

    private fun showTimerScreen(packageName: String) {
        val intent = Intent(this, TimerActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("target_package", packageName)
        }
        startActivity(intent)
    }

    private fun showBlockedScreen(packageName: String) {
        val intent = Intent(this, BlockedActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("target_package", packageName)
        }
        startActivity(intent)
    }

    override fun onInterrupt() {}
}
