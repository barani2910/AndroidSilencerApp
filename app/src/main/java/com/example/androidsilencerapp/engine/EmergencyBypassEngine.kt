package com.example.androidsilencerapp.engine

import android.content.Context
import android.media.AudioManager
import android.provider.ContactsContract
import android.telephony.PhoneNumberUtils

class EmergencyBypassEngine(private val context: Context, private val audioManager: AudioManager) {

    private val callHistory = mutableListOf<CallRecord>()
    private val REPEATED_CALL_THRESHOLD_MS = 3 * 60 * 1000 // 3 minutes

    data class CallRecord(val phoneNumber: String, val timestamp: Long)

    fun onCallReceived(phoneNumber: String) {
        val now = System.currentTimeMillis()
        
        // Check repeated calls
        val recentCalls = callHistory.filter { 
            it.phoneNumber == phoneNumber && (now - it.timestamp) < REPEATED_CALL_THRESHOLD_MS 
        }

        if (recentCalls.size >= 1) { // This is the 2nd call in 3 mins
            bypassSilence("Repeated Call from $phoneNumber")
        }

        callHistory.add(CallRecord(phoneNumber, now))
        // Cleanup old history
        callHistory.removeIf { (now - it.timestamp) > REPEATED_CALL_THRESHOLD_MS }
    }

    fun onSmsReceived(message: String) {
        if (message.contains("URGENT", ignoreCase = true)) {
            bypassSilence("Emergency Keyword Detected")
        }
    }

    private fun bypassSilence(reason: String) {
        // Raise volume temporarily
        audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
        audioManager.setStreamVolume(
            AudioManager.STREAM_RING,
            audioManager.getStreamMaxVolume(AudioManager.STREAM_RING) / 2,
            0
        )
    }
}
