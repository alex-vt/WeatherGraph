/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.android

import android.os.Bundle
import androidx.lifecycle.Observer
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.callbacks.onCancel
import com.afollestad.materialdialogs.list.listItems
import com.alexvt.weathergraph.R
import com.alexvt.weathergraph.viewmodel.EventObserver
import com.alexvt.weathergraph.viewmodel.WelcomeViewModel

class WelcomeFragment : BaseFragment(R.layout.fragment_blank) {

    private val viewModel by lazy {
        viewModelProvider[WelcomeViewModel::class.java]
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        bindWelcome()
    }

    private fun bindWelcome() {
        val actions = listOf(
            R.string.mozilla_license to { viewModel.openLicense() },
            R.string.data_privacy_policy to { viewModel.openData() }
        )
        viewModel.welcomeDialogLiveData.observe(viewLifecycleOwner, Observer { showWelcome ->
            if (!showWelcome) return@Observer
            MaterialDialog(activity!!, BottomSheet()).show {
                title(R.string.welcome)
                message(R.string.weather_graph_can_visualize)
                cancelOnTouchOutside(false)
                positiveButton(R.string.accept) { viewModel.clickWelcomeAccept() }
                negativeButton(R.string.close) { viewModel.clickWelcomeRefuse() }
                onCancel { viewModel.clickWelcomeRefuse() }
                listItems(
                    items = actions.map { (stringRes, _) -> getString(stringRes) },
                    waitForPositiveButton = false
                ) { _, index, _ ->
                    actions[index].let { (_, listener) -> listener() }
                }
            }
        })
        viewModel.licenseNavigationLiveData.observe(viewLifecycleOwner, EventObserver {
            startActivity(LicenseActivity::class.java)
        })
        viewModel.dataNavigationLiveData.observe(viewLifecycleOwner, EventObserver {
            startActivity(DataActivity::class.java)
        })
        viewModel.rejectLiveData.observe(viewLifecycleOwner, EventObserver {
            finish()
        })
    }
}