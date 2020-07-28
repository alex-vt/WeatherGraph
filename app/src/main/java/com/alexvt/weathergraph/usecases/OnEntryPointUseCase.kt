/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.usecases

import com.alexvt.weathergraph.repositories.LogRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Does all possibly necessary operations when the app is started via an entry point.
 */
class OnEntryPointUseCase private constructor(
    private val scheduleUpdatesUseCaseFactory: ScheduleUpdatesUseCase.Factory,
    private val log: LogRepository,
    private val entryPointDescription: String
) {
    @Singleton
    class Factory @Inject constructor(
        private val scheduleUpdatesUseCaseFactory: ScheduleUpdatesUseCase.Factory,
        private val log: LogRepository
    ) {
        fun createFor(entryPointDescription: String) =
            OnEntryPointUseCase(scheduleUpdatesUseCaseFactory, log, entryPointDescription)
    }

    fun rescheduleUpdates() {
        log.d("Entered app via $entryPointDescription, going to reschedule widget updates")
        scheduleUpdatesUseCaseFactory.single.scheduleUpdates()
    }

}