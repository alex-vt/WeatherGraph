/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.android

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle
import com.alexvt.weathergraph.repositories.LogRepository
import com.alexvt.weathergraph.usecases.OnEntryPointUseCase
import com.alexvt.weathergraph.usecases.UserRemoveWidgetUseCase
import dagger.android.AndroidInjection
import javax.inject.Inject

/**
 * Weather Android widget event receiver.
 *
 * Widget creation is managed by the activity that opens on its addition.
 * Widget updating is managed by the app use cases launched elsewhere but is still watched here.
 * Widget removal triggers the deletion use case from here.
 */
class WidgetEventAppWidgetProvider : AppWidgetProvider() {

    @Inject
    lateinit var onEntryPointUseCaseFactory: OnEntryPointUseCase.Factory

    @Inject
    lateinit var userRemoveWidgetUseCaseFactory: UserRemoveWidgetUseCase.Factory

    @Inject
    lateinit var log: LogRepository

    override fun onAppWidgetOptionsChanged(
        context: Context,
        widgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        AndroidInjection.inject(this, context)
        log.d("Widget $appWidgetId update with new options invoked by system")
        onEntryPointUseCaseFactory.createFor(this::class.simpleName + ".onAppWidgetOptionsChanged")
            .rescheduleUpdates()
        super.onAppWidgetOptionsChanged(context, widgetManager, appWidgetId, newOptions)
    }

    override fun onUpdate(context: Context, widgetManager: AppWidgetManager, widgetIds: IntArray) {
        AndroidInjection.inject(this, context)
        log.d("Widget ${widgetIds.joinToString()} timely update invoked by system")
        onEntryPointUseCaseFactory.createFor(this::class.simpleName + ".onUpdate").rescheduleUpdates()
        super.onUpdate(context, widgetManager, widgetIds)
    }

    override fun onDeleted(context: Context, widgetIds: IntArray) {
        AndroidInjection.inject(this, context)
        log.d("Widget ${widgetIds.joinToString()} deletion invoked by system")
        widgetIds.forEach { userRemoveWidgetUseCaseFactory.createFor(it).remove() }
        super.onDeleted(context, widgetIds)
    }
}