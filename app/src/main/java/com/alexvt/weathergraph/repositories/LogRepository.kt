/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.repositories

interface LogRepository {

    fun v(message: String, throwable: Throwable? = null)

    fun d(message: String, throwable: Throwable? = null)

    fun e(message: String, throwable: Throwable? = null)

}