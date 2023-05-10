/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.android.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.view.Menu
import android.view.View
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuPopupHelper

object MenuUtil {

    @SuppressLint("RestrictedApi")
    fun Menu.setVisibleIcons(context: Context, theme: Resources.Theme, anchorView: View) {
        (this as MenuBuilder).run {
            MenuPopupHelper(context, this, anchorView).run {
                setForceShowIcon(true)
            }
            setOptionalIconsVisible(true)
        }
    }

}