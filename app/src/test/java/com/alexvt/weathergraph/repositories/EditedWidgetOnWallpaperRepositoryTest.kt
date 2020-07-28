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
class EditedWidgetOnWallpaperRepositoryTest {

    val repository = spyk(EditedWidgetOnWallpaperRepository())

    @Test
    fun `Observing values after subscription`() {
        val testObserver = TestObserver<Boolean>()
        repository.observe().subscribe(testObserver)
        repository.set(true)
        repository.set(false)
        repository.set(false)
        repository.set(true)
        testObserver.assertNoErrors()
        testObserver.assertValues(true, false, false, true)
        testObserver.assertNotComplete()
    }

    @Test
    fun `Observing last value before and then values after subscription`() {
        val testObserver = TestObserver<Boolean>()
        repository.set(false)
        repository.set(true)
        repository.set(false)
        repository.observe().subscribe(testObserver)
        repository.set(false)
        repository.set(true)
        testObserver.assertNoErrors()
        testObserver.assertValues(false, false, true)
        testObserver.assertNotComplete()
    }

    @Test
    fun `Observing no values`() {
        val testObserver = TestObserver<Boolean>()
        repository.observe().subscribe(testObserver)
        testObserver.assertNoErrors()
        testObserver.assertValueCount(0)
        testObserver.assertNotComplete()
    }

    @Test
    fun `Observing no values and completed`() {
        val testObserver = TestObserver<Boolean>()
        repository.observe().subscribe(testObserver)
        repository.clear()
        testObserver.assertNoErrors()
        testObserver.assertValueCount(0)
        testObserver.assertComplete()
    }
}
