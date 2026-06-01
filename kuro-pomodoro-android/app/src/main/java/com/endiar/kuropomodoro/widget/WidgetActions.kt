package com.endiar.kuropomodoro.widget

import android.content.Context
import android.content.Intent
import androidx.glance.action.Action
import androidx.glance.appwidget.action.actionStartActivity
import com.endiar.kuropomodoro.MainActivity
import com.endiar.kuropomodoro.data.AppRepository
import com.endiar.kuropomodoro.model.AppTab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Bridges the reactive 4x4 widgets to the host [MainActivity].
 *
 * A widget tap turns a [NudgeTarget] into a launch intent carrying the right
 * extras; [MainActivity] reads them and either auto-opens the Start Focus sheet
 * or switches the active tab. Reuses the pre-existing
 * `EXTRA_AUTO_OPEN_START_FOCUS` contract so behavior matches the Quick Start
 * widget.
 */
object WidgetActions {

    /** String extra naming the [AppTab] (by enum name) the app should open on. */
    const val EXTRA_OPEN_TAB = "com.endiar.kuropomodoro.widget.OPEN_TAB"

    /** Builds the launch [Intent] for a [NudgeTarget]. */
    fun intentFor(context: Context, target: NudgeTarget): Intent =
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            when (target) {
                NudgeTarget.StartFocus ->
                    putExtra(QuickStartWidget.EXTRA_AUTO_OPEN_START_FOCUS, true)

                NudgeTarget.OpenHabits -> putExtra(EXTRA_OPEN_TAB, AppTab.Habits.name)
                NudgeTarget.OpenTasks -> putExtra(EXTRA_OPEN_TAB, AppTab.Todo.name)
                NudgeTarget.OpenTimer -> putExtra(EXTRA_OPEN_TAB, AppTab.Timer.name)
                NudgeTarget.OpenApp -> Unit
            }
        }

    /** Glance [Action] wrapper around [intentFor]. */
    fun action(context: Context, target: NudgeTarget): Action =
        actionStartActivity(intentFor(context, target))
}

/**
 * Loads [KuroWidgetData] from the repository off the main thread.
 *
 * Kept separate from the pure [WidgetState.deriveData] so the derivation stays
 * unit-testable without Android / DataStore.
 */
object WidgetDataLoader {

    suspend fun load(context: Context): KuroWidgetData = withContext(Dispatchers.IO) {
        val repo = AppRepository.get(context)
        val app = repo.appState.first()
        val runtime = repo.runtimeSnapshot.first()
        val pendingAck = repo.pendingCycleCompletionAckFlow.first()
        WidgetState.deriveData(
            stamps = app.stamps,
            settings = app.settings,
            habits = app.habits,
            tasks = app.tasks,
            runtime = runtime,
            pendingCycleAck = pendingAck,
            today = LocalDate.now(),
        )
    }
}
