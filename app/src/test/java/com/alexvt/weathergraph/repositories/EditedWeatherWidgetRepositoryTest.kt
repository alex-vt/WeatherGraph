/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.repositories

import com.alexvt.weathergraph.entities.WeatherWidget
import io.mockk.impl.annotations.SpyK
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import io.reactivex.observers.TestObserver
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class EditedWeatherWidgetRepositoryTest {
    @SpyK
    val exampleWidget = mockk<WeatherWidget>()

    val repository = spyk(EditedWeatherWidgetRepository())

    @Test
    fun `Setting and getting edited widget return that widget`() {
        repository.set(exampleWidget)
        assertEquals(repository.getCurrent(), exampleWidget)
        verify(exactly = 1) {
            repository.set(exampleWidget)
            repository.getCurrent()
        }
    }

    @Test
    fun `Widget remains after cleaning`() {
        repository.set(exampleWidget)
        repository.clear()
        assertEquals(repository.getCurrent(), exampleWidget)
        verify(exactly = 1) {
            repository.set(exampleWidget)
            repository.clear()
            repository.getCurrent()
        }
    }

    @Test
    fun `Single written value observed`() {
        val testObserver = TestObserver<WeatherWidget>()
        repository.observe().subscribe(testObserver)
        repository.set(exampleWidget)
        testObserver.assertNoErrors()
        testObserver.assertValueCount(1)
        testObserver.assertNotComplete()
    }

    @Test
    fun `No written values observed`() {
        val testObserver = TestObserver<WeatherWidget>()
        repository.observe().subscribe(testObserver)
        testObserver.assertNoErrors()
        testObserver.assertValueCount(0)
        testObserver.assertNotComplete()
    }

    @Test
    fun `No written values observed and then completed`() {
        val testObserver = TestObserver<WeatherWidget>()
        repository.observe().subscribe(testObserver)
        repository.clear()
        testObserver.assertNoErrors()
        testObserver.assertValueCount(0)
        testObserver.assertComplete()
    }

    @Test
    fun `Two values observed, then completed`() {
        val testObserver = TestObserver<WeatherWidget>()
        repository.observe().subscribe(testObserver)
        repository.set(exampleWidget)
        repository.set(exampleWidget)
        repository.clear()
        testObserver.assertNoErrors()
        testObserver.assertValueCount(2)
        testObserver.assertComplete()
    }

    @Test
    fun `Last written value was observed on subscription`() {
        val testObserver = TestObserver<WeatherWidget>()
        repository.set(exampleWidget)
        repository.set(exampleWidget)
        repository.observe().subscribe(testObserver)
        testObserver.assertNoErrors()
        testObserver.assertValueCount(1)
        testObserver.assertNotComplete()
    }
}
