/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.android

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import com.alexvt.weathergraph.R
import com.alexvt.weathergraph.viewmodel.BaseViewModel
import com.alexvt.weathergraph.viewmodel.Event
import com.alexvt.weathergraph.viewmodel.EventObserver

interface SubViewModelProvider {
    fun provideSubViewModels(): Collection<ViewModel>

    @Suppress("UNCHECKED_CAST")
    operator fun <VM : ViewModel?> get(modelClass: Class<VM>) =
        provideSubViewModels().first { it.javaClass == modelClass } as VM
}

abstract class BaseAppCompatActivity(
    layoutRes: Int, private val canGoBack: Boolean = true
) : AppCompatActivity(layoutRes) {

    protected abstract val viewModel: BaseViewModel

    protected val styles by lazy {
        arrayOf(R.style.Theme1, R.style.Theme2, R.style.Theme3, R.style.Theme4, R.style.Theme5)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(styles[viewModel.getStyleIndex()])
        super.onCreate(savedInstanceState)
        viewModel.styleChangeLiveData.observeEvent { recreate() }
        takeIf { canGoBack }?.supportActionBar?.apply {
            setDisplayShowHomeEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    protected fun <T> LiveData<T>.observe(listener: (T) -> Unit) =
        observe(this@BaseAppCompatActivity, Observer(listener))

    protected fun <T> LiveData<Event<T>>.observeEvent(listener: (T) -> Unit) =
        observe(this@BaseAppCompatActivity, EventObserver(listener))

    fun startActivity(activityClass: Class<out Activity>) =
        startActivity(Intent(this, activityClass))

    protected fun openLink(linkString: String) = CustomTabsIntent.Builder().apply {
        TypedValue().apply { theme.resolveAttribute(R.attr.colorPrimary, this, true) }.data
            .let { setToolbarColor(it) }
    }.build().launchUrl(this, Uri.parse(linkString))
}
