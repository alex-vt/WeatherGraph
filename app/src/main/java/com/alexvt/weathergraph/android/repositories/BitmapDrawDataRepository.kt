/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.android.repositories

import android.content.Context
import android.graphics.*
import android.graphics.Bitmap.CompressFormat
import com.alexvt.weathergraph.entities.WeatherWidget
import com.alexvt.weathergraph.math.WidgetColorUtil.parseColor
import com.alexvt.weathergraph.repositories.DrawDataRepository
import java.io.ByteArrayOutputStream
import java.time.*
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.round


class BitmapDrawDataRepository @Inject constructor(
    private val context: Context
) : DrawDataRepository {

    override fun drawPalette(
        palette: List<Pair<Double, String>>,
        targetSizePx: Pair<Int, Int>
    ): ByteArray {
        val (width, height) = targetSizePx
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        canvas.drawRect(
            Rect(0, 0, width, height),
            getPalettePaint(palette.normalize(), height)
        )

        return ByteArrayOutputStream().apply {
            bitmap.compress(CompressFormat.PNG, 0, this) // todo async
        }.toByteArray()
    }

    private fun List<Pair<Double, String>>.normalize() = run {
        val min = map { it.first }.min() ?: 0.0
        val max = map { it.first }.max() ?: 1.0
        map {
            Pair(
                it.first.normalizedBetween(min, max),
                it.second.parseColor()
            )
        }
    }

    private fun Double.normalizedBetween(min: Double, max: Double) = (this - min) / (max - min)

    private fun getPalettePaint(palette: List<Pair<Double, Int>>, height: Int) = Paint().apply {
        style = Paint.Style.FILL
        shader = LinearGradient(
            0f, height.toFloat(), 0f, 0f,
            palette.map { it.second }.toIntArray(),
            palette.map { it.first.toFloat() }.toFloatArray(),
            Shader.TileMode.CLAMP
        )
    }

    override fun draw(
        widget: WeatherWidget,
        targetSizePx: Pair<Int, Int>,
        withMargins: Boolean,
        withRoundedCorners: Boolean
    ): ByteArray {
        val (width, height) = targetSizePx
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).draw(
            widget.normalizeDataForThisDrawing(width, height, withMargins), withRoundedCorners
        )
        return ByteArrayOutputStream().apply {
            bitmap.compress(CompressFormat.PNG, 0, this) // todo async
        }.toByteArray()
    }

    /**
     * Adjusts data points to fit them within bitmap size and according to visual settings
     */
    private fun WeatherWidget.normalizeDataForThisDrawing(
        width: Int,
        height: Int,
        withMargins: Boolean
    ) = copy(
        visualSettings = visualSettings.copy(
            marginTopPx = if (withMargins) visualSettings.marginTopPx else 0,
            marginBottomPx = if (withMargins) visualSettings.marginBottomPx else 0,
            marginLeftPx = if (withMargins) visualSettings.marginLeftPx else 0,
            marginRightPx = if (withMargins) visualSettings.marginRightPx else 0
        ),
        weatherData = weatherData // todo
    )

    private fun Canvas.draw(widget: WeatherWidget, withRoundedCorners: Boolean) {
        // todo draw
        if (widget.weatherData.temperatureKelvinPoints.isEmpty()) return // todo check data
        drawColor(Color.argb(0, 0, 0, 0))
        drawWidgetBackground(widget, withRoundedCorners)
        if (widget.visualSettings.showPrecipitation) {
            drawPrecipitationGraph(widget)
        }
        drawTemperatureGraph(widget)
        drawWarningOverlay(widget)
        drawHeadline(widget)
        drawTemperatureScale(widget)
        if (widget.visualSettings.showPrecipitation) {
            drawPrecipitationScale(widget)
        }
        drawTimeline(widget)
    }

    private fun Canvas.getWidgetBounds(widget: WeatherWidget) = Rect(
        widget.visualSettings.marginLeftPx + widget.getPadding(),
        widget.visualSettings.marginTopPx + widget.getPadding(),
        width - widget.visualSettings.marginRightPx - widget.getPadding(),
        height - widget.visualSettings.marginBottomPx - widget.getPadding()
    )

    private fun Canvas.getWidgetBackgroundBounds(widget: WeatherWidget) = RectF(
        widget.visualSettings.marginLeftPx.toFloat(),
        widget.visualSettings.marginTopPx.toFloat(),
        width - widget.visualSettings.marginRightPx.toFloat(),
        height - widget.visualSettings.marginBottomPx.toFloat()
    )

    private fun Canvas.getGraphBounds(widget: WeatherWidget) = Rect(
        getWidgetBounds(widget).left + widget.getTemperatureLeftMargin(),
        getWidgetBounds(widget).top + getTemperatureTopMargin(),
        getWidgetBounds(widget).right - widget.getTemperatureRightMargin(),
        getWidgetBounds(widget).bottom - getTemperatureBottomMargin()
    )

    private fun WeatherWidget.getPadding() = visualSettings.textSizePx / 3

    private fun WeatherWidget.getCornerRadius(withRoundedCorners: Boolean) =
        if (withRoundedCorners) visualSettings.textSizePx / 2f else 0f

    private fun Canvas.drawWidgetBackground(
        widget: WeatherWidget, withRoundedCorners: Boolean
    ) = drawRoundRect(
        getWidgetBackgroundBounds(widget),
        widget.getCornerRadius(withRoundedCorners),
        widget.getCornerRadius(withRoundedCorners),
        widget.getBackgroundPaint()
    )

    private fun WeatherWidget.getBackgroundPaint() = Paint().apply {
        color = Color.parseColor(visualSettings.backgroundColor)
        isAntiAlias = true
    }

    private fun WeatherWidget.getUpdateLocalTimeText() = Instant
        .ofEpochMilli(status.lastUpdatedTimeMillis)
        .atOffset(getZoneOffset())
        .let { DateTimeFormatter.ofPattern("HH:mm").format(it) }

    private fun WeatherWidget.getRecentLocalMidnightMillis() = LocalDate
        .now(getZoneOffset())
        .let { LocalDateTime.of(it, LocalTime.MIDNIGHT) }

    private fun WeatherWidget.getLocalDayOfWeekText(timeMillis: Long) = Instant
        .ofEpochMilli(timeMillis)
        .atOffset(getZoneOffset())
        .let { DateTimeFormatter.ofPattern("ccc").format(it) }

    private fun WeatherWidget.getZoneOffset() =
        ZoneOffset.ofTotalSeconds(weatherData.timezoneShiftMillis.toInt() / 1000)

    private fun WeatherWidget.getNextLocalMidnightsMillis() =
        (1..visualSettings.showDaysAhead).map { daysSinceRecentMidnight ->
            getRecentLocalMidnightMillis()
                .plusDays(daysSinceRecentMidnight.toLong())
                .toInstant(getZoneOffset())
                .toEpochMilli()
        }.toList()

    private fun WeatherWidget.getTextPaint(
        isSmall: Boolean = false,
        isBold: Boolean = false,
        isTranslucent: Boolean = false
    ) = Paint().apply {
        textSize = (visualSettings.textSizePx * (if (isSmall) 0.67 else 1.0)).toFloat()
        color = visualSettings.textColor.parseColor()
        if (isBold) {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        alpha = if (isTranslucent) 160 else 255
        isAntiAlias = true
    }

    private fun WeatherWidget.getGridPaint() = Paint().apply {
        color = visualSettings.gridColor.parseColor()
        strokeWidth = visualSettings.gridThicknessPx.toFloat()
    }

    private fun Canvas.drawHeadline(widget: WeatherWidget) {
        val temperatureSymbol = "\uD83C\uDF21"
        val showUpdateTime =
            widget.status.lastUpdatedTimeMillis > 0 && widget.visualSettings.showLastUpdateTime
        val showLocation = widget.visualSettings.showLocationName
        val locationLabel = if (showLocation) widget.dataSource.locationName + "  " else ""
        val updateTimeLabel = if (showUpdateTime) widget.getUpdateLocalTimeText() + "⟳" else ""
        val leftText = temperatureSymbol + " " + locationLabel + updateTimeLabel
        drawText(
            leftText,
            getWidgetBounds(widget).left.toFloat(),
            getWidgetBounds(widget).top + 30f,
            widget.getTextPaint()
        )
        val showAirQuality = widget.status.isOk
                && widget.dataSource.aquicnCityName.isNotBlank()
                && widget.visualSettings.showAirQuality
                && widget.weatherData.airQualityPoints.isNotEmpty()
        val airQualityLabel = if (showAirQuality) {
            "Air quality: " + widget.weatherData.airQualityPoints.first().first
        } else {
            ""
        }
        val showError = !widget.status.isOk
        val errorLabel = if (showError) {
            if (widget.status.lastUpdatedTimeMillis == 0L) {
                "Loading data..."
            } else {
                "⚠️ Couldn't update"
            }
        } else {
            ""
        }
        val precipitationSymbol =
            if (widget.visualSettings.showPrecipitation) "  \uD83D\uDCA7" else ""
        val rightText = errorLabel + airQualityLabel + precipitationSymbol
        val rightTextPaint = widget.getTextPaint()
        drawText(
            rightText,
            getWidgetBounds(widget).right - rightText.getTextWidth(rightTextPaint).toFloat(),
            getWidgetBounds(widget).top + 30f,
            rightTextPaint
        )
    }

    private fun Canvas.drawTimeline(widget: WeatherWidget) {
        val timelineY = getWidgetBounds(widget).bottom - 5f
        drawText(
            widget.getLocalDayOfWeekText(System.currentTimeMillis()),
            getWidgetBounds(widget).left.toFloat(),
            timelineY,
            widget.getTextPaint()
        )
        widget.getNextLocalMidnightsMillis().forEach { midnightMillis ->
            val timelineX = getProportionalTargetPixel(
                sourceLow = widget.getTimeRange().first.toDouble(),
                sourceHigh = widget.getTimeRange().second.toDouble(),
                source = midnightMillis.toDouble(),
                targetLow = getGraphBounds(widget).left,
                targetHigh = getGraphBounds(widget).right
            )
            drawLine(
                timelineX, getGraphBounds(widget).top.toFloat(),
                timelineX, timelineY,
                widget.getGridPaint()
            )
            val weekDayText = widget.getLocalDayOfWeekText(midnightMillis)
            val weekDayTextWidth = weekDayText.getTextWidth(widget.getTextPaint())
            if (timelineX + weekDayTextWidth < getGraphBounds(widget).right) {
                drawText(weekDayText, timelineX, timelineY, widget.getTextPaint())
            }
        }
    }

    private val kelvinToCelsiusDiff = -273.16

    private fun Double.toCelsiusText() = "${round(this + kelvinToCelsiusDiff).toInt()}˚"

    private fun WeatherWidget.getTempMinText() = getTemperatures().min()!!.toCelsiusText()

    private fun WeatherWidget.getTempMaxText() = getTemperatures().max()!!.toCelsiusText()

    private fun WeatherWidget.getTempCurrentText() = getTemperatures().first().toCelsiusText()

    private fun Canvas.getMaxTempScaleY(widget: WeatherWidget) =
        getGraphBounds(widget).top + 30

    private fun Canvas.getMinTempScaleY(widget: WeatherWidget) =
        getGraphBounds(widget).bottom

    private fun Canvas.getCurrentTempScaleY(widget: WeatherWidget) = getProportionalTargetPixel(
        sourceLow = widget.getTemperatures().min()!!,
        sourceHigh = widget.getTemperatures().max()!!,
        source = widget.getTemperatures().first(),
        targetLow = getMinTempScaleY(widget),
        targetHigh = getMaxTempScaleY(widget)
    )

    private fun getPrecipMinText() = 0.toString()

    private fun WeatherWidget.getPrecipMaxText() =
        round(getPrecipScaleSizeMmHour()).toInt().toString()

    private fun getPrecipUnit() = "mm" // todo get elsewhere

    private fun Canvas.drawPrecipitationScale(widget: WeatherWidget) {
        drawText(
            widget.getPrecipMaxText(),
            getGraphBounds(widget).right.toFloat(),
            getGraphBounds(widget).top + 30f,
            widget.getTextPaint(isTranslucent = true)
        )
        drawText(
            getPrecipUnit(),
            getGraphBounds(widget).right.toFloat(),
            getGraphBounds(widget).top + 50f,
            widget.getTextPaint(isSmall = true, isTranslucent = true)
        )
        drawText(
            getPrecipMinText(),
            getGraphBounds(widget).right.toFloat(),
            getGraphBounds(widget).bottom - 5f,
            widget.getTextPaint(isTranslucent = true)
        )
    }

    private fun Canvas.drawTemperatureScale(widget: WeatherWidget) {
        val maxTempY = getMaxTempScaleY(widget).toFloat()
        val minTempY = getMinTempScaleY(widget).toFloat()
        val currentTempY = getCurrentTempScaleY(widget)
        val minScaleMarkDistance =
            widget.getTempCurrentText().getTextHeight(widget.getTextPaint()) * 1.2

        drawLine(
            getGraphBounds(widget).left.toFloat(),
            getGraphBounds(widget).top.toFloat(),
            getGraphBounds(widget).right.toFloat(),
            getGraphBounds(widget).top.toFloat(),
            widget.getGridPaint()
        )
        drawLine(
            getGraphBounds(widget).left.toFloat(),
            getGraphBounds(widget).bottom.toFloat(),
            getGraphBounds(widget).right.toFloat(),
            getGraphBounds(widget).bottom.toFloat(),
            widget.getGridPaint()
        )

        drawText(
            widget.getTempCurrentText(),
            getWidgetBounds(widget).left.toFloat(),
            currentTempY,
            widget.getTextPaint(isBold = true)
        )
        if (abs(maxTempY - currentTempY) > minScaleMarkDistance) {
            drawText(
                widget.getTempMaxText(),
                getWidgetBounds(widget).left.toFloat(),
                maxTempY,
                widget.getTextPaint(isTranslucent = true)
            )
        }
        if (abs(minTempY - currentTempY) > minScaleMarkDistance) {
            drawText(
                widget.getTempMinText(),
                getWidgetBounds(widget).left.toFloat(),
                minTempY,
                widget.getTextPaint(isTranslucent = true)
            )
        }
    }

    private fun String.getTextHeight(paint: Paint) = with(Rect()) {
        paint.getTextBounds(this@getTextHeight, 0, this@getTextHeight.length, this)
        height()
    }

    private fun String.getTextWidth(paint: Paint) = with(Rect()) {
        paint.getTextBounds(this@getTextWidth, 0, this@getTextWidth.length, this)
        width()
    }

    private fun WeatherWidget.getTemperatureLeftMargin() = listOf(
        getTempMinText().getTextWidth(getTextPaint()),
        getTempCurrentText().getTextWidth(getTextPaint(isBold = true)),
        getTempMaxText().getTextWidth(getTextPaint()),
        getLocalDayOfWeekText(System.currentTimeMillis()).getTextWidth(getTextPaint())
    ).max()!!

    private fun getTemperatureBottomMargin() = 35

    private fun getTemperatureTopMargin() = 35

    private fun WeatherWidget.getTemperatureRightMargin() = listOf(
        getPrecipMinText().getTextWidth(getTextPaint()),
        getPrecipMaxText().getTextWidth(getTextPaint()),
        getPrecipUnit().getTextWidth(getTextPaint(isSmall = true))
    ).max()!!

    private fun WeatherWidget.getPrecipScaleSizeMmHour() = visualSettings.precipitationCutoffValue

    private fun Canvas.drawPrecipitationGraph(widget: WeatherWidget) {
        val bars = widget.weatherData.precipitationMmHourPoints.windowed(2) { pointPair ->
            val topY = getProportionalTargetPixel(
                sourceLow = 0.0,
                sourceHigh = widget.getPrecipScaleSizeMmHour(),
                source = min(widget.getPrecipScaleSizeMmHour(), pointPair.last().first),
                targetLow = getGraphBounds(widget).bottom,
                targetHigh = getGraphBounds(widget).top
            )
            val leftX = getProportionalTargetPixel(
                sourceLow = widget.getTimeRange().first.toDouble(),
                sourceHigh = widget.getTimeRange().second.toDouble(),
                source = pointPair.first().second.toDouble(),
                targetLow = getGraphBounds(widget).left,
                targetHigh = getGraphBounds(widget).right
            )
            val rightX = getProportionalTargetPixel(
                sourceLow = widget.getTimeRange().first.toDouble(),
                sourceHigh = widget.getTimeRange().second.toDouble(),
                source = pointPair.last().second.toDouble(),
                targetLow = getGraphBounds(widget).left,
                targetHigh = getGraphBounds(widget).right
            )
            val bottomY = getGraphBounds(widget).bottom.toFloat()
            RectF(leftX, topY, rightX, bottomY)
        }.filter { it.top != it.bottom && it.left != it.right }
        bars.forEach { drawRect(it, getPrecipitationPaint(widget)) }
    }

    private fun Canvas.getPrecipitationPaint(widget: WeatherWidget) = Paint().apply {
        style = Paint.Style.FILL
        val tempPalette = widget.visualSettings.precipitationBarsPalette
            .map { Color.parseColor(it.second) }
            .toIntArray()
        val tempWeights = widget.visualSettings.precipitationBarsPalette
            .map { it.first.toFloat() }
            .toFloatArray()
        shader = LinearGradient(
            0f,
            getGraphBounds(widget).bottom.toFloat(),
            0f,
            getGraphBounds(widget).top.toFloat(),
            tempPalette, tempWeights, Shader.TileMode.CLAMP
        )
    }

    private fun Canvas.drawWarningOverlay(widget: WeatherWidget) {
        if (widget.status.isOk) return
        drawRect(getGraphBounds(widget), getOldDataWarningOverlay())
    }

    private fun getOldDataWarningOverlay() = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
        shader = LinearGradient(
            0f, 40f, 40f, 0f,
            listOf(Color.GRAY, Color.GRAY, Color.TRANSPARENT, Color.TRANSPARENT).toIntArray(),
            listOf(0f, 0.5f, 0.5f, 1f).toFloatArray(),
            Shader.TileMode.REPEAT
        )
    }

    private fun Canvas.drawTemperatureGraph(widget: WeatherWidget) {
        val pointCoords = widget.weatherData.temperatureKelvinPoints.map {
            PointF(
                getProportionalTargetPixel(
                    sourceLow = widget.getTimeRange().first.toDouble(),
                    sourceHigh = widget.getTimeRange().second.toDouble(),
                    source = it.second.toDouble(),
                    targetLow = getGraphBounds(widget).left,
                    targetHigh = getGraphBounds(widget).right
                ),
                getProportionalTargetPixel(
                    sourceLow = widget.getTemperatureRange().first,
                    sourceHigh = widget.getTemperatureRange().second,
                    source = it.first,
                    targetLow = getGraphBounds(widget).bottom,
                    targetHigh = getGraphBounds(widget).top
                )
            )
        }
        drawSmoothCurve(pointCoords, getTemperatureGraphPaint(widget))
    }

    private fun Canvas.getTemperatureGraphPaint(widget: WeatherWidget) = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
        strokeWidth = widget.visualSettings.temperatureThicknessPx.toFloat()
        val tempPalette = widget.visualSettings.temperatureGraphPalette
            .map { Color.parseColor(it.second) }
            .toIntArray()
        shader = LinearGradient(
            0f,
            getTemperatureColorPixelY(
                widget, widget.visualSettings.temperatureGraphPalette.first().first
            ),
            0f,
            getTemperatureColorPixelY(
                widget, widget.visualSettings.temperatureGraphPalette.last().first
            ),
            tempPalette, null, Shader.TileMode.CLAMP
        )
    }

    private fun Canvas.getTemperatureColorPixelY(widget: WeatherWidget, temperature: Double) =
        getProportionalTargetPixel(
            sourceLow = widget.getTemperatureRange().first,
            sourceHigh = widget.getTemperatureRange().second,
            source = temperature,
            targetLow = getGraphBounds(widget).bottom,
            targetHigh = getGraphBounds(widget).top
        )

    private fun getProportionalTargetPixel(
        sourceLow: Double, sourceHigh: Double,
        source: Double,
        targetLow: Int, targetHigh: Int
    ) = (targetLow + (source - sourceLow) / (sourceHigh - sourceLow) * (targetHigh - targetLow))
        .toFloat()

    private fun Canvas.drawSmoothCurve(points: List<PointF>, paint: Paint) {
        val path = Path()
        path.moveTo(points.first().x, points.first().y)
        for (i in 1 until points.size - 2) {
            path.quadTo(
                points[i].x, points[i].y,
                (points[i].x + points[i + 1].x) / 2, (points[i].y + points[i + 1].y) / 2
            )
        }
        // curve through the last two points
        path.quadTo(
            points.penultimate().x, points.penultimate().y,
            points.last().x, points.last().y
        )
        drawPath(path, paint)
    }

    private fun <T> List<T>.penultimate() = dropLast(1).last()

    private fun WeatherWidget.getTimeRange() =
        with(weatherData.temperatureKelvinPoints.map { it.second }) {
            Pair(System.currentTimeMillis(), max()!!)
        }

    private fun WeatherWidget.getTemperatureRange() =
        with(getTemperatures()) {
            Pair(min()!!, max()!!)
        }

    private fun WeatherWidget.getTemperatures() =
        weatherData.temperatureKelvinPoints.map { it.first }
}
