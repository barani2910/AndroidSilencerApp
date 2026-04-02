package com.example.androidsilencerapp.engine

import android.app.NotificationManager
import android.media.AudioManager

class SmartRestoreEngine(
    private val audioManager: AudioManager,
    private val notificationManager: NotificationManager? = null
) {

    private var ringerModeSnapshot: Int? = null
    private var ringVolumeSnapshot: Int? = null
    private var mediaVolumeSnapshot: Int? = null
    private var notificationVolumeSnapshot: Int? = null
    private var dndSnapshot: Int? = null

    private var snapshotTaken = false

    fun takeSnapshot() {
        ringerModeSnapshot = audioManager.ringerMode
        ringVolumeSnapshot = audioManager.getStreamVolume(AudioManager.STREAM_RING)
        mediaVolumeSnapshot = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        notificationVolumeSnapshot = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION)
        
        dndSnapshot = notificationManager?.currentInterruptionFilter
        
        snapshotTaken = true
    }

    fun restore() {
        dndSnapshot?.let { notificationManager?.setInterruptionFilter(it) }
        
        ringerModeSnapshot?.let { audioManager.ringerMode = it }
        ringVolumeSnapshot?.let { audioManager.setStreamVolume(AudioManager.STREAM_RING, it, 0) }
        mediaVolumeSnapshot?.let { audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, it, 0) }
        notificationVolumeSnapshot?.let { audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, it, 0) }
        clearSnapshot()
    }

    fun isSnapshotTaken(): Boolean = snapshotTaken

    private fun clearSnapshot() {
        ringerModeSnapshot = null
        ringVolumeSnapshot = null
        mediaVolumeSnapshot = null
        notificationVolumeSnapshot = null
        dndSnapshot = null
        snapshotTaken = false
    }
}
