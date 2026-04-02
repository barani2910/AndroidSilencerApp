package com.example.androidsilencerapp.ui.trigger

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.androidsilencerapp.databinding.FragmentDeviceStateConfigBinding
import com.example.androidsilencerapp.ui.profile.ProfileViewModel

class DeviceStateConfigFragment : Fragment() {

    private var _binding: FragmentDeviceStateConfigBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDeviceStateConfigBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSaveState.setOnClickListener {
            val isCharging = binding.switchCharging.isChecked
            val wifiSsid = binding.etWifiSsid.text.toString()
            val bluetoothName = binding.etBluetoothName.text.toString()

            val stateType = if (isCharging) "CHARGING" else if (wifiSsid.isNotEmpty()) "WIFI" else if (bluetoothName.isNotEmpty()) "BLUETOOTH" else "NONE"
            val stateValue = if (isCharging) "true" else if (wifiSsid.isNotEmpty()) wifiSsid else if (bluetoothName.isNotEmpty()) bluetoothName else ""

            val current = viewModel.currentProfile.value
            current?.let {
                viewModel.updateCurrentProfile(it.copy(
                    stateType = stateType,
                    stateValue = stateValue,
                    triggerType = "STATE"
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
