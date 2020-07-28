/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.math

import android.util.Log
import kotlin.math.sqrt

object WidgetColorUtil {

    /**
     * #AARRGGBB or #RRGGBB to 0xAARRGGBB
     */
    fun String.parseColor() = substring(1).toLong(16).let { longColor ->
        when (length) {
            7 -> longColor or -0x1000000 // adding opaque alpha
            9 -> longColor
            else -> throw IllegalStateException("Wrong color string length")
        }.toInt()
    }

    fun areBlending(themeColor: Int, backgroundColor: Int, textColor: Int) = areColorsAlike(
        themeColor.coverWith(backgroundColor),
        themeColor.coverWith(backgroundColor).coverWith(textColor)
    )

    fun Int.coverWith(topColor: Int) = run {
        val aA = topColor.getAlpha() // typically 255
        val rA = topColor.getRed()
        val gA = topColor.getGreen()
        val bA = topColor.getBlue()

        val aB = this.getAlpha()
        val rB = this.getRed()
        val gB = this.getGreen()
        val bB = this.getBlue()

        val aOut = aA + (aB * (255 - aA) / 255)
        val rOut = (rA * aA / 255) + (rB * aB * (255 - aA) / (255*255))
        val gOut = (gA * aA / 255) + (gB * aB * (255 - aA) / (255*255))
        val bOut = (bA * aA / 255) + (bB * aB * (255 - aA) / (255*255))

        aOut shl 24 or (rOut shl 16) or (gOut shl 8) or bOut
    }
        .also { Log.d("aaa", "${this.toHexString()} covered by ${topColor.toHexString()} mix to ${it.toHexString()}") } // todo remove

    private fun areColorsAlike(color1: Int, color2: Int) =
        getColorSimilarity(color1, color2) < colorSimilarityThreshold

    private const val colorSimilarityThreshold = 100 // todo visually test and adjust

    /**
     * Color similarity according to https://stackoverflow.com/a/40950076
     */
    private fun getColorSimilarity(color1: Int, color2: Int) = run {
        val rmean = getChannelAverage(color1, color2) { getRed() }
        val r = getChannelDiff(color1, color2) { getRed() }
        val g = getChannelDiff(color1, color2) { getGreen() }
        val b = getChannelDiff(color1, color2) { getBlue() }

        val f = ((512 + rmean) * r * r shr 8) + 4 * g * g + ((767 - rmean) * b * b shr 8)
        sqrt(f.toDouble())
    }
        .also { Log.d("aaa", "${color1.toHexString()} and ${color2.toHexString()} diff is $it") } // todo remove

    private fun Int.toHexString() = String.format("#%08X", 0xFFFFFFFF and this.toLong())

    private fun getChannelAverage(color1: Int, color2: Int, channelExtractor: (Int).() -> Int) =
        (color1.channelExtractor() + color2.channelExtractor()) / 2

    private fun getChannelDiff(color1: Int, color2: Int, channelExtractor: (Int).() -> Int) =
        color1.channelExtractor() - color2.channelExtractor()

    private fun Int.getAlpha() = this ushr 24

    private fun Int.getRed() = this shr 16 and 0xFF

    private fun Int.getGreen() = this shr 8 and 0xFF

    private fun Int.getBlue() = this and 0xFF

}