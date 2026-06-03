package com.endiar.purrodoro.ui.tutorial

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.toMutableStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.endiar.purrodoro.R

/**
 * Real UI elements the spotlight tour can point at. Each value is registered by
 * a [tutorialAnchor] modifier somewhere in the tree; the overlay reads back the
 * element's on-screen [Rect] to punch the spotlight hole and place the tooltip.
 */
enum class TutorialTarget { StartFocus, TasksTab, HabitsTab, JourneyTab }

/** Which side of the spotlit element the tooltip card sits on. */
enum class TooltipPlacement { Below, Above }

/**
 * One coachmark step: the element to highlight, the copy to show, where the
 * tooltip sits, and the shape of the spotlight cutout around the target.
 */
@Stable
data class TutorialStep(
    val target: TutorialTarget,
    @param:StringRes val titleRes: Int,
    @param:StringRes val bodyRes: Int,
    val placement: TooltipPlacement,
    val spotlightCornerRadius: Dp,
    val spotlightPadding: Dp,
)

/**
 * The default 4-step product tour, mirroring the onboarding's "act → plan →
 * reflect" arc but anchored to the live UI:
 *
 *  1. Start Focus CTA (Home)  — tooltip below, points up
 *  2. Tasks dock tab          — tooltip above, points down
 *  3. Habits dock tab         — tooltip above, points down
 *  4. Journey dock tab        — tooltip above, points down
 */
val defaultTutorialSteps: List<TutorialStep> = listOf(
    TutorialStep(
        target = TutorialTarget.StartFocus,
        titleRes = R.string.tutorial_start_focus_title,
        bodyRes = R.string.tutorial_start_focus_body,
        placement = TooltipPlacement.Below,
        spotlightCornerRadius = 34.dp,
        spotlightPadding = 6.dp,
    ),
    TutorialStep(
        target = TutorialTarget.TasksTab,
        titleRes = R.string.tutorial_tasks_title,
        bodyRes = R.string.tutorial_tasks_body,
        placement = TooltipPlacement.Above,
        spotlightCornerRadius = 26.dp,
        spotlightPadding = 4.dp,
    ),
    TutorialStep(
        target = TutorialTarget.HabitsTab,
        titleRes = R.string.tutorial_habits_title,
        bodyRes = R.string.tutorial_habits_body,
        placement = TooltipPlacement.Above,
        spotlightCornerRadius = 26.dp,
        spotlightPadding = 4.dp,
    ),
    TutorialStep(
        target = TutorialTarget.JourneyTab,
        titleRes = R.string.tutorial_journey_title,
        bodyRes = R.string.tutorial_journey_body,
        placement = TooltipPlacement.Above,
        spotlightCornerRadius = 26.dp,
        spotlightPadding = 4.dp,
    ),
)

/**
 * Holds tour state: which step is active, whether the overlay is visible, and a
 * live map of measured target bounds. Anchored elements report their bounds via
 * [reportBounds]; the overlay observes [bounds] (a snapshot map) so it
 * recomposes when a target moves or first lays out.
 */
@Stable
class TutorialController(val steps: List<TutorialStep>) {

    private val bounds: SnapshotStateMap<TutorialTarget, Rect> =
        emptyList<Pair<TutorialTarget, Rect>>().toMutableStateMap()

    var visible by mutableStateOf(false)
        private set

    var stepIndex by mutableIntStateOf(0)
        private set

    val currentStep: TutorialStep?
        get() = steps.getOrNull(stepIndex)

    val isLastStep: Boolean
        get() = stepIndex >= steps.lastIndex

    val canGoBack: Boolean
        get() = stepIndex > 0

    fun boundsFor(target: TutorialTarget): Rect? = bounds[target]

    fun reportBounds(target: TutorialTarget, rect: Rect) {
        bounds[target] = rect
    }

    /** (Re)start the tour from the first step. */
    fun start() {
        if (steps.isEmpty()) return
        stepIndex = 0
        visible = true
    }

    /** Advance to the next step, or finish if we're on the last one. */
    fun next(onFinish: () -> Unit) {
        if (stepIndex < steps.lastIndex) {
            stepIndex++
        } else {
            dismiss(onFinish)
        }
    }

    fun previous() {
        if (stepIndex > 0) stepIndex--
    }

    /** End the tour early (Skip / tap-out). */
    fun skip(onFinish: () -> Unit) = dismiss(onFinish)

    private fun dismiss(onFinish: () -> Unit) {
        if (!visible) return
        visible = false
        onFinish()
    }
}

@Composable
fun rememberTutorialController(
    steps: List<TutorialStep> = defaultTutorialSteps,
): TutorialController = remember(steps) { TutorialController(steps) }

/**
 * Ambient controller so any element can register itself as a spotlight target
 * with a single [tutorialAnchor] call, without threading callbacks through the
 * whole tree. Absent (null) in previews / debug catalog, where the anchor
 * modifier is a no-op.
 */
val LocalTutorialController = staticCompositionLocalOf<TutorialController?> { null }

/**
 * Registers this element's root-space bounds with the ambient
 * [TutorialController] under [target]. No-op when no controller is provided.
 */
@Composable
fun Modifier.tutorialAnchor(target: TutorialTarget): Modifier {
    val controller = LocalTutorialController.current ?: return this
    return this.onGloballyPositioned { coordinates ->
        controller.reportBounds(target, coordinates.boundsInRoot())
    }
}
