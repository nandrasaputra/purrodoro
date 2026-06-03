package com.endiar.purrodoro.timer

import com.endiar.purrodoro.model.FocusTask
import com.endiar.purrodoro.model.FocusType
import com.endiar.purrodoro.model.TimerSettings

interface TimerController {
    fun start(settings: TimerSettings, task: FocusTask?, activeHabitId: Int? = null, focusType: FocusType = FocusType.Empty)
    fun pause()
    fun resume()
    fun stop()
    fun skip()
}