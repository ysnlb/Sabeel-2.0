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
import androidx.glance.layout.fillMaxSize
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

class NextPrayerWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = SettingsRepository(context)
        val lat = repo.latitude.first() ?: 21.4225
        val lng = repo.longitude.first() ?: 39.8262
        val city = repo.cityName.first() ?: "Makkah"
        val method = repo.calculationMethod.first()
        val adjustments = repo.getPrayerAdjustments().first()
        
        val calcUseCase = PrayerCalculationUseCase()
        val now = LocalDateTime.now()
        
        // Find next prayer
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

        provideContent {
            GlanceTheme {
                NextPrayerContent(city, next)
            }
        }
    }
}

@Composable
fun NextPrayerContent(city: String, next: PrayerInfo) {
    val context = LocalContext.current
    val timeStr = next.time.format(DateTimeFormatter.ofPattern("HH:mm"))
    val nameStr = context.getString(next.nameResId)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(16.dp)
            .background(GlanceTheme.colors.surface),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = city,
            style = TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                fontSize = 14.sp
            )
        )
        Text(
            text = nameStr,
            style = TextStyle(
                color = GlanceTheme.colors.primary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            ),
            modifier = GlanceModifier.padding(vertical = 4.dp)
        )
        Text(
            text = "at $timeStr",
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = 18.sp
            )
        )
    }
}
