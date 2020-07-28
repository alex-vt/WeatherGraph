/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.usecases

import com.alexvt.weathergraph.repositories.LogRepository
import com.alexvt.weathergraph.repositories.ScheduledUpdateRepository
import com.alexvt.weathergraph.repositories.WeatherWidgetRepository
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * From the present widgets, derives times when they should be updated, and schedules these updates.
 */
class ScheduleUpdatesUseCase private constructor(
    private val widgetRepository: WeatherWidgetRepository,
    private val scheduledUpdateRepository: ScheduledUpdateRepository,
    private val onScheduleUseCaseFactory: OnScheduleUseCase.Factory,
    private val log: LogRepository
) {
    @Singleton
    class Factory @Inject constructor(
        private val widgetRepository: WeatherWidgetRepository,
        private val scheduledUpdateRepository: ScheduledUpdateRepository,
        private val onScheduleUseCaseFactory: OnScheduleUseCase.Factory,
        private val log: LogRepository
    ) {
        val single by lazy {
            ScheduleUpdatesUseCase(
                widgetRepository, scheduledUpdateRepository, onScheduleUseCaseFactory, log
            )
        }
    }

    private val minUpdateSpacingMillis = 1000L

    private val disposable = scheduledUpdateRepository.getUpdateTimes()
        .throttleLatest(minUpdateSpacingMillis, TimeUnit.MILLISECONDS)
        .subscribe {
            onScheduleUseCaseFactory.createFor(it).updateWidgets()
        }

    fun scheduleUpdates() {
        val minPeriod = widgetRepository.getCurrentAll().map {
            it.dataSource.updatePeriodMillis
        }.min()
        if (minPeriod != null) {
            log.d("Rescheduling widget updates with requested period ${minPeriod / 1000} s")
            scheduledUpdateRepository.schedule(minPeriod)
        } else {
            log.d("Rescheduling to no future updates because there are no widgets")
            scheduledUpdateRepository.scheduleJustNow()
        }
    }

}