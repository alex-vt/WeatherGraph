/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.repositories

import io.reactivex.subjects.BehaviorSubject

/**
 * Provides in-memory storage of the kind of background of the edited widget
 */
class EditedWidgetOnWallpaperRepository {

    private lateinit var isOnWallpaperSubject: BehaviorSubject<Boolean>

    private fun getStateSubject() = run {
        if (!::isOnWallpaperSubject.isInitialized || isOnWallpaperSubject.hasComplete()) {
            isOnWallpaperSubject = BehaviorSubject.create()
        }
        isOnWallpaperSubject
    }

    fun set(newState: Boolean) = getStateSubject().onNext(newState)

    fun observe() = getStateSubject().hide()

    fun clear() = getStateSubject().onComplete()

}