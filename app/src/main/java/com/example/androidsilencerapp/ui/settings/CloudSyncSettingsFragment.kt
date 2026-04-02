package com.example.androidsilencerapp.ui.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.androidsilencerapp.databinding.FragmentCloudSyncSettingsBinding
import com.example.androidsilencerapp.ui.dashboard.DashboardViewModel

class CloudSyncSettingsFragment : Fragment() {

    private var _binding: FragmentCloudSyncSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DashboardViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCloudSyncSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
        
        // Initial state
        binding.switchAutoSync.isChecked = prefs.getBoolean("auto_sync", true)
        binding.switchWifiOnly.isChecked = prefs.getBoolean("wifi_only", false)

        binding.switchAutoSync.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("auto_sync", isChecked).apply()
        }

        binding.switchWifiOnly.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("wifi_only", isChecked).apply()
        }

        binding.btnManualSync.setOnClickListener {
            viewModel.syncProfiles()
            Toast.makeText(context, "Syncing...", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
