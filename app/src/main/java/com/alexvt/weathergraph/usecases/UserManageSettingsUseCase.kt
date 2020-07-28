/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.usecases

import com.alexvt.weathergraph.entities.AppTheme
import com.alexvt.weathergraph.entities.SortingMethod
import com.alexvt.weathergraph.repositories.SettingsRepository
import com.alexvt.weathergraph.repositories.ThemeRepository
import io.reactivex.Observable
import javax.inject.Inject
import javax.inject.Singleton

class UserManageSettingsUseCase private constructor(
    private val settingsRepository: SettingsRepository,
    private val themeRepository: ThemeRepository
) {
    @Singleton
    class Factory @Inject constructor(
        private val settingsRepository: SettingsRepository,
        private val themeRepository: ThemeRepository

    ) {
        val single by lazy { UserManageSettingsUseCase(settingsRepository, themeRepository) }
    }

    private val settings = settingsRepository.observe()

    init {
        settings.map { it.theme }.subscribe { themeRepository.applyTheme(it) }
    }

    fun reset() = settingsRepository.saveDefault()

    fun observeWelcomed(): Observable<Boolean> = settings.map { it.welcomed }.distinctUntilChanged()
    fun setWelcomed(value: Boolean)
            = settingsRepository.save(settingsRepository.get().copy(welcomed = value))

    fun observeShowWallpaper(): Observable<Boolean> = settings.map { it.showWallpaper }
    fun setShowWallpaper(show: Boolean) =
        settingsRepository.save(settingsRepository.get().copy(showWallpaper = show))

    fun setSortAscending(ascending: Boolean) =
        settingsRepository.save(settingsRepository.get().copy(sortingAscending = ascending))
    fun observeSortingAscending(): Observable<Boolean> = settings.map { it.sortingAscending }

    val themes = AppTheme.values()
    fun observeThemeIndex(): Observable<Int> = settings.map { AppTheme.values().indexOf(it.theme) }
    fun setThemeIndex(index: Int) = settingsRepository.save(
        settingsRepository.get().copy(theme = AppTheme.values()[index])
    )

    fun observeStyleIndex(): Observable<Int> = settings.map { it.styleIndex }

    fun getStyleIndex() = settingsRepository.get().styleIndex
    fun setStyleIndex(index: Int) = settingsRepository.save(
        settingsRepository.get().copy(styleIndex = index)
    )

    val sortingMethods = SortingMethod.values()
    fun observeSortingMethodIndex(): Observable<Int> =
        settings.map { SortingMethod.values().indexOf(it.sortingMethod) }
    fun setSortingMethodIndex(index: Int) = settingsRepository.save(
        settingsRepository.get().copy(sortingMethod = SortingMethod.values()[index])
    )
}