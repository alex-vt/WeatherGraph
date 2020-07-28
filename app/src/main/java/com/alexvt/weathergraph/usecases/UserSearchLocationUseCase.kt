/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.usecases

import com.alexvt.weathergraph.repositories.*
import javax.inject.Inject
import javax.inject.Singleton

class UserSearchLocationUseCase private constructor(
    private val locationRepository: LocationRepository,
    private val log: LogRepository
) {
    @Singleton
    class Factory @Inject constructor(
        private val locationRepository: LocationRepository,
        private val log: LogRepository
    ) {
        val single by lazy {
            UserSearchLocationUseCase(locationRepository, log)
        }
    }

    fun getSuggestions(searchText: String) =
        locationRepository.getSuggestions(searchText)

    fun getNearestLocationSuggestions(latitude: Double, longitude: Double) =
        locationRepository.getNearestLocationSuggestions(latitude, longitude)

    fun getLocationSuggestionsInBounds(
        north: Double, south: Double, west: Double, east: Double, vararg fixedLocationIds: Int
    ) = locationRepository.getLocationSuggestionsInBounds(
        north, south, west, east, *fixedLocationIds
    )

    fun get(id: Int) = locationRepository.get(id)

}