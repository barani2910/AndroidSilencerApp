package com.example.androidsilencerapp.ui.trigger

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.androidsilencerapp.databinding.FragmentTimeSelectionBinding
import com.example.androidsilencerapp.ui.profile.ProfileViewModel

class TimeSelectionFragment : Fragment() {

    private var _binding: FragmentTimeSelectionBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTimeSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSaveTime.setOnClickListener {
            val startHour = binding.startTimePicker.hour
            val startMinute = binding.startTimePicker.minute
            val endHour = binding.endTimePicker.hour
            val endMinute = binding.endTimePicker.minute

            val startTime = String.format("%02d:%02d", startHour, startMinute)
            val endTime = String.format("%02d:%02d", endHour, endMinute)

            val current = viewModel.currentProfile.value
            current?.let {
                viewModel.updateCurrentProfile(it.copy(
                    startTime = startTime,
                    endTime = endTime,
                    triggerType = "TIME"
                ))
            }
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
