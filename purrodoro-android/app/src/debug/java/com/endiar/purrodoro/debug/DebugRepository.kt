package com.endiar.purrodoro.debug

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.endiar.purrodoro.model.ClockDebugSource
import com.endiar.purrodoro.model.PremiumDebugSource
import com.endiar.purrodoro.model.PremiumOverride
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
private val KEY_IS_PREMIUM_OVERRIDE = booleanPreferencesKey("is_premium_override")
private val KEY_PREMIUM_OVERRIDE_VALUE = booleanPreferencesKey("premium_override_value")
private val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")

class DebugRepository(private val context: Context? = null) : ClockDebugSource, PremiumDebugSource {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val clockOverride = MutableStateFlow<LocalTime?>(null)
    override val isPremiumOverrideEnabled = MutableStateFlow(false)
    override val premiumOverrideValue = MutableStateFlow(true)

    init {
        if (context != null) {
            val prefs = runBlocking { context.debugDataStore.data.first() }
            clockOverride.value = prefs[KEY_CLOCK_OVERRIDE]
                ?.takeIf { it.isNotEmpty() }
                ?.let { runCatching { LocalTime.parse(it, TIME_FORMAT) }.getOrNull() }
            isPremiumOverrideEnabled.value = prefs[KEY_IS_PREMIUM_OVERRIDE] ?: false
            premiumOverrideValue.value = prefs[KEY_PREMIUM_OVERRIDE_VALUE] ?: true
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

    fun setPremiumOverrideEnabled(enabled: Boolean) {
        isPremiumOverrideEnabled.value = enabled
        persistPremiumOverride()
    }

    fun setPremiumOverrideValue(value: Boolean) {
        premiumOverrideValue.value = value
        persistPremiumOverride()
    }

    private fun persistPremiumOverride() {
        context?.let { ctx ->
            scope.launch {
                ctx.debugDataStore.edit { prefs ->
                    prefs[KEY_IS_PREMIUM_OVERRIDE] = isPremiumOverrideEnabled.value
                    prefs[KEY_PREMIUM_OVERRIDE_VALUE] = premiumOverrideValue.value
                }
            }
        }
    }

    companion object {
        lateinit var instance: DebugRepository
            private set

        fun init(context: Context): DebugRepository =
            DebugRepository(context.applicationContext).also {
                instance = it
                PremiumOverride.source = it
            }

        /** No-arg factory for unit tests only — no persistence. */
        fun init(): DebugRepository =
            DebugRepository().also { instance = it }
    }
}
