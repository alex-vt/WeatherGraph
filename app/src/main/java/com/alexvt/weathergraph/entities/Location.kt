/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.entities

import java.text.SimpleDateFormat
import java.util.*

data class OwmLocation(
    val id: Int,
    val longitude: Double,
    val latitude: Double,
    val name: String,
    val country: String,
    val population: Long
)
