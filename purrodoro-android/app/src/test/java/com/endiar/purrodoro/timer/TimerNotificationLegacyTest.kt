package com.endiar.purrodoro.timer

import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import com.endiar.purrodoro.model.FocusType
import com.endiar.purrodoro.model.SessionItemType
import com.endiar.purrodoro.model.TimerRuntimeState
import com.endiar.purrodoro.model.TimerSession
import com.endiar.purrodoro.model.TimerSessionItem
import com.endiar.purrodoro.model.TimerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class TimerNotificationLegacyTest {

    @Test
    fun `below API 36 the running notification stays a compat progress bar`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val state = TimerRuntimeState(
            session = TimerSession(
                items = listOf(TimerSessionItem(SessionItemType.Focus(25 * 60))),
                currentIndex = 0,
                remainingSeconds = 754,
                timerState = TimerState.Playing,
            ),
            endsAtMillis = null,
            activeTask = null,
            openRecord = null,
            activeHabitId = null,
            focusType = FocusType.Empty,
        )
        val notification =
            TimerNotification.buildRunningNotification(context, state, System.currentTimeMillis())
        assertTrue((notification.flags and android.app.Notification.FLAG_ONGOING_EVENT) != 0)
        assertEquals(
            NotificationCompat.CATEGORY_PROGRESS,
            notification.category,
        )
    }
}
