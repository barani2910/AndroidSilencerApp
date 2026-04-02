package com.example.androidsilencerapp.ui.logs

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.androidsilencerapp.data.model.AutomationLog
import com.example.androidsilencerapp.databinding.ItemLogBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LogsAdapter : ListAdapter<AutomationLog, LogsAdapter.LogViewHolder>(DiffCallback) {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val binding = ItemLogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class LogViewHolder(private val binding: ItemLogBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(log: AutomationLog) {
            binding.tvLogAction.text = log.actionTaken
            binding.tvLogProfile.text = "Profile: ${log.profileName} (${log.triggerType})"
            binding.tvLogTime.text = dateFormat.format(Date(log.timestamp))
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<AutomationLog>() {
            override fun areItemsTheSame(oldItem: AutomationLog, newItem: AutomationLog): Boolean = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: AutomationLog, newItem: AutomationLog): Boolean = oldItem == newItem
        }
    }
}
