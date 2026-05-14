package com.endiar.kuropomodoro.debug

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.endiar.kuropomodoro.model.ClockDebugSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val Context.debugDataStore by preferencesDataStore(name = "kuro_debug_prefs")

private val KEY_CLOCK_OVERRIDE = stringPreferencesKey("clock_override")
private val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")

class DebugRepository(private val context: Context? = null) : ClockDebugSource {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val clockOverride = MutableStateFlow<LocalTime?>(null)

    init {
        if (context != null) {
            val prefs = runBlocking { context.debugDataStore.data.first() }
            clockOverride.value = prefs[KEY_CLOCK_OVERRIDE]
                ?.takeIf { it.isNotEmpty() }
                ?.let { runCatching { LocalTime.parse(it, TIME_FORMAT) }.getOrNull() }
        }
    }

    fun setClockOverride(time: LocalTime?) {
        clockOverride.value = time
        context?.let { ctx ->
            scope.launch {
                ctx.debugDataStore.edit { prefs ->
                    prefs[KEY_CLOCK_OVERRIDE] = time?.format(TIME_FORMAT) ?: ""
                }
            }
        }
    }

    companion object {
        lateinit var instance: DebugRepository
            private set

        fun init(context: Context): DebugRepository =
            DebugRepository(context.applicationContext).also { instance = it }

        /** No-arg factory for unit tests only — no persistence. */
        fun init(): DebugRepository =
            DebugRepository().also { instance = it }
    }
}
