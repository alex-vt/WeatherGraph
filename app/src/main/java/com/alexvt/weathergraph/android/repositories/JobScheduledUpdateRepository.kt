/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.android.repositories

import android.content.Context
import androidx.work.*
import com.alexvt.weathergraph.repositories.LogRepository
import com.alexvt.weathergraph.repositories.ScheduledUpdateRepository
import dagger.android.HasAndroidInjector
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class JobScheduledUpdateRepository @Inject constructor(
    private val context: Context,
    private val log: LogRepository
) : ScheduledUpdateRepository {

    class ScheduleTimeWorker(
        private val context: Context,
        workerParams: WorkerParameters
    ) : Worker(context, workerParams) {
        @Inject
        lateinit var scheduledUpdateRepository: ScheduledUpdateRepository

        override fun doWork(): Result {
            (context.applicationContext as HasAndroidInjector).androidInjector().inject(this)
            (scheduledUpdateRepository as JobScheduledUpdateRepository).postUpdateTime(id.toString())
            return Result.success()
        }
    }

    private val infiniteDelay = Long.MAX_VALUE / 2 // preventing overfills

    override fun scheduleJustNow() = schedule(infiniteDelay)

    override fun schedule(periodMillis: Long) {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            JobScheduledUpdateRepository::class.java.simpleName,
            ExistingPeriodicWorkPolicy.REPLACE,
            PeriodicWorkRequestBuilder<ScheduleTimeWorker>(periodMillis, TimeUnit.MILLISECONDS)
                .setInputData(Data.EMPTY)
                .setConstraints(Constraints.Builder().build())
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    PeriodicWorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()
        )
    }


    private val updateTimeSubject: BehaviorSubject<String> = BehaviorSubject.create()

    private fun postUpdateTime(id: String) {
        updateTimeSubject.onNext(id)
    }

    override fun getUpdateTimes() : Observable<String> = updateTimeSubject
}