package com.endiar.kuropomodoro.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.draw.scale
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Bell
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Clock
import com.composables.icons.lucide.Coffee
import com.composables.icons.lucide.Info
import com.composables.icons.lucide.Languages
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.RefreshCw
import com.composables.icons.lucide.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.endiar.kuropomodoro.R
import com.endiar.kuropomodoro.model.AppLanguage
import com.endiar.kuropomodoro.model.AppearanceMode
import com.endiar.kuropomodoro.model.TimerSettings
import com.endiar.kuropomodoro.ui.components.KuroCard
import com.endiar.kuropomodoro.ui.designsystem.KuroAdaptiveBottomSheet
import com.endiar.kuropomodoro.ui.designsystem.KuroDivider
import com.endiar.kuropomodoro.ui.designsystem.KuroSectionHeader
import com.endiar.kuropomodoro.ui.designsystem.KuroSpacing
import com.endiar.kuropomodoro.ui.designsystem.KuroStepper
import com.endiar.kuropomodoro.ui.designsystem.KuroType
import com.endiar.kuropomodoro.ui.designsystem.pressScale
import com.endiar.kuropomodoro.ui.theme.NunitoFamily
import com.endiar.kuropomodoro.ui.theme.kuroTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    settings: TimerSettings,
    onBack: () -> Unit,
    onUpdateSettings: (TimerSettings) -> Unit,
    notificationSoundEnabled: Boolean,
    onUpdateNotificationSound: (Boolean) -> Unit,
    appearanceMode: AppearanceMode = AppearanceMode.Auto,
    onUpdateAppearanceMode: (AppearanceMode) -> Unit = {},
    language: AppLanguage = AppLanguage.System,
    onUpdateLanguage: (AppLanguage) -> Unit = {},
    onRequestEnableNotifications: () -> Unit = {},
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    var editingField by remember { mutableStateOf<String?>(null) }
    var activePicker by remember { mutableStateOf<PreferencePicker?>(null) }

    // Detect whether the user has actually granted notifications. We re-check on
    // every ON_RESUME so the gentle Kuro reminder disappears the moment the user
    // returns from the system permission dialog or the OS settings screen.
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var notificationsEnabled by remember {
        mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled())
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsEnabled =
                    NotificationManagerCompat.from(context).areNotificationsEnabled()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Lucide.ArrowLeft,
                            contentDescription = stringResource(R.string.common_back),
                            tint = tokens.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = tokens.pageBackground,
                    scrolledContainerColor = tokens.pageBackground
                )
            )
        },
        containerColor = tokens.pageBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = KuroSpacing.lg)
        ) {
            Spacer(modifier = Modifier.height(KuroSpacing.xs))
            Text(
                text = stringResource(R.string.settings_title),
                style = KuroType.headingSettings,
                color = tokens.textPrimary,
            )
            Text(
                text = stringResource(R.string.settings_subtitle),
                style = KuroType.labelStrong,
                color = tokens.textSecondary,
                modifier = Modifier.padding(top = KuroSpacing.xs)
            )
            Spacer(modifier = Modifier.height(KuroSpacing.xl))

            KuroSectionHeader(title = stringResource(R.string.settings_section_timer))
            Spacer(modifier = Modifier.height(KuroSpacing.sm))
            KuroCard(modifier = Modifier.fillMaxWidth()) {
                TappableRow(
                    icon = Lucide.Clock,
                    label = stringResource(R.string.settings_focus_time),
                    value = stringResource(R.string.value_minutes, settings.focusMinutes),
                    onClick = { editingField = "focus" },
                )
                KuroDivider()
                TappableRow(
                    icon = Lucide.Coffee,
                    label = stringResource(R.string.settings_short_break),
                    value = stringResource(R.string.value_minutes, settings.shortBreakMinutes),
                    onClick = { editingField = "short" },
                )
                KuroDivider()
                TappableRow(
                    icon = Lucide.Coffee,
                    label = stringResource(R.string.settings_long_break),
                    value = stringResource(R.string.value_minutes, settings.longBreakMinutes),
                    onClick = { editingField = "long" },
                )
                KuroDivider()
                TappableRow(
                    icon = Lucide.RefreshCw,
                    label = stringResource(R.string.settings_long_break_after),
                    value = stringResource(R.string.value_sessions, settings.focusSessionsPerCycle),
                    onClick = { editingField = "cycle" },
                )
                KuroDivider()
                ToggleRow(
                    icon = Lucide.Play,
                    label = stringResource(R.string.settings_auto_start_label),
                    subtitle = stringResource(R.string.settings_auto_start_subtitle),
                    checked = settings.autoStartNextSession,
                    onCheckedChange = { onUpdateSettings(settings.copy(autoStartNextSession = it)) },
                )
            }

            Spacer(modifier = Modifier.height(KuroSpacing.xl))

            KuroSectionHeader(title = stringResource(R.string.settings_section_notifications))
            Spacer(modifier = Modifier.height(KuroSpacing.sm))
            if (!notificationsEnabled) {
                NotificationPermissionHero(onAllow = onRequestEnableNotifications)
                Spacer(modifier = Modifier.height(KuroSpacing.md))
            }
            KuroCard(modifier = Modifier.fillMaxWidth()) {
                ToggleRow(
                    icon = Lucide.Bell,
                    label = stringResource(R.string.settings_notif_sound_label),
                    subtitle = stringResource(R.string.settings_notif_sound_subtitle),
                    checked = notificationSoundEnabled,
                    onCheckedChange = onUpdateNotificationSound,
                )
            }

            Spacer(modifier = Modifier.height(KuroSpacing.xl))

            KuroSectionHeader(title = stringResource(R.string.settings_section_appearance))
            Spacer(modifier = Modifier.height(KuroSpacing.sm))
            KuroCard(modifier = Modifier.fillMaxWidth()) {
                TappableRow(
                    icon = Lucide.Settings,
                    label = stringResource(R.string.settings_theme_label),
                    value = appearanceModeLabel(appearanceMode),
                    onClick = { activePicker = PreferencePicker.Theme },
                )
            }

            Spacer(modifier = Modifier.height(KuroSpacing.xl))

            KuroSectionHeader(title = stringResource(R.string.settings_section_language))
            Spacer(modifier = Modifier.height(KuroSpacing.sm))
            KuroCard(modifier = Modifier.fillMaxWidth()) {
                TappableRow(
                    icon = Lucide.Languages,
                    label = stringResource(R.string.settings_language_label),
                    value = languageLabel(
                        language = language,
                        systemLabel = stringResource(R.string.settings_language_system),
                    ),
                    onClick = { activePicker = PreferencePicker.Language },
                )
            }

            Spacer(modifier = Modifier.height(KuroSpacing.xl))

            KuroSectionHeader(title = stringResource(R.string.settings_section_about))
            Spacer(modifier = Modifier.height(KuroSpacing.sm))
            KuroCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Lucide.Info,
                            contentDescription = null,
                            tint = tokens.accent,
                            modifier = Modifier.size(KuroSpacing.lg),
                        )
                        Spacer(modifier = Modifier.width(KuroSpacing.md))
                        Text(text = stringResource(R.string.app_name), style = KuroType.bodyPrimary, color = tokens.textPrimary)
                    }
                    Text(text = stringResource(R.string.settings_version), style = KuroType.bodySecondary, color = tokens.textSecondary)
                }
            }

            Spacer(modifier = Modifier.height(KuroSpacing.xxl))
        }
    }

    when (editingField) {
        "focus" -> NumericEditDialog(
            title = stringResource(R.string.settings_focus_time),
            value = settings.focusMinutes,
            min = 1, max = 120, unit = stringResource(R.string.unit_min),
            onConfirm = { onUpdateSettings(settings.copy(focusMinutes = it)); editingField = null },
            onDismiss = { editingField = null }
        )
        "short" -> NumericEditDialog(
            title = stringResource(R.string.settings_short_break),
            value = settings.shortBreakMinutes,
            min = 1, max = 120, unit = stringResource(R.string.unit_min),
            onConfirm = { onUpdateSettings(settings.copy(shortBreakMinutes = it)); editingField = null },
            onDismiss = { editingField = null }
        )
        "long" -> NumericEditDialog(
            title = stringResource(R.string.settings_long_break),
            value = settings.longBreakMinutes,
            min = 1, max = 120, unit = stringResource(R.string.unit_min),
            onConfirm = { onUpdateSettings(settings.copy(longBreakMinutes = it)); editingField = null },
            onDismiss = { editingField = null }
        )
        "cycle" -> NumericEditDialog(
            title = stringResource(R.string.settings_long_break_after),
            value = settings.focusSessionsPerCycle,
            min = 1, max = TimerSettings.MAX_FOCUS_SESSIONS_PER_CYCLE, unit = stringResource(R.string.unit_sessions),
            onConfirm = { onUpdateSettings(settings.copy(focusSessionsPerCycle = it)); editingField = null },
            onDismiss = { editingField = null }
        )
    }

    when (activePicker) {
        PreferencePicker.Theme -> AppearanceModePickerSheet(
            selected = appearanceMode,
            onSelect = {
                onUpdateAppearanceMode(it)
                activePicker = null
            },
            onDismiss = { activePicker = null },
        )
        PreferencePicker.Language -> LanguagePickerSheet(
            selected = language,
            onSelect = {
                onUpdateLanguage(it)
                activePicker = null
            },
            onDismiss = { activePicker = null },
        )
        null -> Unit
    }
}

private enum class PreferencePicker { Theme, Language }

@Composable
private fun appearanceModeLabel(mode: AppearanceMode): String = when (mode) {
    AppearanceMode.Light -> stringResource(R.string.theme_light)
    AppearanceMode.Dark -> stringResource(R.string.theme_dark)
    AppearanceMode.Auto -> stringResource(R.string.theme_auto)
}

private fun languageLabel(language: AppLanguage, systemLabel: String): String =
    if (language == AppLanguage.System) systemLabel else language.label

@Composable
private fun TappableRow(icon: ImageVector, label: String, value: String, onClick: () -> Unit) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = KuroSpacing.md, vertical = KuroSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tokens.accent, modifier = Modifier.size(KuroSpacing.lg))
        Spacer(modifier = Modifier.width(KuroSpacing.md))
        Text(text = label, style = KuroType.bodyPrimary, color = tokens.textPrimary, modifier = Modifier.weight(1f))
        Text(text = value, style = KuroType.bodySecondary, color = tokens.textSecondary)
        Spacer(modifier = Modifier.width(KuroSpacing.xs))
        Text(text = "›", style = KuroType.chevron, color = tokens.textSecondary)
    }
}

@Composable
private fun ToggleRow(icon: ImageVector, label: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = KuroSpacing.md, vertical = KuroSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tokens.accent, modifier = Modifier.size(KuroSpacing.lg))
        Spacer(modifier = Modifier.width(KuroSpacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = KuroType.bodyPrimary, color = tokens.textPrimary)
            Text(text = subtitle, style = KuroType.labelMeta, color = tokens.textSecondary)
        }
        Spacer(modifier = Modifier.width(KuroSpacing.sm))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = tokens.accentOn,
                checkedTrackColor = tokens.accent,
                uncheckedThumbColor = tokens.accentOn,
                uncheckedTrackColor = tokens.cardBorder,
                uncheckedBorderColor = tokens.cardBorder,
            ),
        )
    }
}

/**
 * Prominent, mascot-led reminder shown above the Notifications card when the OS
 * notification permission is off. A warm hero card: Kuro looking up hopefully,
 * a serif headline, body copy, and a full-width "Turn on notifications" button.
 * Auto-hidden by the caller the moment notifications become enabled.
 */
@Composable
private fun NotificationPermissionHero(onAllow: () -> Unit) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(KuroSpacing.radiusLg))
            .background(
                Brush.verticalGradient(listOf(tokens.activeCardFill, tokens.accentSoft))
            )
            .padding(horizontal = KuroSpacing.lg, vertical = KuroSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(id = R.drawable.cat_notify_hero),
            contentDescription = stringResource(R.string.settings_notif_permission_cd),
            contentScale = ContentScale.Fit,
            modifier = Modifier.height(KuroSpacing.habitMotivationCat),
        )
        Spacer(modifier = Modifier.height(KuroSpacing.md))
        Text(
            text = stringResource(R.string.settings_notif_hero_title),
            style = KuroType.headingCard,
            fontFamily = NunitoFamily,
            fontWeight = FontWeight.SemiBold,
            color = tokens.textPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(KuroSpacing.xs))
        Text(
            text = stringResource(R.string.settings_notif_hero_body),
            style = KuroType.bodySecondary,
            color = tokens.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 320.dp),
        )
        Spacer(modifier = Modifier.height(KuroSpacing.lg))
        Button(
            onClick = onAllow,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(KuroSpacing.radiusPill),
            colors = ButtonDefaults.buttonColors(
                containerColor = tokens.interactiveFill,
                contentColor = tokens.interactiveText,
            ),
        ) {
            Text(
                text = stringResource(R.string.settings_notif_hero_action),
                style = KuroType.bodyPrimary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun AppearanceModePickerSheet(
    selected: AppearanceMode,
    onSelect: (AppearanceMode) -> Unit,
    onDismiss: () -> Unit,
) {
    PickerBottomSheet(
        title = stringResource(R.string.settings_theme_label),
        onDismiss = onDismiss,
    ) {
        AppearanceMode.entries.forEach { mode ->
            PickerOptionRow(
                label = appearanceModeLabel(mode),
                selected = mode == selected,
                onClick = { onSelect(mode) },
            )
        }
    }
}

@Composable
private fun LanguagePickerSheet(
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    onDismiss: () -> Unit,
) {
    val systemLabel = stringResource(R.string.settings_language_system)
    PickerBottomSheet(
        title = stringResource(R.string.settings_language_label),
        onDismiss = onDismiss,
    ) {
        AppLanguage.entries.forEach { language ->
            PickerOptionRow(
                label = languageLabel(language, systemLabel),
                selected = language == selected,
                onClick = { onSelect(language) },
            )
        }
        Text(
            text = stringResource(R.string.settings_language_restart_hint),
            style = KuroType.labelMeta,
            color = MaterialTheme.colorScheme.kuroTokens.textSecondary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = KuroSpacing.xs),
        )
    }
}

@Composable
private fun PickerBottomSheet(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    KuroAdaptiveBottomSheet(
        onDismiss = onDismiss,
        showCloseButton = true,
        horizontalPadding = KuroSpacing.lg,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = KuroSpacing.xs, bottom = KuroSpacing.xxl),
            verticalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
        ) {
            Text(
                text = title,
                style = KuroType.headingCard,
                color = tokens.textPrimary,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(KuroSpacing.xs))
            content()
        }
    }
}

@Composable
private fun PickerOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val interactionSource = remember { MutableInteractionSource() }
    // State feedback = colour crossfade + a check that pops in. The fill/text
    // ease between fully-opaque tokens (clean, no muddy mid-tones) on a calm
    // 240ms emphasized curve; the check uses a light overshoot spring so the
    // selection lands with a small, satisfying "tick".
    val selectEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) tokens.interactiveFill else tokens.segmentedTrack,
        animationSpec = tween(durationMillis = 240, easing = selectEasing),
        label = "pickerBackground",
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) tokens.interactiveText else tokens.textPrimary,
        animationSpec = tween(durationMillis = 200, easing = selectEasing),
        label = "pickerText",
    )
    val checkScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "pickerCheckScale",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(interactionSource, pressedScale = 0.98f)
            .clip(RoundedCornerShape(14.dp))
            .background(backgroundColor)
            .selectable(
                selected = selected,
                interactionSource = interactionSource,
                indication = null,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(horizontal = KuroSpacing.md, vertical = KuroSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = KuroType.bodyPrimary,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = textColor,
            modifier = Modifier.weight(1f),
        )
        // Reserve the trailing slot so the row never reflows; the glyph pops in.
        Box(modifier = Modifier.size(18.dp), contentAlignment = Alignment.Center) {
            if (checkScale > 0.01f) {
                Icon(
                    imageVector = Lucide.Check,
                    contentDescription = null,
                    tint = tokens.interactiveText,
                    modifier = Modifier
                        .size(18.dp)
                        .scale(checkScale),
                )
            }
        }
    }
}

@Composable
private fun NumericEditDialog(
    title: String, value: Int, min: Int, max: Int, unit: String,
    onConfirm: (Int) -> Unit, onDismiss: () -> Unit
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    var current by remember(value) { mutableIntStateOf(value) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = tokens.pageBackground,
        title = { Text(text = title, style = KuroType.headingCard, color = tokens.textPrimary) },
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth(),
            ) {
                KuroStepper(label = "−", enabled = current > min, large = true, onClick = { if (current > min) current-- })
                Spacer(modifier = Modifier.width(KuroSpacing.md))
                Text(text = "$current $unit", style = KuroType.headingCard, color = tokens.textPrimary)
                Spacer(modifier = Modifier.width(KuroSpacing.md))
                KuroStepper(label = "+", enabled = current < max, large = true, onClick = { if (current < max) current++ })
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(current) }) {
                Text(stringResource(R.string.common_ok), color = tokens.accent, style = KuroType.labelStrong)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel), color = tokens.textSecondary)
            }
        }
    )
}
