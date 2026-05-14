package com.endiar.kuropomodoro.timer

import com.endiar.kuropomodoro.model.FocusTask
import com.endiar.kuropomodoro.model.TimerSettings

interface TimerController {
    fun start(settings: TimerSettings, task: FocusTask?)
    fun pause()
    fun resume()
    fun stop()
    fun skip()
}
