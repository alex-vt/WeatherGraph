/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.repositories

import com.alexvt.weathergraph.entities.OwmLocation
import java.util.*
import javax.inject.Inject
import kotlin.math.pow

class LocationRepository @Inject constructor(
    private val knownLocationRepository: KnownLocationRepository,
    private val log: LogRepository
) {

    private val allLocations = knownLocationRepository.getAll()
        .sortedByDescending { it.population }

    private val maxSuggestions = 5

    fun getSuggestions(text: String) =
        text.trim().toLowerCase(Locale.getDefault()).let { normalizedText ->
            allLocations.takeIfText(normalizedText)
                .filter { it.name.toLowerCase(Locale.getDefault()).startsWith(normalizedText) }
        }.take(maxSuggestions)

    fun getNearestLocationSuggestions(latitude: Double, longitude: Double) =
        allLocations.sortedBy { it.distanceTo(latitude, longitude) }.take(maxSuggestions)

    private val maxMarkers = 20
    private val relativeMinMarkerDistance = 0.1

    // todo longitude 180 bounds check
    // todo try taking at least 1 in each visible country
    fun getLocationSuggestionsInBounds(
        north: Double, south: Double, west: Double, east: Double, vararg fixedLocationIds: Int
    ) = allLocations.filter {
        it.latitude.isInGeoBounds(south, north) && it.longitude.isInGeoBounds(west, east)
    }.toSet().let { locationsInBounds ->
        rarifyLocations(
            fixedLocations = locationsInBounds.filter { it.id in fixedLocationIds },
            remainingLocations = locationsInBounds.filter { it.id !in fixedLocationIds },
            minDistance = (north - south) * relativeMinMarkerDistance,
            takeMax = maxMarkers
        )
    }

    /**
     * Universal range check for latitude and longitude.
     * Latitude is always ordered correctly
     * Cases when true for longitude:
     * -2 (-1) 2
     * -2 (1) 2
     * 178 (179) -178 -> 178 (179) 182
     * 178 (-179) -178 -> 178 (181) 182
     */
    private fun Double.isInGeoBounds(low: Double, high: Double): Boolean {
        val highAdjustment = if (low > high) 360.0 else 0.0
        val thisAdjustment = if (low > this) 360.0 else 0.0
        return (this + thisAdjustment) in low..(high + highAdjustment)
    }

    private fun rarifyLocations(
        fixedLocations: List<OwmLocation>, remainingLocations: List<OwmLocation>,
        minDistance: Double, takeMax: Int
    ) = run {
        val includedLocations = fixedLocations.toMutableList()
        for (location in remainingLocations) {
            if (includedLocations.size == takeMax) break
            val minDistanceToIncluded = includedLocations.map { it.distanceTo(location) }.min()
            val willInclude = includedLocations.isEmpty() || minDistanceToIncluded > minDistance
            if (willInclude) {
                includedLocations.add(location)
            }
        }
        includedLocations
    }

    private fun OwmLocation.distanceTo(latitude: Double, longitude: Double) =
        ((latitude - this.latitude).pow(2) + ((longitude - this.longitude) % 360).pow(2)).pow(0.5)

    private fun OwmLocation.distanceTo(other: OwmLocation) =
        distanceTo(other.latitude, other.longitude)

    fun get(id: Int) = allLocations.first { it.id == id }

    private fun List<OwmLocation>.takeIfText(text: String) =
        takeIf { text.isNotEmpty() } ?: getDefaultSuggestions()

    private val defaultSuggestionNames = listOf("New York", "London", "Berlin", "Mumbai", "Tokyo")

    private fun getDefaultSuggestions() = defaultSuggestionNames.map { name ->
        allLocations.first { it.name == name }
    }

}