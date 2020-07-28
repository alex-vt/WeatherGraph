/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.Observer
import com.alexvt.weathergraph.usecases.UserManageDataSourcesUseCase
import com.alexvt.weathergraph.usecases.UserManageSettingsUseCase
import io.mockk.*
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class DataViewModelTest {

    val userManageDataSourcesUseCaseFactory =
        mockk<UserManageDataSourcesUseCase.Factory>(relaxed = true)
    val userManageSettingsUseCaseFactory = mockk<UserManageSettingsUseCase.Factory>()
    val userManageDataSourcesUseCase = mockk<UserManageDataSourcesUseCase>()

    val viewModel = DataViewModel(
        userManageDataSourcesUseCaseFactory,
        userManageSettingsUseCaseFactory
    ).let {
        spyk(it)
    }

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()
    val lifecycleOwner = mockk<LifecycleOwner>()
    val lifecycle = LifecycleRegistry(mockk())

    @Before
    fun setUp() {
        every { lifecycleOwner.lifecycle } returns lifecycle
    }

    @Test
    fun `Simply going back`() {
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        val backObserver = mockk<Observer<Event<Unit>>>(relaxed = true)
        viewModel.backNavigationLiveData.observe(lifecycleOwner, backObserver)
        val linkObserver = mockk<Observer<Event<String>>>(relaxed = true)
        viewModel.linkNavigationLiveData.observe(lifecycleOwner, linkObserver)

        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        viewModel.clickBack()

        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

        verify(exactly = 1) {
            viewModel.clickBack()
            backObserver.onChanged(any())
        }
        verify(exactly = 0) {
            viewModel.clickLink(any())
            linkObserver.onChanged(any())
        }
    }

    @Test
    fun `Clicking 2 links and going back`() {
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        val backObserver = mockk<Observer<Event<Unit>>>(relaxed = true)
        viewModel.backNavigationLiveData.observe(lifecycleOwner, backObserver)
        val linkObserver = mockk<Observer<Event<String>>>(relaxed = true)
        viewModel.linkNavigationLiveData.observe(lifecycleOwner, linkObserver)

        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        viewModel.clickBack()
        val links = listOf("Link1", "Link2")
        links.forEach {
            viewModel.clickLink(it)
        }
        // todo verify each link text

        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

        verify(exactly = 1) {
            viewModel.clickBack()
            backObserver.onChanged(any())
        }
        verify(exactly = 2) {
            viewModel.clickLink(any())
            linkObserver.onChanged(any())
        }
    }

}