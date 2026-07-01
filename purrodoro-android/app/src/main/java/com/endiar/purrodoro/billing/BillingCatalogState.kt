package com.endiar.purrodoro.billing

enum class BillingCatalogState {
    /** No paywall fetch yet — treat like loading in premium UI. */
    Idle,
    Loading,
    Ready,
    Unavailable,
}
