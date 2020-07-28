/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.alexvt.weathergraph.usecases.*
import javax.inject.Inject

class SettingsViewModel @Inject constructor(
    private val userManageSettingsUseCase: UserManageSettingsUseCase.Factory,
    val quickSettings: QuickSettingsViewModel,
    override val userManageSettingsUseCaseFactory: UserManageSettingsUseCase.Factory
) : BaseViewModel() {

    private val case = userManageSettingsUseCase.single

    val resetDialogLiveData: LiveData<Unit?> = MutableLiveData()
    val backNavigationLiveData: LiveData<Event<Unit>> = MutableLiveData()

    fun clickReset() = resetDialogLiveData.setValue(Unit)

    fun confirmReset() = case.reset().also { resetDialogLiveData.setValue(null) }

    fun dismissReset() = resetDialogLiveData.setValue(null)

    fun clickBack() = backNavigationLiveData.setEvent()
}