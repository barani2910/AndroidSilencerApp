package com.example.androidsilencerapp.ui.trigger

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.androidsilencerapp.R
import com.example.androidsilencerapp.databinding.FragmentLocationSelectionBinding
import com.example.androidsilencerapp.ui.profile.ProfileViewModel
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class LocationSelectionFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentLocationSelectionBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by activityViewModels()

    private var mMap: GoogleMap? = null
    private var selectedLatLng: LatLng? = null
    private var selectedRadius: Float = 100f

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            ) {
                enableMyLocation()
                moveToCurrentLocation(forceSelection = true)
            } else {
                Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLocationSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.sliderRadius.addOnChangeListener { _, value, _ ->
            selectedRadius = value
            binding.tvRadius.text = "Radius: ${value.toInt()}m"
            updateMapOverlay()
        }

        binding.fabMyLocation.setOnClickListener {
            checkLocationPermissions(forceSelection = true)
        }

        binding.btnSaveLocation.setOnClickListener {
            selectedLatLng?.let { latLng ->
                val current = viewModel.currentProfile.value
                current?.let {
                    viewModel.updateCurrentProfile(it.copy(
                        latitude = latLng.latitude,
                        longitude = latLng.longitude,
                        radius = selectedRadius,
                        triggerType = "LOCATION"
                    ))
                }
                findNavController().popBackStack()
            } ?: Toast.makeText(requireContext(), "Please select a location on the map", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        
        mMap?.setOnMapClickListener { latLng ->
            selectedLatLng = latLng
            updateMapOverlay()
        }

        // Initialize with current profile location if exists, otherwise ask for current GPS
        viewModel.currentProfile.value?.let { profile ->
            if (profile.latitude != null && profile.longitude != null) {
                val pos = LatLng(profile.latitude, profile.longitude)
                selectedLatLng = pos
                selectedRadius = profile.radius ?: 100f
                binding.sliderRadius.value = selectedRadius
                updateMapOverlay()
                mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 15f))
            } else {
                checkLocationPermissions(forceSelection = false)
            }
        }
    }

    private fun checkLocationPermissions(forceSelection: Boolean) {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                enableMyLocation()
                moveToCurrentLocation(forceSelection)
            }
            else -> {
                requestPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    private fun enableMyLocation() {
        try {
            mMap?.isMyLocationEnabled = true
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun moveToCurrentLocation(forceSelection: Boolean) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        try {
            // First try last location for speed
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    updateSelection(currentLatLng, forceSelection)
                } else {
                    // If last location is null, request current location
                    requestFreshLocation(forceSelection)
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun requestFreshLocation(forceSelection: Boolean) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        val locationRequest = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .build()

        try {
            fusedLocationClient.getCurrentLocation(locationRequest, null).addOnSuccessListener { location ->
                if (location != null) {
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    updateSelection(currentLatLng, forceSelection)
                } else {
                    Toast.makeText(requireContext(), "Could not identify current location. Please ensure GPS is on.", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun updateSelection(latLng: LatLng, forceSelection: Boolean) {
        if (selectedLatLng == null || forceSelection) {
            selectedLatLng = latLng
            updateMapOverlay()
        }
        mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
    }

    private fun updateMapOverlay() {
        val latLng = selectedLatLng ?: return
        val map = mMap ?: return

        map.clear()
        map.addMarker(MarkerOptions().position(latLng).title("Selected Trigger Point"))
        map.addCircle(
            CircleOptions()
                .center(latLng)
                .radius(selectedRadius.toDouble())
                .strokeWidth(2f)
                .fillColor(0x550000FF)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
