/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.android.util

import android.annotation.SuppressLint
import android.content.res.Resources
import android.graphics.Color
import android.util.TypedValue
import android.view.Menu
import android.view.View
import android.widget.ImageView
import androidx.appcompat.view.menu.ActionMenuItem
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuItemImpl
import androidx.appcompat.widget.SearchView
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.iterator
import com.alexvt.weathergraph.R

object MenuUtil {

    @SuppressLint("RestrictedApi")
    fun Menu?.setVisibleIcons(theme: Resources.Theme) {
        (this as? MenuBuilder)?.setOptionalIconsVisible(true)
        this?.iterator()?.forEach {
            if ((it as? MenuItemImpl)?.requiresOverflow() == true) {
                var drawable = it.icon
                drawable = DrawableCompat.wrap(drawable)
                TypedValue().apply {
                    theme.resolveAttribute(R.attr.colorOnSurface, this, true)
                    DrawableCompat.setTint(drawable, data)
                }
                it.icon = drawable
            }
        }
    }

}