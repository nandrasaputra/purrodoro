package com.endiar.purrodoro.debug

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.endiar.purrodoro.model.ClockDebugSource
import com.endiar.purrodoro.model.PremiumDebugSource
import com.endiar.purrodoro.model.PremiumOverride
import com.endiar.purrodoro.model.TimerSkipDebugSource
import com.endiar.purrodoro.model.TimerSkipOverride
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
private val KEY_SYNC_NOTIFICATION_ENABLED = booleanPreferencesKey("sync_notification_enabled")
private val KEY_SKIP_TICKS_ENABLED = booleanPreferencesKey("skip_ticks_enabled")
private val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")

class DebugRepository(private val context: Context? = null) : ClockDebugSource, PremiumDebugSource, TimerSkipDebugSource {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val clockOverride = MutableStateFlow<LocalTime?>(null)
    override val isPremiumOverrideEnabled = MutableStateFlow(false)
    override val premiumOverrideValue = MutableStateFlow(true)
    override val isSkipTicksEnabled = MutableStateFlow(false)
    val syncNotificationEnabled = MutableStateFlow(true)

    init {
        if (context != null) {
            val prefs = runBlocking { context.debugDataStore.data.first() }
            clockOverride.value = prefs[KEY_CLOCK_OVERRIDE]
                ?.takeIf { it.isNotEmpty() }
                ?.let { runCatching { LocalTime.parse(it, TIME_FORMAT) }.getOrNull() }
            isPremiumOverrideEnabled.value = prefs[KEY_IS_PREMIUM_OVERRIDE] ?: false
            premiumOverrideValue.value = prefs[KEY_PREMIUM_OVERRIDE_VALUE] ?: true
            isSkipTicksEnabled.value = prefs[KEY_SKIP_TICKS_ENABLED] ?: false
            syncNotificationEnabled.value = prefs[KEY_SYNC_NOTIFICATION_ENABLED] ?: true
            applySyncNotificationPreference()
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

    fun setSkipTicksEnabled(enabled: Boolean) {
        isSkipTicksEnabled.value = enabled
        context?.let { ctx ->
            scope.launch {
                ctx.debugDataStore.edit { prefs ->
                    prefs[KEY_SKIP_TICKS_ENABLED] = enabled
                }
            }
        }
    }

    fun setSyncNotificationEnabled(enabled: Boolean) {
        syncNotificationEnabled.value = enabled
        val ctx = context ?: return
        scope.launch {
            ctx.debugDataStore.edit { prefs ->
                prefs[KEY_SYNC_NOTIFICATION_ENABLED] = enabled
            }
            SyncDebugController.setEnabled(ctx, enabled)
        }
    }

    fun applySyncNotificationPreference() {
        context?.let { SyncDebugController.setEnabled(it, syncNotificationEnabled.value) }
    }

    companion object {
        lateinit var instance: DebugRepository
            private set

        fun init(context: Context): DebugRepository =
            DebugRepository(context.applicationContext).also {
                instance = it
                PremiumOverride.source = it
                TimerSkipOverride.source = it
            }

        /** No-arg factory for unit tests only — no persistence. */
        fun init(): DebugRepository =
            DebugRepository().also { instance = it }
    }
}
