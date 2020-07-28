/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.usecases

import com.alexvt.weathergraph.entities.AppTheme
import com.alexvt.weathergraph.entities.SortingMethod
import com.alexvt.weathergraph.entities.WeatherWidget
import com.alexvt.weathergraph.repositories.DrawDataRepository
import com.alexvt.weathergraph.repositories.WeatherWidgetRepository
import com.alexvt.weathergraph.math.WidgetColorUtil.parseColor
import com.alexvt.weathergraph.repositories.SettingsRepository
import io.reactivex.Observable
import io.reactivex.functions.Function3
import javax.inject.Inject
import javax.inject.Singleton

class UserObserveWidgetsUseCase private constructor(
    private val widgetRepository: WeatherWidgetRepository,
    private val drawDataRepository: DrawDataRepository,
    private val settingsRepository: SettingsRepository
) {
    @Singleton
    class Factory @Inject constructor(
        private val widgetRepository: WeatherWidgetRepository,
        private val drawDataRepository: DrawDataRepository,
        private val settingsRepository: SettingsRepository
    ) {
        fun create() = UserObserveWidgetsUseCase(
            widgetRepository, drawDataRepository, settingsRepository
        )
    }

    data class WidgetVisualData(
        val widgetId: Int, val imageData: ByteArray
    )

    private val settings = settingsRepository.observe()

    fun observeAll(targetSizePx: Pair<Int, Int>): Observable<List<WidgetVisualData>> =
        Observable.combineLatest<List<WeatherWidget>, SortingMethod, Boolean, List<WidgetVisualData>>(
            widgetRepository.observeAll(),
            settings.map { it.sortingMethod },
            settings.map { it.sortingAscending },
            Function3 { widgets, sortingMethod, isSortingAscending ->
                widgets.sort(sortingMethod, isSortingAscending).map { widget ->
                    WidgetVisualData(
                        widget.widgetId,
                        drawDataRepository.draw(widget, targetSizePx, withRoundedCorners = false)
                    )
                }
            }
        )

    private fun List<WeatherWidget>.sort(sortingMethod: SortingMethod, isAscending: Boolean) =
        when (sortingMethod) {
            SortingMethod.NAME -> sortedBy { it.dataSource.locationName }
            SortingMethod.LATITUDE -> sortedBy { it.dataSource.latitude }
            SortingMethod.LONGITUDE -> sortedBy { it.dataSource.longitude }
        }.let { if (isAscending) it else it.reversed() }
}