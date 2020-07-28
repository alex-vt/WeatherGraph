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

class LegalInfoViewModel @Inject constructor(
    override val userManageSettingsUseCaseFactory: UserManageSettingsUseCase.Factory
) : BaseViewModel() {

    val backNavigationLiveData: LiveData<Event<Unit>> = MutableLiveData()
    fun clickBack() = backNavigationLiveData.setEvent()

    val licenseNavigationLiveData: LiveData<Event<Unit>> = MutableLiveData()
    fun clickLicense() = licenseNavigationLiveData.setEvent()

    val dataNavigationLiveData: LiveData<Event<Unit>> = MutableLiveData()
    fun clickData() = dataNavigationLiveData.setEvent()

    val thirdPartyLicensesNavigationLiveData: LiveData<Event<Unit>> = MutableLiveData()
    fun clickLicensesThirdParty() = thirdPartyLicensesNavigationLiveData.setEvent()

    val aboutAppNavigationLiveData: LiveData<Event<Unit>> = MutableLiveData()
    fun clickAboutApp() = aboutAppNavigationLiveData.setEvent()
}