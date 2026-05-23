package com.example.domain.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.domain.repository.SettingsRepository
import com.example.domain.usecase.PrayerCalculationUseCase
import com.example.domain.model.PrayerInfo
import com.example.domain.model.PrayerType
import com.example.R
import com.example.ui.widget.NextPrayerWidgetReceiver
import com.example.ui.widget.TimetableWidgetReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val repo = SettingsRepository(context)
                    val lat = repo.latitude.first() ?: 21.4225
                    val lng = repo.longitude.first() ?: 39.8262
                    val method = repo.calculationMethod.first()
                    val adjustments = repo.getPrayerAdjustments().first()
                    
                    val calcUseCase = PrayerCalculationUseCase()
                    val scheduler = AdhanAlarmScheduler(context)
                    scheduler.scheduleAllPrayers()
                    
                    // Update widgets
                    NextPrayerWidgetReceiver.update(context)
                    TimetableWidgetReceiver.update(context)
                    NextPrayerNotificationManager(context).calculateAndUpdateNextPrayer()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
