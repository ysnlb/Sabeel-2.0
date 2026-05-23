package com.example.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.domain.model.PrayerType
import com.example.domain.repository.SettingsRepository
import com.example.ui.widget.NextPrayerWidgetReceiver
import com.example.ui.widget.TimetableWidgetReceiver
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.domain.repository.LocationSearchRepository
import com.example.domain.repository.LocationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import android.media.MediaPlayer
import java.io.File
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.widget.Toast

enum class AppLanguage(val displayName: String, val tag: String) {
    SYSTEM_DEFAULT("System Default", ""),
    ARABIC("Arabic", "ar"),
    ENGLISH("English", "en"),
    FRENCH("French", "fr")
}

enum class LocationMode(val displayName: String) {
    AUTOMATIC("Automatic (GPS)"),
    MANUAL("Manual City Search")
}

enum class NotificationType(val displayName: String) {
    ADHAN("Adhan"),
    STANDARD_CHIME("Notification"),
    MUTE("Mute")
}

data class SettingsUiState(
    val language: AppLanguage = AppLanguage.SYSTEM_DEFAULT,
    val locationMode: LocationMode = LocationMode.AUTOMATIC,
    val notificationType: NotificationType = NotificationType.ADHAN,
    val themeMode: SettingsRepository.AppTheme = SettingsRepository.AppTheme.SYSTEM_DEFAULT,
    val prayerAdjustments: Map<PrayerType, Int> = PrayerType.values().associateWith { 0 },
    val searchQuery: String = "",
    val searchResults: List<LocationResult> = emptyList(),
    val isSearching: Boolean = false,
    val currentCity: String = ""
)

class SettingsViewModel(
    private val repository: SettingsRepository, 
    private val locationSearchRepo: LocationSearchRepository,
    private val context: android.content.Context
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _searchResults = MutableStateFlow<List<LocationResult>>(emptyList())
    private val _isSearching = MutableStateFlow(false)
    private var searchJob: Job? = null
    
    private var mediaPlayer: MediaPlayer? = null
    private val _downloadingAdhans = MutableStateFlow<Set<NotificationType>>(emptySet())
    val downloadingAdhans: StateFlow<Set<NotificationType>> = _downloadingAdhans.asStateFlow()

    val uiState: StateFlow<SettingsUiState> = combine(
        combine(
            repository.appLanguage,
            repository.locationMode,
            repository.notificationType,
            repository.themeMode,
            repository.cityName
        ) { language, locationMode, notificationType, themeMode, currentCity ->
            SettingsUiState(
                language = language,
                locationMode = locationMode,
                notificationType = notificationType,
                themeMode = themeMode,
                currentCity = currentCity ?: "Unknown"
            )
        },
        repository.getPrayerAdjustments(),
        _searchQuery,
        _searchResults,
        _isSearching
    ) { baseState, prayerAdjustments, query, results, isSearching ->
        baseState.copy(
            prayerAdjustments = prayerAdjustments,
            searchQuery = query,
            searchResults = results,
            isSearching = isSearching
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun updateLanguage(language: AppLanguage) {
        viewModelScope.launch {
            repository.setAppLanguage(language)
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                val localeManager = context.getSystemService(android.app.LocaleManager::class.java)
                val localeList = if (language.tag.isEmpty()) {
                    android.os.LocaleList.getEmptyLocaleList()
                } else {
                    android.os.LocaleList.forLanguageTags(language.tag)
                }
                localeManager?.applicationLocales = localeList
            } else {
                val localeList = if (language.tag.isEmpty()) {
                    LocaleListCompat.getEmptyLocaleList()
                } else {
                    LocaleListCompat.forLanguageTags(language.tag)
                }
                AppCompatDelegate.setApplicationLocales(localeList)
            }
            
            updateWidgets()
        }
    }

    fun updateThemeMode(theme: SettingsRepository.AppTheme) {
        viewModelScope.launch {
            repository.setThemeMode(theme)
        }
    }

    fun searchLocation(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        searchJob = viewModelScope.launch {
            _isSearching.value = true
            delay(500) // debounce
            _searchResults.value = locationSearchRepo.searchLocations(query)
            _isSearching.value = false
        }
    }

    fun selectLocation(result: LocationResult) {
        viewModelScope.launch {
            repository.setLocation(result.lat, result.lon, result.name)
            repository.setLocationMode(LocationMode.MANUAL) // Force manual mode when a location is picked
            _searchQuery.value = ""
            _searchResults.value = emptyList()
            updateWidgets()
        }
    }

    fun updateLocationMode(mode: LocationMode) {
        viewModelScope.launch {
            repository.setLocationMode(mode)
            if (mode == LocationMode.AUTOMATIC) {
                if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                    androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    val tracker = com.example.domain.location.GpsLocationTracker(context)
                    val result = tracker.getCurrentLocation()
                    if (result != null) {
                        repository.setLocation(result.lat, result.lon, result.name)
                    }
                }
            }
            updateWidgets()
        }
    }

    fun updateNotificationType(type: NotificationType) {
        viewModelScope.launch {
            repository.setNotificationType(type)
            updateWidgets()
        }
    }

    private val _playingPreview = MutableStateFlow<NotificationType?>(null)
    val playingPreview: StateFlow<NotificationType?> = _playingPreview.asStateFlow()

    fun togglePreview(type: NotificationType) {
        if (_playingPreview.value == type) {
            stopPreview()
        } else {
            playPreview(type)
        }
    }

    private fun playPreview(type: NotificationType) {
        stopPreview()

        try {
            if (type == NotificationType.MUTE) return

            if (type == NotificationType.STANDARD_CHIME) {
                val defaultUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                mediaPlayer = MediaPlayer.create(context, defaultUri).apply { 
                    setOnCompletionListener { _playingPreview.value = null }
                    start() 
                }
                _playingPreview.value = type
                return
            }

            mediaPlayer = MediaPlayer.create(context, com.example.R.raw.adhan).apply {
                setOnCompletionListener { _playingPreview.value = null }
                start()
            }
            _playingPreview.value = type
        } catch (e: Exception) {
            e.printStackTrace()
            _playingPreview.value = null
        }
    }
    
    fun stopPreview() {
        mediaPlayer?.release()
        mediaPlayer = null
        _playingPreview.value = null
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayer?.release()
    }

    fun adjustPrayerTime(prayer: PrayerType, delta: Int) {
        viewModelScope.launch {
            repository.setPrayerAdjustment(prayer, delta)
            updateWidgets()
        }
    }
    
    private fun updateWidgets() {
        NextPrayerWidgetReceiver.update(context)
        TimetableWidgetReceiver.update(context)
        kotlinx.coroutines.GlobalScope.launch {
            com.example.domain.alarm.NextPrayerNotificationManager(context).calculateAndUpdateNextPrayer()
            com.example.domain.alarm.AdhanAlarmScheduler(context).scheduleAllPrayers()
        }
    }
}

class SettingsViewModelFactory(private val repository: SettingsRepository, private val locationSearchRepo: LocationSearchRepository, private val context: android.content.Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(repository, locationSearchRepo, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
