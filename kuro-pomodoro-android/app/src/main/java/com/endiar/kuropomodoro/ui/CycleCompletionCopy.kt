package com.endiar.kuropomodoro.ui

import androidx.annotation.StringRes
import com.endiar.kuropomodoro.R

/**
 * Title/body copy variant for the cycle-completion dialog, stored as string
 * resource ids so the chosen variant follows the active app language. The
 * caller picks one variant (stably remembered) and resolves the ids with
 * `stringResource` inside composition.
 */
data class CycleCompletionCopyVariant(
    @StringRes val titleRes: Int,
    @StringRes val bodyRes: Int,
)

object CycleCompletionCopy {
    @StringRes
    val buttonLabelRes: Int = R.string.cycle_done

    val variants: List<CycleCompletionCopyVariant> = listOf(
        CycleCompletionCopyVariant(R.string.cycle_v1_title, R.string.cycle_v1_body),
        CycleCompletionCopyVariant(R.string.cycle_v2_title, R.string.cycle_v2_body),
        CycleCompletionCopyVariant(R.string.cycle_v3_title, R.string.cycle_v3_body),
        CycleCompletionCopyVariant(R.string.cycle_v4_title, R.string.cycle_v4_body),
        CycleCompletionCopyVariant(R.string.cycle_v5_title, R.string.cycle_v5_body),
    )
}
