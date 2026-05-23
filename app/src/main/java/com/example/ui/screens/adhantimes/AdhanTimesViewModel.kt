package com.example.ui.screens.adhantimes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.batoulapps.adhan.CalculationMethod
import com.example.R
import com.example.domain.model.PrayerInfo
import com.example.domain.model.PrayerType
import com.example.domain.usecase.PrayerCalculationUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import androidx.lifecycle.ViewModelProvider
import com.example.domain.alarm.NextPrayerNotificationManager
import com.example.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull

data class AdhanTimesUiState(
    val city: String = "Makkah",
    val dateStr: String = "",
    val prayers: List<PrayerInfo> = emptyList(),
    val nextPrayer: PrayerInfo? = null,
    val countdown: String = "00:00:00",
    val mutedPrayers: Set<PrayerType> = emptySet()
)

class AdhanTimesViewModel(private val repository: SettingsRepository, private val context: android.content.Context) : ViewModel() {
    private val _uiState = MutableStateFlow(AdhanTimesUiState())
    val uiState: StateFlow<AdhanTimesUiState> = _uiState.asStateFlow()

    private val calculatePrayers = PrayerCalculationUseCase()
    private val notificationManager = NextPrayerNotificationManager(context)

    private var currentLat: Double = 21.4225
    private var currentLng: Double = 39.8262
    private var currentCity: String = "Makkah"
    private var currentMethod: CalculationMethod = CalculationMethod.UMM_AL_QURA
    private var currentAdjustments: Map<PrayerType, Int> = emptyMap()

    init {
        observeSettings()
        startClock()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            repository.mutedPrayers.collect { muted ->
                _uiState.update { it.copy(mutedPrayers = muted) }
            }
        }
        viewModelScope.launch {
            combine(
                repository.latitude,
                repository.longitude,
                repository.cityName,
                repository.calculationMethod,
                repository.getPrayerAdjustments()
            ) { lat, lon, city, method, adjustments ->
                // Default to Makkah if not set
                val finalLat = lat ?: 21.4225
                val finalLon = lon ?: 39.8262
                val finalCity = city ?: "Makkah"
                
                Triple(Triple(finalLat, finalLon, finalCity), method, adjustments)
            }.collect { (latLonCity, method, adjustments) ->
                val (lat, lon, city) = latLonCity
                currentLat = lat
                currentLng = lon
                currentCity = city
                currentMethod = method
                currentAdjustments = adjustments
                _uiState.update { it.copy(city = city) }
                updateTimetable()
                
                // Update widgets
                com.example.ui.widget.NextPrayerWidgetReceiver.update(context)
                com.example.ui.widget.TimetableWidgetReceiver.update(context)
            }
        }
    }

    private fun updateTimetable() {
        val lat = currentLat
        val lng = currentLng
        val city = currentCity
        val method = currentMethod
        val adjustments = currentAdjustments
        
        val now = LocalDateTime.now()
        val today = now.toLocalDate()

        var daysOffset = 0L
        var foundNext = false

        while (!foundNext && daysOffset < 10) { // Limit to avoid infinite loop just in case
            val targetDate = today.plusDays(daysOffset)
            val times = calculatePrayers(lat, lng, targetDate, method, adjustments)
            
            val calculations = listOf(
                PrayerInfo(PrayerType.FAJR, R.string.fajr, times.fajr),
                PrayerInfo(PrayerType.SUNRISE, R.string.sunrise, times.sunrise),
                PrayerInfo(PrayerType.DHUHR, R.string.dhuhr, times.dhuhr),
                PrayerInfo(PrayerType.ASR, R.string.asr, times.asr),
                PrayerInfo(PrayerType.MAGHRIB, R.string.maghrib, times.maghrib),
                PrayerInfo(PrayerType.ISHA, R.string.isha, times.isha)
            )

            if (daysOffset == 0L) {
                // Populate today's display list
                val nextInToday = calculations.firstOrNull { it.time.isAfter(now) }
                val updatedPrayers = calculations.map { 
                    it.copy(isNext = nextInToday != null && it.type == nextInToday.type) 
                }

                val dateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")
                
                _uiState.update { it.copy(
                    city = city, // Use the dynamically retrieved city instead of hardcoded Makkah
                    dateStr = today.format(dateFormatter),
                    prayers = updatedPrayers,
                    nextPrayer = nextInToday
                ) }
                
                if (nextInToday != null) {
                    foundNext = true
                    notificationManager.updateNextPrayer(
                        prayerName = context.getString(nextInToday.nameResId),
                        timeInMillis = nextInToday.time.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                    )
                }
            } else {
                // We are looking for the next prayer in the following days
                val next = calculations.firstOrNull { it.time.isAfter(now) }
                if (next != null) {
                    _uiState.update { it.copy(nextPrayer = next) }
                    foundNext = true
                    notificationManager.updateNextPrayer(
                        prayerName = context.getString(next.nameResId),
                        timeInMillis = next.time.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                    )
                }
            }
            daysOffset++
        }
    }

    private fun startClock() {
        viewModelScope.launch {
            while (true) {
                val now = LocalDateTime.now()
                val next = _uiState.value.nextPrayer
                
                if (next != null) {
                    if (now.isAfter(next.time) || now.isEqual(next.time)) {
                        // The time for the next prayer has arrived! Recalculate to slide forward
                        updateTimetable()
                    } else {
                        val duration = Duration.between(now, next.time)
                        val hours = duration.toHours()
                        val minutes = duration.toMinutes() % 60
                        val seconds = duration.seconds % 60
                        val countdownStr = String.format("-%02d:%02d:%02d", hours, minutes, seconds)
                        
                        _uiState.update { it.copy(countdown = countdownStr) }
                    }
                }
                delay(1000)
            }
        }
    }

    fun toggleMute(prayer: PrayerType) {
        val currentMuted = _uiState.value.mutedPrayers
        val isMuted = currentMuted.contains(prayer)
        viewModelScope.launch {
            repository.setPrayerMuted(prayer, !isMuted)
        }
    }
}

class AdhanTimesViewModelFactory(private val repository: SettingsRepository, private val context: android.content.Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AdhanTimesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AdhanTimesViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
