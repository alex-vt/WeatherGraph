/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.android.repositories

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.alexvt.weathergraph.R
import com.alexvt.weathergraph.android.WidgetDetailsActivity
import com.alexvt.weathergraph.android.WidgetEventAppWidgetProvider
import com.alexvt.weathergraph.entities.WeatherWidget
import com.alexvt.weathergraph.repositories.DrawDataRepository
import com.alexvt.weathergraph.repositories.DrawTargetRepository
import javax.inject.Inject


class ViewDrawTargetRepository @Inject constructor(
    private val drawDataRepository: DrawDataRepository,
    private val context: Context
) : DrawTargetRepository {

    override fun getAllTargetIds(): List<Int> {
        val name = ComponentName(context, WidgetEventAppWidgetProvider::class.java)
        val ids = getAppWidgetManager().getAppWidgetIds(name)
        return ids.asList()
    }

    private fun getAppWidgetManager() = AppWidgetManager.getInstance(context)

    override fun draw(widget: WeatherWidget) =
        RemoteViews(
            context.packageName,
            R.layout.weather_widget_initial
        ).apply {
            drawDataRepository.draw(widget, getWidgetSize(widget.widgetId), true).let {
                setImageViewBitmap(R.id.ivWidget, BitmapFactory.decodeByteArray(it, 0, it.size))
            }
            setViewVisibility(R.id.rlBackground, View.GONE)
            setOnClickPendingIntent(R.id.ivWidget, getPendingIntent(widget.widgetId))
        }.let {
            getAppWidgetManager().updateAppWidget(widget.widgetId, it)
        }

    private fun getWidgetSize(id: Int) = run {
        val options = getAppWidgetManager().getAppWidgetOptions(id)
        val portWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 1)
        val landWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 1)
        val landHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 1)
        val portHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 1)
        val isLandscape =
            context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        if (isLandscape) {
            Pair(landWidth, landHeight)
        } else {
            Pair(portWidth, portHeight)
        }.let {
            Pair(dpToPx(it.first), dpToPx(it.second))
        }
    }

    private fun dpToPx(dp: Number) =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            context.resources.displayMetrics
        ).toInt()

    override fun clear(targetId: Int) {
        RemoteViews(
            context.packageName,
            R.layout.weather_widget_initial
        ).apply {
            setTextViewText(R.id.tvTitle, "Please select location to show weather for")
            setViewVisibility(R.id.rlBackground, View.VISIBLE)
            setOnClickPendingIntent(R.id.rlBackground, getPendingIntent(targetId))
            setImageViewBitmap(R.id.ivWidget, null)
        }.let {
            getAppWidgetManager().updateAppWidget(targetId, it)
        }
    }

    private fun getPendingIntent(targetId: Int) =
        Intent(context, WidgetDetailsActivity::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE + System.currentTimeMillis()
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, targetId)
        }.let { intent ->
            PendingIntent.getActivity(context, 0, intent, 0)
        }

}