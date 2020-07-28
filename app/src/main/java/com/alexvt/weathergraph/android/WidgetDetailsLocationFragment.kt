/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.android

import android.Manifest
import android.app.SearchManager
import android.content.Context
import android.location.Location
import android.os.Bundle
import android.os.StrictMode
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.callbacks.onDismiss
import com.afollestad.materialdialogs.utils.MDUtil.inflate
import com.alexvt.weathergraph.R
import com.alexvt.weathergraph.android.util.SearchViewUtil
import com.alexvt.weathergraph.viewmodel.WidgetDetailsLocationViewModel
import com.google.android.gms.location.LocationRequest
import com.yayandroid.locationmanager.base.LocationBaseFragment
import com.yayandroid.locationmanager.configuration.DefaultProviderConfiguration
import com.yayandroid.locationmanager.configuration.GooglePlayServicesConfiguration
import com.yayandroid.locationmanager.configuration.LocationConfiguration
import com.yayandroid.locationmanager.configuration.PermissionConfiguration
import com.yayandroid.locationmanager.constants.FailType
import com.yayandroid.locationmanager.constants.ProviderType
import kotlinx.android.synthetic.main.fragment_widget_details_location.*
import kotlinx.android.synthetic.main.view_location_suggestion_item.view.*
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import java.io.File

class WidgetDetailsLocationFragment : LocationBaseFragment() { // todo use BaseFragment

    private val viewModel by lazy {
        (activity as SubViewModelProvider)[WidgetDetailsLocationViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        prepareMapSettings()
        return inflater.inflate(R.layout.fragment_widget_details_location, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel.loadByWidgetId((activity as WidgetDetailsActivity).editedWidgetDrawTargetId)

        bindLocationSearchField()
        bindUseDeviceLocationButton()
        bindDeviceLocationDialogs()
        bindLocationSearchResults()
        bindMap()
        bindMapScrolling()
        bindLocationKindSwitch()
    }

    private fun bindLocationKindSwitch() {
        mbSearch.setOnClickListener { viewModel.switchLocationToMap(false) }
        mbMap.setOnClickListener { viewModel.switchLocationToMap(true) }
        viewModel.locationSwitchedToMapLiveData.observe(viewLifecycleOwner, Observer { toMap ->
            mbSearch.isChecked = !toMap
            mbMap.isChecked = toMap
        })
        viewModel.searchVisibilityLiveData.observe(viewLifecycleOwner, Observer { isVisible ->
            rlSearch.visibility = if (isVisible) View.VISIBLE else View.GONE
        })
        viewModel.mapVisibilityLiveData.observe(viewLifecycleOwner, Observer { isVisible ->
            rlMap.visibility = if (isVisible) View.VISIBLE else View.GONE
        })
    }

    override fun onResume() {
        super.onResume()
        mvMap.onResume()
    }

    override fun onPause() {
        mvMap.onPause()
        super.onPause()
    }

    private fun prepareMapSettings() {
        val policy: StrictMode.ThreadPolicy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        val osmConf = Configuration.getInstance()
        val basePath = File(activity!!.cacheDir.absolutePath, "osmdroid")
        osmConf.userAgentValue = activity!!.packageName
        osmConf.osmdroidBasePath = basePath
        val tileCache = File(osmConf.osmdroidBasePath.absolutePath, "tile")
        osmConf.osmdroidTileCache = tileCache
        osmConf.save(activity, PreferenceManager.getDefaultSharedPreferences(activity))
    }

    private fun bindMapScrolling() {
        mvMap.setMultiTouchControls(true)
        mvMap.addMapListener(object : MapListener {
            override fun onScroll(event: ScrollEvent?) = updateMarkers().let { false }
            override fun onZoom(event: ZoomEvent?) = updateMarkers().let { false }

            private fun updateMarkers() = with(mvMap.boundingBox) {
                viewModel.updateMarkers(latNorth, latSouth, lonWest, lonEast)
            }
        })
    }

    private fun bindMap() {
        mvMap.setTileSource(TileSourceFactory.MAPNIK)
        mvMap.isTilesScaledToDpi = true
        mvMap.tilesScaleFactor = 0.8f
        mvMap.minZoomLevel = 3.0
        mvMap.maxZoomLevel = 12.0
        mvMap.controller.setZoom(5)
        viewModel.mapMarkersLiveData.observe(viewLifecycleOwner, Observer { markerData ->
            mvMap.overlays.removeAll(mvMap.overlays.map { it as Marker })
            mvMap.overlays.addAll(markerData.map { getMarker(it) })
            mvMap.invalidate()
        })
        viewModel.selectionMapMarkerLiveData.observe(viewLifecycleOwner, Observer {
            val point = GeoPoint(it.latitude, it.longitude)
            mvMap.controller.apply {
                if (it.canAnimateTo) animateTo(point) else setCenter(point)
            }
        })
    }

    private fun getMarker(item: WidgetDetailsLocationViewModel.MapMarkerItem) =
        Marker(mvMap).apply {
            position = GeoPoint(item.latitude, item.longitude)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = activity!!.getDrawable(
                if (item.selected) {
                    R.drawable.ic_location_on_black_32dp
                } else {
                    R.drawable.ic_add_location_black_32dp
                }
            )
            setOnMarkerClickListener { marker, _ ->
                viewModel.clickLocation(marker.position.latitude, marker.position.longitude)
                false
            }
            infoWindow = null
        }

    override fun getLocationConfiguration(): LocationConfiguration =
        LocationConfiguration.Builder()
            .keepTracking(false)
            .askForPermission(
                PermissionConfiguration.Builder()
                    .requiredPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
                    .build()
            )
            .useGooglePlayServices(
                GooglePlayServicesConfiguration.Builder()
                    .locationRequest(LocationRequest.create())
                    .fallbackToDefault(true)
                    .askForGooglePlayServices(false)
                    .askForSettingsApi(true)
                    .failOnConnectionSuspended(true)
                    .failOnSettingsApiSuspended(false)
                    .ignoreLastKnowLocation(false)
                    .setWaitPeriod(20 * 1000)
                    .build()
            )
            .useDefaultProviders(
                DefaultProviderConfiguration.Builder()
                    .requiredTimeInterval(5 * 60 * 1000)
                    .requiredDistanceInterval(0)
                    .acceptableAccuracy(5.0f)
                    .acceptableTimePeriod(5 * 60 * 1000)
                    .gpsMessage(getString(R.string.turn_gps_on))
                    .setWaitPeriod(ProviderType.GPS, 20 * 1000)
                    .setWaitPeriod(ProviderType.NETWORK, 20 * 1000)
                    .build()
            )
            .build()

    override fun onLocationChanged(location: Location?) =
        if (location != null) {
            viewModel.useDeviceLocation(location.latitude, location.longitude)
        } else {
            viewModel.deviceLocationFailed()
        }

    override fun onLocationFailed(type: Int) = when (type) {
        FailType.PERMISSION_DENIED -> getString(R.string.permission_denied)
        FailType.NETWORK_NOT_AVAILABLE -> getString(R.string.network_connection_issues)
        else -> ""
    }.let {
        viewModel.deviceLocationFailed(it)
    }

    private fun bindLocationSearchField() {
        (activity?.getSystemService(Context.SEARCH_SERVICE) as? SearchManager).let {
            svLocation.setSearchableInfo(it?.getSearchableInfo(activity?.componentName))
        }
        SearchViewUtil.fixMicIconBackground(svLocation, R.drawable.rounded_padded_clickable)
        svLocation.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String) = true

            override fun onQueryTextChange(newText: String) =
                viewModel.setLocationSearchText(newText).let { true } // todo search first
        })
    }

    private fun bindUseDeviceLocationButton() {
        mbUseMyLocationSearch.setOnClickListener { viewModel.clickUseDeviceLocation() }
        mbUseMyLocationSearchMap.setOnClickListener { viewModel.clickUseDeviceLocation() }
    }

    private val deviceLocationProgressDialog by lazy {
        MaterialDialog(activity!!)
            .title(R.string.getting_location)
            .positiveButton(R.string.cancel) {
                viewModel.onDeviceLocationCancel()
            }
            .onDismiss { viewModel.onDeviceLocationCancel() }
    }

    private val deviceLocationFailedDialog by lazy {
        MaterialDialog(activity!!)
            .title(R.string.couldnt_get_location)
            .negativeButton(R.string.retry) {
                viewModel.onDeviceLocationFailedRetryClick()
            }
            .positiveButton(R.string.cancel) {
                viewModel.onDeviceLocationCancel()
            }
            .onDismiss { viewModel.onDeviceLocationCancel() }
    }

    private fun bindDeviceLocationDialogs() {
        viewModel.locationProgressDialogLiveData.observe(viewLifecycleOwner, Observer { showing ->
            if (showing) {
                deviceLocationProgressDialog.show()
                getLocation()
            } else {
                deviceLocationProgressDialog.cancel()
            }
        })
        viewModel.locationFailedDialogLiveData.observe(viewLifecycleOwner, Observer { message ->
            if (message != null) {
                deviceLocationFailedDialog.message(text = message).show()
            } else {
                deviceLocationFailedDialog.cancel()
            }
        })
    }

    private fun bindLocationSearchResults() {
        rvLocationSuggestions.adapter = SuggestionRecyclerAdapter(
            clickListener = { locationId -> viewModel.clickSuggestion(locationId) }
        )
        viewModel.searchSuggestionsLiveData.observe(viewLifecycleOwner, Observer {
            (rvLocationSuggestions.adapter as SuggestionRecyclerAdapter).setItems(it)
        })
    }
}

private class SuggestionRecyclerAdapter(
    private val items: MutableList<WidgetDetailsLocationViewModel.SuggestionItem> = mutableListOf(),
    val clickListener: (Int) -> Unit = {}
) : RecyclerView.Adapter<SuggestionViewHolder>() {
    fun setItems(items: List<WidgetDetailsLocationViewModel.SuggestionItem>) = with(this.items) {
        clear()
        addAll(items)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        SuggestionViewHolder(parent.inflate(parent.context, R.layout.view_location_suggestion_item))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: SuggestionViewHolder, position: Int) =
        holder.bind(items[position], clickListener)

}

private class SuggestionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    fun bind(
        item: WidgetDetailsLocationViewModel.SuggestionItem,
        clickListener: (Int) -> Unit
    ) = with(itemView) {
        val (id, locationName, country) = item
        tvName.text = locationName
        tvCountry.text = country
        setOnClickListener { clickListener(id) }
    }
}