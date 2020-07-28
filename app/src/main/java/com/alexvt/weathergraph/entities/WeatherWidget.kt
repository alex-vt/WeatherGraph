/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.entities

import java.text.SimpleDateFormat
import java.util.*

data class WeatherWidget(
    val widgetId: Int,
    val status: WidgetStatus,
    val dataSource: WeatherDataSource,
    val weatherData: WeatherData,
    val visualSettings: WidgetVisualSettings
) {
    override fun toString(): String {
        val timestamp = if (status.lastUpdatedTimeMillis > 0) {
            ", id $widgetId, " + SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
                .format(status.lastUpdatedTimeMillis)
        } else {
            ""
        }
        return "WeatherWidget (${dataSource.locationName}, ${status.statusMessage}$timestamp)"
    }
}

data class WidgetStatus(
    val lastUpdatedTimeMillis: Long,
    val isOk: Boolean,
    val statusMessage: String
)

data class WeatherDataSource(
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
    val openWeatherMapLocationId: Int,
    val updatePeriodMillis: Long,
    val aquicnCityName: String
)

data class WeatherData(
    val timezoneShiftMillis: Long,

    val temperatureKelvinPoints: List<Pair<Double, Long>>,
    val cloudPercentPoints: List<Pair<Int, Long>>,
    val precipitationMmHourPoints: List<Pair<Double, Long>>,
    val windMsPoints: List<Pair<Double, Long>>,

    val airQualityPoints: List<Pair<Int, Long>>,
    val uvIndexPoints: List<Pair<Int, Long>>,

    val sunriseSunsetIntervals: List<Pair<Long, Long>>
)

data class WidgetVisualSettings(
    val temperatureGraphPalette: List<Pair<Double, String>>,
    val cloudPercentPalette: List<Pair<Double, String>>,
    val precipitationBarsPalette: List<Pair<Double, String>>,
    val windSpeedPalette: List<Pair<Double, String>>,
    val sunriseSunsetPalette: List<Pair<Double, String>>,
    val airQualityPalette: List<Pair<Double, String>>,

    val showDaysAhead: Int,
    val marginTopPx: Int,
    val marginBottomPx: Int,
    val marginLeftPx: Int,
    val marginRightPx: Int,

    val backgroundColor: String,
    val gridColor: String,
    val textColor: String,

    val showCloudPercent: Boolean,
    val showPrecipitation: Boolean,
    val showWind: Boolean,
    val showAirQuality: Boolean,
    val showSunriseSunset: Boolean,

    val temperatureUnit: TemperatureUnit,
    val cloudPercentUnit: CloudPercentUnit,
    val precipitationUnit: PrecipitationUnit,
    val windSpeedUnit: WindSpeedUnit,
    val time24h: Boolean,

    val precipitationCutoffValue: Double,
    val windSpeedCutoffValue: Double,

    val showLocationName: Boolean,
    val showLastUpdateTime: Boolean,
    val showUnits: Boolean,
    val textSizePx: Int,
    val gridThicknessPx: Int,
    val temperatureThicknessPx: Int,
    val windSpeedThicknessPx: Int
)

object UnitConverter {

    fun kelvinsTo(unitTo: TemperatureUnit, value: Double) = when (unitTo) {
        TemperatureUnit.K -> value
        TemperatureUnit.C -> value - 273.15
        TemperatureUnit.F -> value * 9 / 5 - 459.67
    }

    fun percentTo(unitTo: CloudPercentUnit, value: Double) = when (unitTo) {
        CloudPercentUnit.PERCENT -> value
        CloudPercentUnit.NORMALIZED -> value / 100
    }

    fun mmTo(unitTo: PrecipitationUnit, value: Double) = when (unitTo) {
        PrecipitationUnit.MMH -> value
        PrecipitationUnit.INH -> value / 25.4
    }

    fun msTo(unitTo: WindSpeedUnit, value: Double) = when (unitTo) {
        WindSpeedUnit.MS -> value
        WindSpeedUnit.FTS -> value / 0.3048
        WindSpeedUnit.KMH -> value * 3.6
        WindSpeedUnit.MPH -> value * 3.6 / 1.60934
        WindSpeedUnit.KN -> value * 3.6 / 1.852
    }
}

enum class TemperatureUnit { K, C, F }

enum class CloudPercentUnit { PERCENT, NORMALIZED }

enum class PrecipitationUnit { MMH, INH }

enum class WindSpeedUnit { MS, FTS, KMH, MPH, KN }