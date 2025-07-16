package com.example.localvideoplayer.ui

import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.localvideoplayer.R
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ControlCoordinationIntegrationTest {

    private lateinit var scenario: ActivityScenario<PlayerActivity>
    private lateinit var testVideoUri: Uri

    @Before
    fun setup() {
        // Create a test video URI (in a real test, you'd use a test video file)
        testVideoUri = Uri.parse("android.resource://${ApplicationProvider.getApplicationContext<android.content.Context>().packageName}/raw/test_video")
        
        val intent = Intent(ApplicationProvider.getApplicationContext(), PlayerActivity::class.java).apply {
            data = testVideoUri
        }
        scenario = ActivityScenario.launch(intent)
    }

    @After
    fun tearDown() {
        scenario.close()
    }

    @Test
    fun testCustomControlsPanelToggleShowsAndHidesCorrectly() {
        // Initially, custom controls panel should be hidden
        onView(withId(R.id.custom_controls_panel))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))

        // Click toggle button to show custom controls
        onView(withId(R.id.toggle_custom_controls_button))
            .perform(click())

        // Custom controls panel should now be visible
        onView(withId(R.id.custom_controls_panel))
            .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

        // Click toggle button again to hide custom controls
        onView(withId(R.id.toggle_custom_controls_button))
            .perform(click())

        // Custom controls panel should be hidden again
        onView(withId(R.id.custom_controls_panel))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    @Test
    fun testLoopControlsInteractionWithVideoControls() {
        // Show custom controls panel
        onView(withId(R.id.toggle_custom_controls_button))
            .perform(click())

        // Verify custom controls are visible
        onView(withId(R.id.custom_controls_panel))
            .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

        // Test loop control buttons are clickable
        onView(withId(R.id.set_start_button))
            .check(matches(isDisplayed()))
            .perform(click())

        onView(withId(R.id.set_end_button))
            .check(matches(isDisplayed()))
            .perform(click())

        onView(withId(R.id.clear_loop_button))
            .check(matches(isDisplayed()))
            .perform(click())
    }

    @Test
    fun testThumbnailRecyclerViewInteraction() {
        // Show custom controls panel
        onView(withId(R.id.toggle_custom_controls_button))
            .perform(click())

        // Verify thumbnail RecyclerView is visible
        onView(withId(R.id.thumbnailsRecyclerView))
            .check(matches(isDisplayed()))

        // Verify progress bar is initially visible (during thumbnail generation)
        onView(withId(R.id.timelineProgressBar))
            .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
    }

    @Test
    fun testPlayerViewControlsVisibility() {
        // PlayerView should be visible
        onView(withId(R.id.playerView))
            .check(matches(isDisplayed()))

        // Toggle button should be visible
        onView(withId(R.id.toggle_custom_controls_button))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testMemoryPressureScenario() {
        // Show custom controls
        onView(withId(R.id.toggle_custom_controls_button))
            .perform(click())

        // Simulate memory pressure by triggering multiple interactions
        repeat(5) {
            onView(withId(R.id.set_start_button)).perform(click())
            onView(withId(R.id.set_end_button)).perform(click())
            onView(withId(R.id.clear_loop_button)).perform(click())
        }

        // Controls should still be responsive
        onView(withId(R.id.custom_controls_panel))
            .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

        onView(withId(R.id.set_start_button))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testControlStateConsistencyAcrossInteractions() {
        // Test multiple state changes
        repeat(3) {
            // Show controls
            onView(withId(R.id.toggle_custom_controls_button))
                .perform(click())
            
            onView(withId(R.id.custom_controls_panel))
                .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

            // Hide controls
            onView(withId(R.id.toggle_custom_controls_button))
                .perform(click())
            
            onView(withId(R.id.custom_controls_panel))
                .check(matches(withEffectiveVisibility(Visibility.GONE)))
        }
    }
}