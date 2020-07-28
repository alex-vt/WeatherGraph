/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.alexvt.weathergraph.usecases.UserEditWidgetUseCase
import com.alexvt.weathergraph.usecases.UserManageSettingsUseCase
import com.alexvt.weathergraph.usecases.UserSearchLocationUseCase
import com.alexvt.weathergraph.usecases.UserViewWeatherDetailsUseCase
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Function4
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import javax.inject.Inject

class WidgetDetailsViewModel @Inject constructor(
    private val userEditWidgetUseCaseFactory: UserEditWidgetUseCase.Factory,
    private val searchLocationUseCaseFactory: UserSearchLocationUseCase.Factory,
    private val userViewWeatherDetailsUseCaseFactory: UserViewWeatherDetailsUseCase.Factory,
    val welcomeViewModel: WelcomeViewModel,
    val locationViewModel: WidgetDetailsLocationViewModel,
    val dataViewModel: WidgetDetailsDataViewModel,
    val appearanceViewModel: WidgetDetailsAppearanceViewModel,
    override val userManageSettingsUseCaseFactory: UserManageSettingsUseCase.Factory
) : BaseViewModel() {
    data class WidgetItem(val widgetId: Int, val locationName: String, val imageData: ByteArray)

    data class DayWeatherItem(
        val isToday: Boolean,
        val dateText: String,
        val weekDayText: String,
        val maxTempText: String?,
        val minTempText: String?,
        val nowTempText: String?,
        val minTempNormalized: Double,
        val maxTempNormalized: Double,
        val nowTempNormalized: Double?
    )

    private lateinit var widgetDisposable: Disposable

    val widgetLiveData: LiveData<WidgetItem> = MutableLiveData()
    val backNavigationLiveData: LiveData<Event<Boolean>> = MutableLiveData()
    val bottomSheetSwipingAllowedLiveData: LiveData<Boolean> = MutableLiveData()
    val tabIndexLiveData by lazy { selectedTabIndexSubject.toLiveData() }
    val editorExpandedLiveData by lazy { editorUpSubject.toLiveData() }
    val removeDialogLiveData: LiveData<Unit?> = MutableLiveData()
    val discardDialogLiveData: LiveData<Unit?> = MutableLiveData()
    val settingsNavigationLiveData: LiveData<Event<Unit>> = MutableLiveData()
    val legalInfoNavigationLiveData: LiveData<Event<Unit>> = MutableLiveData()

    private val userViewWeatherDetailsUseCase by lazy {
        userViewWeatherDetailsUseCaseFactory.create()
    }
    val dayWeatherLiveData = userViewWeatherDetailsUseCase.dayWeatherItemsObservable.map { list ->
        list.map {
            DayWeatherItem(
                isToday = it.isToday,
                dateText = it.dateText,
                weekDayText = it.weekDayText,
                nowTempText = it.nowTempText,
                minTempText = it.minTempText,
                maxTempText = it.maxTempText,
                nowTempNormalized = it.nowTempNormalized,
                minTempNormalized = it.minTempNormalized,
                maxTempNormalized = it.maxTempNormalized
            )
        }
    }.toLiveData()

    val sunriseSunsetLiveData = userViewWeatherDetailsUseCase.sunriseSunsetObservable.toLiveData()

    fun setWidgetSize(targetSizePx: Pair<Int, Int>) = run {
        if (::widgetDisposable.isInitialized) {
            widgetDisposable.dispose()
        }
        widgetDisposable = userEditWidgetUseCase.observe(targetSizePx)
            .subscribeOn(Schedulers.newThread())
            .subscribe { (widget, imageData) ->
                widgetLiveData.postValue(
                    WidgetItem(widget.widgetId, widget.dataSource.locationName, imageData)
                )
            }
    }

    private lateinit var userEditWidgetUseCase: UserEditWidgetUseCase

    private val selectedTabIndexSubject = BehaviorSubject.create<Int>()
    private val editorUpSubject = BehaviorSubject.create<Boolean>()

    // todo move logic to use case
    fun setLocationSearchText(location: String) =
        searchLocationUseCaseFactory.single.getSuggestions(location).takeIf { it.isNotEmpty() }
            ?.first()?.apply {
                userEditWidgetUseCase.updateWithLocationName(name, id, latitude, longitude)
            }

    val isOnWallpaperLiveData: LiveData<Boolean> by lazy {
        MutableLiveData<Boolean>().also { liveData ->
            Observable.combineLatest<Boolean, Boolean, Int, Boolean, Boolean>(
                userEditWidgetUseCase.appearancePreviewOnWallpaperObservable,
                userEditWidgetUseCase.showWallpaperSettingsObservable,
                selectedTabIndexSubject,
                editorUpSubject,
                Function4 { previewWallpaper, showOnWallpaper, tabIndex, editorUp ->
                    if (tabIndex == lastTabIndex && editorUp) previewWallpaper else showOnWallpaper
                }

            ).subscribe { liveData.postValue(it) }
        }
    }

    fun loadById(id: Int) {
        if (!::userEditWidgetUseCase.isInitialized) {
            userEditWidgetUseCase = userEditWidgetUseCaseFactory.createFor(id)
            bottomSheetSwipingAllowedLiveData.setValue(userEditWidgetUseCase.existsAsSaved())
            selectedTabIndexSubject.onNext(firstTabIndex)
            editorUpSubject.onNext(!userEditWidgetUseCase.existsAsSaved())
        }
    }

    private val firstTabIndex = 0
    private val lastTabIndex = 2

    fun selectTab(tabIndex: Int) {
        if (selectedTabIndexSubject.value != tabIndex) {
            selectedTabIndexSubject.onNext(tabIndex)
        }
    }

    fun expandEditor(expand: Boolean) {
        editorUpSubject.onNext(expand)
        bottomSheetSwipingAllowedLiveData.setValue(userEditWidgetUseCase.existsAsSaved() && !expand)
    }

    fun clickSaveButton() =
        if (userEditWidgetUseCase.existsAsSaved()) {
            editorUpSubject.onNext(false)
            bottomSheetSwipingAllowedLiveData.setValue(userEditWidgetUseCase.existsAsSaved())
        } else {
            backNavigationLiveData.setEvent(true)
        }.also {
            userEditWidgetUseCase.saveChanges()
        }

    fun clickRemoveButton() = removeDialogLiveData.setValue(Unit)

    fun clickRemovePositiveButton() = userEditWidgetUseCase.remove()
        .also { backNavigationLiveData.setEvent(false) }

    fun dismissDialogs() {
        removeDialogLiveData.setValue(null)
        discardDialogLiveData.setValue(null)
    }

    fun clickTopBackButton() = safelyDiscardChanges()

    private fun canCollapseOnBack() =
        editorUpSubject.value == true && userEditWidgetUseCase.existsAsSaved()

    fun clickBack() = if (canCollapseOnBack()) {
        editorUpSubject.onNext(false)
    } else {
        safelyDiscardChanges()
    }

    private fun safelyDiscardChanges() {
        if (userEditWidgetUseCase.hasUnsavedChanges()) {
            discardDialogLiveData.setValue(Unit)
            expandEditor(true) // showing Save button there
        } else {
            discardChangesNow()
        }
    }

    fun clickDiscardPositiveButton() = discardChangesNow()

    private fun discardChangesNow() = userEditWidgetUseCase.discardChanges()
        .also { backNavigationLiveData.setEvent(false) }

    fun getRemoveDetailedMessageIndex() = userEditWidgetUseCase.getRemoveDetailedMessageIndex()

    fun getRemovePositiveTextIndex() = userEditWidgetUseCase.getRemovePositiveTextIndex()

    fun clickSettings() = settingsNavigationLiveData.setEvent()

    fun clickLegalInfo() = legalInfoNavigationLiveData.setEvent()

    override fun onCleared() {
        if (::widgetDisposable.isInitialized) {
            widgetDisposable.dispose()
        }
        selectedTabIndexSubject.onComplete()
        editorUpSubject.onComplete()
        super.onCleared()
    }
}
