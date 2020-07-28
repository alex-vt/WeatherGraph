/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.di

import androidx.lifecycle.ViewModelProvider
import com.alexvt.weathergraph.android.DataActivity
import com.alexvt.weathergraph.android.LegalInfoActivity
import com.alexvt.weathergraph.android.MainActivity
import com.alexvt.weathergraph.android.SettingsActivity
import com.alexvt.weathergraph.viewmodel.*
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import javax.inject.Provider

@Module
abstract class LegalInfoDependenciesModule {
    @ContributesAndroidInjector(modules = [ViewModelModule::class])
    abstract fun bindActivity(): LegalInfoActivity

    @Module
    class ViewModelModule {
        @Provides
        fun providesVmFactory(
            vm: Provider<LegalInfoViewModel>
        ): ViewModelProvider.Factory = ViewModelFactory(vm)
    }
}


