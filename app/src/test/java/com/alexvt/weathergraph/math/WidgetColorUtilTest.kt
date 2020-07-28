/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.math

import com.alexvt.weathergraph.math.WidgetColorUtil.parseColor
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class WidgetColorUtilTest {
    @Test
    fun `Color #00000000 is parsed as transparent (0)`() {
        assertEquals("#00000000".parseColor(), 0)
    }

    @Test
    fun `Color #FFFFFFFF is parsed as white (0xFFFFFFFF)`() {
        assertEquals("#FFFFFFFF".parseColor(), -0x1)
    }

    @Test
    fun `Color #000000 is parsed as black (0xFF000000)`() {
        assertEquals("#000000".parseColor(), -0x1000000)
    }

    @Test(expected = IllegalStateException::class)
    fun `Color 000000 fails to parse`() {
        "000000".parseColor()
    }

    @Test(expected = NumberFormatException::class)
    fun `Color ##000000 fails to parse`() {
        "##000000".parseColor()
    }
}
