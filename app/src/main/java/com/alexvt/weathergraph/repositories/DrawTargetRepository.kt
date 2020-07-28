/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.repositories

import com.alexvt.weathergraph.entities.WeatherWidget

interface DrawTargetRepository {

    fun getAllTargetIds(): List<Int>

    fun draw(widget: WeatherWidget)

    fun clear(targetId: Int)

}