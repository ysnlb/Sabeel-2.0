package com.example.domain.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.domain.repository.SettingsRepository
import com.example.ui.screens.settings.NotificationType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AdhanPlaybackService : Service() {

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val CHANNEL_ID = "adhan_channel"
        const val NOTIFICATION_ID = 1001
    }

    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var settingsRepository: SettingsRepository
    private var currentNotificationId = 1001

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(applicationContext)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP) {
            val notifId = intent.getIntExtra("NOTIFICATION_ID", 1001)
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(notifId)
            stopPlaybackAndService(dismissNotification = true)
            return START_NOT_STICKY
        }

        val prayerName = intent?.getStringExtra("PRAYER_NAME") ?: "Prayer"
        val prayerType = intent?.getStringExtra("PRAYER_TYPE")
        currentNotificationId = intent?.getIntExtra("NOTIFICATION_ID", prayerName.hashCode()) ?: prayerName.hashCode()
        
        // Start foreground immediately with a loading/playing notification
        val notification = buildNotification(prayerName)
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(currentNotificationId, notification, 2048) // FOREGROUND_SERVICE_TYPE_SHORT_SERVICE
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(currentNotificationId, notification, 0)
        } else {
            startForeground(currentNotificationId, notification)
        }

        // Read settings to decide what to play
        scope.launch {
            val notificationType = settingsRepository.notificationType.first()
            withContext(Dispatchers.Main) {
                 playAudio(notificationType, prayerType)
            }
        }

        return START_NOT_STICKY
    }

    private fun playAudio(type: NotificationType, prayerTypeStr: String?) {
        try {
            if (type == NotificationType.MUTE) {
                stopPlaybackAndService()
                return
            }

            if (type == NotificationType.STANDARD_CHIME) {
                val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        android.media.AudioAttributes.Builder()
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                            .build()
                    )
                    setDataSource(this@AdhanPlaybackService, defaultUri)
                    setOnCompletionListener {
                        stopPlaybackAndService()
                    }
                    setOnErrorListener { _, _, _ ->
                        stopPlaybackAndService()
                        true
                    }
                    prepare()
                    start()
                }
                return
            }

            val isFajr = prayerTypeStr == "FAJR"
            val resId = if (isFajr) com.example.R.raw.adhan_fajr else com.example.R.raw.adhan
            
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                        .build()
                )
                
                val afd = resources.openRawResourceFd(resId)
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                
                setOnCompletionListener {
                    stopPlaybackAndService()
                }
                setOnErrorListener { _, _, _ ->
                    stopPlaybackAndService()
                    true
                }
                
                prepare()
                start()
            }
            
            if (mediaPlayer == null) {
                stopPlaybackAndService()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            stopPlaybackAndService()
        }
    }

    private fun buildNotification(prayerName: String): Notification {
        val stopIntent = Intent(this, AdhanPlaybackService::class.java).apply {
            action = ACTION_STOP
            putExtra("NOTIFICATION_ID", currentNotificationId)
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            currentNotificationId,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("It's time for $prayerName")
            .setContentText("Tap Stop to dismiss")
            .setSmallIcon(R.drawable.ic_launcher_foreground) 
            .setOngoing(true)
            .addAction(0, "Stop", stopPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Adhan Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for Adhan and prayer notifications"
                setSound(null, null) 
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun stopPlaybackAndService(dismissNotification: Boolean = false) {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
            }
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            // Ignore
        }
        if (dismissNotification) {
            stopForeground(true)
        } else {
            stopForeground(false)
        }
        stopSelf()
    }

    override fun onDestroy() {
        stopPlaybackAndService(true)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
