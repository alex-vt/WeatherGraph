/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.repositories

import com.alexvt.weathergraph.entities.*
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Provides ready weather widget data with all updating under the hood.
 */
class WeatherWidgetRepository @Inject constructor(
    private val aquicnRemoteRepository: AquicnRemoteRepository,
    private val owmRemoteRepository: OwmRemoteRepository,
    private val localRepository: WeatherWidgetLocalRepository,
    private val log: LogRepository
) {
    fun add(weatherWidget: WeatherWidget) = localRepository.addOrUpdate(weatherWidget)

    private fun WeatherData?.hasAllDataSets() = this != null // todo consider data kind toggled off
            && this.temperatureKelvinPoints.isNotEmpty()
            && this.sunriseSunsetIntervals.isNotEmpty()

    private fun getStatus(
        previousTimeUpdated: Long,
        weatherData: WeatherData?,
        errorMessage: String? = null
    ) = WidgetStatus(
        lastUpdatedTimeMillis = if (weatherData.hasAllDataSets()) {
            System.currentTimeMillis()
        } else {
            previousTimeUpdated
        },
        isOk = weatherData.hasAllDataSets(),
        statusMessage = when {
            weatherData.hasAllDataSets() ->
                "OK"
            previousTimeUpdated > 0 ->
                errorMessage ?: "Data is outdated"
            else ->
                "No data yet"
        } // todo enum
    )

    private fun getWeatherData(oldData: WeatherData?, newData: WeatherData?) = when {
        newData != null ->
            newData
        oldData != null ->
            oldData
        else ->
            WeatherData(
                0, emptyList(), emptyList(), emptyList(), emptyList(), emptyList(),
                emptyList(), emptyList()
            )
    }

    private fun makeWidget(
        dataSource: WeatherDataSource,
        visualSettings: WidgetVisualSettings,
        widgetId: Int,
        previousTimeUpdated: Long = 0,
        oldWeatherData: WeatherData? = null,
        newWeatherData: WeatherData? = null,
        errorMessage: String? = null
    ) = WeatherWidget(
        widgetId = widgetId,
        status = getStatus(previousTimeUpdated, newWeatherData, errorMessage),
        dataSource = dataSource,
        weatherData = getWeatherData(oldWeatherData, newWeatherData),
        visualSettings = visualSettings
    )

    fun getCurrentAll() = localRepository.getAll()

    fun observeAll() = localRepository.observeAll()

    fun getCurrent(id: Int) = localRepository.getAll().firstOrNull { it.widgetId == id }

    private val defaultLocation = "London"
    private val defaultLatitude = 51.50853
    private val defaultLongitude = -0.12574
    private val defaultId = 2643743

    fun getUpdateTimeMillisOptions() = listOf(
        TimeUnit.MINUTES.toMillis(30),
        TimeUnit.HOURS.toMillis(1),
        TimeUnit.HOURS.toMillis(3),
        TimeUnit.HOURS.toMillis(12)
    )

    private fun celsiusToKelvin(celsius: Int) = celsius.toDouble() + 273.16

    fun getDefaultFreshStatus() = generateDefault(0).status

    fun generateDefault(widgetId: Int) = makeWidget( // todo evaluate default settings
        WeatherDataSource(
            defaultLocation, defaultLatitude, defaultLongitude, defaultId,
            getUpdateTimeMillisOptions().first(), ""
        ),
        WidgetVisualSettings(
            getTemperaturePaletteOptions().first(),
            getCloudPercentPaletteOptions().first(),
            getPrecipitationPaletteOptions().first(),
            getWindSpeedPaletteOptions().first(),
            getAirQualityPaletteOptions().first(),
            getSunriseSunsetPaletteOptions().first(),
            getShowDaysAheadOptions().last(),
            0/*20*/, 0/*45*/, 0, 0,
            getBackgroundColorOptions().second(),
            getGridColorOptions().second(),
            getTextColorOptions().first(),
            true,
            true,
            false,
            true,
            false,
            getTemperatureUnitOptions().first(),
            getCloudPercentUnitOptions().first(),
            getPrecipitationUnitOptions().first(),
            getWindSpeedUnitOptions().first(),
            true,
            getPrecipitationCutoffOptions(getPrecipitationUnitOptions().first()).first(),
            getWindSpeedCutoffOptions(getWindSpeedUnitOptions().first()).first(),
            true,
            true,
            false,
            getTextSizePxOptions().middle(),
            getGridThicknessPxOptions().first(),
            getGraphThicknessPxOptions().middle(),
            getGraphThicknessPxOptions().middle()
        ),
        generateId(widgetId)
    )

    fun getMaxMarginPx() = 50

    // todo choose based on text color
    fun getBackgroundColorOptions() = listOf(
        "#FFFFFFFF", "#AAFFFFFF", "#77FFFFFF", "#33FFFFFF", "#00000000", "#33000000", "#77000000", "#AA000000", "#FF000000"
    )

    fun getGridColorOptions() = listOf(
        "#FF000000", "#77000000", "#00000000", "#77FFFFFF", "#FFFFFFFF"
    )

    // todo choose based on text color
    fun getTextColorOptions() = listOf(
        "#FF000000", "#77000000", "#77FFFFFF", "#FFFFFFFF"
    )

    // todo choose based on dp (which should be from repository)
    fun getTextSizePxOptions() = listOf(20, 30, 45)

    // todo choose based on dp (which should be from repository)
    fun getGridThicknessPxOptions() = listOf(1, 2, 3)

    // todo choose based on dp (which should be from repository)
    fun getGraphThicknessPxOptions() = listOf(3, 5, 8)

    private fun <T> List<T>.second() = this[1]
    private fun <T> List<T>.middle() = this[size / 2]

    fun getSunriseSunsetPaletteOptions() = listOf(
        listOf(
            -1.0 to "#650000FF",
            0.0 to "#650000FF",
            0.0 to "#65FFFF00",
            1.0 to "#65FFFF00"
        ),
        listOf(
            -1.0 to "#A00000FF",
            0.0 to "#A00000FF",
            0.0 to "#A0FFFF00",
            1.0 to "#A0FFFF00"
        ),
        listOf(
            -1.0 to "#650000FF",
            0.0 to "#650000FF",
            0.0 to "#00000000",
            1.0 to "#00000000"
        ),
        listOf(
            -1.0 to "#A00000FF",
            0.0 to "#A00000FF",
            0.0 to "#00000000",
            1.0 to "#00000000"
        ),
        listOf(
            -1.0 to "#00000000",
            0.0 to "#00000000",
            0.0 to "#65FFFF00",
            1.0 to "#65FFFF00"
        ),
        listOf(
            -1.0 to "#00000000",
            0.0 to "#00000000",
            0.0 to "#A0FFFF00",
            1.0 to "#A0FFFF00"
        )
    )

    fun getCloudPercentPaletteOptions() = listOf(
        listOf(
            0.0 to "#65FFFF00",
            50.0 to "#65FFFFFF",
            100.0 to "#80CCCCCC"
        ),
        listOf(
            0.0 to "#A0FFFF00",
            50.0 to "#A0FFFFFF",
            100.0 to "#C0CCCCCC"
        ),
        listOf(
            0.0 to "#00000000",
            50.0 to "#65FFFFFF",
            100.0 to "#80CCCCCC"
        ),
        listOf(
            0.0 to "#00000000",
            50.0 to "#A0FFFFFF",
            100.0 to "#C0CCCCCC"
        )
    )

    fun getWindSpeedPaletteOptions() = listOf(
        listOf(
            0.0 to "#80DDDDFF",
            20.0 to "#DDDDFF",
            25.0 to "#CC7777",
            30.0 to "#CC7777"
        ),
        listOf(
            0.0 to "#80FFFFFF",
            25.0 to "#FFFFFFFF",
            30.0 to "#FFFFFFFF"
        ),
        listOf(
            0.0 to "#FFFFFF",
            20.0 to "#FFFFFF",
            25.0 to "#CC7777",
            30.0 to "#CC7777"
        )
    )

    fun getAirQualityPaletteOptions() = listOf(
        listOf(
            0.0 to "#009969",
            50.0 to "#009969",
            50.0 to "#ffdb52",
            100.0 to "#ffdb52",
            100.0 to "#ff9546",
            150.0 to "#ff9546",
            150.0 to "#d30037",
            200.0 to "#d30037",
            200.0 to "#671094",
            300.0 to "#671094",
            350.0 to "#830025"
        ),
        listOf(
            0.0 to "#00000000",
            50.0 to "#00000000",
            50.0 to "#80ffdb52",
            100.0 to "#80ffdb52",
            100.0 to "#A0ff9546",
            150.0 to "#A0ff9546",
            150.0 to "#d30037",
            200.0 to "#d30037",
            200.0 to "#671094",
            300.0 to "#671094",
            350.0 to "#830025"
        )
    )

    fun getPrecipitationPaletteOptions() = listOf(
        listOf(
            0.0 to "#104a64",
            0.8 to "#1894c7",
            0.9 to "#00eefd",
            1.0 to "#00000000"
        ),
        listOf(
            0.0 to "#A0104a64",
            0.8 to "#A01894c7",
            0.9 to "#A000eefd",
            1.0 to "#00000000"
        ),
        listOf(
            0.0 to "#0089c4",
            0.9 to "#0089c4",
            1.0 to "#00000000"
        ),
        listOf(
            0.0 to "#009969",
            0.9 to "#009969",
            1.0 to "#00000000"
        )
    )

    fun getTemperaturePaletteOptions() = listOf(
        listOf(
            -50 to "#7b8282", -40 to "#c1c1c1", -35 to "#edf9fe", -30 to "#fae0f7",
            -25 to "#c299d8", -20 to "#633d8a", -15 to "#0020c0", -10 to "#009cf3",
            -5 to "#00c9fb", 0 to "#aae1f6", 5 to "#009727", 10 to "#94df3d",
            15 to "#c6ed43", 20 to "#fffc4b", 25 to "#ffc33c", 30 to "#ff9331",
            35 to "#ff1e21", 40 to "#c70215", 45 to "#d810bc", 50 to "#f47bc4"
        ).map { Pair(celsiusToKelvin(it.first), it.second) },
        listOf(
            -50 to "#5555FF", 0 to "#5555FF", 0 to "#FF3333", 50 to "#FF3333"
        ).map { Pair(celsiusToKelvin(it.first), it.second) },
        listOf(
            -50 to "#CC00CC", -25 to "#5555FF", 0 to "#00CCCC",
            0 to "#33FF33", 25 to "#CCCC00", 50 to "#FF3333"
        ).map { Pair(celsiusToKelvin(it.first), it.second) }
    )

    fun getShowDaysAheadOptions() = listOf(1, 2, 3, 4, 5)

    fun getPrecipitationCutoffOptions(unit: PrecipitationUnit) = when (unit) {
        PrecipitationUnit.MMH -> listOf(3.0, 5.0, 10.0)
        PrecipitationUnit.INH -> listOf(0.1, 0.2, 0.5)
    }

    fun getWindSpeedCutoffOptions(unit: WindSpeedUnit) = when (unit) {
        WindSpeedUnit.MS -> listOf(10.0, 20.0, 50.0)
        WindSpeedUnit.FTS -> listOf(20.0, 50.0, 200.0)
        WindSpeedUnit.KMH -> listOf(20.0, 50.0, 200.0)
        WindSpeedUnit.MPH -> listOf(20.0, 40.0, 100.0)
        WindSpeedUnit.KN -> listOf(20.0, 40.0, 100.0)
    }

    fun getTemperatureUnitOptions() = listOf(TemperatureUnit.C, TemperatureUnit.F, TemperatureUnit.K)

    fun getCloudPercentUnitOptions() = listOf(CloudPercentUnit.PERCENT, CloudPercentUnit.NORMALIZED)

    fun getPrecipitationUnitOptions() = listOf(PrecipitationUnit.MMH, PrecipitationUnit.INH)

    fun getWindSpeedUnitOptions() = listOf(
        WindSpeedUnit.MS, WindSpeedUnit.FTS, WindSpeedUnit.KMH, WindSpeedUnit.MPH, WindSpeedUnit.KN
    )

    /**
     * Widget IDs are positive, so we need 1 less than the smaller non-positive ID.
     */
    private fun generateId(widgetId: Int) = if (widgetId > 0) widgetId else getMinId() - 1

    private fun getMinId() = localRepository.getAll()
        .map { it.widgetId }
        .filter { it < 0 }.minOrNull() ?: 0

    fun updateAndGet(widget: WeatherWidget, saveResult: Boolean = true) = runBlocking {
        try {
            val owmLocation = widget.dataSource.openWeatherMapLocationId
            Pair(
                WeatherData( // todo more providers for each
                    timezoneShiftMillis = owmRemoteRepository.getTimezoneShift(
                        owmLocation
                    ),
                    temperatureKelvinPoints = owmRemoteRepository.getTemperatureKelvinPoints(
                        owmLocation
                    ),
                    windMsPoints = owmRemoteRepository.getWindMsPoints(
                        owmLocation
                    ),
                    precipitationMmHourPoints = owmRemoteRepository.getPrecipitationMmPoints(
                        owmLocation
                    ),
                    cloudPercentPoints = owmRemoteRepository.getCloudPercentPoints(
                        owmLocation
                    ),
                    sunriseSunsetIntervals = owmRemoteRepository.getSunriseSunset(
                        owmLocation
                    ).let { listOf(it) }, // todo improve
                    airQualityPoints = widget.dataSource.aquicnCityName.let { location ->
                        if (location.isBlank()) {
                            emptyList()
                        } else {
                            listOf(aquicnRemoteRepository.getAqiNow(location))
                        }
                    },
                    uvIndexPoints = emptyList() // todo
                ), null
            )
        } catch (t: Throwable) {
            val errorMessage = "Couldn't get weather update"
            log.e(errorMessage, t)
            Pair(
                WeatherData(
                    0, emptyList(), emptyList(), emptyList(), emptyList(), emptyList(),
                    emptyList(), emptyList()
                ), errorMessage
            )
        }
    }.let {
        makeWidget(
            dataSource = widget.dataSource,
            visualSettings = widget.visualSettings,
            widgetId = widget.widgetId,
            previousTimeUpdated = widget.status.lastUpdatedTimeMillis,
            oldWeatherData = widget.weatherData,
            newWeatherData = it.first,
            errorMessage = it.second
        )
    }.let {
        if (saveResult) {
            localRepository.addOrUpdate(it)
        } else {
            it
        }
    }

    fun remove(widget: WeatherWidget) = localRepository.remove(widget)

}