/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.repositories

import com.alexvt.weathergraph.entities.AppSettings
import com.alexvt.weathergraph.entities.AppTheme
import com.alexvt.weathergraph.entities.SortingMethod
import io.reactivex.Observable

interface SettingsRepository {

    fun get(): AppSettings

    fun observe(): Observable<AppSettings>

    fun save(appSettings: AppSettings)

    fun getDefaultSettings() = AppSettings(
        welcomed = false,
        showWallpaper = false,
        theme = AppTheme.AUTO,
        styleIndex = 0,
        sortingMethod = SortingMethod.NAME,
        sortingAscending = true
    )

    fun saveDefault() = save(getDefaultSettings())
}