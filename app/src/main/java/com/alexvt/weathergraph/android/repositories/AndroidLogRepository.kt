/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.android.repositories

import androidx.core.text.isDigitsOnly
import com.alexvt.weathergraph.repositories.LogRepository
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

class AndroidLogRepository : LogRepository {

    init {
        Timber.plant(Timber.DebugTree())
        d("Logging (re)started\n\nNew log session at: ${getTimestampNow()}")
    }

    private fun getTimestampNow() =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH).format(System.currentTimeMillis())

    override fun v(message: String, throwable: Throwable?) {
        getClassNameTagTimber().v(throwable, message)
    }

    override fun d(message: String, throwable: Throwable?) {
        getClassNameTagTimber().d(throwable, message)
    }

    override fun e(message: String, throwable: Throwable?) {
        getClassNameTagTimber().e(throwable, message)
    }

    /**
     * Clean tag of the calling class from the current stack trace
     */
    private fun getClassNameTagTimber(
        stackTrace: Array<StackTraceElement> = Throwable().stackTrace,
        stackTraceIndex: Int = 3, // empiric value for the calling method
        fallbackTag: String = "NoTag-AndroidLogRepo"
    ) = stackTrace.let {
        if (stackTraceIndex < it.size) {
            with(it[stackTraceIndex]) {
                "($fileName:$lineNumber)${getSimpleClassName()}.$methodName"
            }
        } else {
            fallbackTag
        }
    }.let {
        Timber.tag(it)
    }

    private fun StackTraceElement.getSimpleClassName() = className.let {
        it.substring(it.lastIndexOf('$') + 1)
    }.let {
        it.substring(it.lastIndexOf('.') + 1)
    }.let {
        if (it.isDigitsOnly()) "(anonymous)" else it
    }.let {
        if (it == fileName.substringBefore(".")) "" else "#$it"
    }

}
