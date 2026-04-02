package com.example.androidsilencerapp.ui.profile

import android.app.NotificationManager
import android.media.AudioManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.androidsilencerapp.R
import com.example.androidsilencerapp.data.local.AppDatabase
import com.example.androidsilencerapp.data.model.Profile
import com.example.androidsilencerapp.databinding.FragmentAddProfileBinding
import kotlinx.coroutines.launch

class AddProfileFragment : Fragment() {

    private var _binding: FragmentAddProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by activityViewModels()
    private val args: AddProfileFragmentArgs by navArgs()

    companion object {
        const val MODE_DND = 3
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val profileId = args.profileId
        if (viewModel.currentProfile.value?.id == "" || (profileId != null && viewModel.currentProfile.value?.id != profileId)) {
            if (profileId != null) {
                loadProfileForEditing(profileId)
            }
        }

        viewModel.currentProfile.observe(viewLifecycleOwner) { profile ->
            updateUI(profile)
        }

        viewModel.saveStatus.observe(viewLifecycleOwner) { status ->
            when (status) {
                is ProfileViewModel.SaveStatus.Loading -> {
                    binding.btnSaveProfile.isEnabled = false
                    binding.btnSaveProfile.text = "Saving..."
                }
                is ProfileViewModel.SaveStatus.Success -> {
                    Toast.makeText(context, "Profile Saved Successfully", Toast.LENGTH_SHORT).show()
                    viewModel.resetStatus()
                    viewModel.updateCurrentProfile(Profile())
                    findNavController().popBackStack(R.id.dashboardFragment, false)
                }
                is ProfileViewModel.SaveStatus.Error -> {
                    binding.btnSaveProfile.isEnabled = true
                    binding.btnSaveProfile.text = "Save Profile"
                    Toast.makeText(context, status.message, Toast.LENGTH_LONG).show()
                    viewModel.resetStatus()
                }
                else -> {
                    binding.btnSaveProfile.isEnabled = true
                    binding.btnSaveProfile.text = "Save Profile"
                }
            }
        }

        binding.btnConfigureTrigger.setOnClickListener {
            val selectedTriggerId = binding.chipGroupTrigger.checkedChipId
            when (selectedTriggerId) {
                R.id.chipTime -> findNavController().navigate(R.id.action_addProfile_to_timeSelection)
                R.id.chipLocation -> findNavController().navigate(R.id.action_addProfile_to_locationSelection)
                R.id.chipApp -> findNavController().navigate(R.id.action_addProfile_to_appSelection)
                R.id.chipState -> findNavController().navigate(R.id.action_addProfile_to_deviceStateConfig)
                else -> Toast.makeText(context, "Select a trigger type first", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnSaveProfile.setOnClickListener {
            val name = binding.etProfileName.text.toString().trim()
            val description = binding.etDescription.text.toString().trim()
            
            val triggerType = when (binding.chipGroupTrigger.checkedChipId) {
                R.id.chipTime -> "TIME"
                R.id.chipLocation -> "LOCATION"
                R.id.chipApp -> "APP"
                R.id.chipState -> "STATE"
                else -> ""
            }

            val soundMode = when (binding.chipGroupSound.checkedChipId) {
                R.id.chipNormal -> AudioManager.RINGER_MODE_NORMAL
                R.id.chipVibrate -> AudioManager.RINGER_MODE_VIBRATE
                R.id.chipSilent -> AudioManager.RINGER_MODE_SILENT
                R.id.chipDnd -> MODE_DND
                else -> AudioManager.RINGER_MODE_NORMAL
            }

            val priority = binding.sliderPriority.value.toInt()

            val current = viewModel.currentProfile.value ?: Profile()
            val updatedProfile = current.copy(
                name = name,
                description = description,
                triggerType = triggerType,
                soundMode = soundMode,
                priorityLevel = priority,
                lastModified = System.currentTimeMillis()
            )
            
            viewModel.updateCurrentProfile(updatedProfile)
            viewModel.saveProfile()
        }
    }

    private fun updateUI(profile: Profile) {
        if (binding.etProfileName.text.toString() != profile.name) {
            binding.etProfileName.setText(profile.name)
        }
        if (binding.etDescription.text.toString() != profile.description) {
            binding.etDescription.setText(profile.description)
        }
        binding.sliderPriority.value = profile.priorityLevel.toFloat()
        
        when (profile.triggerType) {
            "TIME" -> {
                binding.chipTime.isChecked = true
                if (profile.startTime != null) {
                    binding.btnConfigureTrigger.text = "Time: ${profile.startTime} - ${profile.endTime}"
                }
            }
            "LOCATION" -> {
                binding.chipLocation.isChecked = true
                if (profile.latitude != null) {
                    binding.btnConfigureTrigger.text = "Location Configured"
                }
            }
            "APP" -> {
                binding.chipApp.isChecked = true
                if (!profile.packageNames.isNullOrEmpty()) {
                    binding.btnConfigureTrigger.text = "Apps Selected"
                }
            }
            "STATE" -> {
                binding.chipState.isChecked = true
                if (profile.stateType != null) {
                    binding.btnConfigureTrigger.text = "State: ${profile.stateType}"
                }
            }
        }

        when (profile.soundMode) {
            AudioManager.RINGER_MODE_NORMAL -> binding.chipNormal.isChecked = true
            AudioManager.RINGER_MODE_VIBRATE -> binding.chipVibrate.isChecked = true
            AudioManager.RINGER_MODE_SILENT -> binding.chipSilent.isChecked = true
            MODE_DND -> binding.chipDnd.isChecked = true
        }
    }

    private fun loadProfileForEditing(id: String) {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            val profile = db.profileDao().getProfileById(id)
            profile?.let {
                viewModel.updateCurrentProfile(it)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
