/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.android

import android.app.Activity
import androidx.fragment.app.Fragment

abstract class BaseFragment(layoutRes: Int) : Fragment(layoutRes) {

    protected val viewModelProvider by lazy { activity as SubViewModelProvider}

    protected fun startActivity(activityClass: Class<out Activity>) =
        getBaseActivity().startActivity(activityClass)

    protected fun finish() = getBaseActivity().finish()

    private fun getBaseActivity() = activity as BaseAppCompatActivity

}