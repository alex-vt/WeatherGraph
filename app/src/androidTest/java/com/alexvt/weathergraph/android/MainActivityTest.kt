/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.android


import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.alexvt.weathergraph.R
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith


@LargeTest
@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @Before
    fun setUp() {
    }

    @Test
    fun mainActivityWithWelcomeRejected() {
        val activityScenario = ActivityScenario.launch(MainActivity::class.java)
        onView(withText("Welcome!")).check(matches(isDisplayed()))
        onView(withText("Close")).check(matches(isDisplayed()))
        onView(withText("Accept")).check(matches(isDisplayed()))
        onView(withText("Close")).perform(click())
        Thread.sleep(200) // todo wait for data
        assertEquals(Lifecycle.State.DESTROYED, activityScenario.state)
    }

    @Test
    fun mainActivityWithWelcomeAccepted() {
        val activityScenario = ActivityScenario.launch(MainActivity::class.java)
        onView(withText("Welcome!")).check(matches(isDisplayed()))
        onView(withText("Close")).check(matches(isDisplayed()))
        onView(withText("Accept")).check(matches(isDisplayed()))
        onView(withText("Accept")).perform(click())
        assertEquals(Lifecycle.State.RESUMED, activityScenario.state)
        Thread.sleep(200) // todo wait for data
        onView(withText("Auto")).check(matches(isDisplayed()))
        onView(withText("Dark")).check(matches(isDisplayed()))
        onView(withText("Light")).check(matches(isDisplayed()))
    }

    @Test
    fun mainActivityClickPlusAndCancel() {
        val activityScenario = ActivityScenario.launch(MainActivity::class.java)
        onView(withText("Accept")).check(matches(isDisplayed()))
        onView(withText("Accept")).perform(click())
        Thread.sleep(200) // todo wait for data
        onView(withId(R.id.fabAdd)).check(matches(isDisplayed()))
        onView(withId(R.id.fabAdd)).perform(click())
        onView(withText("Add new widget")).check(matches(isDisplayed()))
        onView(withText("Cancel")).check(matches(isDisplayed()))
        onView(withText("Add here anyway")).check(matches(isDisplayed()))
        onView(withId(R.id.fabAdd)).check(doesNotExist())
        assertEquals(Lifecycle.State.RESUMED, activityScenario.state)
        onView(withText("Cancel")).perform(click())
        onView(withId(R.id.fabAdd)).check(matches(isDisplayed()))
    }

    @Test
    fun mainActivityClickPlusAndAddHereAnyway() {
        val activityScenario = ActivityScenario.launch(MainActivity::class.java)
        onView(withText("Accept")).perform(click())
        Thread.sleep(200) // todo wait for data
        onView(withId(R.id.fabAdd)).perform(click())
        assertEquals(Lifecycle.State.RESUMED, activityScenario.state)
        onView(withText("Add here anyway")).perform(click())
        assertEquals(Lifecycle.State.CREATED, activityScenario.state)
    }

    @Test
    fun mainActivityProceedToAddHere() {
        val activityScenario = ActivityScenario.launch(MainActivity::class.java)
        onView(withText("Accept")).perform(click())
        Thread.sleep(200) // todo wait for data
        onView(withId(R.id.fabAdd)).perform(click())
        onView(withText("Add here anyway")).perform(click())
        assertEquals(Lifecycle.State.CREATED, activityScenario.state)

        val activityDetailsScenario = ActivityScenario.launch(WidgetDetailsActivity::class.java)
        assertEquals(Lifecycle.State.RESUMED, activityDetailsScenario.state)
    }

}
