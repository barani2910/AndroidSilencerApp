package com.example.androidsilencerapp.ui.logs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.androidsilencerapp.data.local.AppDatabase
import com.example.androidsilencerapp.databinding.FragmentLogsBinding
import com.google.firebase.auth.FirebaseAuth

class LogsFragment : Fragment() {

    private var _binding: FragmentLogsBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: LogsAdapter

    private val viewModel: LogsViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val db = AppDatabase.getDatabase(requireContext())
                return LogsViewModel(db.automationLogDao()) as T
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLogsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        adapter = LogsAdapter()
        binding.rvLogs.layoutManager = LinearLayoutManager(context)
        binding.rvLogs.adapter = adapter

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        viewModel.getLogs(userId).observe(viewLifecycleOwner) { logs ->
            adapter.submitList(logs)
            binding.tvEmptyLogs.visibility = if (logs.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
