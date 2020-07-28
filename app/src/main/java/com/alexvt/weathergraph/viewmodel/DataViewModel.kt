/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.alexvt.weathergraph.usecases.*
import javax.inject.Inject

class DataViewModel @Inject constructor(
    userManageDataSourcesUseCaseFactory: UserManageDataSourcesUseCase.Factory,
    override val userManageSettingsUseCaseFactory: UserManageSettingsUseCase.Factory
) : BaseViewModel() {

    private val case = userManageDataSourcesUseCaseFactory.single

    val backNavigationLiveData: LiveData<Event<Unit>> = MutableLiveData()
    val linkNavigationLiveData: LiveData<Event<String>> = MutableLiveData()

    fun clickBack() = backNavigationLiveData.setEvent()

    fun clickLink(link: String) = linkNavigationLiveData.setEvent(link)

    data class DataSource(
        val name: String, val link: String, val shortLink: String, val rawDataPath: String? = null
    )

    private fun UserManageDataSourcesUseCase.DataProvider.toSource() =
        DataSource(name, link, shortLink, rawDataPath)

    fun getKnownLocationsProvider() = case.getKnownLocationProvider().toSource()
    fun getWeatherDataProvider() = case.getWeatherDataProvider().toSource()
    fun getAirQualityProvider() = case.getAirQualityProvider().toSource()
}