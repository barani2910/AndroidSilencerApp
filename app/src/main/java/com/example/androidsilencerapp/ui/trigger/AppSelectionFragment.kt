package com.example.androidsilencerapp.ui.trigger

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        }

        binding.rvApps.layoutManager = LinearLayoutManager(context)
        binding.rvApps.adapter = adapter

        loadApps()

        binding.etSearch.addTextChangedListener { text ->
            adapter.filter(text.toString())
        }

        binding.btnSaveApps.setOnClickListener {
            val packageListString = selectedPackages.joinToString(",")
            val current = viewModel.currentProfile.value
            current?.let {
                viewModel.updateCurrentProfile(it.copy(
                    packageNames = packageListString,
                    triggerType = "APP"
                ))
            }
            findNavController().popBackStack()
        }
    }

    private fun loadApps() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val apps = withContext(Dispatchers.IO) {
                val pm = requireContext().packageManager
                val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                packages.filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
                    .map { AppInfo(it.packageName, it.loadLabel(pm).toString()) }
                    .sortedBy { it.name }
            }
            
            // Restore previously selected apps from ViewModel if any
            val alreadySelected = viewModel.currentProfile.value?.packageNames?.split(",") ?: emptyList()
            apps.forEach { app ->
                if (alreadySelected.contains(app.packageName)) {
                    app.isSelected = true
                    selectedPackages.add(app.packageName)
                }
            }

            adapter.setApps(apps)
            binding.progressBar.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
