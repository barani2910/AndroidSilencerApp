package com.example.androidsilencerapp.data.remote

import android.util.Log
import com.example.androidsilencerapp.data.local.ProfileDao
import com.example.androidsilencerapp.data.local.CalendarExceptionDao
import com.example.androidsilencerapp.data.model.Profile
import com.example.androidsilencerapp.data.model.CalendarException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseSyncManager(
    private val profileDao: ProfileDao,
    private val exceptionDao: CalendarExceptionDao,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val tag = "FirebaseSyncManager"

    suspend fun syncAllData() {
        val userId = auth.currentUser?.uid ?: return
        syncProfiles(userId)
        syncCalendarExceptions(userId)
    }

    private suspend fun syncProfiles(userId: String) {
        try {
            val localProfiles = profileDao.getProfilesForUserSync(userId)
            val remoteSnapshot = firestore.collection("users").document(userId)
                .collection("profiles").get().await()
            val remoteProfiles = remoteSnapshot.toObjects(Profile::class.java)

            val remoteMap = remoteProfiles.associateBy { it.id }
            val localMap = localProfiles.associateBy { it.id }

            localProfiles.forEach { local ->
                val remote = remoteMap[local.id]
                if (remote == null || local.lastModified > remote.lastModified) {
                    uploadProfile(local)
                }
            }

            remoteProfiles.forEach { remote ->
                val local = localMap[remote.id]
                if (local == null || remote.lastModified > local.lastModified) {
                    profileDao.insertProfile(remote)
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Profile sync failed", e)
        }
    }

    private suspend fun syncCalendarExceptions(userId: String) {
        try {
            val localExceptions = exceptionDao.getAllExceptions()
            val remoteSnapshot = firestore.collection("users").document(userId)
                .collection("calendar_exceptions").get().await()
            val remoteExceptions = remoteSnapshot.toObjects(CalendarException::class.java)

            // For simplicity, we merge sets based on the date timestamp
            val localDates = localExceptions.map { it.date }.toSet()
            val remoteDates = remoteExceptions.map { it.date }.toSet()

            // Upload missing to remote
            (localDates - remoteDates).forEach { date ->
                uploadCalendarException(CalendarException(date, userId))
            }

            // Download missing to local
            (remoteDates - localDates).forEach { date ->
                exceptionDao.addException(CalendarException(date, userId))
            }
        } catch (e: Exception) {
            Log.e(tag, "Calendar sync failed", e)
        }
    }

    suspend fun uploadProfile(profile: Profile) {
        val userId = auth.currentUser?.uid ?: return
        try {
            firestore.collection("users").document(userId)
                .collection("profiles").document(profile.id).set(profile).await()
        } catch (e: Exception) {
            Log.e(tag, "Profile upload failed", e)
        }
    }

    suspend fun uploadCalendarException(exception: CalendarException) {
        val userId = auth.currentUser?.uid ?: return
        try {
            firestore.collection("users").document(userId)
                .collection("calendar_exceptions").document(exception.date.toString()).set(exception).await()
        } catch (e: Exception) {
            Log.e(tag, "Calendar upload failed", e)
        }
    }

    suspend fun deleteProfile(profileId: String) {
        val userId = auth.currentUser?.uid ?: return
        try {
            firestore.collection("users").document(userId)
                .collection("profiles").document(profileId).delete().await()
        } catch (e: Exception) {
            Log.e(tag, "Delete profile failed", e)
        }
    }

    suspend fun deleteCalendarException(date: Long) {
        val userId = auth.currentUser?.uid ?: return
        try {
            firestore.collection("users").document(userId)
                .collection("calendar_exceptions").document(date.toString()).delete().await()
        } catch (e: Exception) {
            Log.e(tag, "Delete calendar failed", e)
        }
    }
}
