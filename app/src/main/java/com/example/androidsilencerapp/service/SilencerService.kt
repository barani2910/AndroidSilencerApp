package com.example.androidsilencerapp.service

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.asFlow
import com.example.androidsilencerapp.data.local.AppDatabase
import com.example.androidsilencerapp.data.model.AutomationLog
import com.example.androidsilencerapp.data.model.Profile
import com.example.androidsilencerapp.engine.*
import com.example.androidsilencerapp.ui.profile.AddProfileFragment
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SilencerService : LifecycleService() {

    private val tag = "SilencerService"
    private val channelId = "SilencerServiceChannel"
    private val notificationId = 1
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private lateinit var audioManager: AudioManager
    private lateinit var notificationManager: NotificationManager
    private lateinit var smartRestoreEngine: SmartRestoreEngine
    private lateinit var priorityResolutionEngine: PriorityResolutionEngine
    private lateinit var automationEngine: AutomationEngine
    private lateinit var geofenceManager: GeofenceManager
    private lateinit var calendarManager: CalendarManager
    private lateinit var db: AppDatabase
    
    private val handler = Handler(Looper.getMainLooper())
    private val evaluationRunnable = object : Runnable {
        override fun run() {
            reEvaluateCurrentProfiles()
            handler.postDelayed(this, 2000)
        }
    }

    private var latestProfiles: List<Profile> = emptyList()
    private var activeProfileId: String? = null

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        db = AppDatabase.getDatabase(this)
        
        smartRestoreEngine = SmartRestoreEngine(audioManager, notificationManager)
        calendarManager = CalendarManager(this)
        
        priorityResolutionEngine = PriorityResolutionEngine()
        automationEngine = AutomationEngine(this, calendarManager)
        geofenceManager = GeofenceManager(this)

        createNotificationChannel()
        val notification = createNotification("Monitoring triggers...")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(notificationId, notification)
        }

        observeProfiles()
        handler.post(evaluationRunnable)
    }

    private fun observeProfiles() {
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            val userId = auth.currentUser?.uid
            if (userId != null) {
                serviceScope.launch {
                    db.profileDao().getProfilesForUser(userId).asFlow().collectLatest { profiles ->
                        val locationTriggersChanged = profiles.map { "${it.id}${it.latitude}${it.longitude}${it.radius}" } != 
                                                    latestProfiles.map { "${it.id}${it.latitude}${it.longitude}${it.radius}" }
                        
                        latestProfiles = profiles
                        
                        if (locationTriggersChanged) {
                            geofenceManager.removeAllGeofences()
                            geofenceManager.addGeofences(profiles)
                        }
                        
                        reEvaluateCurrentProfiles()
                    }
                }
            }
        }
    }

    private fun reEvaluateCurrentProfiles() {
        val activeProfiles = latestProfiles.filter { automationEngine.isProfileActive(it) }
        val winningProfile = priorityResolutionEngine.resolve(activeProfiles)

        if (winningProfile != null) {
            if (activeProfileId != winningProfile.id) {
                applyProfile(winningProfile)
                activeProfileId = winningProfile.id
            }
        } else {
            if (activeProfileId != null) {
                restoreOriginalState()
                activeProfileId = null
            }
        }
    }

    private fun applyProfile(profile: Profile) {
        if (!smartRestoreEngine.isSnapshotTaken()) {
            smartRestoreEngine.takeSnapshot()
        }
        
        Log.d(tag, "Applying profile: ${profile.name} (SoundMode: ${profile.soundMode})")
        
        try {
            val hasDndPermission = notificationManager.isNotificationPolicyAccessGranted

            // Force DND OFF for all modes except explicit DND
            if (hasDndPermission) {
                val targetFilter = if (profile.soundMode == AddProfileFragment.MODE_DND) {
                    NotificationManager.INTERRUPTION_FILTER_PRIORITY
                } else {
                    NotificationManager.INTERRUPTION_FILTER_ALL
                }
                
                if (notificationManager.currentInterruptionFilter != targetFilter) {
                    notificationManager.setInterruptionFilter(targetFilter)
                }
            }

            // Apply Ringer Mode
            if (profile.soundMode != AddProfileFragment.MODE_DND) {
                if (audioManager.ringerMode != profile.soundMode) {
                    if (profile.soundMode == AudioManager.RINGER_MODE_SILENT && !hasDndPermission) {
                        audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                        updateNotification("DND Access required for Silent mode. Used Vibrate.")
                    } else {
                        audioManager.ringerMode = profile.soundMode
                        logAutomation(profile, "Activated (${getModeName(profile.soundMode)})")
                    }
                }
            } else {
                logAutomation(profile, "Activated (DND)")
            }

            updateNotification("Active Profile: ${profile.name}")
        } catch (e: Exception) {
            Log.e(tag, "Failed to apply sound mode", e)
        }
    }

    private fun getModeName(mode: Int): String {
        return when (mode) {
            AudioManager.RINGER_MODE_SILENT -> "Silent"
            AudioManager.RINGER_MODE_VIBRATE -> "Vibrate"
            AudioManager.RINGER_MODE_NORMAL -> "Normal"
            else -> "DND"
        }
    }

    private fun restoreOriginalState() {
        if (smartRestoreEngine.isSnapshotTaken()) {
            smartRestoreEngine.restore()
            logAutomation(null, "Restored original state")
            updateNotification("Monitoring triggers...")
        }
    }

    private fun logAutomation(profile: Profile?, action: String) {
        serviceScope.launch(Dispatchers.IO) {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            val log = AutomationLog(
                triggerType = profile?.triggerType ?: "None",
                profileName = profile?.name ?: "System",
                actionTaken = action,
                userId = userId
            )
            db.automationLogDao().insertLog(log)
        }
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            channelId,
            "Silencer Service Channel",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }

    private fun createNotification(content: String): Notification {
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Cloud Silencer Active")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(content: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }
        val notification = createNotification(content)
        notificationManager.notify(notificationId, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(evaluationRunnable)
        serviceJob.cancel()
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }
}
