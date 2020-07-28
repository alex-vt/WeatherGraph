/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.repositories

interface AquicnRemoteRepository {

    suspend fun getAqiNow(cityName: String): Pair<Int, Long>

    fun isAqiAvailable(cityName: String): Boolean

    fun getProviderName(): String
    fun getProviderLink(): String

}