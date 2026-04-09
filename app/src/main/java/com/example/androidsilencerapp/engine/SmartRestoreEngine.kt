package com.example.androidsilencerapp.engine

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.util.Log

class SmartRestoreEngine(
    private val context: Context,
    private val audioManager: AudioManager,
    private val notificationManager: NotificationManager? = null
) {
    private val prefs = context.getSharedPreferences("smart_restore_prefs", Context.MODE_PRIVATE)
    private val TAG = "SmartRestoreEngine"

    fun isSnapshotTaken(): Boolean = prefs.getBoolean("snapshot_taken", false)

    fun takeSnapshot() {
        if (isSnapshotTaken()) return

        try {
            prefs.edit()
                .putInt("ringer_mode", audioManager.ringerMode)
                .putInt("ring_vol", audioManager.getStreamVolume(AudioManager.STREAM_RING))
                .putInt("media_vol", audioManager.getStreamVolume(AudioManager.STREAM_MUSIC))
                .putInt("notif_vol", audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION))
                .putInt("dnd_filter", notificationManager?.currentInterruptionFilter ?: -1)
                .putBoolean("snapshot_taken", true)
                .apply()
            Log.d(TAG, "Snapshot taken successfully: RingerMode=${audioManager.ringerMode}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to take snapshot", e)
        }
    }

    fun restore() {
        if (!isSnapshotTaken()) {
            Log.d(TAG, "No snapshot to restore")
            return
        }

        try {
            val dnd = prefs.getInt("dnd_filter", -1)
            if (dnd != -1 && notificationManager?.isNotificationPolicyAccessGranted == true) {
                notificationManager.setInterruptionFilter(dnd)
            }

            val ringerMode = prefs.getInt("ringer_mode", AudioManager.RINGER_MODE_NORMAL)
            audioManager.ringerMode = ringerMode
            
            // Restore volumes only if ringer mode allows
            if (ringerMode == AudioManager.RINGER_MODE_NORMAL) {
                audioManager.setStreamVolume(AudioManager.STREAM_RING, prefs.getInt("ring_vol", 0), 0)
                audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, prefs.getInt("notif_vol", 0), 0)
            }
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, prefs.getInt("media_vol", 0), 0)
            
            clearSnapshot()
            Log.d(TAG, "Snapshot restored successfully: RingerMode=$ringerMode")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore snapshot", e)
        }
    }

    fun clearSnapshot() {
        prefs.edit().putBoolean("snapshot_taken", false).apply()
    }
}
