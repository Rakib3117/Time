package com.kazi.timegate

import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// ---- Monitored Apps (in MainActivity) ----
class MonitoredAppAdapter(
    private val packages: List<String>,
    private val prefs: SharedPreferences,
    private val onRemoved: () -> Unit
) : RecyclerView.Adapter<MonitoredAppAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = view.findViewById(R.id.monitoredAppName)
        val removeBtn: ImageButton = view.findViewById(R.id.removeAppBtn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_monitored_app, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val pkg = packages[position]
        val pm = holder.itemView.context.packageManager
        val appName = try {
            val info = pm.getApplicationInfo(pkg, 0)
            pm.getApplicationLabel(info).toString()
        } catch (e: Exception) { pkg }

        holder.nameText.text = appName
        holder.removeBtn.setOnClickListener {
            AppBlockService.removeMonitoredApp(prefs, pkg)
            onRemoved()
        }
    }

    override fun getItemCount() = packages.size
}

// ---- App Selection List ----
class AppSelectAdapter(
    private val apps: List<AppInfo>,
    private val prefs: SharedPreferences,
    private val onToggle: (String, Boolean) -> Unit
) : RecyclerView.Adapter<AppSelectAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.selectAppIcon)
        val name: TextView = view.findViewById(R.id.selectAppName)
        val addBtn: ImageButton = view.findViewById(R.id.selectAppToggleBtn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_select, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val app = apps[position]
        val isMonitored = AppBlockService.getMonitoredApps(prefs).contains(app.packageName)

        holder.icon.setImageDrawable(app.icon)
        holder.name.text = app.appName

        if (isMonitored) {
            holder.addBtn.setImageResource(android.R.drawable.ic_menu_delete)
        } else {
            holder.addBtn.setImageResource(android.R.drawable.ic_input_add)
        }

        holder.addBtn.setOnClickListener {
            val nowMonitored = AppBlockService.getMonitoredApps(prefs).contains(app.packageName)
            onToggle(app.packageName, !nowMonitored)
            notifyItemChanged(position)
        }
    }

    override fun getItemCount() = apps.size
}
