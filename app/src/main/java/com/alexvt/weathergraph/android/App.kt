/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.android

import android.app.Application
import android.content.res.Configuration
import com.alexvt.weathergraph.di.DaggerAppDependenciesComponent
import com.alexvt.weathergraph.usecases.OnEntryPointUseCase
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import javax.inject.Inject


class App : Application(), HasAndroidInjector {

    @Inject
    lateinit var dispatchingAndroidInjector: DispatchingAndroidInjector<Any>

    @Inject
    lateinit var onEntryPointUseCaseFactory: OnEntryPointUseCase.Factory

    override fun androidInjector(): AndroidInjector<Any> = dispatchingAndroidInjector

    override fun onCreate() {
        super.onCreate()

        DaggerAppDependenciesComponent.factory()
            .create(this)
            .inject(this)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        onEntryPointUseCaseFactory.createFor(this::class.simpleName + ".onConfigurationChanged")
            .rescheduleUpdates()
    }

}