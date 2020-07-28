/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.viewmodel

import com.alexvt.weathergraph.usecases.*
import javax.inject.Inject

class QuickSettingsViewModel @Inject constructor(
    userManageSettingsUseCase: UserManageSettingsUseCase.Factory,
    override val userManageSettingsUseCaseFactory: UserManageSettingsUseCase.Factory
) : BaseViewModel() {

    private val case = userManageSettingsUseCase.single

    val sortingMethods = case.sortingMethods
    val sortingMethodIndexLiveData by lazy { case.observeSortingMethodIndex().toLiveData() }

    fun clickSortingMethod(index: Int) = case.setSortingMethodIndex(index)

    fun setSortingAscending(value: Boolean) = case.setSortAscending(value)
    val sortingAscendingLiveData by lazy { case.observeSortingAscending().toLiveData() }

    fun setShowWallpaper(show: Boolean) = case.setShowWallpaper(show)
    val showWallpaperLiveData by lazy { case.observeShowWallpaper().toLiveData() }

    val themes = case.themes
    val themeIndexLiveData by lazy { case.observeThemeIndex().toLiveData() }
    fun clickTheme(index: Int) = case.setThemeIndex(index)

    val styleIndexLiveData by lazy { case.observeStyleIndex().toLiveData() }
    fun clickStyle(index: Int) = case.setStyleIndex(index)
}