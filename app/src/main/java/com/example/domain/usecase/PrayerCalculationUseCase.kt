package com.example.domain.usecase

import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.PrayerTimes
import com.batoulapps.adhan.data.DateComponents
import com.example.domain.model.DayPrayerTimes
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

class PrayerCalculationUseCase {
    operator fun invoke(
        lat: Double,
        lng: Double,
        date: LocalDate,
        method: CalculationMethod = CalculationMethod.UMM_AL_QURA,
        adjustments: Map<com.example.domain.model.PrayerType, Int> = emptyMap()
    ): DayPrayerTimes {
        val coordinates = Coordinates(lat, lng)
        val dateComponents = DateComponents(date.year, date.monthValue, date.dayOfMonth)
        val parameters = method.parameters
        
        val prayerTimes = PrayerTimes(coordinates, dateComponents, parameters)
        val zoneId = ZoneId.systemDefault()
        
        return DayPrayerTimes(
            date = date,
            city = "", // City label will be assigned via Location services later
            fajr = prayerTimes.fajr.toInstant().atZone(zoneId).toLocalDateTime().plusMinutes(adjustments[com.example.domain.model.PrayerType.FAJR]?.toLong() ?: 0L),
            sunrise = prayerTimes.sunrise.toInstant().atZone(zoneId).toLocalDateTime().plusMinutes(adjustments[com.example.domain.model.PrayerType.SUNRISE]?.toLong() ?: 0L),
            dhuhr = prayerTimes.dhuhr.toInstant().atZone(zoneId).toLocalDateTime().plusMinutes(adjustments[com.example.domain.model.PrayerType.DHUHR]?.toLong() ?: 0L),
            asr = prayerTimes.asr.toInstant().atZone(zoneId).toLocalDateTime().plusMinutes(adjustments[com.example.domain.model.PrayerType.ASR]?.toLong() ?: 0L),
            maghrib = prayerTimes.maghrib.toInstant().atZone(zoneId).toLocalDateTime().plusMinutes(adjustments[com.example.domain.model.PrayerType.MAGHRIB]?.toLong() ?: 0L),
            isha = prayerTimes.isha.toInstant().atZone(zoneId).toLocalDateTime().plusMinutes(adjustments[com.example.domain.model.PrayerType.ISHA]?.toLong() ?: 0L)
        )
    }

    private fun Date.toLocalDateTime(): LocalDateTime {
        return this.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
    }
}
