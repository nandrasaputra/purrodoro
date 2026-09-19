package com.endiar.purrodoro

import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FindHostActivityTest {

    private val activityController = Robolectric.buildActivity(ComponentActivity::class.java)

    @Test
    fun `direct activity returns itself`() {
        val activity = activityController.get()
        assertSame(activity, activity.findHostActivity())
    }

    @Test
    fun `configuration wrapper layers unwrap to host activity`() {
        val activity = activityController.get()
        // Mirrors KuroLocalizationProvider: createConfigurationContext returns a
        // ContextWrapper whose base chain leads back to the host Activity.
        val wrapped = ContextWrapper(ContextWrapper(activity))
        assertSame(activity, wrapped.findHostActivity())
    }

    @Test
    fun `non-activity context returns null`() {
        val appContext = ApplicationProvider.getApplicationContext<android.content.Context>()
        assertNull(appContext.findHostActivity())
        assertNull(ContextWrapper(appContext).findHostActivity())
    }
}
