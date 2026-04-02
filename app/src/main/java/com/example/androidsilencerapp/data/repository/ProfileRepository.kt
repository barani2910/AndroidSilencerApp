package com.example.androidsilencerapp.data.repository

import android.content.Context
import androidx.lifecycle.LiveData
import com.example.androidsilencerapp.data.local.ProfileDao
import com.example.androidsilencerapp.data.model.Profile
import com.example.androidsilencerapp.data.remote.FirebaseSyncManager
import com.google.firebase.auth.FirebaseAuth

class ProfileRepository(
    private val profileDao: ProfileDao,
    private val syncManager: FirebaseSyncManager,
    private val context: Context
) {
    private val auth = FirebaseAuth.getInstance()
    private val userId: String get() = auth.currentUser?.uid ?: ""
    private val prefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)

    fun getProfiles(): LiveData<List<Profile>> {
        return profileDao.getProfilesForUser(userId)
    }

    suspend fun insertProfile(profile: Profile) {
        val profileWithUser = profile.copy(userId = userId, lastModified = System.currentTimeMillis())
        profileDao.insertProfile(profileWithUser)
        if (isAutoSyncEnabled()) {
            syncManager.uploadProfile(profileWithUser)
        }
    }

    suspend fun updateProfile(profile: Profile) {
        val updatedProfile = profile.copy(lastModified = System.currentTimeMillis())
        profileDao.updateProfile(updatedProfile)
        if (isAutoSyncEnabled()) {
            syncManager.uploadProfile(updatedProfile)
        }
    }

    suspend fun deleteProfile(profile: Profile) {
        profileDao.deleteProfile(profile)
        if (isAutoSyncEnabled()) {
            syncManager.deleteProfile(profile.id)
        }
    }

    suspend fun syncWithCloud() {
        syncManager.syncAllProfiles()
    }

    private fun isAutoSyncEnabled(): Boolean {
        return prefs.getBoolean("auto_sync", true)
    }
}
