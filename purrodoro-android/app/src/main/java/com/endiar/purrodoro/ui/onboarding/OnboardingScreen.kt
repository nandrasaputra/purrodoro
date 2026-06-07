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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.CircleCheck
import com.composables.icons.lucide.Languages
import com.composables.icons.lucide.Leaf
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.NotebookPen
import com.composables.icons.lucide.Square
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.endiar.purrodoro.R
import com.endiar.purrodoro.auth.AuthRepository
import com.endiar.purrodoro.model.AppLanguage
import com.endiar.purrodoro.ui.auth.LoginSheet
import com.endiar.purrodoro.ui.designsystem.KuroSpacing
import com.endiar.purrodoro.ui.designsystem.KuroType
import com.endiar.purrodoro.ui.theme.NunitoFamily
import com.endiar.purrodoro.ui.theme.kuroTokens
import kotlinx.coroutines.launch

private const val PAGE_COUNT = 5

/** Max characters accepted for the onboarding nickname. */
private const val NAME_MAX_LENGTH = 24

/**
 * First-run onboarding. Five pages, theme-native:
 *
 *  0. Welcome     — mascot moment, primary CTA "Get started"
 *  1. Name        — optional nickname, CTA "Next"
 *  2. Intent      — show the four FocusType tiles, CTA "Next"
 *  3. Rhythm      — illustrative week preview, CTA "Next"
 *  4. Stay on track — soft notification ask, CTA "Turn on notifications"
 *
 * The screen owns only the in-progress nickname; final persistence is the
 * host's job. Both completion callbacks pass back the (trimmed) name and
 * dismiss onboarding; [onAllowNotifications] additionally asks the host to
 * fire the Android system permission prompt. Neither callback starts a
 * focus — that's the user's first decision on Home. The name is also handed
 * over on skip, so a nickname typed before skipping isn't lost.
 */
@Composable
fun OnboardingScreen(
    authRepository: AuthRepository,
    language: AppLanguage = AppLanguage.System,
    onUpdateLanguage: (AppLanguage) -> Unit = {},
    onSkip: (name: String) -> Unit,
    onAllowNotifications: (name: String) -> Unit,
    onPersistUserName: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })
    val scope = rememberCoroutineScope()
    val currentPage = pagerState.currentPage
    val isFirstPage = currentPage == 0
    val isLastPage = currentPage == PAGE_COUNT - 1
    var name by remember { mutableStateOf("") }
    var showLoginSheet by remember { mutableStateOf(false) }

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
                showSkip = currentPage in 1 until PAGE_COUNT - 1,
                onSkip = { onSkip(name.trim()) },
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                pageSpacing = KuroSpacing.md,
            ) { page ->
                // Center each page's content vertically so we don't leave a
                // huge cream void at the bottom on shorter pages (intent /
                // rhythm / stay on track). The welcome page has the most
                // content and still fits comfortably with centering.
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = KuroSpacing.xl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    when (page) {
                        0 -> WelcomePage()
                        1 -> NamePage(
                            name = name,
                            onNameChange = { if (it.length <= NAME_MAX_LENGTH) name = it },
                        )
                        2 -> IntentPage()
                        3 -> RhythmPage()
                        else -> StayOnTrackPage()
                    }
                }
            }

            PageDots(
                pageCount = PAGE_COUNT,
                currentPage = currentPage,
                modifier = Modifier.padding(bottom = KuroSpacing.md),
            )

            BottomCtaBlock(
                isFirstPage = isFirstPage,
                isLastPage = isLastPage,
                onPrimary = {
                    when {
                        isFirstPage -> showLoginSheet = true
                        isLastPage -> onAllowNotifications(name.trim())
                        else -> scope.launch { pagerState.animateScrollToPage(currentPage + 1) }
                    }
                },
                onSecondary = {
                    when {
                        isFirstPage -> scope.launch { pagerState.animateScrollToPage(1) }
                        isLastPage -> onSkip(name.trim())
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = KuroSpacing.xl,
                        end = KuroSpacing.xl,
                        bottom = KuroSpacing.lg,
                    ),
            )
        }
    }

    if (showLoginSheet) {
        LoginSheet(
            authRepository = authRepository,
            onDismiss = { showLoginSheet = false },
            onSignedIn = { user ->
                showLoginSheet = false
                user.displayName?.trim()?.takeIf { it.isNotBlank() }?.let { accountName ->
                    name = accountName
                    onPersistUserName(accountName)
                }
                scope.launch { pagerState.animateScrollToPage(1) }
            },
            onContinueAsGuest = { showLoginSheet = false },
            fallbackDisplayName = name.trim(),
        )
    }
}

// ── Top bar ──────────────────────────────────────────────────────────────────

@Composable
private fun OnboardingTopBar(
    language: AppLanguage,
    onUpdateLanguage: (AppLanguage) -> Unit,
    showSkip: Boolean,
    onSkip: () -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = KuroSpacing.xxxl)
            .padding(horizontal = KuroSpacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OnboardingLanguageSelector(
            language = language,
            onSelect = onUpdateLanguage,
        )
        if (showSkip) {
            TextButton(onClick = onSkip) {
                Text(
                    text = stringResource(R.string.onboarding_skip),
                    color = tokens.textSecondary,
                    style = KuroType.labelStrong,
                )
            }
        }
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
                        onSelect(option)
                        expanded = false
                    },
                    leadingIcon = if (option == language) {
                        {
                            Icon(
                                imageVector = Lucide.Check,
                                contentDescription = null,
                                tint = tokens.accent,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    } else {
                        null
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

// ── Page 0: Welcome ──────────────────────────────────────────────────────────

@Composable
private fun WelcomePage() {
    val tokens = MaterialTheme.colorScheme.kuroTokens
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

    // Hero card — fixed height so subtitle below is guaranteed to fit on
    // every device. Crop-aligned to BottomCenter so the cat stays visible
    // even when the tall portrait asset is cropped to a wide rectangle.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(340.dp)
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

    Spacer(Modifier.height(KuroSpacing.lg))

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
        text = stringResource(R.string.onboarding_welcome_subtitle),
        color = tokens.textSecondary,
        style = KuroType.bodySecondary,
        textAlign = TextAlign.Center,
        modifier = Modifier.widthIn(max = 320.dp),
    )
}

// ── Page 1: Name ─────────────────────────────────────────────────────────────

@Composable
private fun NamePage(
    name: String,
    onNameChange: (String) -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Image(
        painter = painterResource(id = R.drawable.cat_face),
        contentDescription = null,
        modifier = Modifier
            .size(96.dp)
            .clip(CircleShape)
            .background(tokens.accentSoft),
        contentScale = ContentScale.Crop,
    )
    Spacer(Modifier.height(KuroSpacing.lg))
    PageTitle(text = stringResource(R.string.onboarding_name_title))
    Spacer(Modifier.height(KuroSpacing.xs))
    PageSubtitle(text = stringResource(R.string.onboarding_name_subtitle))
    Spacer(Modifier.height(KuroSpacing.xl))
    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        placeholder = { Text(stringResource(R.string.onboarding_name_placeholder), color = tokens.textMuted) },
        singleLine = true,
        shape = RoundedCornerShape(KuroSpacing.radiusMd),
        supportingText = {
            Text(
                text = "${name.length}/$NAME_MAX_LENGTH",
                color = tokens.textMuted,
                style = KuroType.labelMeta,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 360.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = tokens.textPrimary,
            unfocusedTextColor = tokens.textPrimary,
            cursorColor = tokens.accent,
            focusedBorderColor = tokens.accent,
            unfocusedBorderColor = tokens.cardBorder,
        ),
    )
    Spacer(Modifier.height(KuroSpacing.md))
    Text(
        text = stringResource(R.string.onboarding_name_hint),
        color = tokens.textMuted,
        style = KuroType.bodySecondary,
        textAlign = TextAlign.Center,
        modifier = Modifier.widthIn(max = 320.dp),
    )
}

// ── Page 2: Intent ───────────────────────────────────────────────────────────

@Composable
private fun IntentPage() {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    PageTitle(text = stringResource(R.string.onboarding_intent_title))
    Spacer(Modifier.height(KuroSpacing.xs))
    PageSubtitle(text = stringResource(R.string.onboarding_intent_subtitle))
    Spacer(Modifier.height(KuroSpacing.xl))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(KuroSpacing.radiusLg))
            .background(tokens.cardFill)
            .border(1.dp, tokens.cardBorder, RoundedCornerShape(KuroSpacing.radiusLg))
            .padding(KuroSpacing.md),
        verticalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
    ) {
        IntentTileRow(
            left = IntentSpec(stringResource(R.string.onboarding_intent_task), Lucide.CircleCheck, selected = true),
            right = IntentSpec(stringResource(R.string.onboarding_intent_habit), Lucide.Leaf),
        )
        IntentTileRow(
            left = IntentSpec(stringResource(R.string.onboarding_intent_note), Lucide.NotebookPen),
            right = IntentSpec(stringResource(R.string.onboarding_intent_free), Lucide.Square),
        )
    }

    Spacer(Modifier.height(KuroSpacing.md))
    Text(
        text = stringResource(R.string.onboarding_intent_hint),
        color = tokens.textMuted,
        style = KuroType.bodySecondary,
        textAlign = TextAlign.Center,
    )
}

private data class IntentSpec(
    val label: String,
    val icon: ImageVector,
    val selected: Boolean = false,
)

@Composable
private fun IntentTileRow(left: IntentSpec, right: IntentSpec) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
    ) {
        IntentTile(spec = left, modifier = Modifier.weight(1f))
        IntentTile(spec = right, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun IntentTile(spec: IntentSpec, modifier: Modifier = Modifier) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val fill = if (spec.selected) tokens.activeCardFill else tokens.cardFill
    val border = if (spec.selected) tokens.accent else tokens.cardBorder
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(KuroSpacing.radiusMd))
            .background(fill)
            .border(1.dp, border, RoundedCornerShape(KuroSpacing.radiusMd))
            .padding(vertical = KuroSpacing.md, horizontal = KuroSpacing.md),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
    ) {
        Icon(
            imageVector = spec.icon,
            contentDescription = null,
            tint = tokens.accent,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = spec.label,
            color = tokens.textPrimary,
            style = KuroType.bodyPrimary.copy(fontWeight = FontWeight.SemiBold),
        )
    }
}

// ── Page 3: Rhythm (week preview) ────────────────────────────────────────────

@Composable
private fun RhythmPage() {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    PageTitle(text = stringResource(R.string.onboarding_rhythm_title))
    Spacer(Modifier.height(KuroSpacing.xs))
    PageSubtitle(text = stringResource(R.string.onboarding_rhythm_subtitle))
    Spacer(Modifier.height(KuroSpacing.xl))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(KuroSpacing.radiusLg))
            .background(tokens.cardFill)
            .border(1.dp, tokens.cardBorder, RoundedCornerShape(KuroSpacing.radiusLg))
            .padding(KuroSpacing.lg),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.onboarding_rhythm_your_week),
                color = tokens.textPrimary,
                style = KuroType.headingCard,
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(KuroSpacing.radiusSm))
                    .background(tokens.accentSoft)
                    .padding(horizontal = KuroSpacing.sm, vertical = KuroSpacing.xs),
            ) {
                Text(
                    text = stringResource(R.string.onboarding_rhythm_preview),
                    color = tokens.accent,
                    style = KuroType.labelMeta,
                )
            }
        }
        Spacer(Modifier.height(KuroSpacing.md))
        WeekDotsPreview(todayIndex = 3)
    }

    Spacer(Modifier.height(KuroSpacing.md))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(KuroSpacing.radiusMd))
            .background(tokens.cardFill)
            .border(1.dp, tokens.cardBorder, RoundedCornerShape(KuroSpacing.radiusMd))
            .padding(KuroSpacing.md),
    ) {
        Text(
            text = stringResource(R.string.onboarding_rhythm_quote),
            color = tokens.textSecondary,
            style = KuroType.bodySecondary.copy(fontFamily = NunitoFamily),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun WeekDotsPreview(todayIndex: Int) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val labels = listOf("M", "T", "W", "T", "F", "S", "S")
    val sampleCounts = listOf(3, 4, 2, 4, 0, 0, 0)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        labels.forEachIndexed { index, day ->
            val isToday = index == todayIndex
            val columnFill = if (isToday) tokens.activeCardFill else tokens.pageBackground
            Column(
                modifier = Modifier
                    .width(28.dp)
                    .clip(RoundedCornerShape(KuroSpacing.radiusSm))
                    .background(columnFill)
                    .padding(vertical = KuroSpacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(KuroSpacing.xs),
            ) {
                Text(
                    text = day,
                    color = if (isToday) tokens.accent else tokens.textMuted,
                    style = KuroType.labelMeta,
                )
                repeat(4) { row ->
                    val filled = row < sampleCounts[index]
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (filled) tokens.accent else tokens.cardBorder),
                    )
                }
            }
        }
    }
}

// ── Page 4: Stay on track ────────────────────────────────────────────────────

@Composable
private fun StayOnTrackPage() {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    PageTitle(text = stringResource(R.string.onboarding_track_title))
    Spacer(Modifier.height(KuroSpacing.xs))
    PageSubtitle(
        text = stringResource(R.string.onboarding_track_subtitle)
    )
    Spacer(Modifier.height(KuroSpacing.xl))

    NotificationPreviewCard()

    Spacer(Modifier.height(KuroSpacing.md))

    Text(
        text = stringResource(R.string.onboarding_track_body),
        color = tokens.textSecondary,
        style = KuroType.bodySecondary,
        textAlign = TextAlign.Center,
        modifier = Modifier.widthIn(max = 320.dp),
    )
}

/**
 * Stylized preview of an Android system notification. Typography is tuned to
 * approximate platform notification text (small regular meta line, semibold
 * title, tighter line heights) so it reads as "this is what you'll see" rather
 * than "this is a Purrodoro card".
 */
@Composable
private fun NotificationPreviewCard() {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(KuroSpacing.radiusMd))
            .background(tokens.cardFill)
            .border(1.dp, tokens.cardBorder, RoundedCornerShape(KuroSpacing.radiusMd))
            .padding(horizontal = KuroSpacing.md, vertical = KuroSpacing.sm),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(tokens.accentSoft),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(id = R.drawable.cat_face),
                contentDescription = null,
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = stringResource(R.string.onboarding_notif_meta),
                color = tokens.textMuted,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Normal,
            )
            Text(
                text = stringResource(R.string.onboarding_notif_title),
                color = tokens.textPrimary,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.onboarding_notif_body),
                color = tokens.textSecondary,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ── Shared bits ──────────────────────────────────────────────────────────────

@Composable
private fun PageTitle(text: String) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Text(
        text = text,
        color = tokens.textPrimary,
        fontFamily = NunitoFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 36.sp,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun PageSubtitle(text: String) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Text(
        text = text,
        color = tokens.textSecondary,
        style = KuroType.bodySecondary,
        textAlign = TextAlign.Center,
        modifier = Modifier.widthIn(max = 320.dp),
    )
}

@Composable
private fun PageDots(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            val isActive = index == currentPage
            val width = if (isActive) 18.dp else 6.dp
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .height(6.dp)
                    .width(width)
                    .clip(CircleShape)
                    .background(if (isActive) tokens.accent else tokens.cardBorder),
            )
        }
    }
}

@Composable
private fun BottomCtaBlock(
    isFirstPage: Boolean,
    isLastPage: Boolean,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val primaryLabel = when {
        isFirstPage -> stringResource(R.string.onboarding_cta_sign_in)
        isLastPage -> stringResource(R.string.onboarding_cta_turn_on_notifications)
        else -> stringResource(R.string.onboarding_cta_next)
    }
    val secondaryLabel = when {
        isFirstPage -> stringResource(R.string.onboarding_cta_continue_as_guest)
        isLastPage -> stringResource(R.string.onboarding_cta_not_now)
        else -> null
    }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(
            onClick = onPrimary,
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
                text = primaryLabel,
                style = KuroType.bodyPrimary.copy(fontWeight = FontWeight.SemiBold),
            )
        }

        secondaryLabel?.let { label ->
            Spacer(Modifier.height(if (isFirstPage) KuroSpacing.sm else KuroSpacing.xs))
            if (isFirstPage) {
                OutlinedButton(
                    onClick = onSecondary,
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
                        text = label,
                        style = KuroType.bodyPrimary.copy(fontWeight = FontWeight.SemiBold),
                    )
                }
            } else {
                TextButton(onClick = onSecondary) {
                    Text(
                        text = label,
                        color = tokens.textSecondary,
                        style = KuroType.labelStrong,
                    )
                }
            }
        }
    }
}
