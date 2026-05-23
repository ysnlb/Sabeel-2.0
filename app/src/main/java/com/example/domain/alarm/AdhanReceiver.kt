package com.example.domain.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

import com.example.ui.widget.NextPrayerWidgetReceiver
import com.example.ui.widget.TimetableWidgetReceiver

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import com.example.domain.repository.SettingsRepository

import android.app.NotificationManager
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import com.example.R

class AdhanReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prayerName = intent.getStringExtra("PRAYER_NAME") ?: "Prayer"
        val prayerType = intent.getStringExtra("PRAYER_TYPE")
        
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = SettingsRepository(context)
                val mutedPrayers = repo.mutedPrayers.first()
                val notificationType = repo.notificationType.first()
                
                val isMuted = prayerType == "SUNRISE" || 
                              (prayerType != null && mutedPrayers.any { it.name == prayerType }) ||
                              notificationType == com.example.ui.screens.settings.NotificationType.MUTE
                              
                val isStandardChime = !isMuted && notificationType == com.example.ui.screens.settings.NotificationType.STANDARD_CHIME

                if (isMuted || isStandardChime) {
                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    val channelId = if (isStandardChime) "adhan_channel_standard" else "adhan_channel_silent"
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val importance = if (isStandardChime) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_DEFAULT
                        val channel = android.app.NotificationChannel(channelId, "Prayer Alerts", importance).apply {
                            if (!isStandardChime) {
                                setSound(null, null)
                            }
                        }
                        notificationManager.createNotificationChannel(channel)
                    }
                    
                    val notification = NotificationCompat.Builder(context, channelId)
                        .setContentTitle("It's time for $prayerName")
                        .setContentText("Tap to open app")
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setAutoCancel(true)
                        .build()
                    notificationManager.notify(prayerName.hashCode(), notification)
                } else {
                    val serviceIntent = Intent(context, AdhanPlaybackService::class.java).apply {
                        putExtra("PRAYER_NAME", prayerName)
                        putExtra("PRAYER_TYPE", prayerType)
                        putExtra("NOTIFICATION_ID", prayerName.hashCode())
                        action = AdhanPlaybackService.ACTION_START
                    }

                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(serviceIntent)
                        } else {
                            context.startService(serviceIntent)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        // Fallback if FGS start fails (e.g. Android 14 restrictions)
                        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        val channelId = "adhan_channel_fallback"
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            val channel = android.app.NotificationChannel(channelId, "Prayer Alerts Fallback", NotificationManager.IMPORTANCE_HIGH)
                            notificationManager.createNotificationChannel(channel)
                        }
                        val notification = NotificationCompat.Builder(context, channelId)
                            .setContentTitle("It's time for $prayerName")
                            .setContentText("Adhan failed to play. Tap to open app")
                            .setSmallIcon(R.drawable.ic_launcher_foreground)
                            .setAutoCancel(true)
                            .build()
                        notificationManager.notify(prayerName.hashCode(), notification)
                    }
                }
                
                // Update widgets and schedule future alarms
                NextPrayerWidgetReceiver.update(context)
                TimetableWidgetReceiver.update(context)
                NextPrayerNotificationManager(context).calculateAndUpdateNextPrayer()
                AdhanAlarmScheduler(context).scheduleAllPrayers()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
