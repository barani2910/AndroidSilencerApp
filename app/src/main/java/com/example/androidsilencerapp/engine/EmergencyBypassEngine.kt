package com.example.androidsilencerapp.engine

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.telephony.PhoneNumberUtils
import android.util.Log

class EmergencyBypassEngine(private val context: Context, private val audioManager: AudioManager) {

    private val callHistory = mutableListOf<CallRecord>()
    private val repeatedCallThresholdMs = 3 * 60 * 1000L // 3 minutes
    private val bypassDurationMs = 5 * 60 * 1000L // 5 minutes
    private var lastBypassTimestamp = 0L
    private val tag = "EmergencyBypass"

    data class CallRecord(val phoneNumber: String, val timestamp: Long)

    fun isBypassActive(): Boolean {
        return (System.currentTimeMillis() - lastBypassTimestamp) < bypassDurationMs
    }

    fun onCallReceived(phoneNumber: String) {
        val now = System.currentTimeMillis()
        
        // Manual cleanup to ensure compatibility and avoid removal while iterating
        val iterator = callHistory.iterator()
        while (iterator.hasNext()) {
            if ((now - iterator.next().timestamp) > repeatedCallThresholdMs) {
                iterator.remove()
            }
        }

        // Check for repeated calls from the same number
        val hasRecentCall = callHistory.any { record ->
            // Use context-aware compare for modern Android (Min SDK 26)
            PhoneNumberUtils.compare(context, record.phoneNumber, phoneNumber) && 
            (now - record.timestamp) < repeatedCallThresholdMs 
        }

        if (hasRecentCall) {
            bypassSilence("Repeated Call from $phoneNumber")
        }

        callHistory.add(CallRecord(phoneNumber, now))
    }

    fun onSmsReceived(message: String) {
        if (message.contains("URGENT", ignoreCase = true)) {
            bypassSilence("Emergency Keyword Detected")
        }
    }

    private fun bypassSilence(reason: String) {
        Log.d(tag, "Bypassing silence: $reason")
        lastBypassTimestamp = System.currentTimeMillis()
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Since minSdk is 26, these are safe to call directly if we have permission
        if (notificationManager.isNotificationPolicyAccessGranted) {
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        }

        audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
        if (maxVol > 0) {
            audioManager.setStreamVolume(AudioManager.STREAM_RING, maxVol / 2, AudioManager.FLAG_SHOW_UI)
        }
    }
}
