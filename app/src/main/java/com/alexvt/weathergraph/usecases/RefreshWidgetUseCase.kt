/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.usecases

import com.alexvt.weathergraph.entities.WeatherWidget
import com.alexvt.weathergraph.repositories.DrawTargetRepository
import com.alexvt.weathergraph.repositories.LogRepository
import com.alexvt.weathergraph.repositories.WeatherWidgetRepository
import javax.inject.Inject
import javax.inject.Singleton

class RefreshWidgetUseCase private constructor(
    private val widget: WeatherWidget,
    private val widgetRepository: WeatherWidgetRepository,
    private val targetRepository: DrawTargetRepository,
    private val log: LogRepository
) {
    @Singleton
    class Factory @Inject constructor(
        private val widgetRepository: WeatherWidgetRepository,
        private val targetRepository: DrawTargetRepository,
        private val log: LogRepository
    ) {
        fun createFor(widget: WeatherWidget) =
            RefreshWidgetUseCase(widget, widgetRepository, targetRepository, log)
    }

    fun updateAndRedraw() {
        if (widget.needsUpdate()) {
            Pair(widgetRepository.updateAndGet(widget), true)
        } else {
            Pair(widget, false)
        }.let { (widgetUpToDate, wasUpdatedNow) ->
            targetRepository.draw(widgetUpToDate)
            if (widgetUpToDate.widgetId !in targetRepository.getAllTargetIds()) {
                log.e("Doesn't have home screen widget - $widgetUpToDate") // todo act on
            }
            if (wasUpdatedNow) {
                log.d("Updated and redrawn $widgetUpToDate")
            } else {
                log.d("Redrawn up to date $widgetUpToDate")
            }
        }
    }

    private fun WeatherWidget.needsUpdate(): Boolean {
        val hasIssues = !this.status.isOk
        val isDataExpired = this.status.lastUpdatedTimeMillis <
                System.currentTimeMillis() - this.dataSource.updatePeriodMillis
        return hasIssues || isDataExpired
    }

}