package com.example.androidsilencerapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.telephony.TelephonyManager
import android.provider.Telephony
import android.util.Log
import com.example.androidsilencerapp.engine.EmergencyBypassEngine

class EmergencyReceiver : BroadcastReceiver() {
    
    private val TAG = "EmergencyReceiver"
    
    companion object {
        private var engine: EmergencyBypassEngine? = null
        
        fun getEngine(context: Context): EmergencyBypassEngine {
            if (engine == null) {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                engine = EmergencyBypassEngine(context.applicationContext, audioManager)
            }
            return engine!!
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val bypassEngine = getEngine(context)
        
        when (intent.action) {
            TelephonyManager.ACTION_PHONE_STATE_CHANGED -> {
                val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
                val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
                
                Log.d(TAG, "Phone State Changed: $state")
                
                if (state == TelephonyManager.EXTRA_STATE_RINGING) {
                    if (incomingNumber != null) {
                        Log.d(TAG, "Incoming call detected from: $incomingNumber")
                        bypassEngine.onCallReceived(incomingNumber)
                    } else {
                        Log.w(TAG, "Incoming call detected but number is NULL (Check READ_CALL_LOG permission)")
                    }
                }
            }
            
            Telephony.Sms.Intents.SMS_RECEIVED_ACTION -> {
                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                for (message in messages) {
                    val body = message.messageBody
                    Log.d(TAG, "SMS received. Content length: ${body?.length}")
                    if (body != null) {
                        bypassEngine.onSmsReceived(body)
                    }
                }
            }
        }
    }
}
