/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import com.alexvt.weathergraph.usecases.UserManageSettingsUseCase
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable

abstract class BaseViewModel : ViewModel() {

    protected abstract val userManageSettingsUseCaseFactory: UserManageSettingsUseCase.Factory

    fun getStyleIndex() = userManageSettingsUseCaseFactory.single.getStyleIndex()

    val styleChangeLiveData by lazy {
        userManageSettingsUseCaseFactory.single.observeStyleIndex().distinctUntilChanged()
            .toEventLiveData()
    }

    protected fun <T> LiveData<T>.setValue(value: T) =
        (this as MutableLiveData<T>).setValue(value)

    protected fun <T> LiveData<Event<T>>.setEvent(value: T) = setValue(Event(value))

    protected fun LiveData<Event<Unit>>.setEvent() = setEvent(Unit)

    protected fun <T> LiveData<T>.postValue(value: T) =
        (this as MutableLiveData<T>).postValue(value)

    protected fun <T> Observable<T>.toLiveData() =
        MutableLiveData<T>().also { liveData ->
            subscribe { liveData.postValue(it) }
                .also { compositeDisposable.add(it) }
        } as LiveData<T>

    protected fun <T> Observable<T>.toEventLiveData() = skip(1).map { Event(it) }.toLiveData()

    private val compositeDisposable = CompositeDisposable()

    override fun onCleared() {
        compositeDisposable.clear()
        super.onCleared()
    }
}

/**
 * Used as a wrapper for data that is exposed via a LiveData that represents an event.
 */
class Event<out T>(private val content: T) {
    private var hasBeenConsumed = false

    fun <R> consume(block: (T) -> R): R? = if (hasBeenConsumed) {
        null
    } else {
        hasBeenConsumed = true
        block(content)
    }
}

class EventObserver<T>(private val action: (T) -> Unit) : Observer<Event<T>> {
    override fun onChanged(t: Event<T>) {
        t.consume { action(it) }
    }
}