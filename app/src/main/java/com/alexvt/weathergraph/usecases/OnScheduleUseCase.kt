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

/**
 * Updates all present widget data and then appearance accordingly.
 */
class OnScheduleUseCase private constructor(
    private val widgetRepository: WeatherWidgetRepository,
    private val targetRepository: DrawTargetRepository,
    private val log: LogRepository,
    private val refreshWidgetUseCaseFactory: RefreshWidgetUseCase.Factory,
    private val eventId: String
) {
    @Singleton
    class Factory @Inject constructor(
        private val widgetRepository: WeatherWidgetRepository,
        private val targetRepository: DrawTargetRepository,
        private val log: LogRepository,
        private val refreshWidgetUseCaseFactory: RefreshWidgetUseCase.Factory
    ) {
        fun createFor(eventId: String) = OnScheduleUseCase(
            widgetRepository, targetRepository, log, refreshWidgetUseCaseFactory, eventId
        )
    }

    fun updateWidgets() {
        log.d("Scheduled update time is now (event $eventId)")
        updateVisibleWidgets()
        clearPhantomWidgets()
    }

    private fun updateVisibleWidgets() = widgetRepository
        .getCurrentAll()
        .forEach { widget ->
            refreshWidgetUseCaseFactory.createFor(widget).updateAndRedraw()
        }

    private fun clearPhantomWidgets() = targetRepository
        .getAllTargetIds()
        .minus(widgetRepository.getCurrentAll().map { it.widgetId })
        .forEach {
            targetRepository.clear(it)
            log.e("Cleared phantom widget for a fresh setup, id $it") // todo act on
        }
}