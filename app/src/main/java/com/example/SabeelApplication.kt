package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class SabeelApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            // Adhan Channel
            val adhanChannel = NotificationChannel(
                "adhan_channel",
                "Adhan Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for Adhan and prayer time alerts"
                setSound(null, null) 
            }

            // Next Prayer Channel
            val nextPrayerChannel = NotificationChannel(
                "next_prayer_channel",
                "Ongoing Prayer Countdown",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows the time remaining for the next prayer"
                setShowBadge(false)
            }

            // Fallback Channel
            val fallbackChannel = NotificationChannel(
                "adhan_channel_fallback",
                "Prayer Alerts Fallback",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Fallback alerts if Adhan fails to play"
            }

            manager?.createNotificationChannels(listOf(adhanChannel, nextPrayerChannel, fallbackChannel))
        }
    }
}
