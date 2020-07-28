/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.usecases

import com.alexvt.weathergraph.entities.TemperatureUnit
import com.alexvt.weathergraph.entities.UnitConverter
import com.alexvt.weathergraph.repositories.*
import io.reactivex.Observable
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

typealias TemperatureDataSet = List<Pair<Double, Long>>
typealias DayTemperatureDataSet = Pair<Int, TemperatureDataSet>
typealias DailyTemperatureDataSet = List<DayTemperatureDataSet>

class UserViewWeatherDetailsUseCase private constructor(
    editedWidgetRepository: EditedWeatherWidgetRepository,
    private val log: LogRepository
) {
    @Singleton
    class Factory @Inject constructor(
        private val editedWidgetRepository: EditedWeatherWidgetRepository,
        private val log: LogRepository
    ) {
        fun create() = UserViewWeatherDetailsUseCase(editedWidgetRepository, log)
    }

    private data class DailyTemperatureItem(
        val nowTempK: Double?,
        val minTempK: Double,
        val maxTempK: Double,
        val timestamp: Long,
        val timezoneShiftMillis: Long
    )

    data class ExtendedDailyTemperatureItem(
        val isToday: Boolean,
        val dateText: String,
        val weekDayText: String,
        val maxTempText: String?,
        val minTempText: String?,
        val nowTempText: String?,
        val minTempNormalized: Double,
        val maxTempNormalized: Double,
        val nowTempNormalized: Double?
    )

    val dayWeatherItemsObservable: Observable<List<ExtendedDailyTemperatureItem>> =
        editedWidgetRepository.observe().filter {
            it.status.isOk
        }.map {
            with(it.weatherData) {
                temperatureKelvinPoints.toDailyTemperatures(timezoneShiftMillis)
            }.take(it.visualSettings.showDaysAhead)
                .getExtended(it.visualSettings.showUnits, it.visualSettings.temperatureUnit)
        }

    val sunriseSunsetObservable: Observable<Pair<String, String>> =
        editedWidgetRepository.observe().filter {
            it.status.isOk
        }.map {
            with(it.weatherData) {
                sunriseSunsetIntervals.first().let { (sunriseTimestamp, sunsetTimestamp) ->
                    Pair(
                        sunriseTimestamp.getDateTime(timezoneShiftMillis).getLocalTimeText(),
                        sunsetTimestamp.getDateTime(timezoneShiftMillis).getLocalTimeText()
                    )
                }
            }
        }

    private fun Long.getDayOfMonth(timezoneShiftMillis: Long) =
        Instant.ofEpochMilli(this).atOffset(timezoneShiftMillis.getZoneOffset()).dayOfMonth

    private fun Int.isToday(timezoneShiftMillis: Long) =
        this == System.currentTimeMillis().getDayOfMonth(timezoneShiftMillis)

    private fun Long.getZoneOffset() = ZoneOffset.ofTotalSeconds(toInt() / 1000)

    private fun DailyTemperatureDataSet.regularCount() = map { it.second.count() }.max() ?: 0

    private fun DayTemperatureDataSet.count() = this.second.count()

    private fun TemperatureDataSet.getTemperatures() = map { (temperature, _) -> temperature }

    private fun DailyTemperatureDataSet.trimIncompleteLastDay() = filter { dayDataSet ->
        dayDataSet != last() || dayDataSet.count() == regularCount()
    }

    private fun TemperatureDataSet.toDailyTemperatures(timezoneShiftMillis: Long) =
        groupBy { (_, timeMillis) ->
            timeMillis.getDayOfMonth(timezoneShiftMillis)
        }.toList().trimIncompleteLastDay().map { (day, dataSet) ->
            DailyTemperatureItem(
                nowTempK = if (day.isToday(timezoneShiftMillis)) {
                    dataSet.getTemperatures().first()
                } else {
                    null
                },
                minTempK = dataSet.getTemperatures().min()!!,
                maxTempK = dataSet.getTemperatures().max()!!,
                timestamp = dataSet.first().let { (_, timestamp) -> timestamp },
                timezoneShiftMillis = timezoneShiftMillis
            )
        }

    private fun Double.normalizeIn(min: Double, max: Double) = (this - min) / (max - min)

    private fun Double.normalizeIn(items: List<DailyTemperatureItem>) = normalizeIn(
        items.map { it.minTempK }.min()!!,
        items.map { it.maxTempK }.max()!!
    )

    private val unitMap = mapOf(
        TemperatureUnit.K to "K",
        TemperatureUnit.F to "F",
        TemperatureUnit.C to "C"
    )

    private val degreeSignMap = mapOf(
        TemperatureUnit.K to "",
        TemperatureUnit.F to "°",
        TemperatureUnit.C to "°"
    )

    private fun Double.formatTemp(showUnits: Boolean, unit: TemperatureUnit) =
        UnitConverter.kelvinsTo(unit, this).roundToInt().let {
            "${it}${degreeSignMap[unit]}"
        }.let {
            if (showUnits) "${it}${unitMap[unit]}" else it
        }

    private fun Long.getDateTime(timezoneShiftMillis: Long) = Instant
        .ofEpochMilli(this)
        .atOffset(timezoneShiftMillis.getZoneOffset())

    private fun OffsetDateTime.getLocalDayOfWeekText() =
        DateTimeFormatter.ofPattern("ccc").format(this)

    private fun OffsetDateTime.getLocalDateText() =
        DateTimeFormatter.ofPattern("MMM d").format(this)

    private fun OffsetDateTime.getLocalTimeText() =
        DateTimeFormatter.ofPattern("HH:mm").format(this)

    private fun Double.isDiffTo(vararg other: Double?) = takeIf { other.all { this != it } }

    private fun List<DailyTemperatureItem>.getExtended(showUnits: Boolean, unit: TemperatureUnit) =
        map { (nowTempK, minTempK, maxTempK, timestamp, timezoneShiftMillis) ->
            ExtendedDailyTemperatureItem(
                isToday = nowTempK != null,
                dateText = timestamp.getDateTime(timezoneShiftMillis).getLocalDateText(),
                weekDayText = timestamp.getDateTime(timezoneShiftMillis).getLocalDayOfWeekText(),
                nowTempText = nowTempK?.formatTemp(showUnits, unit),
                minTempText = minTempK.isDiffTo(nowTempK, maxTempK)?.formatTemp(showUnits, unit),
                maxTempText = maxTempK.isDiffTo(nowTempK)?.formatTemp(showUnits, unit),
                nowTempNormalized = nowTempK?.normalizeIn(this),
                minTempNormalized = minTempK.normalizeIn(this),
                maxTempNormalized = maxTempK.normalizeIn(this)
            )
        }

}