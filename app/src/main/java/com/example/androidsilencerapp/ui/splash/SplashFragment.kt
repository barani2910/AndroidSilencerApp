package com.example.androidsilencerapp.ui.splash

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.androidsilencerapp.R
import com.example.androidsilencerapp.databinding.FragmentSplashBinding
import com.google.firebase.auth.FirebaseAuth

class SplashFragment : Fragment() {

    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "SplashFragment"

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        Log.d(TAG, "Permissions result received")
        checkUserStatus()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSplashBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Give the UI a moment to breath before requesting permissions
        Handler(Looper.getMainLooper()).postDelayed({
            if (isAdded) {
                checkPermissions()
            }
        }, 2000)
    }

    private fun checkPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_PHONE_STATE
        )

        // Note: ACCESS_BACKGROUND_LOCATION should generally be requested separately 
        // after foreground location is granted to comply with Android 11+ policies.
        // We'll request the basics first to get the app open.
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            Log.d(TAG, "Requesting missing permissions: $missingPermissions")
            requestPermissionLauncher.launch(missingPermissions.toTypedArray())
        } else {
            Log.d(TAG, "All basic permissions already granted")
            checkUserStatus()
        }
    }

    private fun checkUserStatus() {
        if (!isAdded) return
        
        try {
            if (auth.currentUser != null) {
                Log.d(TAG, "User logged in, navigating to Dashboard")
                findNavController().navigate(R.id.action_splash_to_dashboard)
            } else {
                Log.d(TAG, "No user, navigating to Login")
                findNavController().navigate(R.id.action_splash_to_login)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Navigation failed", e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
