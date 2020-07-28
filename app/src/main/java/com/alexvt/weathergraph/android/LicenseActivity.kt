/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.android

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.alexvt.weathergraph.R
import com.alexvt.weathergraph.viewmodel.EventObserver
import com.alexvt.weathergraph.viewmodel.LicenseViewModel
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.activity_license.*
import javax.inject.Inject


class LicenseActivity : BaseAppCompatActivity(R.layout.activity_license) {

    @Inject
    lateinit var vmFactory: ViewModelProvider.Factory

    override val viewModel by lazy {
        ViewModelProvider(this, vmFactory)[LicenseViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)

        bindNavigation()
        bindText()
    }

    private fun bindNavigation() {
        fabOk.setOnClickListener { viewModel.clickBack() }
        viewModel.backNavigationLiveData.observe(this, EventObserver { finish() })
    }

    // todo use viewmodel
    private fun bindText() = assets.open("license.txt").bufferedReader()
        .use { it.readText() }.let { mtvText.text = it }

    override fun onSupportNavigateUp() = viewModel.clickBack().let { true }

    override fun onBackPressed() = viewModel.clickBack()
}
