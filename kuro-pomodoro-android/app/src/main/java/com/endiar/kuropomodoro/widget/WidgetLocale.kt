package com.endiar.kuropomodoro.widget

import android.content.Context
import android.content.res.Configuration
import com.endiar.kuropomodoro.R
import com.endiar.kuropomodoro.model.AppLanguage
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

internal fun Context.quickStartTitle(mood: QuickStartMood): String = getString(
    when (mood) {
        QuickStartMood.Ready -> R.string.widget_qs_ready_title
        QuickStartMood.Focusing -> R.string.widget_qs_focusing_title
        QuickStartMood.Waiting -> R.string.widget_qs_waiting_title
        QuickStartMood.Celebrating -> R.string.widget_qs_celebrating_title
    }
)

internal fun Context.quickStartSubtitle(mood: QuickStartMood): String = getString(
    when (mood) {
        QuickStartMood.Ready -> R.string.widget_qs_ready_subtitle
        QuickStartMood.Focusing -> R.string.widget_qs_focusing_subtitle
        QuickStartMood.Waiting -> R.string.widget_qs_waiting_subtitle
        QuickStartMood.Celebrating -> R.string.widget_qs_celebrating_subtitle
    }
)

internal fun Context.quickStartAction(mood: QuickStartMood): String = getString(
    when (mood) {
        QuickStartMood.Ready -> R.string.widget_qs_ready_action
        QuickStartMood.Focusing -> R.string.widget_qs_focusing_action
        QuickStartMood.Waiting -> R.string.widget_qs_waiting_action
        QuickStartMood.Celebrating -> R.string.widget_qs_celebrating_action
    }
)

internal fun Context.todayEncouragement(kind: TodayEncouragement): String = getString(
    when (kind) {
        TodayEncouragement.SoftStart -> R.string.widget_today_enc_soft
        TodayEncouragement.FirstFocus -> R.string.widget_today_enc_first
        TodayEncouragement.GoalReached -> R.string.widget_today_enc_goal
        TodayEncouragement.Proud -> R.string.widget_today_enc_proud
    }
)
