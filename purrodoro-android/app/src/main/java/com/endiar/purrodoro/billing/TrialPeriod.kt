package com.endiar.purrodoro.billing

enum class TrialPeriodUnit {
    Day,
    Week,
    Month,
    Year,
}

/** Trial length parsed from Play [ProductDetails] pricing phase billing period. */
data class TrialPeriod(
    val count: Int,
    val unit: TrialPeriodUnit,
)

/**
 * Parses ISO 8601 billing periods from Play (e.g. `P1W`, `P7D`, `P1M`).
 * [billingCycleCount] multiplies the parsed amount when Play specifies multiple cycles.
 */
internal fun parseBillingPeriod(
    billingPeriod: String,
    billingCycleCount: Int = 1,
): TrialPeriod? {
    if (billingPeriod.isBlank()) return null
    val match = BILLING_PERIOD_PATTERN.matchEntire(billingPeriod.trim()) ?: return null
    val amount = match.groupValues[1].toIntOrNull() ?: return null
    val unit = when (match.groupValues[2]) {
        "D" -> TrialPeriodUnit.Day
        "W" -> TrialPeriodUnit.Week
        "M" -> TrialPeriodUnit.Month
        "Y" -> TrialPeriodUnit.Year
        else -> return null
    }
    val cycles = billingCycleCount.coerceAtLeast(1)
    return TrialPeriod(count = amount * cycles, unit = unit)
}

private val BILLING_PERIOD_PATTERN = Regex("""P(\d+)([DWMY])""", RegexOption.IGNORE_CASE)
