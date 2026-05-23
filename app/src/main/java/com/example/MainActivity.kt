package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.ui.theme.MyApplicationTheme
import com.example.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    val repository = SettingsRepository(this)
    
    // Update the next prayer notification and alarms
    kotlinx.coroutines.GlobalScope.launch {
        com.example.domain.alarm.NextPrayerNotificationManager(this@MainActivity).calculateAndUpdateNextPrayer()
        com.example.domain.alarm.AdhanAlarmScheduler(this@MainActivity).scheduleAllPrayers()
    }
    
    // Fetch location if automatic
    kotlinx.coroutines.GlobalScope.launch {
        if (repository.locationMode.first() == com.example.ui.screens.settings.LocationMode.AUTOMATIC) {
            val fine = androidx.core.content.ContextCompat.checkSelfPermission(this@MainActivity, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
            val coarse = androidx.core.content.ContextCompat.checkSelfPermission(this@MainActivity, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
            
            if (fine || coarse) {
                val tracker = com.example.domain.location.GpsLocationTracker(this@MainActivity)
                val result = tracker.getCurrentLocation()
                if (result != null) {
                    repository.setLocation(result.lat, result.lon, result.name)
                }
            }
        }
    }
    
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
        }
    }

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        val alarmManager = getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
        if (!alarmManager.canScheduleExactAlarms()) {
            val intent = android.content.Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            intent.data = android.net.Uri.parse("package:$packageName")
            startActivity(intent)
        }
    }
    
    setContent {
      val themeMode by repository.themeMode.collectAsState(initial = SettingsRepository.AppTheme.SYSTEM_DEFAULT)
      
      val isDarkTheme = when (themeMode) {
          SettingsRepository.AppTheme.LIGHT -> false
          SettingsRepository.AppTheme.DARK -> true
          SettingsRepository.AppTheme.SYSTEM_DEFAULT -> isSystemInDarkTheme()
      }
      
      MyApplicationTheme(darkTheme = isDarkTheme) {
        com.example.ui.navigation.AppNavGraph()
      }
    }
  }
}
