/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.android

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.alexvt.weathergraph.R
import com.alexvt.weathergraph.entities.CloudPercentUnit
import com.alexvt.weathergraph.entities.PrecipitationUnit
import com.alexvt.weathergraph.entities.TemperatureUnit
import com.alexvt.weathergraph.entities.WindSpeedUnit
import com.alexvt.weathergraph.viewmodel.EventObserver
import com.alexvt.weathergraph.viewmodel.WidgetDetailsDataViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.android.synthetic.main.fragment_widget_details_data.*
import kotlinx.android.synthetic.main.view_material_5_button_group.view.*
import kotlinx.android.synthetic.main.view_widget_details_data_item.view.*

class WidgetDetailsDataFragment : BaseFragment(R.layout.fragment_widget_details_data) {

    private val viewModel by lazy {
        viewModelProvider[WidgetDetailsDataViewModel::class.java]
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel.loadByWidgetId((activity as WidgetDetailsActivity).editedWidgetDrawTargetId)

        bindShowDays()
        bindUpdateEvery()
        bindTemperature()
        bindCloudPercent()
        bindPrecipitation()
        bindWindSpeed()
        bindAirQuality()
        bindSunriseSunsetTime()
        bindUseTime24h()
        bindDataSources()
    }

    private fun bindShowDays() = bindToggleDataOptionGroup(
        buttonGroup = vShowDaysButtons,
        optionList = viewModel.showDaysAheadOptions,
        optionIndexLiveData = viewModel.showDaysAheadIndexLiveData,
        optionIndexClickListener = { viewModel.setShowForIndex(it) }
    )

    private fun bindUpdateEvery() = bindToggleDataOptionGroup(
        buttonGroup = vUpdateTimeButtons,
        optionList = viewModel.updateTimeHourOptions,
        optionIndexLiveData = viewModel.updateEveryIndexLiveData,
        optionIndexClickListener = { viewModel.setUpdateEveryIndex(it) }
    )

    private fun bindTemperature() = bindNamedToggleDataOptionGroup(
        viewGroup = vTemperature,
        title = "Temperature",
        titleSwitchEnabled = false,
        unitList = viewModel.temperatureUnitOptions.map {
            when (it) {
                TemperatureUnit.K -> "K"
                TemperatureUnit.F -> "F"
                TemperatureUnit.C -> "C"
            }
        },
        unitIndexLiveData = viewModel.temperatureUnitIndexLiveData,
        unitIndexClickListener = { viewModel.setTemperatureUnitIndex(it) }
    )

    private fun bindCloudPercent() = bindNamedToggleDataOptionGroup(
        viewGroup = vCloudPercent,
        title = "Show Cloudiness",
        switchedOnLiveData = viewModel.cloudPercentEnabledLiveData,
        switchListener = { viewModel.setCloudPercentEnabled(it) },
        unitList = viewModel.cloudPercentUnitOptions.map {
            when (it) {
                CloudPercentUnit.PERCENT -> "%"
                CloudPercentUnit.NORMALIZED -> "0..1"
            }
        },
        unitIndexLiveData = viewModel.cloudPercentUnitIndexLiveData,
        unitIndexClickListener = { viewModel.setCloudPercentUnitIndex(it) }
    )

    private fun bindPrecipitation() = bindNamedToggleDataOptionGroup(
        viewGroup = vPrecipitation,
        title = "Show Precipitation",
        switchedOnLiveData = viewModel.precipitationEnabledLiveData,
        switchListener = { viewModel.setPrecipitationEnabled(it) },
        unitList = viewModel.precipitationUnitOptions.map {
            when (it) {
                PrecipitationUnit.MMH -> "mm/h"
                PrecipitationUnit.INH -> "in/h"
            }
        },
        cutoffOptionsLiveData = viewModel.precipitationCutoffOptionsLiveData,
        unitIndexLiveData = viewModel.precipitationUnitIndexLiveData,
        cutoffIndexLiveData = viewModel.precipitationCutoffIndexLiveData,
        unitIndexClickListener = { viewModel.setPrecipitationUnitIndex(it) },
        cutoffIndexClickListener = { viewModel.setPrecipitationCutoffIndex(it) }
    )

    private fun bindWindSpeed() = bindNamedToggleDataOptionGroup(
        viewGroup = vWindSpeed,
        title = "Show Wind Speed",
        switchedOnLiveData = viewModel.windSpeedEnabledLiveData,
        switchListener = { viewModel.setWindSpeedEnabled(it) },
        unitList = viewModel.windSpeedUnitOptions.map {
            when (it) {
                WindSpeedUnit.MS -> "m/s"
                WindSpeedUnit.FTS -> "ft/s"
                WindSpeedUnit.KMH -> "km/h"
                WindSpeedUnit.MPH -> "mph"
                WindSpeedUnit.KN -> "kn"
            }
        },
        cutoffOptionsLiveData = viewModel.windSpeedCutoffOptionsLiveData,
        unitIndexLiveData = viewModel.windSpeedUnitIndexLiveData,
        cutoffIndexLiveData = viewModel.windSpeedCutoffIndexLiveData,
        unitIndexClickListener = { viewModel.setWindSpeedUnitIndex(it) },
        cutoffIndexClickListener = { viewModel.setWindSpeedCutoffIndex(it) }
    )

    private fun bindAirQuality() = viewModel.airQualityEnabledLiveData.observe(
        viewLifecycleOwner,
        Observer { (checked, available) ->
            smAirQuality.setOnCheckedChangeListener { _, isChecked ->
                if (available) viewModel.setAirQualityEnabled(isChecked) else null
            }
            smAirQuality.isChecked = checked
            smAirQuality.isEnabled = available
            mtvAirQualityNotAvailable.visibility = if (available) View.GONE else View.VISIBLE
        })

    private fun bindSunriseSunsetTime() = bindToggleDataOption(
        switch = smSunriseSunset,
        switchedOnLiveData = viewModel.sunriseSunsetEnabledLiveData,
        switchListener = { viewModel.setSunriseSunsetEnabled(it) }
    )

    private fun bindUseTime24h() = bindToggleDataOption(
        switch = smTime24h,
        switchedOnLiveData = viewModel.time24hEnabledLiveData,
        switchListener = { viewModel.setTime24hEnabled(it) }
    )

    private fun bindDataSources() {
        mbDataSources.setOnClickListener { viewModel.openDataSources() }
        viewModel.dataSourcesNavigationLiveData.observe(viewLifecycleOwner, EventObserver {
            startActivity(DataActivity::class.java)
        })
    }


    private fun bindToggleDataOptionGroup(
        buttonGroup: View,
        optionList: List<String>,
        optionIndexLiveData: LiveData<Int>,
        optionIndexClickListener: (Int) -> Unit
    ) {
        fillToggleGroupWithOptions(buttonGroup, optionList)
        optionIndexLiveData.observe(viewLifecycleOwner, Observer { selectButton(buttonGroup, it) })
        setButtonClickListeners(buttonGroup, optionIndexClickListener)
    }

    private fun bindToggleDataOption(
        switch: SwitchMaterial,
        switchedOnLiveData: LiveData<Boolean>,
        switchListener: ((Boolean) -> Unit)
    ) {
        switchedOnLiveData.observe(viewLifecycleOwner, Observer { switch.isChecked = it })
        switch.setOnCheckedChangeListener { _, isChecked -> switchListener(isChecked) }
    }

    private fun bindNamedToggleDataOptionGroup(
        viewGroup: View,
        title: String,
        titleSwitchEnabled: Boolean = true,
        switchedOnLiveData: LiveData<Boolean>? = null,
        switchListener: ((Boolean) -> Unit)? = null,
        unitList: List<String>,
        cutoffOptionsLiveData: LiveData<List<String>>? = null,
        unitIndexLiveData: LiveData<Int>,
        cutoffIndexLiveData: LiveData<Int>? = null,
        unitIndexClickListener: (Int) -> Unit,
        cutoffIndexClickListener: ((Int) -> Unit)? = null
    ) {
        fillToggleWithText(viewGroup, title, titleSwitchEnabled)
        switchedOnLiveData?.observe(viewLifecycleOwner, Observer {
            viewGroup.smTitle.isChecked = it
            with(if (it) View.VISIBLE else View.GONE) { // todo animate
                viewGroup.vUnits.visibility = this
                viewGroup.vCutoff.post {
                    if (cutoffOptionsLiveData != null) {
                        viewGroup.vCutoff.visibility = this
                    }
                }
            }
        })
        switchListener?.let { listener ->
            viewGroup.smTitle.setOnCheckedChangeListener { _, isChecked -> listener(isChecked) }
        }
        fillToggleGroupWithOptions(viewGroup.vUnits, unitList)
        cutoffOptionsLiveData?.observe(viewLifecycleOwner, Observer {
            fillToggleGroupWithOptions(viewGroup.vCutoff, it)
        })
        unitIndexLiveData.observe(viewLifecycleOwner, Observer {
            selectButton(viewGroup.vUnits, it)
            viewGroup.vCutoff.mtvMaxValue.text = unitList[it]
        })
        cutoffIndexLiveData?.observe(viewLifecycleOwner, Observer {
            selectButton(viewGroup.vCutoff, it)
        })
        unitIndexClickListener.let { setButtonClickListeners(viewGroup.vUnits, it) }
        cutoffIndexClickListener?.let { setButtonClickListeners(viewGroup.vCutoff, it) }
    }

    private fun fillToggleGroupWithOptions(view: View, options: List<String>) = with(view) {
        listOf(vButton1, vButton2, vButton3, vButton4, vButton5).map { it as MaterialButton }
    }.let { buttons ->
        view.visibility = View.VISIBLE
        options.forEachIndexed { index, optionText ->
            buttons[index].apply {
                visibility = View.VISIBLE
                text = optionText
            }
        }
    }

    private fun selectButton(view: View, index: Int) = with(view) {
        listOf(vButton1, vButton2, vButton3, vButton4, vButton5).map { it as MaterialButton }
    }.let { buttons ->
        buttons[index].isChecked = true
    }

    private fun setButtonClickListeners(view: View, onClick: (Int) -> Unit) = with(view) {
        listOf(vButton1, vButton2, vButton3, vButton4, vButton5).map { it as MaterialButton }
    }.mapIndexed { index, button ->
        button.setOnClickListener { onClick(index) }
    }

    private fun fillToggleWithText(view: View, text: String, enable: Boolean = true) {
        view.smTitle.text = text
        view.smTitle.isEnabled = enable
    }
}