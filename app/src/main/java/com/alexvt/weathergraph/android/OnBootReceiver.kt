/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.alexvt.weathergraph.usecases.OnEntryPointUseCase
import dagger.android.AndroidInjection
import javax.inject.Inject

class OnBootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var onEntryPointUseCaseFactory: OnEntryPointUseCase.Factory
    private val onEntryPointUseCase by lazy {
        onEntryPointUseCaseFactory.createFor(this::class.simpleName!!)
    }

    override fun onReceive(context: Context, intent: Intent) {
        // todo secure
        AndroidInjection.inject(this, context)
        onEntryPointUseCase.rescheduleUpdates()
    }

}