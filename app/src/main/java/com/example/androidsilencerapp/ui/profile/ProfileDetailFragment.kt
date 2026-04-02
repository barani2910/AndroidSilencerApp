package com.example.androidsilencerapp.ui.profile

import android.media.AudioManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.androidsilencerapp.data.local.AppDatabase
import com.example.androidsilencerapp.databinding.FragmentProfileDetailBinding
import kotlinx.coroutines.launch

class ProfileDetailFragment : Fragment() {

    private var _binding: FragmentProfileDetailBinding? = null
    private val binding get() = _binding!!
    private val args: ProfileDetailFragmentArgs by navArgs()
    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val profileId = args.profileId
        loadProfile(profileId)

        binding.btnDeleteProfile.setOnClickListener {
            viewModel.currentProfile.value?.let { profile ->
                viewModel.deleteProfile(profile)
                Toast.makeText(context, "Profile Deleted", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
        }
        
        binding.btnEditProfile.setOnClickListener {
            val action = ProfileDetailFragmentDirections.actionProfileDetailToAddProfile(profileId)
            findNavController().navigate(action)
        }
    }

    private fun loadProfile(id: String) {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            val profile = db.profileDao().getProfileById(id)
            profile?.let {
                viewModel.updateCurrentProfile(it)
                binding.tvDetailName.text = it.name
                binding.tvDetailDescription.text = it.description
                binding.tvDetailTrigger.text = "Type: ${it.triggerType}\nDetails: ${getTriggerDetails(it)}"
                binding.tvDetailSound.text = when(it.soundMode) {
                    AudioManager.RINGER_MODE_NORMAL -> "Normal"
                    AudioManager.RINGER_MODE_VIBRATE -> "Vibrate"
                    AudioManager.RINGER_MODE_SILENT -> "Silent"
                    AddProfileFragment.MODE_DND -> "DND"
                    else -> "Unknown"
                }
                binding.tvDetailPriority.text = it.priorityLevel.toString()
            }
        }
    }

    private fun getTriggerDetails(profile: com.example.androidsilencerapp.data.model.Profile): String {
        return when(profile.triggerType) {
            "TIME" -> "${profile.startTime} - ${profile.endTime}"
            "LOCATION" -> "Lat: ${profile.latitude}, Lng: ${profile.longitude}, Rad: ${profile.radius}m"
            "APP" -> profile.packageNames ?: "None"
            "STATE" -> "${profile.stateType}: ${profile.stateValue}"
            else -> "None"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
