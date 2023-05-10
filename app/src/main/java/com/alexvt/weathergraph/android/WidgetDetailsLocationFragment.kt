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
import com.alexvt.weathergraph.R
import com.alexvt.weathergraph.android.util.SearchViewUtil
import com.alexvt.weathergraph.databinding.FragmentWidgetDetailsLocationBinding
import com.alexvt.weathergraph.databinding.ViewLocationSuggestionItemBinding
import com.alexvt.weathergraph.viewmodel.WidgetDetailsLocationViewModel
import com.google.android.gms.location.LocationRequest
import com.yayandroid.locationmanager.base.LocationBaseFragment
import com.yayandroid.locationmanager.configuration.DefaultProviderConfiguration
import com.yayandroid.locationmanager.configuration.GooglePlayServicesConfiguration
import com.yayandroid.locationmanager.configuration.LocationConfiguration
import com.yayandroid.locationmanager.configuration.PermissionConfiguration
import com.yayandroid.locationmanager.constants.FailType
import com.yayandroid.locationmanager.constants.ProviderType
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

    private var _binding: FragmentWidgetDetailsLocationBinding? = null
    private val binding get() = _binding!! // see https://developer.android.com/topic/libraries/view-binding#fragments

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWidgetDetailsLocationBinding.inflate(inflater, container, false)
        prepareMapSettings()
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
        binding.mbSearch.setOnClickListener { viewModel.switchLocationToMap(false) }
        binding.mbMap.setOnClickListener { viewModel.switchLocationToMap(true) }
        viewModel.locationSwitchedToMapLiveData.observe(viewLifecycleOwner, Observer { toMap ->
            binding.mbSearch.isChecked = !toMap
            binding.mbMap.isChecked = toMap
        })
        viewModel.searchVisibilityLiveData.observe(viewLifecycleOwner, Observer { isVisible ->
            binding.rlSearch.visibility = if (isVisible) View.VISIBLE else View.GONE
        })
        viewModel.mapVisibilityLiveData.observe(viewLifecycleOwner, Observer { isVisible ->
            binding.rlMap.visibility = if (isVisible) View.VISIBLE else View.GONE
        })
    }

    override fun onResume() {
        super.onResume()
        binding.mvMap.onResume()
    }

    override fun onPause() {
        binding.mvMap.onPause()
        super.onPause()
    }

    private fun prepareMapSettings() {
        val policy: StrictMode.ThreadPolicy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        val osmConf = Configuration.getInstance()
        val basePath = File(requireActivity().cacheDir.absolutePath, "osmdroid")
        osmConf.userAgentValue = requireActivity().packageName
        osmConf.osmdroidBasePath = basePath
        val tileCache = File(osmConf.osmdroidBasePath.absolutePath, "tile")
        osmConf.osmdroidTileCache = tileCache
        osmConf.save(activity, PreferenceManager.getDefaultSharedPreferences(activity))
    }

    private fun bindMapScrolling() {
        binding.mvMap.setMultiTouchControls(true)
        binding.mvMap.addMapListener(object : MapListener {
            override fun onScroll(event: ScrollEvent?) = updateMarkers().let { false }
            override fun onZoom(event: ZoomEvent?) = updateMarkers().let { false }

            private fun updateMarkers() = with(binding.mvMap.boundingBox) {
                viewModel.updateMarkers(latNorth, latSouth, lonWest, lonEast)
            }
        })
    }

    private fun bindMap() {
        binding.mvMap.setTileSource(TileSourceFactory.MAPNIK)
        binding.mvMap.isTilesScaledToDpi = true
        binding.mvMap.tilesScaleFactor = 0.8f
        binding.mvMap.minZoomLevel = 3.0
        binding.mvMap.maxZoomLevel = 12.0
        binding.mvMap.controller.setZoom(5)
        viewModel.mapMarkersLiveData.observe(viewLifecycleOwner, Observer { markerData ->
            binding.mvMap.overlays.removeAll(binding.mvMap.overlays.map { it as Marker })
            binding.mvMap.overlays.addAll(markerData.map { getMarker(it) })
            binding.mvMap.invalidate()
        })
        viewModel.selectionMapMarkerLiveData.observe(viewLifecycleOwner, Observer {
            val point = GeoPoint(it.latitude, it.longitude)
            binding.mvMap.controller.apply {
                if (it.canAnimateTo) animateTo(point) else setCenter(point)
            }
        })
    }

    private fun getMarker(item: WidgetDetailsLocationViewModel.MapMarkerItem) =
        Marker(binding.mvMap).apply {
            position = GeoPoint(item.latitude, item.longitude)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = requireActivity().getDrawable(
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
            binding.svLocation.setSearchableInfo(it?.getSearchableInfo(activity?.componentName))
        }
        SearchViewUtil.fixMicIconBackground(binding.svLocation, R.drawable.rounded_padded_clickable)
        binding.svLocation.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String) = true

            override fun onQueryTextChange(newText: String) =
                viewModel.setLocationSearchText(newText).let { true } // todo search first
        })
    }

    private fun bindUseDeviceLocationButton() {
        binding.mbUseMyLocationSearch.setOnClickListener { viewModel.clickUseDeviceLocation() }
        binding.mbUseMyLocationSearchMap.setOnClickListener { viewModel.clickUseDeviceLocation() }
    }

    private val deviceLocationProgressDialog by lazy {
        MaterialDialog(requireActivity())
            .title(R.string.getting_location)
            .positiveButton(R.string.cancel) {
                viewModel.onDeviceLocationCancel()
            }
            .onDismiss { viewModel.onDeviceLocationCancel() }
    }

    private val deviceLocationFailedDialog by lazy {
        MaterialDialog(requireActivity())
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
        binding.rvLocationSuggestions.adapter = SuggestionRecyclerAdapter(
            clickListener = { locationId -> viewModel.clickSuggestion(locationId) }
        )
        viewModel.searchSuggestionsLiveData.observe(viewLifecycleOwner, Observer {
            (binding.rvLocationSuggestions.adapter as SuggestionRecyclerAdapter).setItems(it)
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
        SuggestionViewHolder(
            ViewLocationSuggestionItemBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: SuggestionViewHolder, position: Int) =
        holder.bind(items[position], clickListener)

}

private class SuggestionViewHolder(private val binding: ViewLocationSuggestionItemBinding) :
    RecyclerView.ViewHolder(binding.root) {
    fun bind(
        item: WidgetDetailsLocationViewModel.SuggestionItem,
        clickListener: (Int) -> Unit
    ) = with(binding) {
        val (id, locationName, country) = item
        tvName.text = locationName
        tvCountry.text = country
        root.setOnClickListener { clickListener(id) }
    }
}