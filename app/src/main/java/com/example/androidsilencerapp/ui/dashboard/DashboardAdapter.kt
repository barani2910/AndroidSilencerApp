package com.example.androidsilencerapp.ui.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.androidsilencerapp.data.model.Profile
import com.example.androidsilencerapp.databinding.ItemProfileBinding

class DashboardAdapter(
    private val onProfileClicked: (Profile) -> Unit,
    private val onStatusToggled: (Profile) -> Unit
) : ListAdapter<Profile, DashboardAdapter.ProfileViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfileViewHolder {
        val binding = ItemProfileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProfileViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProfileViewHolder, position: Int) {
        val profile = getItem(position)
        holder.bind(profile)
        holder.itemView.setOnClickListener { onProfileClicked(profile) }
        holder.binding.switchStatus.setOnClickListener { onStatusToggled(profile) }
    }

    inner class ProfileViewHolder(val binding: ItemProfileBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(profile: Profile) {
            binding.tvProfileName.text = profile.name
            binding.tvTriggerType.text = "Trigger: ${profile.triggerType}"
            binding.tvPriority.text = "Priority: ${profile.priorityLevel}"
            binding.switchStatus.isChecked = profile.isActive
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<Profile>() {
            override fun areItemsTheSame(oldItem: Profile, newItem: Profile): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Profile, newItem: Profile): Boolean {
                return oldItem == newItem
            }
        }
    }
}
