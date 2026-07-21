package com.endiar.purrodoro.ui

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.endiar.purrodoro.R
import com.endiar.purrodoro.focusshield.FocusShieldApps
import com.endiar.purrodoro.focusshield.FocusShieldOverlayAccess
import com.endiar.purrodoro.focusshield.FocusShieldUsageAccess
import com.endiar.purrodoro.focusshield.LaunchableApp
import com.endiar.purrodoro.ui.components.KuroCard
import com.endiar.purrodoro.ui.designsystem.KuroBackScreenTopBar
import com.endiar.purrodoro.ui.designsystem.KuroDivider
import com.endiar.purrodoro.ui.designsystem.KuroSectionHeader
import com.endiar.purrodoro.ui.designsystem.KuroSpacing
import com.endiar.purrodoro.ui.designsystem.KuroType
import com.endiar.purrodoro.ui.theme.kuroTokens

@Composable
private fun focusShieldSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = MaterialTheme.colorScheme.kuroTokens.accentOn,
    checkedTrackColor = MaterialTheme.colorScheme.kuroTokens.accent,
    uncheckedThumbColor = Color.White,
    uncheckedTrackColor = MaterialTheme.colorScheme.kuroTokens.textSecondary.copy(alpha = 0.35f),
    uncheckedBorderColor = MaterialTheme.colorScheme.kuroTokens.textSecondary,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusShieldScreen(
    enabled: Boolean,
    blockedPackages: Set<String>,
    onBack: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onBlockedPackagesChange: (Set<String>) -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var usageGranted by remember { mutableStateOf(FocusShieldUsageAccess.isGranted(context)) }
    var overlayGranted by remember { mutableStateOf(FocusShieldOverlayAccess.isGranted(context)) }
    var picking by remember { mutableStateOf(false) }
    val apps = remember {
        FocusShieldApps.loadLaunchableApps(context.packageManager, context.packageName)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                usageGranted = FocusShieldUsageAccess.isGranted(context)
                overlayGranted = FocusShieldOverlayAccess.isGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (picking) {
        FocusShieldAppPicker(
            apps = apps,
            selected = blockedPackages,
            onBack = { picking = false },
            onConfirm = { selected ->
                onBlockedPackagesChange(selected)
                picking = false
            },
        )
        return
    }

    Scaffold(
        topBar = {
            KuroBackScreenTopBar(
                title = stringResource(R.string.focus_shield_title),
                onBack = onBack,
            )
        },
        containerColor = tokens.pageBackground,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = KuroSpacing.lg),
            contentPadding = PaddingValues(bottom = KuroSpacing.xxl),
        ) {
            item {
                Text(
                    text = stringResource(R.string.focus_shield_how_body),
                    style = KuroType.bodySecondary,
                    color = tokens.textSecondary,
                )
                Spacer(Modifier.height(KuroSpacing.xl))
            }

            item {
                KuroSectionHeader(title = stringResource(R.string.focus_shield_section_during_focus))
                Spacer(Modifier.height(KuroSpacing.sm))
                KuroCard(modifier = Modifier.fillMaxWidth()) {
                    val stateLabel = stringResource(
                        if (enabled) R.string.focus_shield_status_on
                        else R.string.focus_shield_status_off,
                    )
                    val detailLabel = statusSubtitle(
                        enabled = enabled,
                        usageGranted = usageGranted,
                        overlayGranted = overlayGranted,
                        blockedCount = blockedPackages.size,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val wantOn = !enabled
                                if (wantOn && !usageGranted) {
                                    FocusShieldUsageAccess.openSettings(context)
                                    return@clickable
                                }
                                if (wantOn && !overlayGranted) {
                                    FocusShieldOverlayAccess.openSettings(context)
                                    return@clickable
                                }
                                if (wantOn && blockedPackages.isEmpty()) {
                                    picking = true
                                    onEnabledChange(true)
                                    return@clickable
                                }
                                onEnabledChange(wantOn)
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_distraction_app_blocker),
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                        )
                        Spacer(Modifier.width(KuroSpacing.md))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.focus_shield_use_during_focus),
                                style = KuroType.bodyPrimary,
                                color = tokens.textPrimary,
                            )
                            Text(
                                text = detailLabel,
                                style = KuroType.bodySecondary,
                                color = tokens.textSecondary,
                            )
                        }
                        Text(
                            text = stateLabel,
                            style = KuroType.labelStrong,
                            color = if (enabled) tokens.accent else tokens.textSecondary,
                        )
                    }
                }
                if (enabled && !usageGranted) {
                    Spacer(Modifier.height(KuroSpacing.sm))
                    TextButton(onClick = { FocusShieldUsageAccess.openSettings(context) }) {
                        Text(stringResource(R.string.focus_shield_open_usage))
                    }
                }
                if (enabled && usageGranted && !overlayGranted) {
                    Spacer(Modifier.height(KuroSpacing.sm))
                    TextButton(onClick = { FocusShieldOverlayAccess.openSettings(context) }) {
                        Text(stringResource(R.string.focus_shield_open_overlay))
                    }
                }
                Spacer(Modifier.height(KuroSpacing.xl))
            }

            item {
                KuroSectionHeader(title = stringResource(R.string.focus_shield_section_apps))
                Spacer(Modifier.height(KuroSpacing.sm))
                KuroCard(modifier = Modifier.fillMaxWidth()) {
                    val blockedApps = apps.filter { it.packageName in blockedPackages }
                    if (blockedApps.isEmpty()) {
                        Text(
                            text = stringResource(R.string.focus_shield_no_apps),
                            style = KuroType.bodySecondary,
                            color = tokens.textSecondary,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        blockedApps.forEachIndexed { index, app ->
                            if (index > 0) KuroDivider()
                            AppRow(
                                app = app,
                                trailing = {
                                    TextButton(
                                        onClick = {
                                            onBlockedPackagesChange(blockedPackages - app.packageName)
                                        },
                                    ) {
                                        Text(stringResource(R.string.focus_shield_remove))
                                    }
                                },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(KuroSpacing.md))
                Button(
                    onClick = { picking = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(KuroSpacing.md),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = tokens.accent,
                        contentColor = tokens.accentOn,
                    ),
                ) {
                    Text(stringResource(R.string.focus_shield_edit_apps))
                }
            }
        }
    }
}

@Composable
private fun statusSubtitle(
    enabled: Boolean,
    usageGranted: Boolean,
    overlayGranted: Boolean,
    blockedCount: Int,
): String = when {
    !enabled -> stringResource(R.string.focus_shield_status_off_hint)
    !usageGranted -> stringResource(R.string.focus_shield_status_need_usage)
    !overlayGranted -> stringResource(R.string.focus_shield_status_need_overlay)
    blockedCount == 0 -> stringResource(R.string.focus_shield_status_need_apps)
    else -> stringResource(R.string.focus_shield_status_ready, blockedCount)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FocusShieldAppPicker(
    apps: List<LaunchableApp>,
    selected: Set<String>,
    onBack: () -> Unit,
    onConfirm: (Set<String>) -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    var draft by remember(selected) { mutableStateOf(selected) }
    val suggested = apps.filter { it.packageName in FocusShieldApps.SUGGESTED_PACKAGES }

    Scaffold(
        topBar = {
            KuroBackScreenTopBar(
                title = stringResource(R.string.focus_shield_pick_title),
                onBack = onBack,
            )
        },
        bottomBar = {
            Button(
                onClick = { onConfirm(draft) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(KuroSpacing.lg),
                shape = RoundedCornerShape(KuroSpacing.md),
                colors = ButtonDefaults.buttonColors(
                    containerColor = tokens.accent,
                    contentColor = tokens.accentOn,
                ),
            ) {
                Text(stringResource(R.string.common_ok))
            }
        },
        containerColor = tokens.pageBackground,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = KuroSpacing.lg),
            contentPadding = PaddingValues(bottom = KuroSpacing.xxl),
        ) {
            if (suggested.isNotEmpty()) {
                item {
                    KuroSectionHeader(title = stringResource(R.string.focus_shield_suggested))
                    Spacer(Modifier.height(KuroSpacing.sm))
                }
                items(suggested, key = { "s-${it.packageName}" }) { app ->
                    KuroCard(modifier = Modifier.fillMaxWidth()) {
                        AppRow(
                            app = app,
                            trailing = {
                                Switch(
                                    checked = app.packageName in draft,
                                    onCheckedChange = { checked ->
                                        draft = if (checked) draft + app.packageName
                                        else draft - app.packageName
                                    },
                                    colors = focusShieldSwitchColors(),
                                )
                            },
                        )
                    }
                    Spacer(Modifier.height(KuroSpacing.sm))
                }
                item {
                    Spacer(Modifier.height(KuroSpacing.lg))
                    KuroSectionHeader(title = stringResource(R.string.focus_shield_all_apps))
                    Spacer(Modifier.height(KuroSpacing.sm))
                }
            }
            items(apps, key = { it.packageName }) { app ->
                KuroCard(modifier = Modifier.fillMaxWidth()) {
                    AppRow(
                        app = app,
                        trailing = {
                            Switch(
                                checked = app.packageName in draft,
                                onCheckedChange = { checked ->
                                    draft = if (checked) draft + app.packageName
                                    else draft - app.packageName
                                },
                                colors = focusShieldSwitchColors(),
                            )
                        },
                    )
                }
                Spacer(Modifier.height(KuroSpacing.sm))
            }
        }
    }
}

@Composable
private fun AppRow(
    app: LaunchableApp,
    trailing: @Composable () -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KuroSpacing.md),
    ) {
        AppIcon(drawable = app.icon)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.label,
                style = KuroType.bodyPrimary,
                color = tokens.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = app.packageName,
                style = KuroType.labelMeta,
                color = tokens.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        trailing()
    }
}

@Composable
private fun AppIcon(drawable: Drawable?) {
    val bitmap = remember(drawable) {
        drawable?.toBitmap(width = 96, height = 96)?.asImageBitmap()
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
        )
    } else {
        Image(
            painter = painterResource(id = R.drawable.ic_distraction_app_blocker),
            contentDescription = null,
            modifier = Modifier.size(40.dp),
        )
    }
}
