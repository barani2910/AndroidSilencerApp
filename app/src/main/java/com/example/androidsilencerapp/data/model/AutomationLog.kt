package com.example.androidsilencerapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "automation_logs")
data class AutomationLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val triggerType: String = "",
    val profileName: String = "",
    val actionTaken: String = "", // e.g., "Activated", "Restored"
    val userId: String = ""
)
