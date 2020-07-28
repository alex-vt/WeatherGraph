/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.di

import com.alexvt.weathergraph.android.OnAppUpdatedReceiver
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class OnAppUpdatedDependenciesModule {
    @ContributesAndroidInjector
    abstract fun bindOnAppUpdatedReceiver(): OnAppUpdatedReceiver
}


