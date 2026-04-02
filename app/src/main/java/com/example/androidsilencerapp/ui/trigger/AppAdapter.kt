package com.example.androidsilencerapp.ui.trigger

import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.androidsilencerapp.databinding.ItemAppBinding

data class AppInfo(
    val packageName: String,
    val name: String,
    var isSelected: Boolean = false
)

class AppAdapter(
    private val packageManager: PackageManager,
    private val onAppSelected: (AppInfo) -> Unit
) : RecyclerView.Adapter<AppAdapter.AppViewHolder>() {

    private var apps = listOf<AppInfo>()
    private var filteredApps = listOf<AppInfo>()

    fun setApps(newApps: List<AppInfo>) {
        apps = newApps
        filteredApps = newApps
        notifyDataSetChanged()
    }

    fun filter(query: String) {
        filteredApps = if (query.isEmpty()) {
            apps
        } else {
            apps.filter { it.name.contains(query, ignoreCase = true) }
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val binding = ItemAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AppViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = filteredApps[position]
        holder.bind(app)
    }

    override fun getItemCount(): Int = filteredApps.size

    inner class AppViewHolder(private val binding: ItemAppBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(app: AppInfo) {
            binding.tvAppName.text = app.name
            try {
                binding.ivAppIcon.setImageDrawable(packageManager.getApplicationIcon(app.packageName))
            } catch (e: Exception) {
                binding.ivAppIcon.setImageResource(android.R.drawable.sym_def_app_icon)
            }
            binding.cbSelected.isChecked = app.isSelected
            binding.root.setOnClickListener {
                app.isSelected = !app.isSelected
                binding.cbSelected.isChecked = app.isSelected
                onAppSelected(app)
            }
        }
    }
}
