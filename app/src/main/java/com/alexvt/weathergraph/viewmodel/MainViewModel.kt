/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.alexvt.weathergraph.usecases.OnEntryPointUseCase
import com.alexvt.weathergraph.usecases.UserManageSettingsUseCase
import com.alexvt.weathergraph.usecases.UserObserveWidgetsUseCase
import com.alexvt.weathergraph.usecases.UserRemoveWidgetUseCase
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import javax.inject.Inject

class MainViewModel @Inject constructor(
    onEntryPointUseCaseFactory: OnEntryPointUseCase.Factory,
    private val removeWidgetUseCaseFactory: UserRemoveWidgetUseCase.Factory,
    private val userObserveWidgetsUseCaseFactory: UserObserveWidgetsUseCase.Factory,
    val quickSettings: QuickSettingsViewModel,
    val welcomeViewModel: WelcomeViewModel,
    override val userManageSettingsUseCaseFactory: UserManageSettingsUseCase.Factory
) : BaseViewModel() {

    private lateinit var widgetsDisposable: Disposable
    private val userObserveWidgetsUseCase: UserObserveWidgetsUseCase

    data class WidgetItem(
        val widgetId: Int, val imageData: ByteArray, val showWallpaper: Boolean
    )

    val closeNavigationLiveData: LiveData<Event<Unit>> = MutableLiveData()
    val widgetsLiveData: LiveData<List<WidgetItem>> = MutableLiveData()
    val addOrEditNavigationLiveData: LiveData<Event<Int>> = MutableLiveData()
    val addDialogLiveData: LiveData<Unit?> = MutableLiveData()
    val removeDialogLiveData: LiveData<Int?> = MutableLiveData()
    val optionsExpandedLiveData: LiveData<Boolean> = MutableLiveData()
    val settingsNavigationLiveData: LiveData<Event<Unit>> = MutableLiveData()
    val legalInfoNavigationLiveData: LiveData<Event<Unit>> = MutableLiveData()

    init {
        onEntryPointUseCaseFactory.createFor(this::class.simpleName!!).rescheduleUpdates()
        userObserveWidgetsUseCase = userObserveWidgetsUseCaseFactory.create()
    }

    fun setWidgetSize(targetSizePx: Pair<Int, Int>) = run {
        if (::widgetsDisposable.isInitialized) {
            widgetsDisposable.dispose()
        }
        widgetsDisposable =
            Observable.combineLatest<List<UserObserveWidgetsUseCase.WidgetVisualData>, Boolean, List<WidgetItem>>(
                userObserveWidgetsUseCase.observeAll(targetSizePx),
                userManageSettingsUseCaseFactory.single.observeShowWallpaper(),
                BiFunction { widgetVisualDataList, showWallpaper ->
                    widgetVisualDataList.map { (widgetId, imageData) ->
                        WidgetItem(widgetId, imageData, showWallpaper)
                    }
                }
            ).subscribe { widgetsLiveData.postValue(it) }
    }

    fun clickAddButton() =
        addDialogLiveData.setValue(Unit)

    fun clickAddConfirmButton() = addOrEditNavigationLiveData.setEvent(0) // todo reason for 0

    fun dismissDialogs() {
        addDialogLiveData.setValue(null)
        removeDialogLiveData.setValue(null)
    }

    fun clickWidget(id: Int) = addOrEditNavigationLiveData.setEvent(id)

    fun clickSettings() = settingsNavigationLiveData.setEvent()

    fun clickLegalInfo() = legalInfoNavigationLiveData.setEvent()

    fun longClickWidget(id: Int) = removeDialogLiveData.setValue(id)

    fun clickRemovePositiveButton(id: Int) =
        removeWidgetUseCaseFactory.createFor(id).remove() // todo one instance for each

    fun getRemoveDetailedMessageIndex(id: Int) =
        removeWidgetUseCaseFactory.createFor(id).getRemoveDetailedMessageIndex()

    fun getRemovePositiveTextIndex(id: Int) =
        removeWidgetUseCaseFactory.createFor(id).getRemovePositiveTextIndex()

    fun expandOptions(expand: Boolean) = optionsExpandedLiveData.setValue(expand)

    fun clickOptions() = expandOptions(optionsExpandedLiveData.value != true)

    private fun canCollapseOnBack() = optionsExpandedLiveData.value == true

    fun clickBack() = if (canCollapseOnBack()) {
        expandOptions(false)
    } else {
        closeNavigationLiveData.setEvent()
    }

    override fun onCleared() {
        if (::widgetsDisposable.isInitialized) {
            widgetsDisposable.dispose()
        }
        super.onCleared()
    }
}