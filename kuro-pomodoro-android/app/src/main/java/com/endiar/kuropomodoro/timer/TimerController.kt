package com.endiar.kuropomodoro.timer

import com.endiar.kuropomodoro.model.FocusTask
import com.endiar.kuropomodoro.model.FocusType
import com.endiar.kuropomodoro.model.TimerSettings

interface TimerController {
    fun start(settings: TimerSettings, task: FocusTask?, activeHabitId: Int? = null, focusType: FocusType = FocusType.Empty)
    fun pause()
    fun resume()
    fun stop()
    fun skip()
    fun dismissCompletion()
}