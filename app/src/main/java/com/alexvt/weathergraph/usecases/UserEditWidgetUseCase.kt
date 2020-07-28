/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.usecases

import com.alexvt.weathergraph.entities.WeatherDataSource
import com.alexvt.weathergraph.entities.WeatherWidget
import com.alexvt.weathergraph.entities.WidgetVisualSettings
import com.alexvt.weathergraph.repositories.*
import com.alexvt.weathergraph.math.WidgetColorUtil.parseColor
import io.reactivex.Observable
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

class UserEditWidgetUseCase private constructor(
    private val aquicnRemoteRepository: AquicnRemoteRepository,
    private val widgetRepository: WeatherWidgetRepository,
    private val editedWidgetRepository: EditedWeatherWidgetRepository,
    private val editedWidgetPreviewOnWallpaperRepository: EditedWidgetOnWallpaperRepository,
    private val scheduleUpdatesUseCaseFactory: ScheduleUpdatesUseCase.Factory,
    removeWidgetUseCaseFactory: UserRemoveWidgetUseCase.Factory,
    private val drawDataRepository: DrawDataRepository,
    private val log: LogRepository,
    settingsRepository: SettingsRepository,
    private val widgetId: Int
) {
    @Singleton
    class Factory @Inject constructor(
        private val aquicnRemoteRepository: AquicnRemoteRepository,
        private val widgetRepository: WeatherWidgetRepository,
        private val editedWidgetRepository: EditedWeatherWidgetRepository,
        private val editedWidgetBackgroundRepository: EditedWidgetOnWallpaperRepository,
        private val scheduleUpdatesUseCaseFactory: ScheduleUpdatesUseCase.Factory,
        private val removeWidgetUseCaseFactory: UserRemoveWidgetUseCase.Factory,
        private val drawDataRepository: DrawDataRepository,
        private val settingsRepository: SettingsRepository,
        private val log: LogRepository
    ) {
        fun createFor(widgetId: Int) = UserEditWidgetUseCase(
            aquicnRemoteRepository, widgetRepository, editedWidgetRepository,
            editedWidgetBackgroundRepository, scheduleUpdatesUseCaseFactory,
            removeWidgetUseCaseFactory, drawDataRepository, log, settingsRepository, widgetId
        ).also {
            log.d("Created for widget ID $widgetId")
        }
    }

    private val paletteBitmapSizePx = 100
    private val removeWidgetUseCase = removeWidgetUseCaseFactory.createFor(widgetId)

    init {
        val existing = getExisting()
        updateWith(existing ?: widgetRepository.generateDefault(widgetId))
        if (existing == null) {
            updateWithWeatherData()
            updateWithAqiIfAvailable()
        }
        setAppearancePreviewOnWallpaper(settingsRepository.get().showWallpaper)
    }

    val showWallpaperSettingsObservable: Observable<Boolean> =
        settingsRepository.observe().map { it.showWallpaper }

    val appearancePreviewOnWallpaperObservable: Observable<Boolean> =
        editedWidgetPreviewOnWallpaperRepository.observe()

    fun setAppearancePreviewOnWallpaper(value: Boolean) =
        editedWidgetPreviewOnWallpaperRepository.set(value)

    private fun List<Double>.removeExtraZeros() = map { DecimalFormat("#.#").format(it) }

    val showDaysAheadOptions = widgetRepository.getShowDaysAheadOptions().map { it.toString() }
    val updateTimeHourOptions = widgetRepository.getUpdateTimeMillisOptions().map {
        (it.toDouble() / TimeUnit.HOURS.toMillis(1))
    }.removeExtraZeros()
    val temperatureUnitOptions = widgetRepository.getTemperatureUnitOptions()
    val cloudPercentUnitOptions = widgetRepository.getCloudPercentUnitOptions()
    val precipitationUnitOptions = widgetRepository.getPrecipitationUnitOptions()
    val rainCutoffOptionsObservable = editedWidgetRepository.observe().map {
        widgetRepository.getPrecipitationCutoffOptions(it.visualSettings.precipitationUnit)
            .removeExtraZeros()
    }
    val windSpeedUnitOptions = widgetRepository.getWindSpeedUnitOptions()
    val windCutoffOptionsObservable = editedWidgetRepository.observe().map {
        widgetRepository.getWindSpeedCutoffOptions(it.visualSettings.windSpeedUnit)
            .removeExtraZeros()
    }

    private val visualsObservable = editedWidgetRepository.observe().map { it.visualSettings }
    val showForIndexObservable = visualsObservable.map {
        widgetRepository.getShowDaysAheadOptions().indexOf(it.showDaysAhead)
    }
    val updateEveryIndexObservable = editedWidgetRepository.observe().map {
        widgetRepository.getUpdateTimeMillisOptions().indexOf(it.dataSource.updatePeriodMillis)
    }
    val temperatureUnitIndexObservable = visualsObservable.map {
        widgetRepository.getTemperatureUnitOptions().indexOf(it.temperatureUnit)
    }
    val cloudPercentUnitIndexObservable = visualsObservable.map {
        widgetRepository.getCloudPercentUnitOptions().indexOf(it.cloudPercentUnit)
    }
    val rainUnitIndexObservable = visualsObservable.map {
        widgetRepository.getPrecipitationUnitOptions().indexOf(it.precipitationUnit)
    }
    val windUnitIndexObservable = visualsObservable.map {
        widgetRepository.getWindSpeedUnitOptions().indexOf(it.windSpeedUnit)
    }
    val rainCutoffIndexObservable = visualsObservable.map {
        widgetRepository.getPrecipitationCutoffOptions(it.precipitationUnit)
            .indexOf(it.precipitationCutoffValue)
    }.filter { it >= 0 }
    val windCutoffIndexObservable = visualsObservable.map {
        widgetRepository.getWindSpeedCutoffOptions(it.windSpeedUnit)
            .indexOf(it.windSpeedCutoffValue)
    }.filter { it >= 0 }

    val cloudPercentEnabledObservable = visualsObservable.map { it.showCloudPercent }
    val rainEnabledObservable = visualsObservable.map { it.showPrecipitation }
    val windEnabledObservable = visualsObservable.map { it.showWind }
    val airEnabledObservable = editedWidgetRepository.observe().map {
        Pair(
            it.dataSource.aquicnCityName.isNotBlank() && it.visualSettings.showAirQuality,
            it.dataSource.aquicnCityName.isNotBlank()
        ).also { log.d(it.toString()) }
    }
    val sunEnabledObservable = visualsObservable.map { it.showSunriseSunset }
    val time24hEnabledObservable = visualsObservable.map { it.time24h }


    val showLocationNameObservable = visualsObservable.map { it.showLocationName }
    fun setShowLocationNameEnabled(enabled: Boolean) =
        getVisuals().copy(showLocationName = enabled).updateWith()

    val showLastUpdateTimeObservable = visualsObservable.map { it.showLastUpdateTime }
    fun setShowLastUpdateTimeEnabled(enabled: Boolean) =
        getVisuals().copy(showLastUpdateTime = enabled).updateWith()

    val showUnitsObservable = visualsObservable.map { it.showUnits }
    fun setShowUnitsEnabled(enabled: Boolean) =
        getVisuals().copy(showUnits = enabled).updateWith()

    val backgroundColorPalettes = widgetRepository.getBackgroundColorOptions().renderAsPalette()
    val backgroundColorObservable = visualsObservable.map { it.backgroundColor.parseColor() }
    val backgroundColorIndexObservable = visualsObservable.map {
        widgetRepository.getBackgroundColorOptions().indexOf(it.backgroundColor)
    }

    fun setBackgroundColorIndex(index: Int) = getVisuals()
        .copy(backgroundColor = widgetRepository.getBackgroundColorOptions()[index])
        .updateWith()

    val gridColorPalettes = widgetRepository.getGridColorOptions().renderAsPalette()
    val gridColorIndexObservable = visualsObservable.map {
        widgetRepository.getGridColorOptions().indexOf(it.gridColor)
    }

    fun setGridColorIndex(index: Int) = getVisuals()
        .copy(gridColor = widgetRepository.getGridColorOptions()[index])
        .updateWith()

    val gridThicknessPxOptions = widgetRepository.getGridThicknessPxOptions()
    val gridThicknessIndexObservable = visualsObservable.map {
        widgetRepository.getGridThicknessPxOptions().indexOf(it.gridThicknessPx)
    }

    fun setGridThicknessIndex(index: Int) = getVisuals()
        .copy(gridThicknessPx = widgetRepository.getGridThicknessPxOptions()[index])
        .updateWith()

    val textColorPalettes = widgetRepository.getTextColorOptions().renderAsPalette()
    val textColorObservable = visualsObservable.map { it.textColor.parseColor() }
    val textColorIndexObservable = visualsObservable.map {
        widgetRepository.getTextColorOptions().indexOf(it.textColor)
    }

    fun setTextColorIndex(index: Int) = getVisuals()
        .copy(textColor = widgetRepository.getTextColorOptions()[index])
        .updateWith()

    val textSizePxOptions = widgetRepository.getTextSizePxOptions()
    val textSizeIndexObservable = visualsObservable.map {
        widgetRepository.getTextSizePxOptions().indexOf(it.textSizePx)
    }

    fun setTextSizeIndex(index: Int) = getVisuals()
        .copy(textSizePx = widgetRepository.getTextSizePxOptions()[index])
        .updateWith()

    private fun List<String>.renderAsPalette() = map { listOf(0.0 to it, 1.0 to it) }.render()

    private fun List<List<Pair<Double, String>>>.render() = map {
        drawDataRepository.drawPalette(it, Pair(paletteBitmapSizePx, paletteBitmapSizePx))
    }

    val tempPalettes = widgetRepository.getTemperaturePaletteOptions().render()
    val tempPaletteIndexObservable = visualsObservable.map {
        widgetRepository.getTemperaturePaletteOptions().indexOf(it.temperatureGraphPalette)
    }

    fun setTempPaletteIndex(index: Int) = getVisuals()
        .copy(temperatureGraphPalette = widgetRepository.getTemperaturePaletteOptions()[index])
        .updateWith()

    val graphThicknessPxOptions = widgetRepository.getGraphThicknessPxOptions()
    val tempThicknessIndexObservable = visualsObservable.map {
        widgetRepository.getGraphThicknessPxOptions().indexOf(it.temperatureThicknessPx)
    }

    fun setTempThicknessIndex(index: Int) = getVisuals()
        .copy(temperatureThicknessPx = widgetRepository.getGraphThicknessPxOptions()[index])
        .updateWith()

    val cloudPalettes = widgetRepository.getCloudPercentPaletteOptions().render()
    val cloudPaletteIndexObservable = visualsObservable.map {
        widgetRepository.getCloudPercentPaletteOptions().indexOf(it.cloudPercentPalette)
    }

    fun setCloudPaletteIndex(index: Int) = getVisuals()
        .copy(cloudPercentPalette = widgetRepository.getCloudPercentPaletteOptions()[index])
        .updateWith()

    val rainPalettes = widgetRepository.getPrecipitationPaletteOptions().render()
    val rainPaletteIndexObservable = visualsObservable.map {
        widgetRepository.getPrecipitationPaletteOptions().indexOf(it.precipitationBarsPalette)
    }

    fun setRainPaletteIndex(index: Int) = getVisuals()
        .copy(precipitationBarsPalette = widgetRepository.getPrecipitationPaletteOptions()[index])
        .updateWith()

    val windPalettes = widgetRepository.getWindSpeedPaletteOptions().render()
    val windPaletteIndexObservable = visualsObservable.map {
        widgetRepository.getWindSpeedPaletteOptions().indexOf(it.windSpeedPalette)
    }

    fun setWindPaletteIndex(index: Int) = getVisuals()
        .copy(windSpeedPalette = widgetRepository.getWindSpeedPaletteOptions()[index])
        .updateWith()

    val windThicknessIndexObservable = visualsObservable.map {
        widgetRepository.getGraphThicknessPxOptions().indexOf(it.windSpeedThicknessPx)
    }

    fun setWindThicknessIndex(index: Int) = getVisuals()
        .copy(windSpeedThicknessPx = widgetRepository.getGraphThicknessPxOptions()[index])
        .updateWith()

    val airPalettes = widgetRepository.getAirQualityPaletteOptions().render()
    val airPaletteIndexObservable = visualsObservable.map {
        widgetRepository.getAirQualityPaletteOptions().indexOf(it.airQualityPalette)
    }

    fun setAirPaletteIndex(index: Int) = getVisuals()
        .copy(airQualityPalette = widgetRepository.getAirQualityPaletteOptions()[index])
        .updateWith()

    val sunPalettes = widgetRepository.getSunriseSunsetPaletteOptions().render()
    val sunPaletteIndexObservable = visualsObservable.map {
        widgetRepository.getSunriseSunsetPaletteOptions().indexOf(it.sunriseSunsetPalette)
    }

    fun setSunPaletteIndex(index: Int) = getVisuals()
        .copy(sunriseSunsetPalette = widgetRepository.getSunriseSunsetPaletteOptions()[index])
        .updateWith()

    val marginMax = widgetRepository.getMaxMarginPx()
    val marginLeftObservable = visualsObservable.map { it.marginLeftPx }
    fun setMarginLeft(value: Int) = getVisuals().copy(marginLeftPx = value).updateWith()
    val marginRightObservable = visualsObservable.map { it.marginRightPx }
    fun setMarginRight(value: Int) = getVisuals().copy(marginRightPx = value).updateWith()
    val marginTopObservable = visualsObservable.map { it.marginTopPx }
    fun setMarginTop(value: Int) = getVisuals().copy(marginTopPx = value).updateWith()
    val marginBottomObservable = visualsObservable.map { it.marginBottomPx }
    fun setMarginBottom(value: Int) = getVisuals().copy(marginBottomPx = value).updateWith()

    private fun getVisuals() = getCurrentEdited().visualSettings
    private fun getDataSource() = getCurrentEdited().dataSource

    private fun WidgetVisualSettings.updateWith() = takeIf {
        getCurrentEdited().visualSettings != this
    }?.let {
        updateWith(getCurrentEdited().copy(visualSettings = this))
    }

    private fun WeatherDataSource.updateWith() = takeIf {
        getCurrentEdited().dataSource != this
    }?.let {
        updateWith(getCurrentEdited().copy(dataSource = this))
    }

    fun setShowForIndex(index: Int) = getVisuals()
        .copy(showDaysAhead = widgetRepository.getShowDaysAheadOptions()[index])
        .updateWith()

    fun setUpdateEveryIndex(index: Int) = getDataSource()
        .copy(updatePeriodMillis = widgetRepository.getUpdateTimeMillisOptions()[index])
        .updateWith()

    fun setTemperatureUnitIndex(index: Int) = getVisuals()
        .copy(temperatureUnit = widgetRepository.getTemperatureUnitOptions()[index])
        .updateWith()

    fun setCloudPercentUnitIndex(index: Int) = getVisuals()
        .copy(cloudPercentUnit = widgetRepository.getCloudPercentUnitOptions()[index])
        .updateWith()

    fun setRainUnitIndex(index: Int) = with(getVisuals()) {
        val previousCutoffIndex = widgetRepository.getPrecipitationCutoffOptions(precipitationUnit)
            .indexOf(precipitationCutoffValue)
        val unit = widgetRepository.getPrecipitationUnitOptions()[index]
        copy(
            precipitationUnit = unit,
            precipitationCutoffValue = widgetRepository
                .getPrecipitationCutoffOptions(unit)[previousCutoffIndex]
        ).updateWith()
    }

    fun setWindUnitIndex(index: Int) = with(getVisuals()) {
        val previousCutoffIndex = widgetRepository.getWindSpeedCutoffOptions(windSpeedUnit)
            .indexOf(windSpeedCutoffValue)
        val unit = widgetRepository.getWindSpeedUnitOptions()[index]
        copy(
            windSpeedUnit = unit,
            windSpeedCutoffValue = widgetRepository
                .getWindSpeedCutoffOptions(unit)[previousCutoffIndex]
        ).updateWith()
    }

    fun setRainCutoffIndex(index: Int) = getVisuals().copy(
        precipitationCutoffValue =
        widgetRepository.getPrecipitationCutoffOptions(getVisuals().precipitationUnit)[index]
    ).updateWith()

    fun setWindCutoffIndex(index: Int) = getVisuals().copy(
        windSpeedCutoffValue =
        widgetRepository.getWindSpeedCutoffOptions(getVisuals().windSpeedUnit)[index]
    ).updateWith()

    fun setCloudPercentEnabled(enabled: Boolean) =
        getVisuals().copy(showCloudPercent = enabled).updateWith()

    fun setRainEnabled(enabled: Boolean) =
        getVisuals().copy(showPrecipitation = enabled).updateWith()

    fun setWindEnabled(enabled: Boolean) =
        getVisuals().copy(showWind = enabled).updateWith()

    fun setAirQualityEnabled(enabled: Boolean) = takeIf {
        getDataSource().aquicnCityName.isNotBlank()
    }.let {
        getVisuals().copy(showAirQuality = enabled).updateWith()
    }

    fun setSunriseSunsetEnabled(enabled: Boolean) =
        getVisuals().copy(showSunriseSunset = enabled).updateWith()

    fun setTime24hEnabled(enabled: Boolean) =
        getVisuals().copy(time24h = enabled).updateWith()

    private fun getExisting() = widgetRepository.getCurrent(widgetId)

    private fun getCurrentEdited() = editedWidgetRepository.getCurrent()

    fun getCurrentLocationId() = getDataSource().openWeatherMapLocationId

    fun hasLocationId(locationId: Int) =
        getDataSource().openWeatherMapLocationId == locationId

    fun observe(targetSizePx: Pair<Int, Int>): Observable<Pair<WeatherWidget, ByteArray>> =
        editedWidgetRepository.observe().map {
            Pair(it, drawDataRepository.draw(it, targetSizePx))
        }

    private fun updateWith(weatherWidget: WeatherWidget) = editedWidgetRepository.set(weatherWidget)

    private fun updateWithAqiIfAvailable() = GlobalScope.launch {
        with(getDataSource().locationName) {
            if (aquicnRemoteRepository.isAqiAvailable(this)) {
                log.d("AQI available for $this")
                updateWithAqi(this)
                updateWithWeatherData() // AQI here will be updated
            } else {
                log.d("AQI not available for $this")
                updateWithAqi("")
            }
        }
    }

    private fun updateWithWeatherData() = GlobalScope.launch {
        updateWith(widgetRepository.updateAndGet(getCurrentEdited(), saveResult = false))
    }

    private fun updateWithAqi(aqiCityName: String) =
        getDataSource().copy(aquicnCityName = aqiCityName).updateWith()

    fun updateWithLocationName(
        location: String, openWeatherMapLocationId: Int, latitude: Double, longitude: Double
    ) = if (location != getDataSource().locationName
        || openWeatherMapLocationId != getDataSource().openWeatherMapLocationId
    ) {
        getDataSource().copy(
            locationName = location,
            latitude = latitude,
            longitude = longitude,
            openWeatherMapLocationId = openWeatherMapLocationId,
            aquicnCityName = ""
        ).let {
            getCurrentEdited().copy(
                dataSource = it,
                status = widgetRepository.getDefaultFreshStatus()
            )
        }.let {
            log.d("Updated with name $location")
            updateWith(it)
        }.also {
            updateWithWeatherData()
            updateWithAqiIfAvailable()
        }
    } else {
        log.d("Already selected name $location")
    }

    fun existsAsSaved() =
        getCurrentEdited().widgetId in widgetRepository.getCurrentAll().map { it.widgetId }

    fun remove() = getCurrentEdited()
        .let { removeWidgetUseCase.remove() }
        .also {
            log.d("Removed $it")
            clearEditedRepositories()
        }

    fun saveChanges() = getCurrentEdited()
        .let { widgetRepository.add(it) }
        .also {
            log.d("Saved $it")
            scheduleUpdatesUseCaseFactory.single.scheduleUpdates()
        }

    fun hasUnsavedChanges() = existsAsSaved() && !hasUnchangedSettings()

    private fun hasUnchangedSettings() = getDataSource() == getExisting()?.dataSource
            && getVisuals() == getExisting()?.visualSettings

    fun discardChanges() = clearEditedRepositories()

    private fun clearEditedRepositories() {
        editedWidgetRepository.clear()
        editedWidgetPreviewOnWallpaperRepository.clear()
    }

    fun getRemoveDetailedMessageIndex() = removeWidgetUseCase.getRemoveDetailedMessageIndex()

    fun getRemovePositiveTextIndex() = removeWidgetUseCase.getRemovePositiveTextIndex()
}