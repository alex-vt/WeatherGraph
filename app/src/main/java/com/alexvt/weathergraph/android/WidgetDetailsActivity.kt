/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.android

import android.app.Activity
import android.app.SearchManager
import android.app.WallpaperManager
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.AttributeSet
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import androidx.constraintlayout.widget.Guideline
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.lifecycle.ViewModelProvider
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.callbacks.onDismiss
import com.alexvt.weathergraph.R
import com.alexvt.weathergraph.android.util.MenuUtil.setVisibleIcons
import com.alexvt.weathergraph.databinding.ActivityWidgetDetailsBinding
import com.alexvt.weathergraph.viewmodel.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.textview.MaterialTextView
import dagger.android.AndroidInjection
import javax.inject.Inject


class WidgetDetailsActivity : BaseAppCompatActivity(R.layout.activity_widget_details),
    SubViewModelProvider {

    @Inject
    lateinit var vmFactory: ViewModelProvider.Factory

    override val viewModel by lazy {
        ViewModelProvider(this, vmFactory)[WidgetDetailsViewModel::class.java]
    }

    private lateinit var binding: ActivityWidgetDetailsBinding

    override fun provideSubViewModels() = listOf(
        viewModel.welcomeViewModel,
        viewModel.locationViewModel, viewModel.dataViewModel, viewModel.appearanceViewModel
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)

        binding = ActivityWidgetDetailsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        bindWelcome()
        bindItem()
        bindDetails()
        bindMenu()
        bindEditButton()
        bindBottomSheetBehavior()
        bindBottomNavigationIcons()
        bindBottomNavigation()
        bindSaveButton()
        bindDeleteMenu()
        bindBackNavigation()
    }

    override fun onNewIntent(intent: Intent) = super.onNewIntent(intent).also {
        takeIf { Intent.ACTION_SEARCH == intent.action }
            ?.intent?.getStringExtra(SearchManager.QUERY)
            ?.let { query -> viewModel.setLocationSearchText(query) }
    }

    private fun bindWelcome() = supportFragmentManager.beginTransaction()
        .add(WelcomeFragment(), "Welcome").commit()

    private fun bindItem() {
        binding.ivWidget.post {
            viewModel.setWidgetSize(
                Pair(
                    binding.ivWidget.width,
                    binding.ivWidget.height
                )
            )
        }
        viewModel.loadById(editedWidgetDrawTargetId)
        viewModel.widgetLiveData.observe { (_, locationName, imageData) ->
            binding.tvCurrentLocation.text = getString(R.string.location, locationName)
            title = locationName
            binding.ivWidget.setImageBitmap(
                BitmapFactory.decodeByteArray(
                    imageData,
                    0,
                    imageData.size
                )
            )
        }
        viewModel.isOnWallpaperLiveData.observe { isOnWallpaper ->
            binding.ivWidgetGrayBackground.visibility =
                if (isOnWallpaper) View.GONE else View.VISIBLE
            binding.ivWholeWallpaperBackground.visibility =
                if (isOnWallpaper) View.VISIBLE else View.GONE
            binding.ivWholeWallpaperBackground.setImageDrawable(
                if (isOnWallpaper) WallpaperManager.getInstance(this).drawable else null
            )
            binding.clEditable.setBackgroundColor(
                if (isOnWallpaper) Color.TRANSPARENT else getColor(R.color.colorGray)
            )
        }
    }

    private fun bindDetails() {
        viewModel.dayWeatherLiveData.observe { items ->
            binding.cvWeatherDetails.visibility = View.VISIBLE
            items.forEachIndexed { index, item ->
                with(dayTemperatureViews[index]) {
                    binding.mtvToday.visibility = if (item.isToday) View.VISIBLE else View.GONE
                    mtvDate.text = getString(R.string.week_date, item.weekDayText, item.dateText)
                    sTemperatures.values = listOf(
                        item.minTempNormalized.toFloat(),
                        item.nowTempNormalized?.toFloat(),
                        item.maxTempNormalized.toFloat()
                    ).filterIsInstance<Float>()
                    setTempTextAtPosition(
                        clTempLabels, mtvTempMin, mtvTempMinLabel, guidelineTempMin,
                        item.minTempText, item.minTempNormalized
                    )
                    setTempTextAtPosition(
                        clTempLabels, mtvTempMax, mtvTempMaxLabel, guidelineTempMax,
                        item.maxTempText, item.maxTempNormalized
                    )
                    setTempTextAtPosition(
                        clTempLabels, mtvTempNow, mtvTempNowLabel, guidelineTempNow,
                        item.nowTempText, item.nowTempNormalized
                    )
                }
            }
        }
        viewModel.sunriseSunsetLiveData.observe { (sunriseTime, sunsetTime) ->
            binding.mtvSunrise.text = getString(R.string.sunrise, sunriseTime)
            binding.mtvSunset.text = getString(R.string.sunset, sunsetTime)
            binding.cvSunriseSunset.visibility = View.VISIBLE
        }
    }

    private fun setTempTextAtPosition(
        container: View,
        tempTextView: MaterialTextView,
        labelTextView: MaterialTextView,
        guideline: Guideline,
        text: String?,
        normalizedPosition: Double?
    ) {
        val containerWidth = container.width.toDouble()
        val barSideMargin = resources.getDimensionPixelSize(R.dimen.temp_range_bar_side_margin)
            .toDouble()
        val barWidth = containerWidth - 2 * barSideMargin
        val barOffset = barWidth * (normalizedPosition ?: 0.0)
        val containerOffset = barSideMargin + barOffset
        val containerPercent = containerOffset / containerWidth
        guideline.setGuidelinePercent(containerPercent.toFloat())
        tempTextView.text = text
        labelTextView.visibility = if (text.isNullOrBlank()) View.GONE else View.VISIBLE
    }

    private val dayTemperatureViews by lazy {
        listOf(
            binding.vDay1,
            binding.vDay2,
            binding.vDay3,
            binding.vDay4,
            binding.vDay5
        )
    }

    private fun bindEditButton() =
        binding.fabEdit.setOnClickListener { viewModel.expandEditor(true) }

    private fun bindBottomNavigationIcons() =
        ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_pressed),
                intArrayOf(android.R.attr.state_checked),
                intArrayOf()
            ), intArrayOf(
                getColor(R.color.colorGray),
                getColor(R.color.colorBackgroundPrimary),
                getColor(R.color.colorBackgroundTranslucent)
            )
        ).let {
            binding.bnView.itemTextColor = it
            binding.bnView.itemIconTintList = it
        }

    private val menuToFragments by lazy {
        listOf(
            R.id.action_location to WidgetDetailsLocationFragment(),
            R.id.action_data to WidgetDetailsDataFragment(),
            R.id.action_appearance to WidgetDetailsAppearanceFragment()
        )
    }

    private fun bindBottomSheetBehavior() {
        editorBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {}

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    viewModel.expandEditor(true)
                } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    viewModel.expandEditor(false)
                }
            }
        })
        viewModel.bottomSheetSwipingAllowedLiveData.observe { swipeAllowed ->
            editorBehavior.swipeEnabled = swipeAllowed
            binding.vPullUpTab.visibility = if (swipeAllowed) View.VISIBLE else View.INVISIBLE
        }
        viewModel.editorExpandedLiveData.observe { expand ->
            editorBehavior.state = if (expand) {
                BottomSheetBehavior.STATE_EXPANDED
            } else {
                BottomSheetBehavior.STATE_COLLAPSED
            }
        }
    }

    private fun bindBottomNavigation() {
        binding.bnView.setOnNavigationItemSelectedListener { menuItem ->
            viewModel.selectTab(
                menuToFragments.indexOfFirst { (itemId, _) ->
                    itemId == menuItem.itemId
                }
            )
            true
        }
        viewModel.tabIndexLiveData.observe { tabIndex ->
            val (itemId, fragment) = menuToFragments[tabIndex]
            supportFragmentManager.beginTransaction()
                .replace(R.id.flSettingSection, fragment)
                .commit()
            binding.bnView.selectedItemId = itemId
        }
    }

    private val editorBehavior by lazy {
        val layoutParams = binding.rlBottomSheet.layoutParams as CoordinatorLayout.LayoutParams
        layoutParams.behavior as LockableBottomSheetBehavior
    }

    private fun bindSaveButton() =
        binding.fabSave.setOnClickListener { viewModel.clickSaveButton() }

    private fun bindDeleteMenu() {
        viewModel.removeDialogLiveData.observe { allowDialog ->
            if (allowDialog == null) return@observe
            MaterialDialog(this).show {
                title(R.string.remove_widget)
                message(
                    listOf(
                        R.string.remove_widget_at_home_screen,
                        R.string.widget_will_be_removed,
                        R.string.new_widget_will_be_discarded
                    )[viewModel.getRemoveDetailedMessageIndex()]
                )
                negativeButton(R.string.cancel)
                positiveButton(
                    listOf(
                        R.string.remove_anyway,
                        R.string.remove
                    )[viewModel.getRemovePositiveTextIndex()]
                ) {
                    viewModel.clickRemovePositiveButton()
                }
                onDismiss { viewModel.dismissDialogs() }
            }
        }
    }

    private fun bindBackNavigation() {
        viewModel.backNavigationLiveData.observeEvent { isClosedOk ->
            if (isClosedOk) setResult(Activity.RESULT_OK)
            finish()
        }
        viewModel.discardDialogLiveData.observe { allowDialog ->
            if (allowDialog == null) return@observe
            MaterialDialog(this).show {
                title(R.string.discard_changes)
                message(R.string.unsaved_changes_lost)
                negativeButton(R.string.cancel)
                positiveButton(R.string.yes_discard) {
                    viewModel.clickDiscardPositiveButton()
                }
                onDismiss { viewModel.dismissDialogs() }
            }
        }
    }

    override fun onSupportNavigateUp() = viewModel.clickTopBackButton().let { true }

    override fun onBackPressed() = viewModel.clickBack()

    override fun onCreateOptionsMenu(menu: Menu?) =
        menuInflater.inflate(R.menu.widget_details_menu, menu).let {
            menu?.setVisibleIcons(this, theme, binding.root)
            true
        }

    private fun bindMenu() {
        viewModel.settingsNavigationLiveData.observeEvent {
            startActivity(SettingsActivity::class.java)
        }
        viewModel.legalInfoNavigationLiveData.observeEvent {
            startActivity(LegalInfoActivity::class.java)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.iDelete -> viewModel.clickRemoveButton()
            // todo share R.id.iShare -> { }
            R.id.iSettings -> viewModel.clickSettings()
            R.id.iInfo -> viewModel.clickLegalInfo()
        }
        return super.onOptionsItemSelected(item)
    }

    val editedWidgetDrawTargetId by lazy {
        intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
    }
}

@Suppress("unused")
class LockableBottomSheetBehavior<V : View> : BottomSheetBehavior<V> {
    constructor() : super()
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    var swipeEnabled = true

    override fun onInterceptTouchEvent(parent: CoordinatorLayout, child: V, event: MotionEvent) =
        if (swipeEnabled) {
            super.onInterceptTouchEvent(parent, child, event)
        } else {
            false
        }

    override fun onTouchEvent(parent: CoordinatorLayout, child: V, event: MotionEvent) =
        if (swipeEnabled) {
            super.onTouchEvent(parent, child, event)
        } else {
            false
        }

    override fun onStartNestedScroll(
        layout: CoordinatorLayout, child: V, directTargetChild: View, target: View, axes: Int,
        type: Int
    ) = if (swipeEnabled) {
        super.onStartNestedScroll(layout, child, directTargetChild, target, axes, type)
    } else {
        false
    }

    override fun onNestedPreScroll(
        layout: CoordinatorLayout, child: V, target: View, dx: Int, dy: Int, consumed: IntArray,
        type: Int
    ) {
        if (swipeEnabled) {
            super.onNestedPreScroll(layout, child, target, dx, dy, consumed, type)
        }
    }

    override fun onStopNestedScroll(layout: CoordinatorLayout, child: V, target: View, type: Int) {
        if (swipeEnabled) {
            super.onStopNestedScroll(layout, child, target, type)
        }
    }

    override fun onNestedPreFling(
        layout: CoordinatorLayout, child: V, target: View, velocityX: Float, velocityY: Float
    ) = if (swipeEnabled) {
        super.onNestedPreFling(layout, child, target, velocityX, velocityY)
    } else {
        false
    }
}