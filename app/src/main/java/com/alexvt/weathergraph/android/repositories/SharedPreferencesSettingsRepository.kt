/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.android.repositories

import android.content.Context
import android.content.Context.MODE_PRIVATE
import com.alexvt.weathergraph.entities.AppSettings
import com.alexvt.weathergraph.entities.AppTheme
import com.alexvt.weathergraph.entities.SortingMethod
import com.alexvt.weathergraph.repositories.SettingsRepository
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import javax.inject.Inject


class SharedPreferencesSettingsRepository @Inject constructor(
    context: Context
) : SettingsRepository {
    private val default = getDefaultSettings()

    private val sharedPreferences = context.getSharedPreferences("", MODE_PRIVATE)
    private val settingsSubject = BehaviorSubject.create<AppSettings>().also { it.onNext(get()) }

    override fun observe(): Observable<AppSettings> = settingsSubject.hide()

    override fun get() = with(sharedPreferences) {
        AppSettings(
            welcomed = getBoolean("welcomed", default.welcomed),
            showWallpaper = getBoolean("showWallpaper", default.showWallpaper),
            sortingAscending = getBoolean("sortingAscending", default.sortingAscending),
            sortingMethod = getInt("sortingMethod", default.sortingMethod.ordinal).let {
                SortingMethod.values()[it]
            },
            theme = getInt("theme", default.theme.ordinal).let { AppTheme.values()[it] },
            styleIndex = getInt("styleIndex", default.styleIndex)
        )
    }

    override fun save(appSettings: AppSettings) =
        with(sharedPreferences.edit()) {
            putBoolean("welcomed", appSettings.welcomed)
            putBoolean("showWallpaper", appSettings.showWallpaper)
            putBoolean("sortingAscending", appSettings.sortingAscending)
            putInt("sortingMethod", appSettings.sortingMethod.ordinal)
            putInt("theme", appSettings.theme.ordinal)
            putInt("styleIndex", appSettings.styleIndex)
            apply()
        }.also { settingsSubject.onNext(appSettings) }

}