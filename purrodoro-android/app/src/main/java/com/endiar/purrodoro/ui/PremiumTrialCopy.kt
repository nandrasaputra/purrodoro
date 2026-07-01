package com.endiar.purrodoro.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.endiar.purrodoro.R
import com.endiar.purrodoro.billing.TrialPeriod
import com.endiar.purrodoro.billing.TrialPeriodUnit

/** Localized duration label from Play billing period (e.g. "1 week", "7 days"). */
@Composable
fun formatTrialPeriodLabel(period: TrialPeriod?): String? {
    period ?: return null
    return when (period.unit) {
        TrialPeriodUnit.Day -> pluralStringResource(
            R.plurals.premium_trial_period_days,
            period.count,
            period.count,
        )
        TrialPeriodUnit.Week -> pluralStringResource(
            R.plurals.premium_trial_period_weeks,
            period.count,
            period.count,
        )
        TrialPeriodUnit.Month -> pluralStringResource(
            R.plurals.premium_trial_period_months,
            period.count,
            period.count,
        )
        TrialPeriodUnit.Year -> pluralStringResource(
            R.plurals.premium_trial_period_years,
            period.count,
            period.count,
        )
    }
}

@Composable
fun trialPillText(period: TrialPeriod?): String {
    val duration = formatTrialPeriodLabel(period)
        ?: return stringResource(R.string.premium_trial_fallback)
    return stringResource(R.string.premium_plan_trial_pill, duration)
}

@Composable
fun trialFreeForText(period: TrialPeriod?): String? {
    val duration = formatTrialPeriodLabel(period) ?: return null
    return stringResource(R.string.premium_plan_free_for, duration)
}

@Composable
fun savingsVsMonthlyCaption(percent: Int): String =
    stringResource(R.string.premium_plan_save_vs_monthly, percent)

@Composable
fun trialBadgeText(period: TrialPeriod?): String {
    val duration = formatTrialPeriodLabel(period)
        ?: return stringResource(R.string.premium_trial_fallback)
    return stringResource(R.string.premium_plan_trial_badge, duration)
}

@Composable
fun trialSublineText(period: TrialPeriod?): String? {
    val duration = formatTrialPeriodLabel(period) ?: return null
    return stringResource(R.string.premium_screen_trial_subline, duration)
}

@Composable
fun trialStickyYearlySummary(period: TrialPeriod?): String {
    val duration = formatTrialPeriodLabel(period)
        ?: return stringResource(R.string.premium_sticky_plan_yearly_trial_fallback)
    return stringResource(R.string.premium_sticky_plan_yearly_trial, duration)
}
