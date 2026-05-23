package com.example.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.batoulapps.adhan.CalculationMethod
import com.example.domain.model.PrayerInfo
import com.example.domain.model.PrayerType
import com.example.domain.repository.SettingsRepository
import com.example.domain.usecase.PrayerCalculationUseCase
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import com.example.R
import androidx.glance.layout.height

class TimetableWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = SettingsRepository(context)
        val lat = repo.latitude.first() ?: 21.4225
        val lng = repo.longitude.first() ?: 39.8262
        val city = repo.cityName.first() ?: "Makkah"
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
        
        val next = todayPrayers.firstOrNull { it.time.isAfter(now) }
            ?: PrayerInfo(PrayerType.FAJR, R.string.fajr, calcUseCase(lat, lng, now.toLocalDate().plusDays(1), method, adjustments).fajr)

        provideContent {
            GlanceTheme {
                TimetableContent(city, todayPrayers, next)
            }
        }
    }
}

@Composable
fun TimetableContent(city: String, prayers: List<PrayerInfo>, next: PrayerInfo) {
    val context = LocalContext.current

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(12.dp)
            .background(GlanceTheme.colors.surface)
    ) {
        Text(
            text = city,
            style = TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            ),
            modifier = GlanceModifier.padding(bottom = 8.dp)
        )

        prayers.forEach { prayer ->
            val isNext = prayer.type == next.type
            val timeStr = prayer.time.format(DateTimeFormatter.ofPattern("HH:mm"))
            val nameStr = context.getString(prayer.nameResId)

            val bgColor = if (isNext) GlanceTheme.colors.primaryContainer else GlanceTheme.colors.surface
            val textColor = if (isNext) GlanceTheme.colors.onPrimaryContainer else GlanceTheme.colors.onSurface

            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp, horizontal = 8.dp)
                    .background(bgColor),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = nameStr,
                    style = TextStyle(color = textColor, fontSize = 14.sp)
                )
                Spacer(GlanceModifier.defaultWeight())
                Text(
                    text = timeStr,
                    style = TextStyle(color = textColor, fontSize = 14.sp, fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal)
                )
            }
        }
    }
}
