package com.endiar.kuropomodoro.debug

import com.endiar.kuropomodoro.model.ClockDebugSource
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.LocalTime

class DebugRepository : ClockDebugSource {

    override val clockOverride = MutableStateFlow<LocalTime?>(null)
    override val speedUpTimer = MutableStateFlow(false)

    fun setClockOverride(time: LocalTime?) {
        clockOverride.value = time
    }

    fun setSpeedUpTimer(enabled: Boolean) {
        speedUpTimer.value = enabled
    }

    companion object {
        lateinit var instance: DebugRepository
            private set

        fun init(): DebugRepository =
            DebugRepository().also { instance = it }
    }
}
