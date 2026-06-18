package com.endiar.purrodoro.widget

import android.content.Context
import android.content.res.Configuration
import com.endiar.purrodoro.R
import com.endiar.purrodoro.model.AppLanguage
import java.util.Locale

/**
 * Glance widgets run outside Compose, so we resolve their copy against a
 * [Context] configured with the user-selected in-app [AppLanguage] rather than
 * the device default.
 */
internal fun localizedWidgetContext(context: Context, language: AppLanguage): Context {
    val tag = language.tag ?: return context
    val config = Configuration(context.resources.configuration).apply {
        setLocale(Locale.forLanguageTag(tag))
    }
    return context.createConfigurationContext(config)
}

internal fun Context.todayEncouragement(kind: TodayEncouragement): String = getString(
    when (kind) {
        TodayEncouragement.SoftStart -> R.string.widget_today_enc_soft
        TodayEncouragement.FirstFocus -> R.string.widget_today_enc_first
        TodayEncouragement.GoalReached -> R.string.widget_today_enc_goal
        TodayEncouragement.Proud -> R.string.widget_today_enc_proud
    }
)
