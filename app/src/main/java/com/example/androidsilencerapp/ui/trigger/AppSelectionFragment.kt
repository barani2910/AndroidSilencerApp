package com.example.androidsilencerapp.ui.trigger

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.androidsilencerapp.databinding.FragmentAppSelectionBinding
import com.example.androidsilencerapp.ui.profile.ProfileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppSelectionFragment : Fragment() {

    private var _binding: FragmentAppSelectionBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by activityViewModels()
    private lateinit var adapter: AppAdapter
    private val selectedPackages = mutableSetOf<String>()
    private val TAG = "AppSelectionFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val pm = requireContext().packageManager
        adapter = AppAdapter(pm) { appInfo ->
            if (appInfo.isSelected) {
                selectedPackages.add(appInfo.packageName)
            } else {
                selectedPackages.remove(appInfo.packageName)
            }
            Log.d(TAG, "Selected apps count: ${selectedPackages.size}")
        }

        binding.rvApps.layoutManager = LinearLayoutManager(context)
        binding.rvApps.adapter = adapter

        loadApps()

        binding.etSearch.addTextChangedListener { text ->
            adapter.filter(text.toString())
        }

        binding.btnSaveApps.setOnClickListener {
            // Filter out any potential empty entries and join
            val packageListString = selectedPackages.filter { it.isNotBlank() }.joinToString(",")
            
            val current = viewModel.currentProfile.value
            if (current != null) {
                val updated = current.copy(
                    packageNames = packageListString,
                    triggerType = "APP"
                )
                viewModel.updateCurrentProfile(updated)
                Log.d(TAG, "Saved package names: $packageListString")
                Toast.makeText(requireContext(), "Apps selection staged", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            } else {
                Log.e(TAG, "Current profile is null, cannot save apps")
                Toast.makeText(requireContext(), "Error saving selection", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadApps() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val apps = withContext(Dispatchers.IO) {
                try {
                    val pm = requireContext().packageManager
                    val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                    packages.filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
                        .map { AppInfo(it.packageName, it.loadLabel(pm).toString()) }
                        .sortedBy { it.name }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load apps", e)
                    emptyList()
                }
            }
            
            selectedPackages.clear()
            val alreadySelected = viewModel.currentProfile.value?.packageNames
                ?.split(",")
                ?.filter { it.isNotBlank() } ?: emptyList()
            
            apps.forEach { app ->
                if (alreadySelected.contains(app.packageName)) {
                    app.isSelected = true
                    selectedPackages.add(app.packageName)
                }
            }

            adapter.setApps(apps)
            binding.progressBar.visibility = View.GONE
            
            if (apps.isEmpty()) {
                Toast.makeText(requireContext(), "No third-party apps found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
