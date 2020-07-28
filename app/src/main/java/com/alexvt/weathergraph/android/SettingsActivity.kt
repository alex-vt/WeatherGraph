/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.android

import android.content.res.Resources
import android.os.Bundle
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.view.ContextThemeWrapper
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.afollestad.assent.Permission
import com.afollestad.assent.askForPermissions
import com.afollestad.assent.rationale.createDialogRationale
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.callbacks.onDismiss
import com.alexvt.weathergraph.R
import com.alexvt.weathergraph.entities.AppTheme
import com.alexvt.weathergraph.entities.SortingMethod
import com.alexvt.weathergraph.viewmodel.EventObserver
import com.alexvt.weathergraph.viewmodel.SettingsViewModel
import com.google.android.material.button.MaterialButton
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.activity_settings.*
import kotlinx.android.synthetic.main.activity_settings.mbAscending
import kotlinx.android.synthetic.main.activity_settings.mbDescending
import kotlinx.android.synthetic.main.activity_settings.smShowWallpaper
import kotlinx.android.synthetic.main.activity_settings.vSortingMethod
import kotlinx.android.synthetic.main.activity_settings.vStyle
import kotlinx.android.synthetic.main.activity_settings.vTheme
import kotlinx.android.synthetic.main.view_material_5_button_group.view.*
import javax.inject.Inject


class SettingsActivity : BaseAppCompatActivity(R.layout.activity_settings) {

    @Inject
    lateinit var vmFactory: ViewModelProvider.Factory

    override val viewModel by lazy {
        ViewModelProvider(this, vmFactory)[SettingsViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)

        bindNavigation()

        bindMenu()

        bindSortingMethod()
        bindSortingDirection()
        bindTheme()
        bindStyle()
        bindShowWallpaper()
    }

    override fun onCreateOptionsMenu(menu: Menu?) =
        menuInflater.inflate(R.menu.settings_menu, menu).let { true }

    private fun bindMenu() {
        viewModel.resetDialogLiveData.observe(this, Observer { allowDialog ->
            if (allowDialog == null) return@Observer
            MaterialDialog(this).show {
                title(R.string.reset_settings)
                message(R.string.settings_will_reset)
                negativeButton(R.string.cancel)
                positiveButton(R.string.confirm) { viewModel.confirmReset() }
                onDismiss { viewModel.dismissReset() }
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.iResetToDefault -> viewModel.clickReset()
        }
        return super.onOptionsItemSelected(item)
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
        vSortingMethod.getOptionButtons().take(sortingMethods.size).mapIndexed { index, button ->
            button.apply {
                visibility = View.VISIBLE
                text = nameMap[index].second
            }
            button.setOnClickListener { viewModel.quickSettings.clickSortingMethod(index) }
        }
        viewModel.quickSettings.sortingMethodIndexLiveData.observe(this, Observer { index ->
            vSortingMethod.getOptionButtons()[index].isChecked = true
        })
    }

    private fun View.getOptionButtons() =
        listOf(this.vButton1, this.vButton2, this.vButton3, this.vButton4, this.vButton5)
            .map { it as MaterialButton }

    private fun bindSortingDirection() {
        mbAscending.setOnClickListener { viewModel.quickSettings.setSortingAscending(true) }
        mbDescending.setOnClickListener { viewModel.quickSettings.setSortingAscending(false) }
        viewModel.quickSettings.sortingAscendingLiveData.observe(this, Observer { ascending ->
            mbAscending.isChecked = ascending
            mbDescending.isChecked = !ascending
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
        vTheme.getOptionButtons().take(themes.size).mapIndexed { index, button ->
            button.apply {
                visibility = View.VISIBLE
                text = nameMap[index].second
            }
            button.setOnClickListener { viewModel.quickSettings.clickTheme(index) }
        }
        viewModel.quickSettings.themeIndexLiveData.observe(this, Observer { index ->
            vTheme.getOptionButtons()[index].isChecked = true
        })
    }

    private fun bindStyle() {
        // todo palettes of varying size
        vStyle.getOptionButtons().mapIndexed { index, button ->
            button.apply {
                visibility = View.VISIBLE
                text = ""
                icon = getDrawable(R.drawable.ic_palette_black_24dp)
                setIconTintResource(getTheme(styles[index]).getColorRes(R.attr.colorPrimary))
            }
            button.setOnClickListener { viewModel.quickSettings.clickStyle(index) }
        }
        viewModel.quickSettings.styleIndexLiveData.observe(this, Observer { index ->
            vStyle.getOptionButtons()[index].isChecked = true
        })
        viewModel.quickSettings.styleChangeLiveData.observe(this, EventObserver { recreate() })
    }

    private fun getTheme(resId: Int) = ContextThemeWrapper(baseContext, resId).theme

    private fun Resources.Theme.getColorRes(attr: Int) = TypedValue().apply {
        this@getColorRes.resolveAttribute(attr, this, true)
    }.resourceId

    private fun bindShowWallpaper() {
        smShowWallpaper.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                getReadStoragePermission { isAllowed ->
                    viewModel.quickSettings.setShowWallpaper(isAllowed)
                }
            } else {
                viewModel.quickSettings.setShowWallpaper(false)
            }
        }
        viewModel.quickSettings.showWallpaperLiveData.observe(this, Observer {
            smShowWallpaper.isChecked = it
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

    private fun bindNavigation() {
        viewModel.backNavigationLiveData.observe(this, EventObserver { finish() })
        fabOk.setOnClickListener { viewModel.clickBack() }
    }

    override fun onSupportNavigateUp() = viewModel.clickBack().let { true }

    override fun onBackPressed() = viewModel.clickBack()
}
