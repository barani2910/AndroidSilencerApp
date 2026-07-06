package com.example.androidsilencerapp.ui.trigger

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.androidsilencerapp.R
import com.example.androidsilencerapp.databinding.FragmentLocationSelectionBinding
import com.example.androidsilencerapp.ui.profile.ProfileViewModel
import com.google.android.gms.location.LocationServices
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
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        
        enableMyLocation()

        mMap?.setOnMapClickListener { latLng ->
            selectedLatLng = latLng
            updateMapOverlay()
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            mMap?.isMyLocationEnabled = true
            
            // Get current location and move camera
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                    
                    // Automatically select current location as default
                    if (selectedLatLng == null) {
                        selectedLatLng = currentLatLng
                        updateMapOverlay()
                    }
                }
            }
        } else {
            // Default location if permission not granted
            val defaultLoc = LatLng(0.0, 0.0)
            mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLoc, 2f))
        }
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
                .strokeColor(0xFF0000FF.toInt())
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
