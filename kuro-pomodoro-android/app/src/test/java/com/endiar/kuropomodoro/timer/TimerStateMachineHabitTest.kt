package com.endiar.kuropomodoro.timer

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.endiar.kuropomodoro.data.AppRepository
import com.endiar.kuropomodoro.model.Habit
import com.endiar.kuropomodoro.model.HabitTargetType
import com.endiar.kuropomodoro.model.SessionItemType
import com.endiar.kuropomodoro.model.SessionRecord
import com.endiar.kuropomodoro.model.SessionStatus
import com.endiar.kuropomodoro.model.TimerRuntimeState
import com.endiar.kuropomodoro.model.TimerSession
import com.endiar.kuropomodoro.model.TimerSessionItem
import com.endiar.kuropomodoro.model.TimerSettings
import com.endiar.kuropomodoro.model.TimerState
import com.endiar.kuropomodoro.model.progressOn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class TimerStateMachineHabitTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private fun makeRepo(): AppRepository {
        val ds = PreferenceDataStoreFactory.create {
            tmpFolder.newFile("sm_habit_${System.nanoTime()}.preferences_pb")
        }
        return AppRepository(ds)
    }

    private val utc = ZoneId.of("UTC")

    private val settings = TimerSettings(
        focusMinutes = 25,
        shortBreakMinutes = 5,
        longBreakMinutes = 15,
        autoStartNextSession = false,
        focusSessionsPerCycle = 2,
    )

    private val baseRecord = SessionRecord(
        id = 1L, startedAt = 1L, endedAt = 0L,
        focusMinutes = 25, shortBreakMinutes = 5, longBreakMinutes = 15,
        completedFocusCount = 0, totalFocusCount = 2,
        status = SessionStatus.Partial, taskName = null,
    )

    private val focusItem = TimerSessionItem(SessionItemType.Focus(1500))
    private val breakItem = TimerSessionItem(SessionItemType.ShortBreak(300))

    private fun playingFocusSnapshot(endsAt: Long, habitId: Int?): TimerRuntimeState =
        TimerRuntimeState(
            session = TimerSession(
                items = listOf(focusItem, breakItem),
                currentIndex = 0,
                remainingSeconds = 1500,
                timerState = TimerState.Playing,
            ),
            endsAtMillis = endsAt,
            activeTask = null,
            openRecord = baseRecord,
            activeHabitId = habitId,
        )

    @Test
    fun `focus completion credits the active habit`() = runTest {
        val repo = makeRepo()
        val habitId = 42
        // Seed the habit in repo
        repo.addHabit("Study", "📚", 0xFF_90CAF9L,
            targetType = HabitTargetType.Pomodoro, targetValue = 2)

        val endsAt = 1_718_000_000_000L
        val snapshot = playingFocusSnapshot(endsAt, habitId = 1)
        TimerStateMachine.applyTimerComplete(snapshot, settings, endsAt + 1000L, repo, utc)

        val habits = repo.appState.first().habits
        assertEquals(1, habits.size)
        val expectedDate = Instant.ofEpochMilli(endsAt).atZone(utc).toLocalDate()
        assertEquals(1, habits[0].progressOn(expectedDate))
    }

    @Test
    fun `focus completion without habit does not touch habits`() = runTest {
        val repo = makeRepo()
        repo.addHabit("Study", "📚", 0xFF_90CAF9L, targetType = HabitTargetType.Binary)

        val endsAt = 1_718_000_000_000L
        val snapshot = playingFocusSnapshot(endsAt, habitId = null)
        TimerStateMachine.applyTimerComplete(snapshot, settings, endsAt + 1000L, repo, utc)

        val habits = repo.appState.first().habits
        assertEquals(0, habits[0].logs.size)
    }

    @Test
    fun `break completion does not credit the habit`() = runTest {
        val repo = makeRepo()
        repo.addHabit("Study", "📚", 0xFF_90CAF9L, targetType = HabitTargetType.Pomodoro, targetValue = 2)

        val endsAt = 1_718_000_000_000L
        val breakSnapshot = TimerRuntimeState(
            session = TimerSession(
                items = listOf(focusItem, breakItem),
                currentIndex = 1,  // currently on break
                remainingSeconds = 300,
                timerState = TimerState.Playing,
            ),
            endsAtMillis = endsAt,
            activeTask = null,
            openRecord = baseRecord,
            activeHabitId = 1,
        )
        TimerStateMachine.applyTimerComplete(breakSnapshot, settings, endsAt + 1000L, repo, utc)

        val habits = repo.appState.first().habits
        assertEquals(0, habits[0].logs.size)
    }
}
