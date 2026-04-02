package com.example.androidsilencerapp.ui.dashboard

import android.content.ContentUris
import android.content.Intent
import android.os.Bundle
import android.provider.CalendarContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.androidsilencerapp.R
import com.example.androidsilencerapp.databinding.FragmentDashboardBinding
import com.example.androidsilencerapp.service.SilencerService

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DashboardViewModel by viewModels()
    private lateinit var adapter: DashboardAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupToolbar()
        startSilencerService()

        binding.fabAddProfile.setOnClickListener {
            val action = DashboardFragmentDirections.actionDashboardToAddProfile(null)
            findNavController().navigate(action)
        }

        viewModel.profiles.observe(viewLifecycleOwner) { profiles ->
            adapter.submitList(profiles)
            binding.tvEmptyState.visibility = if (profiles.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun setupRecyclerView() {
        adapter = DashboardAdapter(
            onProfileClicked = { profile ->
                val action = DashboardFragmentDirections.actionDashboardToProfileDetail(profile.id)
                findNavController().navigate(action)
            },
            onStatusToggled = { profile ->
                viewModel.toggleProfileStatus(profile)
            }
        )
        binding.rvProfiles.layoutManager = LinearLayoutManager(context)
        binding.rvProfiles.adapter = adapter
    }

    private fun setupToolbar() {
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_sync -> {
                    viewModel.syncProfiles()
                    true
                }
                R.id.action_calendar -> {
                    openCalendar()
                    true
                }
                R.id.action_logs -> {
                    findNavController().navigate(R.id.action_dashboard_to_logs)
                    true
                }
                R.id.action_account -> {
                    findNavController().navigate(R.id.action_dashboard_to_account)
                    true
                }
                R.id.action_settings -> {
                    findNavController().navigate(R.id.action_dashboard_to_settings)
                    true
                }
                else -> false
            }
        }
    }

    private fun openCalendar() {
        val builder = CalendarContract.CONTENT_URI.buildUpon()
        builder.appendPath("time")
        ContentUris.appendId(builder, System.currentTimeMillis())
        val intent = Intent(Intent.ACTION_VIEW).setData(builder.build())
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "No Calendar app found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startSilencerService() {
        val serviceIntent = Intent(requireContext(), SilencerService::class.java)
        ContextCompat.startForegroundService(requireContext(), serviceIntent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
