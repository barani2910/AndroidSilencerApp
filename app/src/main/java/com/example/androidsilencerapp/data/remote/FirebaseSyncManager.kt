package com.example.androidsilencerapp.data.remote

import android.util.Log
import com.example.androidsilencerapp.data.local.ProfileDao
import com.example.androidsilencerapp.data.model.Profile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseSyncManager(
    private val profileDao: ProfileDao,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val tag = "FirebaseSyncManager"

    suspend fun syncAllProfiles() {
        val userId = auth.currentUser?.uid ?: return
        try {
            // Get local profiles
            val localProfiles = profileDao.getProfilesForUserSync(userId)
            
            // Get remote profiles
            val remoteSnapshot = firestore.collection("users").document(userId)
                .collection("profiles").get().await()
            val remoteProfiles = remoteSnapshot.toObjects(Profile::class.java)

            // Sync Logic: Compare lastModified
            val remoteMap = remoteProfiles.associateBy { it.id }
            val localMap = localProfiles.associateBy { it.id }

            // Update/Add to Firestore if local is newer
            localProfiles.forEach { local ->
                val remote = remoteMap[local.id]
                if (remote == null || local.lastModified > remote.lastModified) {
                    uploadProfile(local)
                }
            }

            // Update/Add to Room if remote is newer
            remoteProfiles.forEach { remote ->
                val local = localMap[remote.id]
                if (local == null || remote.lastModified > local.lastModified) {
                    profileDao.insertProfile(remote)
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Sync failed", e)
        }
    }

    suspend fun uploadProfile(profile: Profile) {
        val userId = auth.currentUser?.uid ?: return
        try {
            firestore.collection("users").document(userId)
                .collection("profiles").document(profile.id).set(profile).await()
        } catch (e: Exception) {
            Log.e(tag, "Upload failed", e)
        }
    }

    suspend fun deleteProfile(profileId: String) {
        val userId = auth.currentUser?.uid ?: return
        try {
            firestore.collection("users").document(userId)
                .collection("profiles").document(profileId).delete().await()
        } catch (e: Exception) {
            Log.e(tag, "Delete failed", e)
        }
    }
}
