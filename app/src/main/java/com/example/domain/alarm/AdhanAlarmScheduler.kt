package com.example.domain.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.flow.first

class AdhanAlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun scheduleExact(timeMs: Long, pendingIntent: PendingIntent) {
        val alarmClockInfo = AlarmManager.AlarmClockInfo(timeMs, pendingIntent)
        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
    }

    suspend fun scheduleAllPrayers() {
        val repo = com.example.domain.repository.SettingsRepository(context)
        val lat = repo.latitude.first() ?: 21.4225
        val lng = repo.longitude.first() ?: 39.8262
        val method = repo.calculationMethod.first()
        val adjustments = repo.getPrayerAdjustments().first()
        
        val calcUseCase = com.example.domain.usecase.PrayerCalculationUseCase()
        val now = LocalDateTime.now()
        
        // Schedule for the nearby 7 days (or minimum 2 days)
        for (daysOffset in 0..7L) {
            val date = now.toLocalDate().plusDays(daysOffset)
            val times = calcUseCase(lat, lng, date, method, adjustments)
            val prayers = listOf(
                com.example.domain.model.PrayerInfo(com.example.domain.model.PrayerType.FAJR, com.example.R.string.fajr, times.fajr),
                com.example.domain.model.PrayerInfo(com.example.domain.model.PrayerType.SUNRISE, com.example.R.string.sunrise, times.sunrise),
                com.example.domain.model.PrayerInfo(com.example.domain.model.PrayerType.DHUHR, com.example.R.string.dhuhr, times.dhuhr),
                com.example.domain.model.PrayerInfo(com.example.domain.model.PrayerType.ASR, com.example.R.string.asr, times.asr),
                com.example.domain.model.PrayerInfo(com.example.domain.model.PrayerType.MAGHRIB, com.example.R.string.maghrib, times.maghrib),
                com.example.domain.model.PrayerInfo(com.example.domain.model.PrayerType.ISHA, com.example.R.string.isha, times.isha)
            )
            for (prayer in prayers) {
                if (prayer.time.isAfter(now)) { // only schedule future
                    // Create a unique hashcode for prayer type and date
                    val uniqueId = ("${prayer.type.name}_${date.toEpochDay()}").hashCode()
                    scheduleAdhan(prayer.time, context.getString(prayer.nameResId), prayer.type.name, uniqueId)
                }
            }
        }
    }

    fun scheduleAdhan(time: LocalDateTime, prayerName: String, prayerType: String, uniqueId: Int = prayerName.hashCode()) {
        val intent = Intent(context, AdhanReceiver::class.java).apply {
            putExtra("PRAYER_NAME", prayerName)
            putExtra("PRAYER_TYPE", prayerType)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            uniqueId, // Unique ID per prayer occurrence
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTimeMs = time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    scheduleExact(triggerTimeMs, pendingIntent)
                } else {
                    // Fallback to inexact alarm if permission somehow denied
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMs,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMs,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
            // Final fallback
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTimeMs,
                pendingIntent
            )
        }
    }

    fun cancelAdhan(prayerName: String) {
        val intent = Intent(context, AdhanReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            prayerName.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
