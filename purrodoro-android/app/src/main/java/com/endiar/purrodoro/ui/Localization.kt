package com.endiar.purrodoro.ui

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.endiar.purrodoro.model.AppLanguage
import java.util.Locale

/**
 * Applies the user-selected [AppLanguage] to the whole Compose tree without
 * an Activity restart or an AppCompat dependency.
 *
 * For an explicit language we build a [Configuration] carrying the chosen
 * [Locale] and provide a matching [LocalContext] (via
 * [android.content.Context.createConfigurationContext]) so that
 * `stringResource` / `Resources.getString` resolve against the right
 * `values-<lang>` folder. [AppLanguage.System] is a no-op — resources fall
 * back to the device configuration.
 *
 * Recomposes automatically when [language] changes because [LocalContext] and
 * [LocalConfiguration] are swapped, which `stringResource` reads.
 */
@Composable
fun KuroLocalizationProvider(
    language: AppLanguage,
    content: @Composable () -> Unit,
) {
    val tag = language.tag
    if (tag == null) {
        content()
        return
    }

    val baseConfiguration = LocalConfiguration.current
    val baseContext = LocalContext.current

    val localizedConfiguration = remember(baseConfiguration, tag) {
        Configuration(baseConfiguration).apply { setLocale(Locale.forLanguageTag(tag)) }
    }
    val localizedContext = remember(localizedConfiguration, baseContext) {
        baseContext.createConfigurationContext(localizedConfiguration)
    }

    CompositionLocalProvider(
        LocalConfiguration provides localizedConfiguration,
        LocalContext provides localizedContext,
    ) {
        content()
    }
}
