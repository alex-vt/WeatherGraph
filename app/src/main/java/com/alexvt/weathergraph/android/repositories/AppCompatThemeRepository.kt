/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.android.repositories

import androidx.appcompat.app.AppCompatDelegate
import com.alexvt.weathergraph.entities.AppTheme
import com.alexvt.weathergraph.repositories.ThemeRepository

class AppCompatThemeRepository : ThemeRepository {

    private val themeMap = mapOf(
        AppTheme.AUTO to AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
        AppTheme.DARK to AppCompatDelegate.MODE_NIGHT_YES,
        AppTheme.LIGHT to AppCompatDelegate.MODE_NIGHT_NO
    )

    override fun applyTheme(appTheme: AppTheme) {
        AppCompatDelegate.setDefaultNightMode(themeMap[appTheme]!!)
    }

}