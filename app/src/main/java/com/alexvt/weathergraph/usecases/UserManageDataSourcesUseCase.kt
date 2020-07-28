/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.usecases

import com.alexvt.weathergraph.repositories.*
import javax.inject.Inject
import javax.inject.Singleton

class UserManageDataSourcesUseCase private constructor(
    private val knownLocationRepository: KnownLocationRepository,
    private val owmRemoteRepository: OwmRemoteRepository,
    private val aquicnRemoteRepository: AquicnRemoteRepository,
    private val log: LogRepository
) {
    @Singleton
    class Factory @Inject constructor(
        private val knownLocationRepository: KnownLocationRepository,
        private val owmRemoteRepository: OwmRemoteRepository,
        private val aquicnRemoteRepository: AquicnRemoteRepository,
        private val log: LogRepository
    ) {
        val single by lazy {
            UserManageDataSourcesUseCase(
                knownLocationRepository, owmRemoteRepository, aquicnRemoteRepository, log
            )
        }
    }

    data class DataProvider(
        val name: String, val link: String, val shortLink: String,
        val rawDataPath: String? = null
    )

    private fun getDataProvider(
        name: String, link: String, rawDataPath: String? = null
    ) = DataProvider(
        name, link,
        link.removePrefix("https://").removePrefix("http://").removeSuffix("/"),
        rawDataPath
    )

    fun getKnownLocationProvider() = with(knownLocationRepository) {
        getDataProvider(getProviderName(), getProviderLink(), getRawDataPath())
    }

    fun getWeatherDataProvider() = with(owmRemoteRepository) {
        getDataProvider(getProviderName(), getProviderLink())
    }

    fun getAirQualityProvider() = with(aquicnRemoteRepository) {
        getDataProvider(getProviderName(), getProviderLink())
    }

}