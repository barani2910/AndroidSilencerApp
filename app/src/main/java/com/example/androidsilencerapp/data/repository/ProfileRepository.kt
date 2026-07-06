package com.example.androidsilencerapp.data.repository

import android.content.Context
import androidx.lifecycle.LiveData
import com.example.androidsilencerapp.data.local.ProfileDao
import com.example.androidsilencerapp.data.local.CalendarExceptionDao
import com.example.androidsilencerapp.data.model.Profile
import com.example.androidsilencerapp.data.model.CalendarException
import com.example.androidsilencerapp.data.remote.FirebaseSyncManager
import com.google.firebase.auth.FirebaseAuth

class ProfileRepository(
    private val profileDao: ProfileDao,
    private val exceptionDao: CalendarExceptionDao,
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

    // Calendar Exception Methods
    suspend fun addCalendarException(date: Long) {
        val exception = CalendarException(date, userId)
        exceptionDao.addException(exception)
        if (isAutoSyncEnabled()) {
            syncManager.uploadCalendarException(exception)
        }
    }

    suspend fun removeCalendarException(date: Long) {
        exceptionDao.removeException(CalendarException(date, userId))
        if (isAutoSyncEnabled()) {
            syncManager.deleteCalendarException(date)
        }
    }

    suspend fun isDateExceptional(date: Long): Boolean {
        return exceptionDao.isDateExceptional(date)
    }

    suspend fun syncWithCloud() {
        syncManager.syncAllData()
    }

    private fun isAutoSyncEnabled(): Boolean {
        return prefs.getBoolean("auto_sync", true)
    }
}
