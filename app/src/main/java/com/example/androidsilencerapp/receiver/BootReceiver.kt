package com.example.androidsilencerapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.androidsilencerapp.service.SilencerService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val serviceIntent = Intent(context, SilencerService::class.java)
            ContextCompat.startForegroundService(context, serviceIntent)
        }
    }
}
