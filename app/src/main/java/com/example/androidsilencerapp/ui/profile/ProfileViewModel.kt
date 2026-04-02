package com.example.androidsilencerapp.ui.profile

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.androidsilencerapp.data.local.AppDatabase
import com.example.androidsilencerapp.data.model.Profile
import com.example.androidsilencerapp.data.remote.FirebaseSyncManager
import com.example.androidsilencerapp.data.repository.ProfileRepository
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ProfileRepository
    private val TAG = "ProfileViewModel"
    
    private val _currentProfile = MutableLiveData<Profile>(Profile(name = ""))
    val currentProfile: LiveData<Profile> = _currentProfile

    private val _saveStatus = MutableLiveData<SaveStatus>(SaveStatus.Idle)
    val saveStatus: LiveData<SaveStatus> = _saveStatus

    init {
        val database = AppDatabase.getDatabase(application)
        val profileDao = database.profileDao()
        val syncManager = FirebaseSyncManager(profileDao)
        repository = ProfileRepository(profileDao, syncManager, application)
    }

    fun updateCurrentProfile(profile: Profile) {
        _currentProfile.value = profile
    }

    fun saveProfile() {
        val profile = _currentProfile.value ?: return
        
        if (profile.name.isEmpty()) {
            _saveStatus.value = SaveStatus.Error("Profile name cannot be empty")
            return
        }
        
        if (profile.triggerType.isEmpty()) {
            _saveStatus.value = SaveStatus.Error("Please select and configure a trigger type")
            return
        }

        _saveStatus.value = SaveStatus.Loading
        viewModelScope.launch {
            try {
                repository.insertProfile(profile)
                Log.d(TAG, "Profile saved successfully: ${profile.name}")
                _saveStatus.value = SaveStatus.Success
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save profile", e)
                _saveStatus.value = SaveStatus.Error(e.message ?: "Unknown error occurred")
            }
        }
    }

    fun deleteProfile(profile: Profile) {
        viewModelScope.launch {
            try {
                repository.deleteProfile(profile)
            } catch (e: Exception) {
                Log.e(TAG, "Delete failed", e)
            }
        }
    }

    fun resetStatus() {
        _saveStatus.value = SaveStatus.Idle
    }

    sealed class SaveStatus {
        object Idle : SaveStatus()
        object Loading : SaveStatus()
        object Success : SaveStatus()
        data class Error(val message: String) : SaveStatus()
    }
}
