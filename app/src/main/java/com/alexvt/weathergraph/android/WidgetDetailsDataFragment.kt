/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.android

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.alexvt.weathergraph.R
import com.alexvt.weathergraph.databinding.FragmentWidgetDetailsDataBinding
import com.alexvt.weathergraph.databinding.ViewMaterial5ButtonGroupBinding
import com.alexvt.weathergraph.databinding.ViewWidgetDetailsDataItemBinding
import com.alexvt.weathergraph.entities.CloudPercentUnit
import com.alexvt.weathergraph.entities.PrecipitationUnit
import com.alexvt.weathergraph.entities.TemperatureUnit
import com.alexvt.weathergraph.entities.WindSpeedUnit
import com.alexvt.weathergraph.viewmodel.EventObserver
import com.alexvt.weathergraph.viewmodel.WidgetDetailsDataViewModel
import com.google.android.material.switchmaterial.SwitchMaterial

class WidgetDetailsDataFragment : BaseFragment(R.layout.fragment_widget_details_data) {

    private val viewModel by lazy {
        viewModelProvider[WidgetDetailsDataViewModel::class.java]
    }

    private var _binding: FragmentWidgetDetailsDataBinding? = null
    private val binding get() = _binding!! // see https://developer.android.com/topic/libraries/view-binding#fragments

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWidgetDetailsDataBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
        buttonsBinding = binding.vShowDaysButtons,
        optionList = viewModel.showDaysAheadOptions,
        optionIndexLiveData = viewModel.showDaysAheadIndexLiveData,
        optionIndexClickListener = { viewModel.setShowForIndex(it) }
    )

    private fun bindUpdateEvery() = bindToggleDataOptionGroup(
        buttonsBinding = binding.vUpdateTimeButtons,
        optionList = viewModel.updateTimeHourOptions,
        optionIndexLiveData = viewModel.updateEveryIndexLiveData,
        optionIndexClickListener = { viewModel.setUpdateEveryIndex(it) }
    )

    private fun bindTemperature() = bindNamedToggleDataOptionGroup(
        toggleBinding = binding.vTemperature,
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
        toggleBinding = binding.vCloudPercent,
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
        toggleBinding = binding.vPrecipitation,
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
        toggleBinding = binding.vWindSpeed,
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
            binding.smAirQuality.setOnCheckedChangeListener { _, isChecked ->
                if (available) viewModel.setAirQualityEnabled(isChecked) else null
            }
            binding.smAirQuality.isChecked = checked
            binding.smAirQuality.isEnabled = available
            binding.mtvAirQualityNotAvailable.visibility =
                if (available) View.GONE else View.VISIBLE
        })

    private fun bindSunriseSunsetTime() = bindToggleDataOption(
        switch = binding.smSunriseSunset,
        switchedOnLiveData = viewModel.sunriseSunsetEnabledLiveData,
        switchListener = { viewModel.setSunriseSunsetEnabled(it) }
    )

    private fun bindUseTime24h() = bindToggleDataOption(
        switch = binding.smTime24h,
        switchedOnLiveData = viewModel.time24hEnabledLiveData,
        switchListener = { viewModel.setTime24hEnabled(it) }
    )

    private fun bindDataSources() {
        binding.mbDataSources.setOnClickListener { viewModel.openDataSources() }
        viewModel.dataSourcesNavigationLiveData.observe(viewLifecycleOwner, EventObserver {
            startActivity(DataActivity::class.java)
        })
    }


    private fun bindToggleDataOptionGroup(
        buttonsBinding: ViewMaterial5ButtonGroupBinding,
        optionList: List<String>,
        optionIndexLiveData: LiveData<Int>,
        optionIndexClickListener: (Int) -> Unit
    ) {
        fillToggleGroupWithOptions(buttonsBinding, optionList)
        optionIndexLiveData.observe(
            viewLifecycleOwner,
            Observer { selectButton(buttonsBinding, it) })
        setButtonClickListeners(buttonsBinding, optionIndexClickListener)
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
        toggleBinding: ViewWidgetDetailsDataItemBinding,
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
        fillToggleWithText(toggleBinding, title, titleSwitchEnabled)
        switchedOnLiveData?.observe(viewLifecycleOwner, Observer {
            toggleBinding.smTitle.isChecked = it
            with(if (it) View.VISIBLE else View.GONE) { // todo animate
                toggleBinding.vUnits.visibility = this
                toggleBinding.vCutoff.post {
                    if (cutoffOptionsLiveData != null) {
                        toggleBinding.vCutoff.visibility = this
                    }
                }
            }
        })
        switchListener?.let { listener ->
            toggleBinding.smTitle.setOnCheckedChangeListener { _, isChecked -> listener(isChecked) }
        }
        fillToggleGroupWithOptions(toggleBinding.vUnitButtons, unitList)
        cutoffOptionsLiveData?.observe(viewLifecycleOwner, Observer {
            fillToggleGroupWithOptions(toggleBinding.vLimitButtons, it)
        })
        unitIndexLiveData.observe(viewLifecycleOwner, Observer {
            selectButton(toggleBinding.vUnitButtons, it)
            toggleBinding.mtvMaxValue.text = unitList[it]
        })
        cutoffIndexLiveData?.observe(viewLifecycleOwner, Observer {
            selectButton(toggleBinding.vLimitButtons, it)
        })
        unitIndexClickListener.let { setButtonClickListeners(toggleBinding.vUnitButtons, it) }
        cutoffIndexClickListener?.let { setButtonClickListeners(toggleBinding.vLimitButtons, it) }
    }

    private fun fillToggleGroupWithOptions(
        binding: ViewMaterial5ButtonGroupBinding,
        options: List<String>
    ) = with(view) {
        listOf(
            binding.vButton1,
            binding.vButton2,
            binding.vButton3,
            binding.vButton4,
            binding.vButton5
        ).map { it.mbButton }
    }.let { buttons ->
        binding.root.visibility = View.VISIBLE
        options.forEachIndexed { index, optionText ->
            buttons[index].apply {
                visibility = View.VISIBLE
                text = optionText
            }
        }
    }

    private fun selectButton(binding: ViewMaterial5ButtonGroupBinding, index: Int) = with(view) {
        listOf(
            binding.vButton1,
            binding.vButton2,
            binding.vButton3,
            binding.vButton4,
            binding.vButton5
        ).map { it.mbButton }
    }.let { buttons ->
        buttons[index].isChecked = true
    }

    private fun setButtonClickListeners(
        binding: ViewMaterial5ButtonGroupBinding,
        onClick: (Int) -> Unit
    ) = with(view) {
        listOf(
            binding.vButton1,
            binding.vButton2,
            binding.vButton3,
            binding.vButton4,
            binding.vButton5
        ).map { it.mbButton }
    }.mapIndexed { index, button ->
        button.setOnClickListener { onClick(index) }
    }

    private fun fillToggleWithText(
        binding: ViewWidgetDetailsDataItemBinding,
        text: String,
        enable: Boolean = true
    ) {
        binding.smTitle.text = text
        binding.smTitle.isEnabled = enable
    }
}