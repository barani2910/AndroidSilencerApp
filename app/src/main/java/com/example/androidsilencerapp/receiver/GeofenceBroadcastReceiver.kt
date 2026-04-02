package com.example.androidsilencerapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.androidsilencerapp.data.local.AppDatabase
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    private val TAG = "GeofenceReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return

        if (geofencingEvent.hasError()) {
            Log.e(TAG, "Geofencing error code ${geofencingEvent.errorCode}")
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
            geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT
        ) {
            val triggeringGeofences = geofencingEvent.triggeringGeofences ?: return
            val isEntering = geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER

            val db = AppDatabase.getDatabase(context)
            CoroutineScope(Dispatchers.IO).launch {
                triggeringGeofences.forEach { geofence ->
                    val profileId = geofence.requestId
                    val profile = db.profileDao().getProfileById(profileId)
                    profile?.let {
                        val updatedProfile = it.copy(isGeofenceTriggered = isEntering)
                        db.profileDao().updateProfile(updatedProfile)
                        Log.d(TAG, "Geofence ${if (isEntering) "ENTERED" else "EXITED"} for profile: ${it.name}")
                    }
                }
            }
        }
    }
}
