/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.alexvt.weathergraph.entities.OwmLocation
import com.alexvt.weathergraph.usecases.UserEditWidgetUseCase
import com.alexvt.weathergraph.usecases.UserManageSettingsUseCase
import com.alexvt.weathergraph.usecases.UserSearchLocationUseCase
import io.reactivex.subjects.BehaviorSubject
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class WidgetDetailsLocationViewModel @Inject constructor(
    private val userEditWidgetUseCaseFactory: UserEditWidgetUseCase.Factory,
    private val searchLocationUseCaseFactory: UserSearchLocationUseCase.Factory,
    override val userManageSettingsUseCaseFactory: UserManageSettingsUseCase.Factory
) : BaseViewModel() {

    data class SuggestionItem(val id: Int, val name: String, val country: String)
    data class MapMarkerItem(
        val id: Int,
        val latitude: Double,
        val longitude: Double,
        val selected: Boolean,
        val canAnimateTo: Boolean
    )

    private lateinit var userEditWidgetUseCase: UserEditWidgetUseCase

    val searchVisibilityLiveData: LiveData<Boolean> = MutableLiveData()
    val mapVisibilityLiveData: LiveData<Boolean> = MutableLiveData()
    val locationSwitchedToMapLiveData: LiveData<Boolean> = MutableLiveData()
    val locationProgressDialogLiveData: LiveData<Boolean> = MutableLiveData()
    val locationFailedDialogLiveData: LiveData<String?> = MutableLiveData()
    val searchSuggestionsLiveData: LiveData<List<SuggestionItem>> = MutableLiveData()
    val mapMarkersLiveData: LiveData<List<MapMarkerItem>> = MutableLiveData()
    val selectionMapMarkerLiveData: LiveData<MapMarkerItem> = MutableLiveData()

    fun loadByWidgetId(widgetId: Int) {
        if (!::userEditWidgetUseCase.isInitialized) {
            userEditWidgetUseCase = userEditWidgetUseCaseFactory.createFor(widgetId)
            setLocationSearchText("")
            locationSwitchedToMapLiveData.postValue(false)
            searchVisibilityLiveData.postValue(true)
            mapVisibilityLiveData.postValue(false)
            userEditWidgetUseCase.let {
                val id = it.getCurrentLocationId()
                val location = searchLocationUseCaseFactory.single.get(id)
                selectionMapMarkerLiveData.postValue(
                    MapMarkerItem(
                        id, location.latitude,
                        location.longitude,
                        selected = true,
                        canAnimateTo = false
                    )
                )
            }
        }
    }

    fun switchLocationToMap(toMap: Boolean) {
        if (locationSwitchedToMapLiveData.value != toMap) {
            locationSwitchedToMapLiveData.postValue(toMap)
            searchVisibilityLiveData.postValue(!toMap)
            mapVisibilityLiveData.postValue(toMap)
        }
    }

    private data class Bounds(
        val latitudeNorth: Double, val latitudeSouth: Double,
        val longitudeWest: Double, val longitudeEast: Double
    )

    private val markerUpdateSubject: BehaviorSubject<Bounds> = BehaviorSubject.create()

    private val mapMarkerUpdateDelayMillis = 500L

    private val markerUpdateDisposable = markerUpdateSubject
        .throttleLatest(mapMarkerUpdateDelayMillis, TimeUnit.MILLISECONDS)
        .subscribe { updateMarkersNow(it) }

    // todo always include selected marker
    fun updateMarkers(
        latitudeNorth: Double, latitudeSouth: Double, longitudeWest: Double, longitudeEast: Double
    ) = Bounds(latitudeNorth, latitudeSouth, longitudeWest, longitudeEast).let {
        markerUpdateSubject.onNext(it)
    }

    private fun updateMarkersNow(bounds: Bounds) =
        searchLocationUseCaseFactory.single.getLocationSuggestionsInBounds(
            bounds.latitudeNorth, bounds.latitudeSouth, bounds.longitudeWest, bounds.longitudeEast,
            userEditWidgetUseCase.getCurrentLocationId()
        ).map {
            MapMarkerItem(
                it.id, it.latitude, it.longitude,
                selected = userEditWidgetUseCase.hasLocationId(it.id),
                canAnimateTo = true
            )
        }.let {
            mapMarkersLiveData.postValue(it)
        }

    fun clickUseDeviceLocation() {
        locationProgressDialogLiveData.postValue(true)
    }

    fun useDeviceLocation(latitude: Double, longitude: Double) {
        if (locationProgressDialogLiveData.value == false) return // already canceled / no request
        locationProgressDialogLiveData.postValue(false)
        searchLocationUseCaseFactory.single
            .getNearestLocationSuggestions(latitude, longitude).let { setSuggestions(it, true) }
    }

    fun deviceLocationFailed(message: String = "") {
        locationProgressDialogLiveData.postValue(false)
        locationFailedDialogLiveData.postValue(message)
    }

    fun onDeviceLocationFailedRetryClick() {
        locationProgressDialogLiveData.postValue(true)
        locationFailedDialogLiveData.postValue(null)
    }

    fun onDeviceLocationCancel() {
        locationProgressDialogLiveData.postValue(false)
        locationFailedDialogLiveData.postValue(null)
    }

    fun setLocationSearchText(location: String) =
        searchLocationUseCaseFactory.single.getSuggestions(location).let { setSuggestions(it) }

    private fun setSuggestions(suggestions: List<OwmLocation>, chooseFirst: Boolean = false) {
        searchSuggestionsLiveData.postValue(suggestions.map {
            SuggestionItem(it.id, it.name, it.country)
        })
        if (chooseFirst && suggestions.isNotEmpty()) {
            clickSuggestion(suggestions.first().id)
        }
    }

    fun clickLocation(latitude: Double, longitude: Double) =
        searchLocationUseCaseFactory.single.getNearestLocationSuggestions(latitude, longitude)
            .first()
            .let { clickSuggestion(it.id) }

    fun clickSuggestion(locationId: Int) {
        val location = searchLocationUseCaseFactory.single.get(locationId)
        userEditWidgetUseCase.updateWithLocationName(
            location.name, locationId, location.latitude, location.longitude
        )
        markerUpdateSubject.value?.let { updateMarkersNow(it) }
        selectionMapMarkerLiveData.postValue(
            MapMarkerItem(
                locationId,
                location.latitude,
                location.longitude,
                selected = true,
                canAnimateTo = true
            )
        )
    }

    override fun onCleared() {
        markerUpdateDisposable.dispose()
        super.onCleared()
    }
}