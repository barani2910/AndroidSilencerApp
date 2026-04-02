package com.example.androidsilencerapp.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.androidsilencerapp.data.local.AppDatabase
import com.example.androidsilencerapp.data.model.Profile
import com.example.androidsilencerapp.data.remote.FirebaseSyncManager
import com.example.androidsilencerapp.data.repository.ProfileRepository
import kotlinx.coroutines.launch

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ProfileRepository
    val profiles: LiveData<List<Profile>>

    init {
        val database = AppDatabase.getDatabase(application)
        val profileDao = database.profileDao()
        val syncManager = FirebaseSyncManager(profileDao)
        repository = ProfileRepository(profileDao, syncManager, application)
        profiles = repository.getProfiles()
        
        // Auto-sync on startup
        syncProfiles()
    }

    fun syncProfiles() {
        viewModelScope.launch {
            repository.syncWithCloud()
        }
    }

    fun toggleProfileStatus(profile: Profile) {
        viewModelScope.launch {
            repository.updateProfile(profile.copy(isActive = !profile.isActive))
        }
    }
}
