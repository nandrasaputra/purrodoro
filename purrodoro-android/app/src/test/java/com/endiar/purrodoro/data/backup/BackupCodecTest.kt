package com.endiar.purrodoro.data.backup

import com.endiar.purrodoro.data.AppState
import com.endiar.purrodoro.model.AppLanguage
import com.endiar.purrodoro.model.AppearanceMode
import com.endiar.purrodoro.model.Category
import com.endiar.purrodoro.model.FocusTask
import com.endiar.purrodoro.model.FocusType
import com.endiar.purrodoro.model.GlassReadabilityMode
import com.endiar.purrodoro.model.Habit
import com.endiar.purrodoro.model.HabitLog
import com.endiar.purrodoro.model.HabitLogState
import com.endiar.purrodoro.model.HabitOccurrence
import com.endiar.purrodoro.model.HomeScene
import com.endiar.purrodoro.model.SessionRecord
import com.endiar.purrodoro.model.SessionStatus
import com.endiar.purrodoro.model.TaskPriority
import com.endiar.purrodoro.model.TimeBlock
import com.endiar.purrodoro.model.TimerSettings
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCodecTest {

    private fun appState(
        userName: String = "Nandra",
        settings: TimerSettings = TimerSettings(),
        categories: List<Category> = emptyList(),
        tasks: List<FocusTask> = emptyList(),
        habits: List<Habit> = emptyList(),
        records: List<SessionRecord> = emptyList(),
        stamps: Map<LocalDate, Int> = emptyMap(),
        isPremium: Boolean = true,
    ): AppState = AppState(
        glassReadabilityMode = GlassReadabilityMode.Adaptive,
        appearanceMode = AppearanceMode.Dark,
        language = AppLanguage.Indonesian,
        userName = userName,
        selectedSoundId = 3,
        completedSessions = 7,
        settings = settings,
        nextTaskId = 5,
        stamps = stamps,
        isPremium = isPremium,
        sessionRecords = records,
        tasks = tasks,
        categories = categories,
        notificationSoundEnabled = false,
        habits = habits,
        homeScene = HomeScene.Room,
    )

    private fun decodeSuccess(raw: String): RestoreBundle {
        val result = BackupCodec.decode(raw)
        assertTrue("expected Success but was $result", result is BackupParseResult.Success)
        return (result as BackupParseResult.Success).bundle
    }

    @Test
    fun roundTrip_preservesProfilePrefsAndCollections() {
        val state = appState(
            settings = TimerSettings(30, 6, 20, autoStartNextSession = true, focusSessionsPerCycle = 3),
            categories = listOf(Category(1, "Work", 0xFF112233)),
            tasks = listOf(
                FocusTask(1, "Write spec", 25, completed = false, categoryId = 1, priority = TaskPriority.High,
                    dueDate = LocalDate.of(2026, 6, 20)),
            ),
            habits = listOf(
                Habit(
                    id = 1, name = "Read", emoji = "📖", colorArgb = 0xFFAABBCC,
                    createdAt = 1_000L,
                    occurrence = HabitOccurrence.Weekdays(setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY)),
                    timeBlock = TimeBlock.Morning,
                    logs = mapOf(
                        LocalDate.of(2026, 6, 18) to HabitLog(HabitLogState.Completed),
                        LocalDate.of(2026, 6, 17) to HabitLog(HabitLogState.Skipped),
                    ),
                ),
            ),
            records = listOf(
                SessionRecord(1, 100, 200, 25, 5, 15, 4, 4, SessionStatus.Completed, "Write spec",
                    completedShortBreakCount = 3, completedLongBreakCount = 1, focusType = FocusType.Task),
            ),
            stamps = mapOf(LocalDate.of(2026, 6, 18) to 4),
        )

        val bundle = decodeSuccess(BackupCodec.encode(state, "1.0", 123L))

        assertEquals("Nandra", bundle.displayName)
        assertEquals(TimerSettings(30, 6, 20, autoStartNextSession = true, focusSessionsPerCycle = 3), bundle.timerSettings)
        assertEquals(AppearanceMode.Dark, bundle.prefs.appearance)
        assertEquals(AppLanguage.Indonesian, bundle.prefs.language)
        assertEquals(3, bundle.prefs.selectedSoundId)
        assertFalse(bundle.prefs.notificationSound)
        assertEquals(state.categories, bundle.categories)
        assertEquals(state.tasks, bundle.tasks)
        assertEquals(state.habits, bundle.habits)
        assertEquals(state.sessionRecords, bundle.records)
        assertEquals(state.stamps, bundle.stamps)
    }

    @Test
    fun encode_neverIncludesPremiumOrOwnerOrSession() {
        val json = BackupCodec.encode(appState(isPremium = true), "1.0", 1L)
        assertFalse(json.contains("premium", ignoreCase = true))
        assertFalse(json.contains("owner", ignoreCase = true))
        assertFalse(json.contains("token", ignoreCase = true))
        // "session" legitimately appears in timer field names (auto_start_next_session,
        // focus_sessions_per_cycle), so guard against the auth-credential keys specifically.
        assertFalse(json.contains("access_token", ignoreCase = true))
        assertFalse(json.contains("refresh_token", ignoreCase = true))
    }

    @Test
    fun decode_emptyString_isMalformed() {
        assertEquals(BackupParseResult.Malformed, BackupCodec.decode(""))
    }

    @Test
    fun decode_garbage_isMalformed() {
        assertEquals(BackupParseResult.Malformed, BackupCodec.decode("not json at all {{{"))
    }

    @Test
    fun decode_missingSchemaVersion_isMalformed() {
        // valid JSON, but no schema_version → version 0 → not a real backup
        assertEquals(BackupParseResult.Malformed, BackupCodec.decode("""{"data":{}}"""))
    }

    @Test
    fun decode_newerSchemaVersion_isUnsupported() {
        val raw = """{"schema_version": 9999, "data": {}}"""
        assertEquals(BackupParseResult.UnsupportedVersion(9999), BackupCodec.decode(raw))
    }

    @Test
    fun decode_danglingCategoryRef_isNulled() {
        val state = appState(
            categories = emptyList(),
            tasks = listOf(FocusTask(1, "Orphan", 25, categoryId = 99)),
        )
        val bundle = decodeSuccess(BackupCodec.encode(state, "1.0", 1L))
        assertEquals(1, bundle.tasks.size)
        assertNull(bundle.tasks.first().categoryId)
    }

    @Test
    fun decode_habitLogForMissingHabit_isDropped() {
        // Hand-rolled JSON: a habit_log referencing habit id 42 that doesn't exist.
        val raw = """
            {"schema_version":1,"data":{
              "habits":[{"id":1,"name":"Read"}],
              "habit_logs":[{"habit_id":42,"date":"2026-06-18","state":"C"}]
            }}
        """.trimIndent()
        val bundle = decodeSuccess(raw)
        assertEquals(1, bundle.habits.size)
        assertTrue(bundle.habits.first().logs.isEmpty())
    }

    @Test
    fun decode_invalidDates_areSkipped() {
        val raw = """
            {"schema_version":1,"data":{
              "tasks":[{"id":1,"title":"T","due_date":"not-a-date"}],
              "stamps":[{"date":"nope","count":3},{"date":"2026-06-18","count":2}],
              "habits":[{"id":1,"name":"H"}],
              "habit_logs":[{"habit_id":1,"date":"bad","state":"C"},{"habit_id":1,"date":"2026-06-18","state":"C"}]
            }}
        """.trimIndent()
        val bundle = decodeSuccess(raw)
        assertNull(bundle.tasks.first().dueDate)
        assertEquals(mapOf(LocalDate.of(2026, 6, 18) to 2), bundle.stamps)
        assertEquals(setOf(LocalDate.of(2026, 6, 18)), bundle.habits.first().logs.keys)
    }

    @Test
    fun decode_coercesOutOfRangeNumbers() {
        val raw = """
            {"schema_version":1,"data":{
              "profile":{"timer_settings":{"focus_minutes":9999,"focus_sessions_per_cycle":50}},
              "records":[{"id":1,"focus_minutes":0,"short_break_minutes":-5,"completed_focus_count":-3}]
            }}
        """.trimIndent()
        val bundle = decodeSuccess(raw)
        assertEquals(180, bundle.timerSettings.focusMinutes)
        assertEquals(4, bundle.timerSettings.focusSessionsPerCycle)
        val r = bundle.records.first()
        assertEquals(1, r.focusMinutes)
        assertEquals(1, r.shortBreakMinutes)
        assertEquals(0, r.completedFocusCount)
    }

    @Test
    fun decode_dropsNonPositiveIds() {
        val raw = """
            {"schema_version":1,"data":{
              "categories":[{"id":0,"name":"zero"},{"id":-1,"name":"neg"},{"id":2,"name":"ok"}],
              "tasks":[{"id":0,"title":"drop"},{"id":3,"title":"keep"}]
            }}
        """.trimIndent()
        val bundle = decodeSuccess(raw)
        assertEquals(listOf(2), bundle.categories.map { it.id })
        assertEquals(listOf(3), bundle.tasks.map { it.id })
    }

    @Test
    fun decode_dedupesById_lastWins() {
        val raw = """
            {"schema_version":1,"data":{
              "tasks":[{"id":1,"title":"first"},{"id":1,"title":"second"}]
            }}
        """.trimIndent()
        val bundle = decodeSuccess(raw)
        assertEquals(1, bundle.tasks.size)
        assertEquals("second", bundle.tasks.first().title)
    }

    @Test
    fun decode_duplicateStampDate_takesMax() {
        val raw = """
            {"schema_version":1,"data":{
              "stamps":[{"date":"2026-06-18","count":2},{"date":"2026-06-18","count":5}]
            }}
        """.trimIndent()
        val bundle = decodeSuccess(raw)
        assertEquals(mapOf(LocalDate.of(2026, 6, 18) to 5), bundle.stamps)
    }

    @Test
    fun decode_unknownEnumTags_fallBackToDefaults() {
        val raw = """
            {"schema_version":1,"data":{
              "prefs":{"appearance":"Neon","language":"Klingon","glass":"Frosted","home_scene":"Mars"},
              "tasks":[{"id":1,"title":"T","priority":"Z"}],
              "habits":[{"id":1,"name":"H","occurrence":"Q","time_block":"Z"}],
              "records":[{"id":1,"status":"Imploded","focus_type":"Nope"}]
            }}
        """.trimIndent()
        val bundle = decodeSuccess(raw)
        assertEquals(AppearanceMode.Light, bundle.prefs.appearance)
        assertEquals(AppLanguage.System, bundle.prefs.language)
        assertEquals(GlassReadabilityMode.Adaptive, bundle.prefs.glass)
        assertEquals(HomeScene.Room, bundle.prefs.homeScene)
        assertEquals(TaskPriority.Normal, bundle.tasks.first().priority)
        assertEquals(HabitOccurrence.Daily, bundle.habits.first().occurrence)
        assertEquals(TimeBlock.Anytime, bundle.habits.first().timeBlock)
        assertEquals(SessionStatus.Partial, bundle.records.first().status)
        assertNull(bundle.records.first().focusType)
    }

    @Test
    fun decode_cleansControlCharsAndCapsLength() {
        val longName = "a".repeat(500)
        val raw = """
            {"schema_version":1,"data":{
              "tasks":[{"id":1,"title":"hi\u0000there\u0007"}],
              "categories":[{"id":1,"name":"$longName"}]
            }}
        """.trimIndent()
        val bundle = decodeSuccess(raw)
        assertEquals("hithere", bundle.tasks.first().title)
        assertEquals(100, bundle.categories.first().name.length)
    }

    @Test
    fun roundTrip_preservesPartialLogProgress() {
        val state = AppState(
            glassReadabilityMode = GlassReadabilityMode.Adaptive,
            appearanceMode = AppearanceMode.Light,
            language = AppLanguage.System,
            userName = "",
            selectedSoundId = 1,
            completedSessions = 0,
            settings = TimerSettings(),
            nextTaskId = 1,
            stamps = emptyMap(),
            habits = listOf(
                Habit(
                    id = 1, name = "Water", emoji = "💧", colorArgb = 1L, createdAt = 0L,
                    logs = mapOf(LocalDate.of(2026, 6, 18) to HabitLog(HabitLogState.Partial(3))),
                ),
            ),
            homeScene = HomeScene.Room,
        )
        val bundle = decodeSuccess(BackupCodec.encode(state, "1.0", 1L))
        val log = bundle.habits.single().logs.values.single()
        assertEquals(HabitLogState.Partial(3), log.state)
    }

    @Test
    fun decode_droppedNegativeStampCounts() {
        val raw = """
            {"schema_version":1,"data":{
              "stamps":[{"date":"2026-06-18","count":0},{"date":"2026-06-19","count":-2},{"date":"2026-06-20","count":1}]
            }}
        """.trimIndent()
        val bundle = decodeSuccess(raw)
        assertEquals(mapOf(LocalDate.of(2026, 6, 20) to 1), bundle.stamps)
    }
}