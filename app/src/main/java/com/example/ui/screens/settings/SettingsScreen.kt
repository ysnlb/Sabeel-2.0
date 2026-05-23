package com.example.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.model.PrayerType
import com.example.domain.repository.SettingsRepository
import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import com.example.R

import com.example.domain.repository.LocationSearchRepository
import com.example.domain.repository.LocationResult

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(SettingsRepository(LocalContext.current.applicationContext), LocationSearchRepository(), LocalContext.current.applicationContext)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    
    val locationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.any { it.value }
        if (granted) {
            viewModel.updateLocationMode(LocationMode.AUTOMATIC)
        } else {
            // Permission denied, maybe fallback to manual
            viewModel.updateLocationMode(LocationMode.MANUAL)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp),
        contentPadding = PaddingValues(top = 32.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            SettingsSection(title = stringResource(R.string.settings_general)) {
                SettingSelector(
                    title = stringResource(R.string.settings_language),
                    options = AppLanguage.values().toList(),
                    selectedOption = uiState.language,
                    onOptionSelected = viewModel::updateLanguage,
                    labelMapper = { stringResource(it.stringRes()) }
                )
                Spacer(modifier = Modifier.height(24.dp))
                SettingSelector(
                    title = stringResource(R.string.settings_theme),
                    options = SettingsRepository.AppTheme.values().toList(),
                    selectedOption = uiState.themeMode,
                    onOptionSelected = viewModel::updateThemeMode,
                    labelMapper = { stringResource(it.stringRes()) }
                )
            }
        }

        item {
            SettingsSection(title = stringResource(R.string.settings_location)) {
                SettingSelector(
                    title = stringResource(R.string.settings_mode),
                    options = LocationMode.values().toList(),
                    selectedOption = uiState.locationMode,
                    onOptionSelected = { mode ->
                        if (mode == LocationMode.AUTOMATIC) {
                            val fine = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            val coarse = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            
                            if (fine || coarse) {
                                viewModel.updateLocationMode(mode)
                            } else {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        } else {
                            viewModel.updateLocationMode(mode)
                        }
                    },
                    labelMapper = { stringResource(it.stringRes()) }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                if (uiState.currentCity.isNotBlank()) {
                    Text(
                        text = stringResource(R.string.settings_current_city, uiState.currentCity),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                if (uiState.locationMode == LocationMode.MANUAL) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = viewModel::searchLocation,
                        label = { Text(stringResource(R.string.settings_search_city)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { keyboardController?.hide() })
                    )
                    
                    if (uiState.isSearching) {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    } else if (uiState.searchResults.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            uiState.searchResults.forEach { result ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { 
                                            viewModel.selectLocation(result) 
                                            keyboardController?.hide()
                                        }
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = result.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            SettingsSection(title = stringResource(R.string.settings_notifications)) {
                CustomAdhanSelector(
                    title = stringResource(R.string.settings_sound_type),
                    options = NotificationType.values().toList(),
                    selectedOption = uiState.notificationType,
                    playingOption = viewModel.playingPreview.collectAsStateWithLifecycle().value,
                    onOptionSelected = viewModel::updateNotificationType,
                    onPreviewClick = viewModel::togglePreview,
                    labelMapper = { stringResource(it.stringRes()) }
                )
            }
        }

        item {
            SettingsSection(title = stringResource(R.string.settings_prayer_adjustments)) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    PrayerType.values().forEach { prayer ->
                        if (prayer != PrayerType.SUNRISE) {
                            PrayerAdjustmentItem(
                                prayer = prayer,
                                adjustments = uiState.prayerAdjustments,
                                onAdjust = viewModel::adjustPrayerTime
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                content()
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> CustomAdhanSelector(
    title: String,
    options: List<T>,
    selectedOption: T,
    playingOption: T?,
    onOptionSelected: (T) -> Unit,
    onPreviewClick: (T) -> Unit,
    labelMapper: @Composable (T) -> String
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { option ->
                val isSelected = option == selectedOption
                FilterChip(
                    selected = isSelected,
                    onClick = { onOptionSelected(option) },
                    label = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(labelMapper(option))
                            val showPlay = (option as? NotificationType).let { 
                                it == NotificationType.ADHAN || it == NotificationType.STANDARD_CHIME 
                            }
                            if (showPlay) {
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = { onPreviewClick(option) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = if (option == playingOption) Icons.Default.Close else Icons.Default.PlayArrow,
                                        contentDescription = if (option == playingOption) "Stop" else "Play",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> SettingSelector(
    title: String,
    options: List<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    labelMapper: @Composable (T) -> String
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(12.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { option ->
                val isSelected = option == selectedOption
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                        .clickable { onOptionSelected(option) }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = labelMapper(option),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun PrayerAdjustmentItem(
    prayer: PrayerType,
    adjustments: Map<PrayerType, Int>,
    onAdjust: (PrayerType, Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val prayerNameRes = when(prayer) {
            PrayerType.FAJR -> R.string.fajr
            PrayerType.SUNRISE -> R.string.sunrise
            PrayerType.DHUHR -> R.string.dhuhr
            PrayerType.ASR -> R.string.asr
            PrayerType.MAGHRIB -> R.string.maghrib
            PrayerType.ISHA -> R.string.isha
        }
        val prayerName = stringResource(prayerNameRes)
        Text(
            text = prayerName,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
        )
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IconButton(
                onClick = { onAdjust(prayer, -1) },
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = stringResource(R.string.decrease),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Text(
                text = "${if ((adjustments[prayer] ?: 0) > 0) "+" else ""}${adjustments[prayer] ?: 0}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.width(36.dp),
                textAlign = TextAlign.Center
            )
            
            IconButton(
                onClick = { onAdjust(prayer, 1) },
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.increase),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

fun AppLanguage.stringRes() = when(this) {
    AppLanguage.SYSTEM_DEFAULT -> R.string.lang_system_default
    AppLanguage.ARABIC -> R.string.lang_arabic
    AppLanguage.ENGLISH -> R.string.lang_english
    AppLanguage.FRENCH -> R.string.lang_french
}

fun SettingsRepository.AppTheme.stringRes() = when(this) {
    SettingsRepository.AppTheme.SYSTEM_DEFAULT -> R.string.theme_system_default
    SettingsRepository.AppTheme.LIGHT -> R.string.theme_light
    SettingsRepository.AppTheme.DARK -> R.string.theme_dark
}

fun LocationMode.stringRes() = when(this) {
    LocationMode.AUTOMATIC -> R.string.loc_mode_automatic
    LocationMode.MANUAL -> R.string.loc_mode_manual
}

fun NotificationType.stringRes() = when(this) {
    NotificationType.ADHAN -> R.string.notif_adhan
    NotificationType.STANDARD_CHIME -> R.string.notif_standard_chime
    NotificationType.MUTE -> R.string.notif_mute
}
