/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.android

import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import com.alexvt.weathergraph.R
import com.alexvt.weathergraph.viewmodel.Event
import com.alexvt.weathergraph.viewmodel.EventObserver
import com.alexvt.weathergraph.viewmodel.LegalInfoViewModel
import com.google.android.material.button.MaterialButton
import com.mikepenz.aboutlibraries.LibsBuilder
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.activity_legal_info.*
import javax.inject.Inject


class LegalInfoActivity : BaseAppCompatActivity(R.layout.activity_legal_info) {

    @Inject
    lateinit var vmFactory: ViewModelProvider.Factory

    override val viewModel by lazy {
        ViewModelProvider(this, vmFactory)[LegalInfoViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)

        bindNavigation()

        bindLicense()
        bindDataAndPrivacyPolicy()
        bindThirdPartyLicenses()
        bindAboutApp()
    }

    private fun bindLicense() = bindSection(
        liveData = viewModel.licenseNavigationLiveData,
        liveDataListener = { startActivity(LicenseActivity::class.java) },
        button = mbLicense,
        buttonListener = { viewModel.clickLicense() }
    )

    private fun bindDataAndPrivacyPolicy() = bindSection(
        liveData = viewModel.dataNavigationLiveData,
        liveDataListener = { startActivity(DataActivity::class.java) },
        button = mbData,
        buttonListener = { viewModel.clickData() }
    )

    private fun bindThirdPartyLicenses() = bindSection(
        liveData = viewModel.thirdPartyLicensesNavigationLiveData,
        liveDataListener = { showThirdPartyLibraryInfo() },
        button = mbLicensesThirdParty,
        buttonListener = { viewModel.clickLicensesThirdParty() }
    )

    private fun bindAboutApp() = bindSection(
        liveData = viewModel.aboutAppNavigationLiveData,
        liveDataListener = { openLink(getString(R.string.app_web_page_url)) },
        button = mbAboutApp,
        buttonListener = { viewModel.clickAboutApp() }
    )

    private fun bindSection(
        liveData: LiveData<Event<Unit>>,
        liveDataListener: () -> Unit,
        button: MaterialButton,
        buttonListener: () -> Unit
    ) {
        liveData.observe(this, EventObserver { liveDataListener() })
        button.setOnClickListener { buttonListener() }
    }

    private fun showThirdPartyLibraryInfo() = LibsBuilder().apply {
        activityTitle = getString(R.string.third_party_software_info)
        showLicense = true
        aboutShowIcon = false
        aboutShowVersion = false
    }.start(this)

    private fun bindNavigation() {
        viewModel.backNavigationLiveData.observe(this, EventObserver { finish() })
        fabOk.setOnClickListener { viewModel.clickBack() }
    }

    override fun onSupportNavigateUp() = viewModel.clickBack().let { true }

    override fun onBackPressed() = viewModel.clickBack()
}
