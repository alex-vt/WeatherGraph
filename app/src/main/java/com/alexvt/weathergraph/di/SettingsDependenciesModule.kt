/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.di

import androidx.lifecycle.ViewModelProvider
import com.alexvt.weathergraph.android.SettingsActivity
import com.alexvt.weathergraph.viewmodel.SettingsViewModel
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import javax.inject.Provider

@Module
abstract class SettingsDependenciesModule {
    @ContributesAndroidInjector(modules = [ViewModelModule::class])
    abstract fun bindActivity(): SettingsActivity

    @Module
    class ViewModelModule {
        @Provides
        fun providesVmFactory(
            vm: Provider<SettingsViewModel>
        ): ViewModelProvider.Factory = ViewModelFactory(vm)
    }
}


