/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.android

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.alexvt.weathergraph.usecases.OnEntryPointUseCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows


@RunWith(RobolectricTestRunner::class)
// todo fix [Robolectric] WARN: Android SDK 29 requires Java 9 (have Java 8). Tests won't be run on SDK 29 unless explicitly requested.
class OnBootReceiverTest {

    val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `Boot receiver registered`() {
        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)

        val suitableReceivers = context.packageManager.queryBroadcastReceivers(intent, 0)
        assertEquals(1, suitableReceivers.size)

        val allReceivers =
            Shadows.shadowOf(ApplicationProvider.getApplicationContext<Application>())
                .registeredReceivers
        val bootReceivers = allReceivers.filter {
            it.broadcastReceiver.javaClass.name == OnBootReceiver::class.java.name
        }
        assertEquals(1, bootReceivers.size)
    }

    @Test
    fun `On boot receiver scheduling widget updates`() {
        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)

        val onEntryPointUseCase = mockk<OnEntryPointUseCase>(relaxed = true)
        val onEntryPointUseCaseFactory = mockk<OnEntryPointUseCase.Factory>(relaxed = true) {
            every { createFor(OnBootReceiver::class.java.simpleName) } returns onEntryPointUseCase
        }
        val onBootReceiver = OnBootReceiver().let {
            it.onEntryPointUseCaseFactory = onEntryPointUseCaseFactory
            spyk(it)
        }

        onBootReceiver.onReceive(context, intent)

        verify(exactly = 1) {
            onBootReceiver.onReceive(context, intent)
            onEntryPointUseCase.rescheduleUpdates()
        }
    }

}