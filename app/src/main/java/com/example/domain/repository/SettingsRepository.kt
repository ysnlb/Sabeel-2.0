package com.example.domain.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.batoulapps.adhan.CalculationMethod
import com.example.domain.model.PrayerType
import com.example.ui.screens.settings.AppLanguage
import com.example.ui.screens.settings.LocationMode
import com.example.ui.screens.settings.NotificationType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private val dataStore = context.dataStore

    enum class AppTheme {
        SYSTEM_DEFAULT,
        LIGHT,
        DARK
    }

    companion object {
        val LANGUAGE_KEY = stringPreferencesKey("app_language")
        val NOTIFICATION_TYPE_KEY = stringPreferencesKey("notification_type")
        val LOCATION_MODE_KEY = stringPreferencesKey("location_mode")
        val CALCULATION_METHOD_KEY = stringPreferencesKey("calculation_method")
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        
        val LATITUDE_KEY = androidx.datastore.preferences.core.doublePreferencesKey("latitude")
        val LONGITUDE_KEY = androidx.datastore.preferences.core.doublePreferencesKey("longitude")
        val CITY_NAME_KEY = stringPreferencesKey("city_name")

        fun getPrayerAdjustmentKey(prayerType: PrayerType) = intPreferencesKey("adjustment_${prayerType.name}")
        fun getPrayerMuteKey(prayerType: PrayerType) = androidx.datastore.preferences.core.booleanPreferencesKey("mute_${prayerType.name}")
    }

    val latitude: Flow<Double?> = dataStore.data.map { it[LATITUDE_KEY] }
    val longitude: Flow<Double?> = dataStore.data.map { it[LONGITUDE_KEY] }
    val cityName: Flow<String?> = dataStore.data.map { it[CITY_NAME_KEY] }

    val mutedPrayers: Flow<Set<PrayerType>> = dataStore.data.map { preferences ->
        PrayerType.values().filter { preferences[getPrayerMuteKey(it)] == true }.toSet()
    }

    val themeMode: Flow<AppTheme> = dataStore.data.map { preferences ->
        preferences[THEME_MODE_KEY]?.let { name ->
            try { AppTheme.valueOf(name) } catch (e: Exception) { AppTheme.SYSTEM_DEFAULT }
        } ?: AppTheme.SYSTEM_DEFAULT
    }

    val appLanguage: Flow<AppLanguage> = dataStore.data.map { preferences ->
        preferences[LANGUAGE_KEY]?.let { name ->
            try { AppLanguage.valueOf(name) } catch (e: Exception) { AppLanguage.SYSTEM_DEFAULT }
        } ?: AppLanguage.SYSTEM_DEFAULT
    }

    val notificationType: Flow<NotificationType> = dataStore.data.map { preferences ->
        preferences[NOTIFICATION_TYPE_KEY]?.let { name ->
            try { NotificationType.valueOf(name) } catch (e: Exception) { NotificationType.ADHAN }
        } ?: NotificationType.ADHAN
    }

    val locationMode: Flow<LocationMode> = dataStore.data.map { preferences ->
        preferences[LOCATION_MODE_KEY]?.let { name ->
            try { LocationMode.valueOf(name) } catch (e: Exception) { LocationMode.AUTOMATIC }
        } ?: LocationMode.AUTOMATIC
    }

    val calculationMethod: Flow<CalculationMethod> = dataStore.data.map { preferences ->
        preferences[CALCULATION_METHOD_KEY]?.let { name ->
            try { CalculationMethod.valueOf(name) } catch (e: Exception) { CalculationMethod.UMM_AL_QURA }
        } ?: CalculationMethod.UMM_AL_QURA
    }

    fun getPrayerAdjustments(): Flow<Map<PrayerType, Int>> = dataStore.data.map { preferences ->
        PrayerType.values().associateWith { prayer ->
            preferences[getPrayerAdjustmentKey(prayer)] ?: 0
        }
    }

    suspend fun setThemeMode(theme: AppTheme) {
        dataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = theme.name
        }
    }

    suspend fun setAppLanguage(language: AppLanguage) {
        dataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = language.name
        }
    }

    suspend fun setNotificationType(type: NotificationType) {
        dataStore.edit { preferences ->
            preferences[NOTIFICATION_TYPE_KEY] = type.name
        }
    }

    suspend fun setLocationMode(mode: LocationMode) {
        dataStore.edit { preferences ->
            preferences[LOCATION_MODE_KEY] = mode.name
        }
    }

    suspend fun setCalculationMethod(method: CalculationMethod) {
        dataStore.edit { preferences ->
            preferences[CALCULATION_METHOD_KEY] = method.name
        }
    }

    suspend fun setLocation(lat: Double, lon: Double, city: String) {
        dataStore.edit { preferences ->
            preferences[LATITUDE_KEY] = lat
            preferences[LONGITUDE_KEY] = lon
            preferences[CITY_NAME_KEY] = city
        }
    }

    suspend fun setPrayerAdjustment(prayer: PrayerType, delta: Int) {
        dataStore.edit { preferences ->
            val current = preferences[getPrayerAdjustmentKey(prayer)] ?: 0
            val newAdjustment = (current + delta).coerceIn(-60, 60)
            preferences[getPrayerAdjustmentKey(prayer)] = newAdjustment
        }
    }

    suspend fun setPrayerMuted(prayer: PrayerType, isMuted: Boolean) {
        dataStore.edit { preferences ->
            preferences[getPrayerMuteKey(prayer)] = isMuted
        }
    }
}
