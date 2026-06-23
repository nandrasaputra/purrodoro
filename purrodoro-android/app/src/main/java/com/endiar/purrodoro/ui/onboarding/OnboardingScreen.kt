package com.endiar.purrodoro.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.composables.icons.lucide.Bell
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.Languages
import com.composables.icons.lucide.Lucide
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import com.endiar.purrodoro.R
import com.endiar.purrodoro.auth.AccountManager
import com.endiar.purrodoro.auth.AuthRepository
import com.endiar.purrodoro.model.AppLanguage
import com.endiar.purrodoro.model.AuthState
import com.endiar.purrodoro.model.AuthUser
import com.endiar.purrodoro.ui.auth.GoogleBrandIcon
import com.endiar.purrodoro.ui.auth.LoginSheet
import com.endiar.purrodoro.ui.auth.SignedInAccountSummary
import com.endiar.purrodoro.ui.designsystem.KuroSpacing
import com.endiar.purrodoro.ui.designsystem.KuroType
import com.endiar.purrodoro.ui.theme.NunitoFamily
import com.endiar.purrodoro.ui.theme.kuroTokens
import kotlinx.coroutines.launch

private enum class OnboardingStep {
    Welcome,
    Notifications,
}

private val OnboardingHeroMaxHeight = 300.dp
private val OnboardingHeroMinHeight = 120.dp
private const val OnboardingCompactHeightDp = 640

/**
 * First-run gate: sign in with Google or continue as guest, then optional
 * notification permission. Product walkthrough is handled afterward by the
 * spotlight tips on Home.
 */
@Composable
fun OnboardingScreen(
    authRepository: AuthRepository,
    accountManager: AccountManager,
    confirmAccountSwitch: suspend () -> Boolean,
    language: AppLanguage = AppLanguage.System,
    onUpdateLanguage: (AppLanguage) -> Unit = {},
    onComplete: () -> Unit,
    onPersistUserName: (String) -> Unit = {},
    onWelcomeGuestChoice: suspend () -> Unit = {},
    onRequestNotificationPermission: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val authState by authRepository.authState.collectAsStateWithLifecycle(initialValue = AuthState.Guest)
    val isWelcomeSignedIn = authState is AuthState.SignedIn
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf(OnboardingStep.Welcome) }
    var showLoginSheet by remember { mutableStateOf(false) }
    var isGoogleSignInInProgress by remember { mutableStateOf(false) }
    var isAuthHandoffInProgress by remember { mutableStateOf(false) }
    var linkedSignInUserId by remember { mutableStateOf<String?>(null) }
    var notificationsEnabled by remember {
        mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled())
    }
    val isAuthBusy = isGoogleSignInInProgress || isAuthHandoffInProgress

    BackHandler(enabled = step == OnboardingStep.Notifications) {
        step = OnboardingStep.Welcome
    }

    DisposableEffect(lifecycleOwner, step) {
        if (step != OnboardingStep.Notifications) {
            return@DisposableEffect onDispose { }
        }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsEnabled =
                    NotificationManagerCompat.from(context).areNotificationsEnabled()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    suspend fun completeSignInHandoff(user: AuthUser): Boolean {
        if (linkedSignInUserId == user.userId) return true
        isAuthHandoffInProgress = true
        try {
            val result = accountManager.onSignedIn(
                incomingUserId = user.userId,
                confirmDrop = confirmAccountSwitch,
            )
            return when (result) {
                AccountManager.Result.DropCancelled -> {
                    authRepository.signOut()
                    linkedSignInUserId = null
                    false
                }
                else -> {
                    linkedSignInUserId = user.userId
                    user.displayName?.trim()?.takeIf { it.isNotBlank() }?.let(onPersistUserName)
                    true
                }
            }
        } finally {
            isAuthHandoffInProgress = false
        }
    }

    fun advanceAfterAuthChoice() {
        showLoginSheet = false
        step = OnboardingStep.Notifications
    }

    suspend fun finishSignedIn(user: AuthUser) {
        if (completeSignInHandoff(user)) {
            advanceAfterAuthChoice()
        } else {
            showLoginSheet = false
        }
    }

    suspend fun finishAsGuest() {
        linkedSignInUserId = null
        onWelcomeGuestChoice()
        advanceAfterAuthChoice()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(tokens.pageBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            OnboardingTopBar(
                language = language,
                onUpdateLanguage = onUpdateLanguage,
                showBack = step == OnboardingStep.Notifications,
                onBack = { step = OnboardingStep.Welcome },
            )

            OnboardingStepIndicator(
                current = step,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = KuroSpacing.sm),
            )

            when (step) {
                OnboardingStep.Welcome -> {
                    OnboardingStepLayout(
                        eyebrow = stringResource(R.string.onboarding_welcome_to),
                        brandHeadline = "Purrodoro",
                        heroMaxHeight = if (isWelcomeSignedIn) 260.dp else OnboardingHeroMaxHeight,
                        heroContent = {
                            Image(
                                painter = painterResource(id = R.drawable.cat_onboarding_pomodoro),
                                contentDescription = stringResource(R.string.onboarding_welcome_cat_cd),
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                alignment = Alignment.Center,
                            )
                        },
                        title = stringResource(R.string.onboarding_welcome_title),
                        subtitle = stringResource(
                            if (isWelcomeSignedIn) {
                                R.string.onboarding_welcome_signed_in_body
                            } else {
                                R.string.onboarding_welcome_subtitle
                            },
                        ),
                        belowSubtitle = {
                            val signedInUser = (authState as? AuthState.SignedIn)?.user
                            if (signedInUser != null) {
                                Spacer(Modifier.height(KuroSpacing.lg))
                                WelcomeSignedInCard(user = signedInUser)
                            }
                        },
                    )

                    OnboardingAuthActions(
                        welcomeSignedIn = isWelcomeSignedIn,
                        enabled = !isAuthBusy,
                        onPrimary = {
                            when {
                                isWelcomeSignedIn -> scope.launch {
                                    val user = (authState as AuthState.SignedIn).user
                                    if (completeSignInHandoff(user)) {
                                        advanceAfterAuthChoice()
                                    }
                                }
                                else -> showLoginSheet = true
                            }
                        },
                        onSecondary = { scope.launch { finishAsGuest() } },
                        modifier = onboardingBottomActionsModifier(),
                    )
                }

                OnboardingStep.Notifications -> {
                    OnboardingStepLayout(
                        eyebrow = null,
                        brandHeadline = null,
                        heroMaxHeight = OnboardingHeroMaxHeight,
                        heroContent = {
                            AndroidNotificationPreview(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(horizontal = KuroSpacing.lg),
                            )
                        },
                        title = stringResource(R.string.onboarding_track_title),
                        subtitle = stringResource(R.string.onboarding_track_subtitle),
                        belowSubtitle = null,
                    )

                    OnboardingNotificationActions(
                        notificationsEnabled = notificationsEnabled,
                        enabled = !isAuthBusy,
                        onPrimary = {
                            if (notificationsEnabled) {
                                onComplete()
                            } else {
                                onRequestNotificationPermission()
                            }
                        },
                        onNotNow = onComplete,
                        modifier = onboardingBottomActionsModifier(),
                    )
                }
            }
        }

        if (isAuthBusy) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    color = tokens.accent,
                    strokeWidth = 2.5.dp,
                )
            }
        }
    }

    if (showLoginSheet) {
        LoginSheet(
            authRepository = authRepository,
            onDismiss = {
                showLoginSheet = false
                isGoogleSignInInProgress = false
            },
            onSigningInChanged = { isGoogleSignInInProgress = it },
            onSignedIn = { user -> scope.launch { finishSignedIn(user) } },
            onContinueAsGuest = { showLoginSheet = false },
        )
    }
}

private fun onboardingBottomActionsModifier() = Modifier
    .fillMaxWidth()
    .padding(
        start = KuroSpacing.xl,
        end = KuroSpacing.xl,
        top = KuroSpacing.lg,
        bottom = KuroSpacing.lg,
    )

@Composable
private fun ColumnScope.OnboardingStepLayout(
    eyebrow: String?,
    brandHeadline: String?,
    heroMaxHeight: Dp,
    heroContent: @Composable BoxScope.() -> Unit,
    title: String,
    subtitle: String,
    belowSubtitle: @Composable (() -> Unit)? = null,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val compactHeight = LocalConfiguration.current.screenHeightDp < OnboardingCompactHeightDp
    val brandFontSize = if (compactHeight) 40.sp else 56.sp
    val brandLineHeight = if (compactHeight) 44.sp else 60.sp
    val titleFontSize = if (compactHeight) 20.sp else 24.sp
    val titleLineHeight = if (compactHeight) 24.sp else 28.sp
    val sectionSpacing = if (compactHeight) KuroSpacing.md else KuroSpacing.lg

    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(horizontal = KuroSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (!eyebrow.isNullOrBlank()) {
            Text(
                text = eyebrow,
                color = tokens.textSecondary,
                style = KuroType.labelStrong,
            )
        }
        if (!brandHeadline.isNullOrBlank()) {
            Text(
                text = brandHeadline,
                color = tokens.textPrimary,
                fontFamily = NunitoFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = brandFontSize,
                lineHeight = brandLineHeight,
                textAlign = TextAlign.Center,
            )
        }
        if (!eyebrow.isNullOrBlank() || !brandHeadline.isNullOrBlank()) {
            Spacer(Modifier.height(sectionSpacing))
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .heightIn(min = OnboardingHeroMinHeight, max = heroMaxHeight)
                .clip(RoundedCornerShape(KuroSpacing.radiusLg))
                .background(tokens.activeCardFill),
            content = heroContent,
        )

        Spacer(Modifier.height(sectionSpacing))

        Text(
            text = title,
            color = tokens.textPrimary,
            fontFamily = NunitoFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = titleFontSize,
            lineHeight = titleLineHeight,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(KuroSpacing.xs))
        Text(
            text = subtitle,
            color = tokens.textSecondary,
            style = KuroType.bodySecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 320.dp),
        )
        belowSubtitle?.invoke()
    }
}

@Composable
private fun OnboardingStepIndicator(
    current: OnboardingStep,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OnboardingStep.entries.forEach { step ->
            val active = step == current
            Box(
                modifier = Modifier
                    .padding(horizontal = KuroSpacing.xs)
                    .size(if (active) 8.dp else 6.dp)
                    .clip(CircleShape)
                    .background(
                        if (active) tokens.accent else tokens.cardBorder.copy(alpha = 0.7f),
                    ),
            )
        }
    }
}

@Composable
private fun OnboardingTopBar(
    language: AppLanguage,
    onUpdateLanguage: (AppLanguage) -> Unit,
    showBack: Boolean,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = KuroSpacing.xxxl)
            .padding(horizontal = KuroSpacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showBack) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Lucide.ChevronLeft,
                    contentDescription = stringResource(R.string.common_back),
                    tint = MaterialTheme.colorScheme.kuroTokens.textSecondary,
                )
            }
        } else {
            Spacer(Modifier.size(48.dp))
        }

        OnboardingLanguageSelector(
            language = language,
            onSelect = onUpdateLanguage,
        )
    }
}

@Composable
private fun OnboardingLanguageSelector(
    language: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val displayLabel = onboardingTopBarLanguageLabel(language)
    var expanded by remember { mutableStateOf(false) }

    Box {
        TextButton(onClick = { expanded = true }) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(KuroSpacing.xs),
            ) {
                Icon(
                    imageVector = Lucide.Languages,
                    contentDescription = stringResource(R.string.settings_language_label),
                    tint = tokens.textSecondary,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = displayLabel,
                    color = tokens.textSecondary,
                    style = KuroType.labelStrong,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 140.dp),
                )
                Icon(
                    imageVector = Lucide.ChevronDown,
                    contentDescription = null,
                    tint = tokens.textMuted,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            AppLanguage.entries.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = onboardingPickerLanguageLabel(option),
                            style = KuroType.bodyPrimary,
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                )
            }
        }
    }
}

private fun onboardingTopBarLanguageLabel(language: AppLanguage): String = when (language) {
    AppLanguage.System -> AppLanguage.resolvedSystemLocaleName()
    else -> language.label
}

@Composable
private fun onboardingPickerLanguageLabel(option: AppLanguage): String = when (option) {
    AppLanguage.System -> stringResource(
        R.string.settings_language_system_picker,
        AppLanguage.resolvedSystemLocaleName(),
    )
    else -> option.label
}

@Composable
private fun WelcomeSignedInCard(user: AuthUser) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 360.dp)
            .clip(RoundedCornerShape(KuroSpacing.radiusLg))
            .background(tokens.cardFill)
            .border(1.dp, tokens.accent.copy(alpha = 0.28f), RoundedCornerShape(KuroSpacing.radiusLg))
            .padding(KuroSpacing.md),
        verticalArrangement = Arrangement.spacedBy(KuroSpacing.md),
    ) {
        Text(
            text = stringResource(R.string.onboarding_welcome_signed_in_heading),
            color = tokens.accent,
            style = KuroType.labelStrong,
        )
        SignedInAccountSummary(user = user)
    }
}

/**
 * Collapsed Android notification row: launcher icon, header line, title, body, expand affordance.
 */
@Composable
private fun AndroidNotificationPreview(modifier: Modifier = Modifier) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val surfaceColor = tokens.cardFill
    val headerColor = tokens.textMuted
    val titleColor = tokens.textPrimary
    val bodyColor = tokens.textSecondary
    val shape = RoundedCornerShape(24.dp)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.12f),
                spotColor = Color.Black.copy(alpha = 0.12f),
            ),
        shape = shape,
        color = surfaceColor,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = KuroSpacing.md, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape),
            ) {
                Image(
                    painter = painterResource(R.mipmap.ic_launcher_background),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Image(
                    painter = painterResource(R.mipmap.ic_launcher_foreground),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_notif_meta),
                        color = headerColor,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = Lucide.ChevronDown,
                        contentDescription = null,
                        tint = headerColor,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.onboarding_notif_title),
                    color = titleColor,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.onboarding_notif_body),
                    color = bodyColor,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun OnboardingAuthActions(
    welcomeSignedIn: Boolean,
    enabled: Boolean,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val showGoogleSignIn = !welcomeSignedIn
    val primaryLabel = if (welcomeSignedIn) {
        stringResource(R.string.onboarding_cta_next)
    } else {
        stringResource(R.string.onboarding_cta_sign_in)
    }
    val secondaryLabel = if (welcomeSignedIn) {
        stringResource(R.string.onboarding_cta_use_guest_instead)
    } else {
        stringResource(R.string.onboarding_cta_continue_as_guest)
    }

    OnboardingBottomActions(
        modifier = modifier,
        primaryLabel = primaryLabel,
        secondaryLabel = secondaryLabel,
        enabled = enabled,
        onPrimary = onPrimary,
        onSecondary = onSecondary,
        primaryLeading = if (showGoogleSignIn) {
            {
                GoogleBrandIcon(
                    modifier = Modifier.size(20.dp),
                    onColoredButton = true,
                )
                Spacer(Modifier.width(KuroSpacing.sm))
            }
        } else {
            null
        },
        primaryColors = ButtonDefaults.buttonColors(
            containerColor = tokens.interactiveFill,
            contentColor = tokens.interactiveText,
        ),
    )
}

@Composable
private fun OnboardingNotificationActions(
    notificationsEnabled: Boolean,
    enabled: Boolean,
    onPrimary: () -> Unit,
    onNotNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val primaryLabel = if (notificationsEnabled) {
        stringResource(R.string.onboarding_cta_get_started)
    } else {
        stringResource(R.string.onboarding_cta_turn_on_notifications)
    }

    OnboardingBottomActions(
        modifier = modifier,
        primaryLabel = primaryLabel,
        secondaryLabel = stringResource(R.string.onboarding_cta_not_now),
        enabled = enabled,
        onPrimary = onPrimary,
        onSecondary = onNotNow,
        primaryColors = ButtonDefaults.buttonColors(
            containerColor = tokens.interactiveFill,
            contentColor = tokens.interactiveText,
        ),
    )
}

@Composable
private fun OnboardingBottomActions(
    primaryLabel: String,
    secondaryLabel: String,
    enabled: Boolean,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
    modifier: Modifier = Modifier,
    primaryLeading: (@Composable () -> Unit)? = null,
    primaryColors: androidx.compose.material3.ButtonColors,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(
            onClick = onPrimary,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(KuroSpacing.radiusPill),
            colors = primaryColors,
        ) {
            if (primaryLeading != null) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    primaryLeading()
                    Text(
                        text = primaryLabel,
                        style = KuroType.bodyPrimary.copy(fontWeight = FontWeight.SemiBold),
                    )
                }
            } else {
                Text(
                    text = primaryLabel,
                    style = KuroType.bodyPrimary.copy(fontWeight = FontWeight.SemiBold),
                )
            }
        }

        Spacer(Modifier.height(KuroSpacing.sm))
        OutlinedButton(
            onClick = onSecondary,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(KuroSpacing.radiusPill),
            border = BorderStroke(1.dp, tokens.accent),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = tokens.pageBackground,
                contentColor = tokens.accent,
            ),
        ) {
            Text(
                text = secondaryLabel,
                style = KuroType.bodyPrimary.copy(fontWeight = FontWeight.SemiBold),
            )
        }
    }
}
