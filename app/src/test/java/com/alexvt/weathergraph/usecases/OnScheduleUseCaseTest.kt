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
import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.reactivex.observers.TestObserver
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import javax.inject.Inject
import javax.inject.Singleton

class OnScheduleUseCaseTest {

    val widgetRepository = mockk<WeatherWidgetRepository>()
    val targetRepository = mockk<DrawTargetRepository>()
    val log = mockk<LogRepository>()
    val refreshWidgetUseCaseFactory = mockk<RefreshWidgetUseCase.Factory>()
    val eventId = "Event ID"

    val case = OnScheduleUseCase.Factory(
        widgetRepository, targetRepository, log, refreshWidgetUseCaseFactory
    ).createFor(eventId).let {
        spyk(it)
    }

    val refreshWidgetUseCase = mockk<RefreshWidgetUseCase>()

    @Before
    fun setUp() {
        every { refreshWidgetUseCaseFactory.createFor(any()) } returns refreshWidgetUseCase
        every { refreshWidgetUseCase.updateAndRedraw() } returns Unit
        every { targetRepository.clear(any()) } returns Unit
        every { log.d(any()) } returns Unit
        every { log.e(any()) } returns Unit
    }

    @Test
    fun `Updating widgets 1 2 3`() {
        val storedWidgetIds = listOf(1, 2, 3)
        val homeScreenWidgetIds = listOf(1, 2, 3)
        fillWidgets(storedWidgetIds, homeScreenWidgetIds)

        case.updateWidgets()
        verify(exactly = 1) {
            case.updateWidgets()
            log.d(any())
        }
        verify(exactly = 2) { // once for updateVisibleWidgets, then once for clearPhantomWidgets
            widgetRepository.getCurrentAll()
        }
        verify(exactly = 3) {
            refreshWidgetUseCaseFactory.createFor(any())
            refreshWidgetUseCase.updateAndRedraw()
        }
        assertEquals(storedWidgetIds, widgetRepository.getCurrentAll().map { it.widgetId })
        verify(exactly = 0) {
            targetRepository.clear(any())
            log.e(any())
        }
    }

    @Test
    fun `Updating widgets 1 2 3 when on home screen there are 2 3 4`() {
        val storedWidgetIds = listOf(1, 2, 3)
        val homeScreenWidgetIds = listOf(2, 3, 4)
        fillWidgets(storedWidgetIds, homeScreenWidgetIds)

        case.updateWidgets()
        verify(exactly = 1) {
            case.updateWidgets()
            log.d(any())
            targetRepository.clear(4)
            log.e(any())
        }
        verify(exactly = 2) { // once for updateVisibleWidgets, then once for clearPhantomWidgets
            widgetRepository.getCurrentAll()
        }
        verify(exactly = 3) {
            refreshWidgetUseCaseFactory.createFor(any())
            refreshWidgetUseCase.updateAndRedraw()
        }
    }

    @Test
    fun `Updating no widgets when on home screen there are 2 3 4`() {
        val storedWidgetIds = listOf<Int>()
        val homeScreenWidgetIds = listOf(2, 3, 4)
        fillWidgets(storedWidgetIds, homeScreenWidgetIds)

        case.updateWidgets()
        verify(exactly = 1) {
            case.updateWidgets()
            log.d(any())
            targetRepository.clear(2)
            targetRepository.clear(3)
            targetRepository.clear(4)
        }
        verify(exactly = 2) { // once for updateVisibleWidgets, then once for clearPhantomWidgets
            widgetRepository.getCurrentAll()
        }
        verify(exactly = 0) {
            refreshWidgetUseCaseFactory.createFor(any())
            refreshWidgetUseCase.updateAndRedraw()
        }
        verify(exactly = 3) {
            log.e(any())
        }
    }

    private fun fillWidgets(storedWidgetIds: List<Int>, homeScreenWidgetIds: List<Int>) {
        every { widgetRepository.getCurrentAll() } returns storedWidgetIds.map { id ->
            mockk<WeatherWidget> {
                every { widgetId } answers { id }
            }
        }
        every { targetRepository.getAllTargetIds() } returns homeScreenWidgetIds
    }
}