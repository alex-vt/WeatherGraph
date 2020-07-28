/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.repositories

import com.alexvt.weathergraph.entities.*
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject

/**
 * Provides updatable and observable single weather widget in-memory storage
 */
class EditedWeatherWidgetRepository {

    private lateinit var editedWidgetSubject: BehaviorSubject<WeatherWidget>

    private lateinit var remainingLastValue: WeatherWidget // for when value requested after clear()

    private fun getActiveEditedWidgetSubject() = run {
        if (!::editedWidgetSubject.isInitialized || editedWidgetSubject.hasComplete()) {
            editedWidgetSubject = BehaviorSubject.create()
        }
        editedWidgetSubject
    }

    fun set(newWidgetState: WeatherWidget) = getActiveEditedWidgetSubject().onNext(newWidgetState)
        .also { remainingLastValue = newWidgetState }

    fun observe(): Observable<WeatherWidget> = getActiveEditedWidgetSubject().hide()

    fun getCurrent() = getActiveEditedWidgetSubject().value ?: remainingLastValue

    fun clear() = getActiveEditedWidgetSubject().onComplete()

}