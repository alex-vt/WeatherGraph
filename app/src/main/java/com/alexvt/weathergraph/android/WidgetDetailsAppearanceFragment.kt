/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.android

import android.app.Activity
import android.app.WallpaperManager
import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.assent.Permission
import com.afollestad.assent.askForPermissions
import com.afollestad.assent.rationale.createDialogRationale
import com.afollestad.materialdialogs.utils.MDUtil.inflate
import com.alexvt.weathergraph.R
import com.alexvt.weathergraph.viewmodel.WidgetDetailsAppearanceViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.Slider
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.android.synthetic.main.fragment_widget_details_appearance.*
import kotlinx.android.synthetic.main.view_material_5_button_group.view.*
import kotlinx.android.synthetic.main.view_palette_and_size.view.*
import kotlinx.android.synthetic.main.view_palette_option_item.view.*
import kotlin.math.roundToInt

class WidgetDetailsAppearanceFragment : BaseFragment(R.layout.fragment_widget_details_appearance) {

    private val viewModel by lazy {
        viewModelProvider[WidgetDetailsAppearanceViewModel::class.java]
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel.loadByWidgetId((activity as WidgetDetailsActivity).editedWidgetDrawTargetId)

        bindShowLocationName()
        bindShowLastUpdateTime()
        bindShowUnits()
        bindBackground()
        bindGrid()
        bindText()
        bindTemperature()
        bindCloudPercent()
        bindPrecipitation()
        bindWindSpeed()
        bindAirQuality()
        bindSunriseSunset()
        bindMargins()

        bindShowWallpaper()
    }


    private fun bindShowLocationName() = bindSwitchSection(
        switch = smLocationName,
        optionLiveData = viewModel.showLocationNameLiveData,
        switchListener = { viewModel.setShowLocationNameEnabled(it) }
    )

    private fun bindShowLastUpdateTime() = bindSwitchSection(
        switch = smLastUpdateTime,
        optionLiveData = viewModel.showLastUpdateTimeLiveData,
        switchListener = { viewModel.setShowLastUpdateTimeEnabled(it) }
    )

    private fun bindShowUnits() = bindSwitchSection(
        switch = smUnits,
        optionLiveData = viewModel.showUnitsLiveData,
        switchListener = { viewModel.setShowUnitsEnabled(it) }
    )

    private fun bindBackground() = bindColorAndSizeSection(
        sectionView = vBackground,
        enabledLiveData = viewModel.enabledLiveData,
        title = "Background color",
        palettesLiveData = viewModel.getBackgroundColorLiveData(),
        paletteIndexListener = { viewModel.setBackgroundColorIndex(it) }
    )

    private fun bindGrid() = bindColorAndSizeSection(
        sectionView = vGrid,
        enabledLiveData = viewModel.enabledLiveData,
        title = "Grid lines color",
        palettesLiveData = viewModel.getGridColorLiveData(),
        paletteIndexListener = { viewModel.setGridColorIndex(it) },
        sizeTitle = "Thickness",
        sizeOptions = viewModel.gridThicknessPxOptions,
        sizeIndexLiveData = viewModel.gridThicknessIndexLiveData,
        sizeIndexListener = { viewModel.setGridThicknessIndex(it) }
    )

    private fun bindText() = bindColorAndSizeSection(
        sectionView = vText,
        enabledLiveData = viewModel.enabledLiveData,
        title = "Text color",
        palettesLiveData = viewModel.getTextColorLiveData(),
        paletteIndexListener = { viewModel.setTextColorIndex(it) },
        sizeTitle = "Size",
        sizeOptions = viewModel.textSizePxOptions,
        sizeIndexLiveData = viewModel.textSizeIndexLiveData,
        sizeIndexListener = { viewModel.setTextSizeIndex(it) }
    )

    private fun bindTemperature() = bindColorAndSizeSection(
        sectionView = vTemperature,
        enabledLiveData = viewModel.enabledLiveData,
        title = "Temperature graph palette",
        palettesLiveData = viewModel.getTemperaturePaletteLiveData("low", "high"),
        bigHeight = true,
        paletteIndexListener = { viewModel.setTemperaturePaletteIndex(it) },
        sizeTitle = "Thickness",
        sizeOptions = viewModel.graphThicknessPxOptions,
        sizeIndexLiveData = viewModel.temperatureThicknessIndexLiveData,
        sizeIndexListener = { viewModel.setTemperatureThicknessIndex(it) }
    )

    private fun bindCloudPercent() = bindColorAndSizeSection(
        sectionView = vCloudPercent,
        title = "Cloudiness visual palette",
        enabledLiveData = viewModel.cloudPercentEnabledLiveData,
        palettesLiveData = viewModel.getCloudPaletteLiveData("clear", "cloudy"),
        bigHeight = true,
        paletteIndexListener = { viewModel.setCloudPaletteIndex(it) }
    )

    private fun bindPrecipitation() = bindColorAndSizeSection(
        sectionView = vPrecipitation,
        title = "Precipitation bars palette",
        enabledLiveData = viewModel.precipitationEnabledLiveData,
        palettesLiveData = viewModel.getPrecipitationPaletteLiveData("none", "high"),
        bigHeight = true,
        paletteIndexListener = { viewModel.setPrecipitationPaletteIndex(it) }
    )

    private fun bindWindSpeed() = bindColorAndSizeSection(
        sectionView = vWindSpeed,
        title = "Wind speed graph palette",
        enabledLiveData = viewModel.windSpeedEnabledLiveData,
        palettesLiveData = viewModel.getWindSpeedPaletteLiveData("none", "high"),
        bigHeight = true,
        paletteIndexListener = { viewModel.setWindSpeedPaletteIndex(it) },
        sizeTitle = "Thickness",
        sizeOptions = viewModel.graphThicknessPxOptions,
        sizeIndexLiveData = viewModel.windSpeedThicknessIndexLiveData,
        sizeIndexListener = { viewModel.setWindSpeedThicknessIndex(it) }
    )

    private fun bindAirQuality() = bindColorAndSizeSection(
        sectionView = vAirQuality,
        title = "Air quality indicator colors",
        enabledLiveData = viewModel.airQualityEnabledLiveData,
        palettesLiveData = viewModel.getAirQualityPaletteLiveData("clean", "polluted"),
        bigHeight = true,
        paletteIndexListener = { viewModel.setAirQualityPaletteIndex(it) }
    )

    private fun bindSunriseSunset() = bindColorAndSizeSection(
        sectionView = vDayNight,
        title = "Day & night time indicator colors",
        enabledLiveData = viewModel.sunriseSunsetEnabledLiveData,
        palettesLiveData = viewModel.getSunriseSunsetPaletteLiveData("night", "day"),
        bigHeight = true,
        paletteIndexListener = { viewModel.setSunriseSunsetPaletteIndex(it) }
    )

    private fun bindMargins() {
        bindSliderSection(
            slider = sMarginTop,
            maxValue = viewModel.marginMax,
            valueLiveData = viewModel.marginTopLiveData,
            slideListener = { viewModel.setMarginTop(it) }
        )
        bindSliderSection(
            slider = sMarginLeft,
            maxValue = viewModel.marginMax,
            valueLiveData = viewModel.marginLeftLiveData,
            slideListener = { viewModel.setMarginLeft(it) }
        )
        bindSliderSection(
            slider = sMarginBottom,
            maxValue = viewModel.marginMax,
            valueLiveData = viewModel.marginBottomLiveData,
            slideListener = { viewModel.setMarginBottom(it) }
        )
        bindSliderSection(
            slider = sMarginRight,
            maxValue = viewModel.marginMax,
            valueLiveData = viewModel.marginRightLiveData,
            slideListener = { viewModel.setMarginRight(it) }
        )
    }


    private fun bindShowWallpaper() {
        smPreviewWallpaper.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                getReadStoragePermission { isAllowed ->
                    viewModel.setAppearancePreviewWallpaper(isAllowed)
                }
            } else {
                viewModel.setAppearancePreviewWallpaper(false)
            }
        }
        viewModel.wallpaperShownLiveData.observe(viewLifecycleOwner, Observer {
            smPreviewWallpaper.isChecked = it
        })
    }

    private fun getReadStoragePermission(resultListener: (Boolean) -> Unit) {
        val permission = Permission.READ_EXTERNAL_STORAGE
        activity?.askForPermissions(
            permission,
            rationaleHandler = createDialogRationale(R.string.storage_read_permission) {
                onPermission(permission, R.string.wallpaper_needs_read)
            }) {
            resultListener(it.isAllGranted(permission))
        }
    }


    private fun bindSliderSection(
        slider: Slider,
        maxValue: Int,
        valueLiveData: LiveData<Int>,
        slideListener: (Int) -> Unit
    ) {
        slider.valueFrom = 0f
        slider.valueTo = maxValue.toFloat()
        valueLiveData.observe(viewLifecycleOwner, Observer { slider.value = it.toFloat() })
        slider.addOnChangeListener { _, value, _ -> slideListener(value.roundToInt()) }
    }

    private fun bindSwitchSection(
        switch: SwitchMaterial,
        optionLiveData: LiveData<Boolean>,
        switchListener: (Boolean) -> Unit
    ) {
        optionLiveData.observe(viewLifecycleOwner, Observer { switch.isChecked = it })
        switch.setOnCheckedChangeListener { _, isChecked -> switchListener(isChecked) }
    }

    private fun bindColorAndSizeSection(
        sectionView: View,
        enabledLiveData: LiveData<Boolean>,
        title: String,
        palettesLiveData: LiveData<List<WidgetDetailsAppearanceViewModel.Palette>>,
        paletteIndexListener: (Int) -> Unit,
        bigHeight: Boolean = false,
        sizeTitle: String? = null,
        sizeOptions: List<String> = emptyList(),
        sizeIndexLiveData: LiveData<Int>? = null,
        sizeIndexListener: ((Int) -> Unit)? = null
    ) {
        enabledLiveData.observe(viewLifecycleOwner, Observer { enabled ->
            sectionView.visibility = if (enabled) View.VISIBLE else View.GONE
        })
        sectionView.tvTitle.text = title
        sectionView.rvPaletteList.layoutParams.height =
            (if (bigHeight) R.dimen.palette_size_big else R.dimen.palette_size_base).let {
                resources.getDimension(it).roundToInt()
            }
        sectionView.rvPaletteList.adapter =
            PaletteRecyclerAdapter(activity, paletteIndexListener)
        palettesLiveData.observe(viewLifecycleOwner, Observer {
            (sectionView.rvPaletteList.adapter as PaletteRecyclerAdapter).setItems(it)
        })
        sectionView.llSize.visibility = if (sizeTitle != null) View.VISIBLE else View.GONE
        sectionView.tvSizeName.text = sizeTitle

        sectionView.getSizeButtons().take(sizeOptions.size).mapIndexed { index, button ->
            button.apply {
                visibility = View.VISIBLE
                text = sizeOptions[index]
            }
            button.setOnClickListener {
                sizeIndexListener?.invoke(index)
            }
        }
        sizeIndexLiveData?.observe(viewLifecycleOwner, Observer { index ->
            sectionView.getSizeButtons()[index].isChecked = true
        })
    }

    private fun View.getSizeButtons() = with(this.vSizeButtons) {
        listOf(this.vButton1, this.vButton2, this.vButton3, this.vButton4, this.vButton5)
            .map { it as MaterialButton }
    }
}

private class PaletteRecyclerAdapter(
    val activity: Activity?, val clickListener: (Int) -> Unit = {}
) : RecyclerView.Adapter<PaletteViewHolder>() {
    private val items = mutableListOf<WidgetDetailsAppearanceViewModel.Palette>()

    fun setItems(items: List<WidgetDetailsAppearanceViewModel.Palette>) = with(this.items) {
        clear()
        addAll(items)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        PaletteViewHolder(parent.inflate(parent.context, R.layout.view_palette_option_item))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: PaletteViewHolder, position: Int) =
        holder.bind(items[position], activity, clickListener)

}

private class PaletteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    fun bind(
        item: WidgetDetailsAppearanceViewModel.Palette, activity: Activity?,
        clickListener: (Int) -> Unit
    ) = with(itemView) {
        val (index, isSelected, textLow, textHigh, imageData, backgroundColor, textColor,
            onWallpaper) = item
        ivPalette.setImageBitmap(BitmapFactory.decodeByteArray(imageData, 0, imageData.size))
        tvLow.visibility = if (textLow.isNotBlank()) View.VISIBLE else View.GONE
        tvLow.text = textLow
        tvLow.setTextColor(textColor)
        tvHigh.visibility = if (textHigh.isNotBlank()) View.VISIBLE else View.GONE
        tvHigh.text = textHigh
        tvHigh.setTextColor(textColor)
        mbSelection.isChecked = isSelected
        if (onWallpaper) {
            WallpaperManager.getInstance(activity).drawable
        } else {
            activity?.getDrawable(R.color.colorGray)
        }?.let { ivBackground.setBackgroundDrawable(it) } // todo
        mbSelection.backgroundTintList = ColorStateList.valueOf(backgroundColor)
        mbSelection.setOnClickListener {
            mbSelection.isChecked = true
            clickListener(index)
        }
    }
}
