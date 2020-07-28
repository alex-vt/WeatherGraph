/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.usecases

import com.alexvt.weathergraph.repositories.DrawTargetRepository
import com.alexvt.weathergraph.repositories.LogRepository
import com.alexvt.weathergraph.repositories.WeatherWidgetRepository
import javax.inject.Inject
import javax.inject.Singleton

class UserRemoveWidgetUseCase private constructor(
    private val widgetRepository: WeatherWidgetRepository,
    private val scheduleUpdatesUseCaseFactory: ScheduleUpdatesUseCase.Factory,
    private val drawTargetRepository: DrawTargetRepository,
    private val log: LogRepository,
    private val widgetId: Int
) {
    @Singleton
    class Factory @Inject constructor(
        private val widgetRepository: WeatherWidgetRepository,
        private val scheduleUpdatesUseCaseFactory: ScheduleUpdatesUseCase.Factory,
        private val drawTargetRepository: DrawTargetRepository,
        private val log: LogRepository
    ) {
        fun createFor(widgetId: Int) = UserRemoveWidgetUseCase(
            widgetRepository, scheduleUpdatesUseCaseFactory, drawTargetRepository, log, widgetId
        )
    }

    fun remove() {
        val widgetsToRemove = widgetRepository.getCurrentAll().filter { it.widgetId == widgetId }
        log.d("Removing (for target ID $widgetId) $widgetsToRemove")
        widgetsToRemove.forEach {
            widgetRepository.remove(it)
        }
        scheduleUpdatesUseCaseFactory.single.scheduleUpdates()
    }

    private fun existsAsSavedOnHomeScreen() = existsAsSaved()
            && widgetId in drawTargetRepository.getAllTargetIds()

    private fun existsAsSaved() = widgetRepository.getCurrentAll().any { it.widgetId == widgetId }

    fun getRemoveDetailedMessageIndex() = when {
        existsAsSavedOnHomeScreen() -> 0
        existsAsSaved() -> 1
        else -> 2
    }

    fun getRemovePositiveTextIndex() = when {
        existsAsSavedOnHomeScreen() -> 0
        else -> 1
    }

}