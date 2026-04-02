package com.example.androidsilencerapp.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.androidsilencerapp.R
import com.example.androidsilencerapp.databinding.FragmentSettingsBinding
import com.example.androidsilencerapp.ui.auth.AuthViewModel

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnPermissionManager.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_permissionManager)
        }

        binding.btnCloudSync.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_cloudSync)
        }

        binding.btnLogout.setOnClickListener {
            authViewModel.logout()
            findNavController().navigate(R.id.action_splash_to_login) // Navigating using a global-like action logic
        }

        authViewModel.user.observe(viewLifecycleOwner) { user ->
            if (user == null) {
                // Double check if logout happened and pop back to login
                findNavController().navigate(R.id.loginFragment, null, 
                    androidx.navigation.NavOptions.Builder()
                        .setPopUpTo(R.id.nav_graph, true)
                        .build()
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
