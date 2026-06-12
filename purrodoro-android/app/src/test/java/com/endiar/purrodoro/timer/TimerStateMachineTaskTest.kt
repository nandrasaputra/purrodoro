package com.endiar.purrodoro.timer

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.endiar.purrodoro.data.AppRepository
import com.endiar.purrodoro.data.local.PurrodoroDatabase
import com.endiar.purrodoro.model.FocusTask
import com.endiar.purrodoro.model.SessionItemType
import com.endiar.purrodoro.model.SessionRecord
import com.endiar.purrodoro.model.SessionStatus
import com.endiar.purrodoro.model.TimerRuntimeState
import com.endiar.purrodoro.model.TimerSession
import com.endiar.purrodoro.model.TimerSessionItem
import com.endiar.purrodoro.model.TimerSettings
import com.endiar.purrodoro.model.TimerState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.ZoneId

/**
 * Coverage for the new "tasks decoupled from Pomodoro" data path.
 *
 * After the redesign, tasks no longer have a per-task pomodoro target — completion
 * is binary, and a successful cycle finish on an attached real task auto-marks it
 * as done. Mid-cycle Focus segments do NOT mutate the task at all (the dot row
 * and per-task counter UI is gone). Habits are unaffected; their progress is
 * still credited per-segment via incrementHabitProgress.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class TimerStateMachineTaskTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val db = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        PurrodoroDatabase::class.java,
    ).allowMainThreadQueries().build()

    @After
    fun tearDown() {
        db.close()
    }

    private fun makeRepo(): AppRepository {
        val ds = PreferenceDataStoreFactory.create {
            tmpFolder.newFile("sm_task_${System.nanoTime()}.preferences_pb")
        }
        return AppRepository(ds, db)
    }

    private val utc = ZoneId.of("UTC")

    private val settings = TimerSettings(
        focusMinutes = 25, shortBreakMinutes = 5, longBreakMinutes = 15,
        autoStartNextSession = false, focusSessionsPerCycle = 1,
    )

    private val baseRecord = SessionRecord(
        id = 1L, startedAt = 1L, endedAt = 0L,
        focusMinutes = 25, shortBreakMinutes = 5, longBreakMinutes = 15,
        completedFocusCount = 0, totalFocusCount = 1,
        status = SessionStatus.Partial, taskName = "Write report",
    )

    private val focusItem = TimerSessionItem(SessionItemType.Focus(1500))
    private val longBreakItem = TimerSessionItem(SessionItemType.LongBreak(900))

    /**
     * One-focus + one-long-break cycle, currently mid-Focus. This is the minimum
     * shape that still routes through both the focus-completes path AND the
     * end-of-cycle close-out, so tests can target either by setting currentIndex.
     */
    private fun snapshot(
        currentIndex: Int,
        task: FocusTask?,
        endsAt: Long,
    ): TimerRuntimeState = TimerRuntimeState(
        session = TimerSession(
            items = listOf(focusItem, longBreakItem),
            currentIndex = currentIndex,
            remainingSeconds = if (currentIndex == 0) 1500 else 900,
            timerState = TimerState.Playing,
        ),
        endsAtMillis = endsAt,
        activeTask = task,
        openRecord = baseRecord,
    )

    @Test
    fun `focus segment completion does not touch attached real task`() = runTest {
        val repo = makeRepo()
        // Seed a real task in the repo.
        repo.setTasks(listOf(FocusTask(id = 7, title = "Write report", minutes = 25)))
        val task = repo.appState.first().tasks.first()

        val endsAt = 1_718_000_000_000L
        // currentIndex = 0 → finishing the Focus segment, advancing to LongBreak.
        TimerStateMachine.applyTimerComplete(
            snapshot(currentIndex = 0, task = task, endsAt = endsAt),
            settings, endsAt + 1000L, repo, utc,
        )

        val storedTask = repo.appState.first().tasks.first()
        // Decoupled: focus completion alone must not flip the completed flag.
        assertFalse(storedTask.completed)
        // And `completedSessions` no longer accrues per-segment for tasks.
        assertEquals(0, storedTask.completedSessions)
    }

    @Test
    fun `cycle end auto-completes attached real task`() = runTest {
        val repo = makeRepo()
        repo.setTasks(listOf(FocusTask(id = 7, title = "Write report", minutes = 25)))
        val task = repo.appState.first().tasks.first()

        val endsAt = 1_718_000_000_000L
        // currentIndex = 1 → completing the final LongBreak ends the cycle.
        val next = TimerStateMachine.applyTimerComplete(
            snapshot(currentIndex = 1, task = task, endsAt = endsAt),
            settings, endsAt + 1000L, repo, utc,
        )

        val stored = repo.appState.first().tasks.first()
        assertTrue("real task should auto-complete on cycle finish", stored.completed)
        // Returned state's activeTask is also flipped, so the receipt UI sees
        // the same truth without round-tripping through DataStore.
        assertTrue(next.activeTask?.completed == true)
        // Cycle ended → state should have been reset to Idle.
        assertEquals(TimerState.Idle, next.session.timerState)
        // Snapshot cleared on cycle end.
        assertNull(repo.runtimeSnapshot.first())
    }

    @Test
    fun `cycle end leaves already-completed task untouched`() = runTest {
        val repo = makeRepo()
        repo.setTasks(listOf(FocusTask(id = 7, title = "Write report", minutes = 25, completed = true)))
        val task = repo.appState.first().tasks.first()

        val endsAt = 1_718_000_000_000L
        TimerStateMachine.applyTimerComplete(
            snapshot(currentIndex = 1, task = task, endsAt = endsAt),
            settings, endsAt + 1000L, repo, utc,
        )

        val stored = repo.appState.first().tasks.first()
        assertTrue(stored.completed)
    }

    @Test
    fun `cycle end ignores synthetic habit task ids`() = runTest {
        val repo = makeRepo()
        // Habit cycles use a synthetic FocusTask with a negative id; that row
        // doesn't exist in the repo and must NOT be created or completed.
        val syntheticHabitTask = FocusTask(id = -42, title = "📚 Study", minutes = 25)

        val endsAt = 1_718_000_000_000L
        TimerStateMachine.applyTimerComplete(
            snapshot(currentIndex = 1, task = syntheticHabitTask, endsAt = endsAt),
            settings, endsAt + 1000L, repo, utc,
        )

        // No tasks were ever stored.
        assertEquals(emptyList<FocusTask>(), repo.appState.first().tasks)
    }

    @Test
    fun `cycle end raises pendingCycleCompletionAck flag`() = runTest {
        // Regression: the alarm-receiver path (and the foreground tick path) both
        // flow through applyTimerComplete. Without this write, completing a cycle
        // while the service was killed by the OS would silently swallow the
        // completion dialog — see TimerAlarmReceiver and TimerService.startTicking.
        val repo = makeRepo()
        assertFalse(repo.pendingCycleCompletionAckFlow.first())

        val endsAt = 1_718_000_000_000L
        val next = TimerStateMachine.applyTimerComplete(
            snapshot(currentIndex = 1, task = null, endsAt = endsAt),
            settings, endsAt + 1000L, repo, utc,
        )

        assertEquals(TimerState.Idle, next.session.timerState)
        assertTrue(
            "completion-dialog ack flag must be raised on cycle end",
            repo.pendingCycleCompletionAckFlow.first()
        )
    }

    @Test
    fun `mid-cycle segment completion does not raise pendingCycleCompletionAck`() = runTest {
        // Sanity: only end-of-cycle should set the flag, not every Focus/Break finish.
        val repo = makeRepo()

        val endsAt = 1_718_000_000_000L
        TimerStateMachine.applyTimerComplete(
            snapshot(currentIndex = 0, task = null, endsAt = endsAt),
            settings, endsAt + 1000L, repo, utc,
        )

        assertFalse(repo.pendingCycleCompletionAckFlow.first())
    }
}
