package com.endiar.kuropomodoro.timer

import com.endiar.kuropomodoro.model.FocusTask
import com.endiar.kuropomodoro.model.TimerSettings

interface TimerController {
    fun start(settings: TimerSettings, task: FocusTask?)
    fun pause()
    fun resume()
    fun stop()
    fun skip()

    /**
     * Acknowledge the post-cycle Idle "warm reset" state and collapse it to a fully
     * cold state. Safe to call at any time; only has an effect when the current
     * runtime state is non-null and Idle (i.e. just after a natural cycle finish or
     * skip-to-end). Without this, the UI keeps reading the previous cycle's frozen
     * `items` for display, so changing settings between dismiss and the next Start
     * would not visibly update the timer/dots until Start was tapped.
     */
    fun dismissCompletion()
}
