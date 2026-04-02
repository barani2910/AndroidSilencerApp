package com.example.androidsilencerapp.engine

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.util.Log
import com.example.androidsilencerapp.data.model.Profile
import java.util.*

class AutomationEngine(
    private val context: Context,
    private val calendarManager: CalendarManager
) {
    private val TAG = "AutomationEngine"

    fun isProfileActive(profile: Profile): Boolean {
        if (!profile.isActive) return false
        
        // Check for Calendar Exception first
        if (calendarManager.isExceptionActive()) {
            Log.d(TAG, "Automation bypassed: Active EXCEPTIONAL event found in calendar")
            return false
        }

        val active = when (profile.triggerType) {
            "TIME" -> evaluateTimeTrigger(profile)
            "LOCATION" -> profile.isGeofenceTriggered
            "APP" -> evaluateAppTrigger(profile)
            "STATE" -> evaluateStateTrigger(profile)
            else -> false
        }
        
        return active
    }

    private fun evaluateTimeTrigger(profile: Profile): Boolean {
        val start = profile.startTime ?: return false
        val end = profile.endTime ?: return false

        if (start.isEmpty() || end.isEmpty()) return false

        val now = Calendar.getInstance()
        val currentStr = String.format("%02d:%02d", now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE))
        
        return if (start <= end) {
            currentStr >= start && currentStr < end
        } else {
            currentStr >= start || currentStr < end
        }
    }

    private fun evaluateAppTrigger(profile: Profile): Boolean {
        val packageNames = profile.packageNames?.split(",") ?: return false
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val time = System.currentTimeMillis()
        
        val events = usageStatsManager.queryEvents(time - 1000 * 30, time)
        val event = UsageEvents.Event()
        var lastForegroundPackage: String? = null
        
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                lastForegroundPackage = event.packageName
            }
        }
        
        if (lastForegroundPackage == null) {
            val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 60, time)
            lastForegroundPackage = stats.maxByOrNull { it.lastTimeUsed }?.packageName
        }

        return packageNames.contains(lastForegroundPackage)
    }

    private fun evaluateStateTrigger(profile: Profile): Boolean {
        return when (profile.stateType) {
            "CHARGING" -> {
                val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
                status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            }
            "WIFI" -> {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val info = wifiManager.connectionInfo
                val currentSsid = info.ssid.replace("\"", "")
                profile.stateValue == currentSsid
            }
            else -> false
        }
    }
}
