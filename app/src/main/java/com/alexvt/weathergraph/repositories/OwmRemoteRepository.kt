/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.repositories

interface OwmRemoteRepository {

    suspend fun getSunriseSunset(locationId: Int): Pair<Long, Long>
    suspend fun getTimezoneShift(locationId: Int): Long

    suspend fun getTemperatureKelvinPoints(locationId: Int): List<Pair<Double, Long>>
    suspend fun getCloudPercentPoints(locationId: Int): List<Pair<Int, Long>>
    suspend fun getPrecipitationMmPoints(locationId: Int): List<Pair<Double, Long>>
    suspend fun getWindMsPoints(locationId: Int): List<Pair<Double, Long>>

    suspend fun getTemperatureKelvinNow(locationId: Int): Pair<Double, Long>
    suspend fun getCloudPercentNow(locationId: Int): Pair<Int, Long>
    suspend fun getPrecipitationMmNow(locationId: Int): Pair<Double, Long>
    suspend fun getWindMsNow(locationId: Int): Pair<Double, Long>

    fun getProviderName(): String
    fun getProviderLink(): String
}