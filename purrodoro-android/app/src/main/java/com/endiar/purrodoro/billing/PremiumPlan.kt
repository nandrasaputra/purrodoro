package com.endiar.purrodoro.billing

enum class PremiumPlan(val basePlanId: String) {
    Monthly(BillingConfig.BASE_PLAN_MONTHLY),
    Yearly(BillingConfig.BASE_PLAN_YEARLY),
}
