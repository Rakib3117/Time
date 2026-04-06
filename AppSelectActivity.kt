package com.kazi.timegate

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AppSelectActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_select)

        prefs = getSharedPreferences(AppBlockService.PREFS_NAME, Context.MODE_PRIVATE)

        val recyclerView = findViewById<RecyclerView>(R.id.appSelectList)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val installedApps = getInstalledApps()
        val adapter = AppSelectAdapter(installedApps, prefs) { packageName, added ->
            if (added) {
                AppBlockService.addMonitoredApp(prefs, packageName)
                Toast.makeText(this, "যোগ করা হয়েছে!", Toast.LENGTH_SHORT).show()
            } else {
                AppBlockService.removeMonitoredApp(prefs, packageName)
                Toast.makeText(this, "সরানো হয়েছে!", Toast.LENGTH_SHORT).show()
            }
        }
        recyclerView.adapter = adapter
    }

    private fun getInstalledApps(): List<AppInfo> {
        val pm = packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        return apps
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 } // only user-installed
            .filter { it.packageName != packageName } // exclude ourselves
            .map {
                AppInfo(
                    packageName = it.packageName,
                    appName = pm.getApplicationLabel(it).toString(),
                    icon = pm.getApplicationIcon(it)
                )
            }
            .sortedBy { it.appName }
    }
}

data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: android.graphics.drawable.Drawable
)
