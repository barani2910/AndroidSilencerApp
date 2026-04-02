package com.example.androidsilencerapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "profiles")
data class Profile(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val name: String = "",
    val description: String = "",
    val triggerType: String = "", // TIME, LOCATION, APP, STATE
    val soundMode: Int = 0, // AudioManager.RINGER_MODE_NORMAL, etc.
    val priorityLevel: Int = 1,
    val isActive: Boolean = true,
    val lastModified: Long = System.currentTimeMillis(),

    // Time-based
    val startTime: String? = null,
    val endTime: String? = null,
    val repeatDays: String? = null,

    // Location-based
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radius: Float? = null,
    val isGeofenceTriggered: Boolean = false, // Track if user is currently inside

    // App-based
    val packageNames: String? = null,

    // State-based
    val stateType: String? = null,
    val stateValue: String? = null
)
