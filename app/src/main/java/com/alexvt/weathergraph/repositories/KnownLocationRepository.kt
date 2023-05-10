/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.repositories

import com.alexvt.weathergraph.entities.OwmLocation

interface KnownLocationRepository {

    fun getAll(): List<OwmLocation>

    fun getRawDataPath(): String?

    fun getProviderName(): String

    fun getProviderLink(): String
}