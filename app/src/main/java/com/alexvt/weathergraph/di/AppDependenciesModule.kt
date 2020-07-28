/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.di

import android.content.Context
import com.alexvt.weathergraph.android.App
import com.alexvt.weathergraph.android.repositories.*
import com.alexvt.weathergraph.repositories.*
import dagger.Module
import dagger.Provides
import dagger.android.AndroidInjectionModule
import javax.inject.Singleton

@Module(includes = [AndroidInjectionModule::class])
class AppDependenciesModule {
    @Provides
    @Singleton
    fun provideApplication(app: App): Context = app.applicationContext

    @Provides
    @Singleton
    fun provideLogRepository(): LogRepository = AndroidLogRepository()

    @Provides
    @Singleton
    fun provideThemeRepository(): ThemeRepository = AppCompatThemeRepository()

    @Provides
    @Singleton
    fun provideKnownLocationRepository(
        context: Context,
        log: LogRepository
    ): KnownLocationRepository = RoomKnownLocationRepository(context, log)

    @Provides
    @Singleton
    fun provideSettingsRepository(
        context: Context
    ): SettingsRepository = SharedPreferencesSettingsRepository(context)

    @Provides
    @Singleton
    fun provideWeatherWidgetLocalRepository(
        context: Context,
        log: LogRepository
    ): WeatherWidgetLocalRepository = RoomWeatherWidgetLocalRepository(context, log)

    @Provides
    @Singleton
    fun provideAquicnRemoteRepository(): AquicnRemoteRepository =
        RetrofitAquicnRemoteRepository()

    @Provides
    @Singleton
    fun provideOwmRemoteRepository(context: Context, log: LogRepository): OwmRemoteRepository =
        RetrofitOwmRemoteRepository(context, log)

    @Provides
    @Singleton
    fun provideEditedWeatherWidgetRepository(): EditedWeatherWidgetRepository =
        EditedWeatherWidgetRepository()

    @Provides
    @Singleton
    fun provideEditedWidgetBackgroundStateRepository(): EditedWidgetOnWallpaperRepository =
        EditedWidgetOnWallpaperRepository()

    @Provides
    @Singleton
    fun provideWeatherWidgetRepository(
        aquicnRemoteRepository: AquicnRemoteRepository,
        owmRemoteRepository: OwmRemoteRepository,
        localRepository: WeatherWidgetLocalRepository,
        log: LogRepository
    ): WeatherWidgetRepository = WeatherWidgetRepository(
        aquicnRemoteRepository,
        owmRemoteRepository,
        localRepository,
        log
    )

    @Provides
    @Singleton
    fun provideDrawTargetRepository(
        drawDataRepository: DrawDataRepository,
        context: Context
    ): DrawTargetRepository =
        ViewDrawTargetRepository(drawDataRepository, context)

    @Provides
    @Singleton
    fun provideDrawDataRepository(context: Context): DrawDataRepository =
        BitmapDrawDataRepository(context)

    @Provides
    @Singleton
    fun provideScheduledUpdateRepository(
        context: Context,
        log: LogRepository
    ): ScheduledUpdateRepository = JobScheduledUpdateRepository(context, log)

}