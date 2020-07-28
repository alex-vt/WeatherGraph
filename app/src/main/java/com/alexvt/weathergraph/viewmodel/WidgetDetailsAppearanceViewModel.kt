/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.viewmodel

import androidx.core.graphics.toColorInt
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.alexvt.weathergraph.usecases.UserEditWidgetUseCase
import com.alexvt.weathergraph.usecases.UserManageSettingsUseCase
import io.reactivex.Observable
import io.reactivex.functions.Function4
import javax.inject.Inject

class WidgetDetailsAppearanceViewModel @Inject constructor(
    private val userEditWidgetUseCaseFactory: UserEditWidgetUseCase.Factory,
    override val userManageSettingsUseCaseFactory: UserManageSettingsUseCase.Factory
) : BaseViewModel() {

    private lateinit var case: UserEditWidgetUseCase

    fun loadByWidgetId(widgetId: Int) {
        if (!::case.isInitialized) {
            case = userEditWidgetUseCaseFactory.createFor(widgetId)
        }
    }

    val wallpaperShownLiveData by lazy { case.appearancePreviewOnWallpaperObservable.toLiveData() }
    fun setAppearancePreviewWallpaper(show: Boolean) = case.setAppearancePreviewOnWallpaper(show)

    val showLocationNameLiveData by lazy { case.showLocationNameObservable.toLiveData() }
    fun setShowLocationNameEnabled(enabled: Boolean) = case.setShowLocationNameEnabled(enabled)

    val showLastUpdateTimeLiveData by lazy { case.showLastUpdateTimeObservable.toLiveData() }
    fun setShowLastUpdateTimeEnabled(enabled: Boolean) = case.setShowLastUpdateTimeEnabled(enabled)

    val showUnitsLiveData by lazy { case.showUnitsObservable.toLiveData() }
    fun setShowUnitsEnabled(enabled: Boolean) = case.setShowUnitsEnabled(enabled)

    fun getBackgroundColorLiveData() = makePaletteLiveData(
        case.backgroundColorIndexObservable, case.backgroundColorPalettes, "", "", false
    )

    fun setBackgroundColorIndex(index: Int) = case.setBackgroundColorIndex(index)

    fun getGridColorLiveData() =
        makePaletteLiveData(case.gridColorIndexObservable, case.gridColorPalettes)

    fun setGridColorIndex(index: Int) = case.setGridColorIndex(index)
    val gridThicknessPxOptions by lazy { case.gridThicknessPxOptions.toStrings() }
    val gridThicknessIndexLiveData by lazy { case.gridThicknessIndexObservable.toLiveData() }
    fun setGridThicknessIndex(index: Int) = case.setGridThicknessIndex(index)

    fun getTextColorLiveData() =
        makePaletteLiveData(case.textColorIndexObservable, case.textColorPalettes)

    fun setTextColorIndex(index: Int) = case.setTextColorIndex(index)
    val textSizePxOptions by lazy { case.textSizePxOptions.toStrings() }
    val textSizeIndexLiveData by lazy { case.textSizeIndexObservable.toLiveData() }
    fun setTextSizeIndex(index: Int) = case.setTextSizeIndex(index)

    private fun List<Int>.toStrings() = map { it.toString() }

    data class Palette(
        val index: Int, val isSelected: Boolean, val lowText: String, val highText: String,
        val imageData: ByteArray, val backgroundColor: Int, val textColor: Int,
        val onWallpaper: Boolean
    )

    private val transparentColor = "#00000000".toColorInt()

    private fun makePaletteLiveData(
        indexObservable: Observable<Int>, palettes: List<ByteArray>,
        low: String = "", high: String = "", onBackgroundColor: Boolean = true
    ) = Observable.combineLatest<Int, Int, Int, Boolean, List<Palette>>(
            indexObservable,
            case.backgroundColorObservable,
            case.textColorObservable,
            case.appearancePreviewOnWallpaperObservable,
            Function4 { selectedPaletteIndex, backgroundColor, textColor, isWallpaperPreview ->
                palettes.mapIndexed { index, imageData ->
                    Palette(
                        index, index == selectedPaletteIndex, low, high, imageData,
                        if (onBackgroundColor) backgroundColor else transparentColor,
                        textColor, isWallpaperPreview
                    )
                }
            })
        .toLiveData()

    fun getTemperaturePaletteLiveData(low: String, high: String) =
        makePaletteLiveData(case.tempPaletteIndexObservable, case.tempPalettes, low, high)

    fun setTemperaturePaletteIndex(index: Int) = case.setTempPaletteIndex(index)
    val graphThicknessPxOptions by lazy { case.graphThicknessPxOptions.toStrings() }
    val temperatureThicknessIndexLiveData by lazy { case.tempThicknessIndexObservable.toLiveData() }
    fun setTemperatureThicknessIndex(index: Int) = case.setTempThicknessIndex(index)

    val cloudPercentEnabledLiveData by lazy { case.cloudPercentEnabledObservable.toLiveData() }
    fun getCloudPaletteLiveData(low: String, high: String) =
        makePaletteLiveData(case.cloudPaletteIndexObservable, case.cloudPalettes, low, high)

    fun setCloudPaletteIndex(index: Int) = case.setCloudPaletteIndex(index)

    val precipitationEnabledLiveData by lazy { case.rainEnabledObservable.toLiveData() }
    fun getPrecipitationPaletteLiveData(low: String, high: String) =
        makePaletteLiveData(case.rainPaletteIndexObservable, case.rainPalettes, low, high)

    fun setPrecipitationPaletteIndex(index: Int) = case.setRainPaletteIndex(index)

    val windSpeedEnabledLiveData by lazy { case.windEnabledObservable.toLiveData() }
    fun getWindSpeedPaletteLiveData(low: String, high: String) =
        makePaletteLiveData(case.windPaletteIndexObservable, case.windPalettes, low, high)

    fun setWindSpeedPaletteIndex(index: Int) = case.setWindPaletteIndex(index)
    val windSpeedThicknessIndexLiveData by lazy { case.windThicknessIndexObservable.toLiveData() }
    fun setWindSpeedThicknessIndex(index: Int) = case.setWindThicknessIndex(index)

    val airQualityEnabledLiveData by lazy {
        case.airEnabledObservable.map { it.first }.toLiveData()
    }

    fun getAirQualityPaletteLiveData(low: String, high: String) =
        makePaletteLiveData(case.airPaletteIndexObservable, case.airPalettes, low, high)

    fun setAirQualityPaletteIndex(index: Int) = case.setAirPaletteIndex(index)

    val enabledLiveData = MutableLiveData<Boolean>().apply { value = true } as LiveData<Boolean>

    val sunriseSunsetEnabledLiveData by lazy { case.sunEnabledObservable.toLiveData() }
    fun getSunriseSunsetPaletteLiveData(low: String, high: String) =
        makePaletteLiveData(case.sunPaletteIndexObservable, case.sunPalettes, low, high)

    fun setSunriseSunsetPaletteIndex(index: Int) = case.setSunPaletteIndex(index)

    val marginMax by lazy { case.marginMax }
    val marginTopLiveData by lazy { case.marginTopObservable.toLiveData() }
    fun setMarginTop(value: Int) = case.setMarginTop(value)
    val marginLeftLiveData by lazy { case.marginLeftObservable.toLiveData() }
    fun setMarginLeft(value: Int) = case.setMarginLeft(value)
    val marginBottomLiveData by lazy { case.marginBottomObservable.toLiveData() }
    fun setMarginBottom(value: Int) = case.setMarginBottom(value)
    val marginRightLiveData by lazy { case.marginRightObservable.toLiveData() }
    fun setMarginRight(value: Int) = case.setMarginRight(value)
}