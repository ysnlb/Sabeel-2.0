package com.example.domain.model

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Represents the daily prayer times for a specific location.
 */
data class DayPrayerTimes(
    val date: LocalDate,
    val city: String,
    val fajr: LocalDateTime,
    val sunrise: LocalDateTime,
    val dhuhr: LocalDateTime,
    val asr: LocalDateTime,
    val maghrib: LocalDateTime,
    val isha: LocalDateTime
)

/**
 * Represents an individual prayer for the UI and notifications.
 */
data class PrayerInfo(
    val type: PrayerType,
    val nameResId: Int, // e.g., R.string.fajr
    val time: LocalDateTime,
    val isNext: Boolean = false
)

enum class PrayerType {
    FAJR, SUNRISE, DHUHR, ASR, MAGHRIB, ISHA
}
