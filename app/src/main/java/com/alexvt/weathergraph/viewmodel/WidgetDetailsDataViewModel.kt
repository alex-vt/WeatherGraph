/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.alexvt.weathergraph.usecases.UserEditWidgetUseCase
import com.alexvt.weathergraph.usecases.UserManageSettingsUseCase
import javax.inject.Inject

class WidgetDetailsDataViewModel @Inject constructor(
    private val userEditWidgetUseCaseFactory: UserEditWidgetUseCase.Factory,
    override val userManageSettingsUseCaseFactory: UserManageSettingsUseCase.Factory
) : BaseViewModel() {

    private lateinit var edit: UserEditWidgetUseCase

    val showDaysAheadOptions by lazy { edit.showDaysAheadOptions }
    val updateTimeHourOptions by lazy { edit.updateTimeHourOptions }
    val temperatureUnitOptions by lazy { edit.temperatureUnitOptions }
    val cloudPercentUnitOptions by lazy { edit.cloudPercentUnitOptions }
    val precipitationUnitOptions by lazy { edit.precipitationUnitOptions }
    val windSpeedUnitOptions by lazy { edit.windSpeedUnitOptions }

    val precipitationCutoffOptionsLiveData by lazy { edit.rainCutoffOptionsObservable.toLiveData() }
    val windSpeedCutoffOptionsLiveData by lazy { edit.windCutoffOptionsObservable.toLiveData() }

    val showDaysAheadIndexLiveData by lazy { edit.showForIndexObservable.toLiveData() }
    val updateEveryIndexLiveData by lazy { edit.updateEveryIndexObservable.toLiveData() }
    val temperatureUnitIndexLiveData by lazy { edit.temperatureUnitIndexObservable.toLiveData() }
    val cloudPercentUnitIndexLiveData by lazy { edit.cloudPercentUnitIndexObservable.toLiveData() }
    val precipitationUnitIndexLiveData by lazy { edit.rainUnitIndexObservable.toLiveData() }
    val windSpeedUnitIndexLiveData by lazy { edit.windUnitIndexObservable.toLiveData() }
    val precipitationCutoffIndexLiveData by lazy { edit.rainCutoffIndexObservable.toLiveData() }
    val windSpeedCutoffIndexLiveData by lazy { edit.windCutoffIndexObservable.toLiveData() }

    val cloudPercentEnabledLiveData by lazy { edit.cloudPercentEnabledObservable.toLiveData() }
    val precipitationEnabledLiveData by lazy { edit.rainEnabledObservable.toLiveData() }
    val windSpeedEnabledLiveData by lazy { edit.windEnabledObservable.toLiveData() }
    val airQualityEnabledLiveData by lazy { edit.airEnabledObservable.toLiveData() }
    val sunriseSunsetEnabledLiveData by lazy { edit.sunEnabledObservable.toLiveData() }
    val time24hEnabledLiveData by lazy { edit.time24hEnabledObservable.toLiveData() }

    fun setShowForIndex(index: Int) = edit.setShowForIndex(index)
    fun setUpdateEveryIndex(index: Int) = edit.setUpdateEveryIndex(index)
    fun setTemperatureUnitIndex(index: Int) = edit.setTemperatureUnitIndex(index)
    fun setCloudPercentUnitIndex(index: Int) = edit.setCloudPercentUnitIndex(index)
    fun setPrecipitationUnitIndex(index: Int) = edit.setRainUnitIndex(index)
    fun setWindSpeedUnitIndex(index: Int) = edit.setWindUnitIndex(index)
    fun setPrecipitationCutoffIndex(index: Int) = edit.setRainCutoffIndex(index)
    fun setWindSpeedCutoffIndex(index: Int) = edit.setWindCutoffIndex(index)

    fun setCloudPercentEnabled(value: Boolean) = edit.setCloudPercentEnabled(value)
    fun setPrecipitationEnabled(value: Boolean) = edit.setRainEnabled(value)
    fun setWindSpeedEnabled(value: Boolean) = edit.setWindEnabled(value)
    fun setAirQualityEnabled(value: Boolean) = edit.setAirQualityEnabled(value)
    fun setSunriseSunsetEnabled(value: Boolean) = edit.setSunriseSunsetEnabled(value)
    fun setTime24hEnabled(value: Boolean) = edit.setTime24hEnabled(value)

    val dataSourcesNavigationLiveData: LiveData<Event<Unit>> = MutableLiveData()

    fun openDataSources() = dataSourcesNavigationLiveData.setEvent()

    fun loadByWidgetId(widgetId: Int) {
        if (!::edit.isInitialized) {
            edit = userEditWidgetUseCaseFactory.createFor(widgetId)
        }
    }
}