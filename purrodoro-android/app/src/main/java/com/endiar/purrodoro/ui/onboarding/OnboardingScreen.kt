package com.endiar.purrodoro.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.Languages
import com.composables.icons.lucide.Lucide
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

/**
 * First-run gate: sign in with Google or continue as guest. Product walkthrough
 * is handled afterward by the spotlight tips on Home.
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
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val authState by authRepository.authState.collectAsStateWithLifecycle(initialValue = AuthState.Guest)
    val isWelcomeSignedIn = authState is AuthState.SignedIn
    val scope = rememberCoroutineScope()
    var showLoginSheet by remember { mutableStateOf(false) }
    var isGoogleSignInInProgress by remember { mutableStateOf(false) }
    var isAuthHandoffInProgress by remember { mutableStateOf(false) }
    var linkedSignInUserId by remember { mutableStateOf<String?>(null) }
    val isAuthBusy = isGoogleSignInInProgress || isAuthHandoffInProgress

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

    suspend fun finishSignedIn(user: AuthUser) {
        if (completeSignInHandoff(user)) {
            showLoginSheet = false
            onComplete()
        } else {
            showLoginSheet = false
        }
    }

    suspend fun finishAsGuest() {
        linkedSignInUserId = null
        onWelcomeGuestChoice()
        onComplete()
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
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = KuroSpacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                WelcomePage(
                    signedInUser = (authState as? AuthState.SignedIn)?.user,
                )
            }

            OnboardingAuthActions(
                welcomeSignedIn = isWelcomeSignedIn,
                enabled = !isAuthBusy,
                onPrimary = {
                    when {
                        isWelcomeSignedIn -> scope.launch {
                            val user = (authState as AuthState.SignedIn).user
                            if (completeSignInHandoff(user)) {
                                onComplete()
                            }
                        }
                        else -> showLoginSheet = true
                    }
                },
                onSecondary = { scope.launch { finishAsGuest() } },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = KuroSpacing.xl,
                        end = KuroSpacing.xl,
                        bottom = KuroSpacing.lg,
                    ),
            )
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

@Composable
private fun OnboardingTopBar(
    language: AppLanguage,
    onUpdateLanguage: (AppLanguage) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = KuroSpacing.xxxl)
            .padding(horizontal = KuroSpacing.md),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
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
private fun WelcomePage(signedInUser: AuthUser?) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val isSignedIn = signedInUser != null
    Text(
        text = stringResource(R.string.onboarding_welcome_to),
        color = tokens.textSecondary,
        style = KuroType.labelStrong,
    )
    Text(
        text = "Purrodoro",
        color = tokens.textPrimary,
        fontFamily = NunitoFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 56.sp,
        lineHeight = 60.sp,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(KuroSpacing.lg))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isSignedIn) 260.dp else 340.dp)
            .clip(RoundedCornerShape(KuroSpacing.radiusLg))
            .background(tokens.activeCardFill),
    ) {
        Image(
            painter = painterResource(id = R.drawable.cat_onboarding_pomodoro),
            contentDescription = stringResource(R.string.onboarding_welcome_cat_cd),
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center,
        )
    }

    Spacer(Modifier.height(if (isSignedIn) KuroSpacing.md else KuroSpacing.lg))

    Text(
        text = stringResource(R.string.onboarding_welcome_title),
        color = tokens.textPrimary,
        fontFamily = NunitoFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 28.sp,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(KuroSpacing.xs))
    Text(
        text = stringResource(
            if (isSignedIn) {
                R.string.onboarding_welcome_signed_in_body
            } else {
                R.string.onboarding_welcome_subtitle
            },
        ),
        color = tokens.textSecondary,
        style = KuroType.bodySecondary,
        textAlign = TextAlign.Center,
        modifier = Modifier.widthIn(max = 320.dp),
    )
    if (signedInUser != null) {
        Spacer(Modifier.height(KuroSpacing.lg))
        WelcomeSignedInCard(user = signedInUser)
    }
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
        stringResource(R.string.onboarding_cta_continue)
    } else {
        stringResource(R.string.onboarding_cta_sign_in)
    }
    val secondaryLabel = if (welcomeSignedIn) {
        stringResource(R.string.onboarding_cta_use_guest_instead)
    } else {
        stringResource(R.string.onboarding_cta_continue_as_guest)
    }

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
            colors = ButtonDefaults.buttonColors(
                containerColor = tokens.interactiveFill,
                contentColor = tokens.interactiveText,
            ),
        ) {
            if (showGoogleSignIn) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GoogleBrandIcon(
                        modifier = Modifier.size(20.dp),
                        onColoredButton = true,
                    )
                    Spacer(Modifier.width(KuroSpacing.sm))
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
