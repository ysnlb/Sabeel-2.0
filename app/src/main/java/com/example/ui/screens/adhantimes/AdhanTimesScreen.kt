package com.example.ui.screens.adhantimes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.format.DateTimeFormatter
import com.example.domain.model.PrayerInfo
import com.example.domain.repository.SettingsRepository
import com.example.R

@Composable
fun AdhanTimesScreen(
    modifier: Modifier = Modifier,
    viewModel: AdhanTimesViewModel = viewModel(
        factory = AdhanTimesViewModelFactory(SettingsRepository(LocalContext.current.applicationContext), LocalContext.current.applicationContext)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp)
            .padding(top = 32.dp)
    ) {
        TopHeader(
            city = uiState.city,
            dateStr = uiState.dateStr
        )
        Spacer(modifier = Modifier.height(32.dp))
        HeroSection(
            nextPrayer = uiState.nextPrayer,
            countdown = uiState.countdown
        )
        Spacer(modifier = Modifier.height(40.dp))
        PrayersList(
            prayers = uiState.prayers,
            mutedPrayers = uiState.mutedPrayers,
            onMuteToggle = viewModel::toggleMute
        )
    }
}

@Composable
private fun TopHeader(city: String, dateStr: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = stringResource(R.string.location),
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = city,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = dateStr,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun HeroSection(nextPrayer: PrayerInfo?, countdown: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (nextPrayer != null) {
            Text(
                text = stringResource(id = nextPrayer.nameResId),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        Text(
            text = countdown.removePrefix("-"),
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 72.sp),
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Light,
            letterSpacing = (-2).sp
        )
    }
}

@Composable
private fun PrayersList(prayers: List<PrayerInfo>, mutedPrayers: Set<com.example.domain.model.PrayerType>, onMuteToggle: (com.example.domain.model.PrayerType) -> Unit) {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        items(prayers) { prayer ->
            val isNext = prayer.isNext
            val isMuted = mutedPrayers.contains(prayer.type)
            
            val containerColor = if (isNext) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
            val contentColor = if (isNext) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            val elevation = if (isNext) 12.dp else 2.dp
            
            Card(
                modifier = Modifier.fillMaxWidth().height(84.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = containerColor),
                elevation = CardDefaults.cardElevation(defaultElevation = elevation)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = prayer.nameResId),
                        style = if (isNext) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                        fontWeight = if (isNext) FontWeight.Bold else FontWeight.Medium,
                        color = contentColor
                    )
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = prayer.time.format(timeFormatter),
                            style = if (isNext) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
                            fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal,
                            color = contentColor
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        if (prayer.type != com.example.domain.model.PrayerType.SUNRISE) {
                            IconButton(onClick = { onMuteToggle(prayer.type) }, modifier = Modifier.size(24.dp)) {
                                Icon(
                                    imageVector = if (isMuted) Icons.Default.NotificationsOff else Icons.Default.NotificationsActive,
                                    contentDescription = if (isMuted) stringResource(R.string.unmute) else stringResource(R.string.mute),
                                    tint = contentColor.copy(alpha = 0.6f)
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }
    }
}

