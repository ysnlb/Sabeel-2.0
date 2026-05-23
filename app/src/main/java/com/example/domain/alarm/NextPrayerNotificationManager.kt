package com.example.domain.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.domain.model.PrayerInfo
import com.example.domain.model.PrayerType
import com.example.domain.repository.SettingsRepository
import com.example.domain.usecase.PrayerCalculationUseCase
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.time.ZoneId

class NextPrayerNotificationManager(private val context: Context) {
    companion object {
        const val CHANNEL_ID = "next_prayer_channel"
        const val NOTIFICATION_ID = 2002
    }

    init {
        createChannel()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Ongoing Prayer Countdown",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows the time remaining for the next prayer"
                setShowBadge(false)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    suspend fun calculateAndUpdateNextPrayer() {
        val repo = SettingsRepository(context)
        val lat = repo.latitude.first() ?: 21.4225
        val lng = repo.longitude.first() ?: 39.8262
        val method = repo.calculationMethod.first()
        val adjustments = repo.getPrayerAdjustments().first()

        val calcUseCase = PrayerCalculationUseCase()
        val now = LocalDateTime.now()

        val todayTimes = calcUseCase(lat, lng, now.toLocalDate(), method, adjustments)
        val todayPrayers = listOf(
            PrayerInfo(PrayerType.FAJR, R.string.fajr, todayTimes.fajr),
            PrayerInfo(PrayerType.SUNRISE, R.string.sunrise, todayTimes.sunrise),
            PrayerInfo(PrayerType.DHUHR, R.string.dhuhr, todayTimes.dhuhr),
            PrayerInfo(PrayerType.ASR, R.string.asr, todayTimes.asr),
            PrayerInfo(PrayerType.MAGHRIB, R.string.maghrib, todayTimes.maghrib),
            PrayerInfo(PrayerType.ISHA, R.string.isha, todayTimes.isha)
        )

        var next = todayPrayers.firstOrNull { it.time.isAfter(now) }
        if (next == null) {
            val tomorrowTimes = calcUseCase(lat, lng, now.toLocalDate().plusDays(1), method, adjustments)
            next = PrayerInfo(PrayerType.FAJR, R.string.fajr, tomorrowTimes.fajr)
        }

        val timeInMillis = next.time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val prayerName = context.getString(next.nameResId)
        updateNextPrayer(prayerName, timeInMillis)
    }

    fun updateNextPrayer(prayerName: String, timeInMillis: Long) {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = intent?.let {
            PendingIntent.getActivity(
                context,
                0,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Next: $prayerName")
            .setContentText("Tap to open Sabeel")
            .setOngoing(true)
            .setWhen(timeInMillis)
            .setUsesChronometer(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setDefaults(0) // Silent notification

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.setChronometerCountDown(true)
        }

        if (pendingIntent != null) {
            builder.setContentIntent(pendingIntent)
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, builder.build())
    }

    fun cancelNotification() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NOTIFICATION_ID)
    }
}
