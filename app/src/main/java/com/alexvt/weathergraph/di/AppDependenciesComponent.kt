/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.alexvt.weathergraph.android.App
import dagger.*
import javax.inject.Singleton
import dagger.android.AndroidInjector
import javax.inject.Provider

@Singleton
@Component(
    modules = [
        AppDependenciesModule::class,
        WorkerDependenciesModule::class,
        OnAppUpdatedDependenciesModule::class,
        OnBootDependenciesModule::class,
        MainDependenciesModule::class,
        WidgetDetailsDependenciesModule::class,
        DataDependenciesModule::class,
        SettingsDependenciesModule::class,
        LegalInfoDependenciesModule::class,
        LicenseDependenciesModule::class,
        WeatherWidgetDependenciesModule::class
    ]
)
interface AppDependenciesComponent : AndroidInjector<App> {
    @Component.Factory
    abstract class Factory : AndroidInjector.Factory<App>
}

class ViewModelFactory(
    private val viewModel: Provider<out ViewModel>
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>) = viewModel.get() as T
}


