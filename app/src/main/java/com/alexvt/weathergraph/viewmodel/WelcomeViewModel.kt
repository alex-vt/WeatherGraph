/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.alexvt.weathergraph.usecases.UserManageSettingsUseCase
import javax.inject.Inject

class WelcomeViewModel @Inject constructor(
    override val userManageSettingsUseCaseFactory: UserManageSettingsUseCase.Factory
) : BaseViewModel() {
    private val userManageSettingsUseCase = userManageSettingsUseCaseFactory.single
    val welcomeDialogLiveData by lazy {
        userManageSettingsUseCase.observeWelcomed().map { !it }.toLiveData()
    }

    fun clickWelcomeAccept() = userManageSettingsUseCase.setWelcomed(true)
    val rejectLiveData: LiveData<Event<Unit>> = MutableLiveData()
    fun clickWelcomeRefuse() = rejectLiveData.setEvent()
    val dataNavigationLiveData: LiveData<Event<Unit>> = MutableLiveData()
    fun openData() = dataNavigationLiveData.setEvent()
    val licenseNavigationLiveData: LiveData<Event<Unit>> = MutableLiveData()
    fun openLicense() = licenseNavigationLiveData.setEvent()

}
