/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.android

import android.app.WallpaperManager
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.assent.Permission
import com.afollestad.assent.askForPermissions
import com.afollestad.assent.rationale.createDialogRationale
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.callbacks.onDismiss
import com.alexvt.weathergraph.R
import com.alexvt.weathergraph.android.util.MenuUtil.setVisibleIcons
import com.alexvt.weathergraph.databinding.ActivityMainBinding
import com.alexvt.weathergraph.databinding.ViewGraphWidgetItemBinding
import com.alexvt.weathergraph.databinding.ViewMaterial5ButtonGroupBinding
import com.alexvt.weathergraph.entities.AppTheme
import com.alexvt.weathergraph.entities.SortingMethod
import com.alexvt.weathergraph.viewmodel.EventObserver
import com.alexvt.weathergraph.viewmodel.MainViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dagger.android.AndroidInjection
import javax.inject.Inject
import kotlin.math.roundToInt

class MainActivity : BaseAppCompatActivity(R.layout.activity_main, canGoBack = false),
    SubViewModelProvider {

    @Inject
    lateinit var vmFactory: ViewModelProvider.Factory

    override val viewModel by lazy {
        ViewModelProvider(this, vmFactory)[MainViewModel::class.java]
    }

    override fun provideSubViewModels() = listOf(
        viewModel.welcomeViewModel
    )

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        bindWelcome()
        bindBackground()
        bindMenu()
        bindList()
        bindAddOrEdit()
        bindDelete()

        bindBottomSheetBehavior()
        bindCloseNavigation()

        bindSortingMethod()
        bindSortingDirection()
        bindTheme()
        bindStyle()
        bindShowWallpaper()
    }

    private fun bindWelcome() = supportFragmentManager.beginTransaction()
        .add(WelcomeFragment(), "Welcome").commit()

    private fun getScreenWidth() = DisplayMetrics().apply {
        windowManager.defaultDisplay.getMetrics(this)
    }.widthPixels

    private fun Int.resourceToPx() = resources.getDimension(this).roundToInt()

    private fun bindBackground() {
        viewModel.quickSettings.showWallpaperLiveData.observe(this, Observer { showWallpaper ->
            binding.ivBackground.setImageDrawable(
                if (showWallpaper) WallpaperManager.getInstance(this).drawable else null
            )
        })
    }

    private fun bindList() {
        viewModel.setWidgetSize(
            Pair(
                getScreenWidth() - 2 * R.dimen.widget_card_margin.resourceToPx(),
                R.dimen.view_widget_height.resourceToPx()
            )
        )
        binding.rvWidgetList.adapter = WidgetRecyclerAdapter(
            clickListener = { widgetId -> viewModel.clickWidget(widgetId) },
            longClickListener = { widgetId -> viewModel.longClickWidget(widgetId) }
        ).also { adapter ->
            viewModel.widgetsLiveData.observe(this, Observer { adapter.setItems(it) })
        }
    }

    private fun bindAddOrEdit() {
        binding.fabAdd.setOnClickListener { viewModel.clickAddButton() }
        viewModel.addDialogLiveData.observe(this, Observer { allowDialog ->
            if (allowDialog == null) return@Observer
            MaterialDialog(this).show {
                title(R.string.add_new_widget)
                message(R.string.add_new_widget_at_home)
                negativeButton(R.string.cancel)
                positiveButton(R.string.add_here_anyway) {
                    viewModel.clickAddConfirmButton()
                }
                onDismiss { viewModel.dismissDialogs() }
            }
        })
        viewModel.addOrEditNavigationLiveData.observe(this, EventObserver { widgetId ->
            Intent(this@MainActivity, WidgetDetailsActivity::class.java)
                .apply { putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId) }
                .let { startActivity(it) }
        })
    }

    private fun bindDelete() {
        viewModel.removeDialogLiveData.observe(this, Observer { widgetId ->
            if (widgetId == null) return@Observer
            MaterialDialog(this).show {
                title(R.string.remove_widget)
                message(
                    listOf(
                        R.string.remove_widget_at_home_screen,
                        R.string.widget_will_be_removed,
                        R.string.new_widget_will_be_discarded
                    )[viewModel.getRemoveDetailedMessageIndex(widgetId)]
                )
                negativeButton(R.string.cancel)
                positiveButton(
                    listOf(
                        R.string.remove_anyway,
                        R.string.remove
                    )[viewModel.getRemovePositiveTextIndex(widgetId)]
                ) {
                    viewModel.clickRemovePositiveButton(widgetId)
                }
                onDismiss { viewModel.dismissDialogs() }
            }
        })
    }

    private fun bindBottomSheetBehavior() {
        optionsBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {}

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    viewModel.expandOptions(true)
                } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    viewModel.expandOptions(false)
                }
            }
        })
        viewModel.optionsExpandedLiveData.observe(this, Observer { isCommandExpand ->
            binding.mbOptions.setImageResource(
                if (isCommandExpand) {
                    R.drawable.ic_keyboard_arrow_down_black_24dp
                } else {
                    R.drawable.ic_keyboard_arrow_up_black_24dp
                }
            )
            optionsBehavior.state = if (isCommandExpand) {
                BottomSheetBehavior.STATE_EXPANDED
            } else {
                BottomSheetBehavior.STATE_COLLAPSED
            }
        })
        binding.mbOptions.setOnClickListener { viewModel.clickOptions() }
    }

    private val optionsBehavior by lazy {
        val layoutParams = binding.llBottomSheet.layoutParams as CoordinatorLayout.LayoutParams
        layoutParams.behavior as BottomSheetBehavior
    }

    private fun bindSortingMethod() {
        val sortingMethods = viewModel.quickSettings.sortingMethods
        val nameMap = sortingMethods.map {
            it to when (it) {
                SortingMethod.NAME -> "Name"
                SortingMethod.LONGITUDE -> "Longitude"
                SortingMethod.LATITUDE -> "Latitude"
            }
        }
        binding.vSortingMethod.getOptionButtons().take(sortingMethods.size)
            .mapIndexed { index, button ->
                button.apply {
                    visibility = View.VISIBLE
                    text = nameMap[index].second
                }
                button.setOnClickListener { viewModel.quickSettings.clickSortingMethod(index) }
            }
        viewModel.quickSettings.sortingMethodIndexLiveData.observe(this, Observer { index ->
            binding.vSortingMethod.getOptionButtons()[index].isChecked = true
        })
    }

    private fun ViewMaterial5ButtonGroupBinding.getOptionButtons() =
        listOf(this.vButton1, this.vButton2, this.vButton3, this.vButton4, this.vButton5)
            .map { it.mbButton }

    private fun bindSortingDirection() {
        binding.mbAscending.setOnClickListener { viewModel.quickSettings.setSortingAscending(true) }
        binding.mbDescending.setOnClickListener { viewModel.quickSettings.setSortingAscending(false) }
        viewModel.quickSettings.sortingAscendingLiveData.observe(this, Observer { ascending ->
            binding.mbAscending.isChecked = ascending
            binding.mbDescending.isChecked = !ascending
        })
    }

    private fun bindTheme() {
        val themes = viewModel.quickSettings.themes
        val nameMap = themes.map {
            it to when (it) {
                AppTheme.AUTO -> "Auto"
                AppTheme.DARK -> "Dark"
                AppTheme.LIGHT -> "Light"
            }
        }
        binding.vTheme.getOptionButtons().take(themes.size).mapIndexed { index, button ->
            button.apply {
                visibility = View.VISIBLE
                text = nameMap[index].second
            }
            button.setOnClickListener { viewModel.quickSettings.clickTheme(index) }
        }
        viewModel.quickSettings.themeIndexLiveData.observe(this, Observer { index ->
            binding.vTheme.getOptionButtons()[index].isChecked = true
        })
    }

    private fun bindStyle() {
        // todo palettes of varying size
        binding.vStyle.getOptionButtons().mapIndexed { index, button ->
            button.apply {
                visibility = View.VISIBLE
                text = ""
                icon = getDrawable(R.drawable.ic_palette_black_24dp)
                setIconTintResource(getTheme(styles[index])
                    .getColorRes(androidx.appcompat.R.attr.colorPrimary))
            }
            button.setOnClickListener { viewModel.quickSettings.clickStyle(index) }
        }
        viewModel.quickSettings.styleIndexLiveData.observe(this, Observer { index ->
            binding.vStyle.getOptionButtons()[index].isChecked = true
        })
    }

    private fun getTheme(resId: Int) = ContextThemeWrapper(baseContext, resId).theme

    private fun Resources.Theme.getColorRes(attr: Int) = TypedValue().apply {
        this@getColorRes.resolveAttribute(attr, this, true)
    }.resourceId

    private fun bindShowWallpaper() {
        binding.smShowWallpaper.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                getReadStoragePermission { isAllowed ->
                    viewModel.quickSettings.setShowWallpaper(isAllowed)
                }
            } else {
                viewModel.quickSettings.setShowWallpaper(false)
            }
        }
        viewModel.quickSettings.showWallpaperLiveData.observe(this, Observer {
            binding.smShowWallpaper.isChecked = it
        })
    }

    private fun getReadStoragePermission(resultListener: (Boolean) -> Unit) {
        val permission = Permission.READ_EXTERNAL_STORAGE
        askForPermissions(
            permission,
            rationaleHandler = createDialogRationale(R.string.storage_read_permission) {
                onPermission(permission, R.string.wallpaper_needs_read)
            }) {
            resultListener(it.isAllGranted(permission))
        }
    }

    override fun onBackPressed() = viewModel.clickBack()

    private fun bindCloseNavigation() {
        viewModel.closeNavigationLiveData.observe(this, EventObserver { finish() })
    }

    override fun onCreateOptionsMenu(menu: Menu?) =
        menuInflater.inflate(R.menu.main_menu, menu).let {
            menu?.setVisibleIcons(this, theme, binding.root)
            true
        }

    private fun bindMenu() {
        viewModel.settingsNavigationLiveData.observe(this, EventObserver {
            startActivity(SettingsActivity::class.java)
        })
        viewModel.legalInfoNavigationLiveData.observe(this, EventObserver {
            startActivity(LegalInfoActivity::class.java)
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            // todo R.id.iShare ->
            R.id.iSettings -> viewModel.clickSettings()
            R.id.iInfo -> viewModel.clickLegalInfo()
        }
        return super.onOptionsItemSelected(item)
    }
}

private class WidgetRecyclerAdapter(
    private val items: MutableList<MainViewModel.WidgetItem> = mutableListOf(),
    val clickListener: (Int) -> Unit = {},
    val longClickListener: (Int) -> Unit = {}
) : RecyclerView.Adapter<WidgetViewHolder>() {
    fun setItems(items: List<MainViewModel.WidgetItem>) = with(this.items) {
        clear()
        addAll(items)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        WidgetViewHolder(
            ViewGraphWidgetItemBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: WidgetViewHolder, position: Int) =
        holder.bind(items[position], clickListener, longClickListener)

}

private class WidgetViewHolder(private val binding: ViewGraphWidgetItemBinding) :
    RecyclerView.ViewHolder(binding.root) {
    fun bind(
        item: MainViewModel.WidgetItem,
        clickListener: (Int) -> Unit,
        longClickListener: (Int) -> Unit
    ) = with(binding) {
        val (widgetId, imageData, showWallpaper) = item

        ivWidget.setBackgroundColor(
            ivWidget.context.getColor(
                if (showWallpaper) android.R.color.transparent else R.color.colorGray
            )
        )
        cvWidget.cardElevation =
            if (showWallpaper) 0f else root.resources.getDimension(R.dimen.widget_card_margin)
        ivWidget.setImageBitmap(BitmapFactory.decodeByteArray(imageData, 0, imageData.size))
        root.setOnClickListener { clickListener(widgetId) }
        root.setOnLongClickListener { longClickListener(widgetId).let { true } }
    }
}