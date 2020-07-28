/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.entities

data class AppSettings(
    val welcomed: Boolean,
    val showWallpaper: Boolean,
    val theme: AppTheme,
    val styleIndex: Int,
    val sortingMethod: SortingMethod,
    val sortingAscending: Boolean
)

enum class AppTheme {
    AUTO, DARK, LIGHT
}

enum class SortingMethod {
    NAME, LATITUDE, LONGITUDE
}
