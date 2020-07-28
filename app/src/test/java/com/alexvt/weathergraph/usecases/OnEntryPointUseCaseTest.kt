/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.usecases

import com.alexvt.weathergraph.repositories.LogRepository
import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.reactivex.observers.TestObserver
import org.junit.Before
import org.junit.Test
import javax.inject.Inject
import javax.inject.Singleton

class OnEntryPointUseCaseTest {

    val scheduleUpdatesUseCaseFactory = mockk<ScheduleUpdatesUseCase.Factory>()
    val log = mockk<LogRepository>()
    val entryPointDescription = "Entry Point Description"

    val case = OnEntryPointUseCase.Factory(
        scheduleUpdatesUseCaseFactory, log
    ).createFor(entryPointDescription).let {
        spyk(it)
    }

    @Before
    fun setUp() {
        every { scheduleUpdatesUseCaseFactory.single.scheduleUpdates() } returns Unit
        every { log.d(any()) } returns Unit
    }

    @Test
    fun `Rescheduling updates`() {
        case.rescheduleUpdates()
        verify(exactly = 1) {
            case.rescheduleUpdates()
            scheduleUpdatesUseCaseFactory.single.scheduleUpdates()
            log.d(any())
        }
    }

}