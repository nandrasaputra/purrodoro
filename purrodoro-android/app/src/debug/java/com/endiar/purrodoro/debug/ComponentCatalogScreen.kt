package com.endiar.purrodoro.debug

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.endiar.purrodoro.ui.components.KuroCard
import com.endiar.purrodoro.ui.components.KuroPrimaryButton
import com.endiar.purrodoro.ui.designsystem.KuroBadge
import com.endiar.purrodoro.ui.designsystem.KuroChip
import com.endiar.purrodoro.ui.designsystem.KuroDivider
import com.endiar.purrodoro.ui.designsystem.KuroFilterBar
import com.endiar.purrodoro.ui.designsystem.KuroInputField
import com.endiar.purrodoro.ui.designsystem.KuroListRow
import com.endiar.purrodoro.ui.designsystem.KuroPill
import com.endiar.purrodoro.ui.designsystem.KuroPillKind
import com.endiar.purrodoro.ui.designsystem.KuroPremiumBadge
import com.endiar.purrodoro.ui.designsystem.KuroPremiumBadgeCompact
import com.endiar.purrodoro.ui.designsystem.KuroSectionHeader
import com.endiar.purrodoro.ui.designsystem.KuroSegmentedControl
import com.endiar.purrodoro.ui.designsystem.KuroSpacing
import com.endiar.purrodoro.ui.designsystem.KuroStepper
import com.endiar.purrodoro.ui.designsystem.KuroType
import com.endiar.purrodoro.ui.theme.Blush
import com.endiar.purrodoro.ui.theme.Cocoa
import com.endiar.purrodoro.ui.theme.Coral
import com.endiar.purrodoro.ui.theme.Cream
import com.endiar.purrodoro.ui.theme.DockCoral
import com.endiar.purrodoro.ui.theme.Mint
import com.endiar.purrodoro.ui.theme.NightCard
import com.endiar.purrodoro.ui.theme.NightDeep
import com.endiar.purrodoro.ui.theme.Peach
import com.endiar.purrodoro.ui.theme.WarmCream
import com.endiar.purrodoro.ui.theme.kuroTokens

private val catalogSections = listOf("Tokens", "Components", "Patterns", "Audit")

@Composable
fun ComponentCatalogScreen() {
    var selectedSection by remember { mutableIntStateOf(0) }
    val tokens = MaterialTheme.colorScheme.kuroTokens

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.pageBackground)
    ) {
        // Header
        Text(
            text = "Design System",
            style = KuroType.headingScreen,
            color = tokens.textPrimary,
            modifier = Modifier.padding(KuroSpacing.lg),
        )

        // Section filter bar
        KuroFilterBar(
            options = catalogSections,
            selectedIndex = selectedSection,
            onSelect = { selectedSection = it },
        )

        Spacer(Modifier.height(KuroSpacing.sm))

        // Section content
        when (selectedSection) {
            0 -> TokensSection()
            1 -> ComponentsSection()
            2 -> PatternsSection()
            3 -> AuditSection()
        }
    }
}

// ── Tokens ────────────────────────────────────────────────────────────────────

@Composable
private fun TokensSection() {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(KuroSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(KuroSpacing.xl),
    ) {
        // Color swatches
        Text("Color Palette", style = KuroType.headingCard, color = tokens.textPrimary)
        val swatches = listOf(
            "Coral" to Coral,
            "Cocoa" to Cocoa,
            "Cream" to Cream,
            "WarmCream" to WarmCream,
            "Peach" to Peach,
            "Blush" to Blush,
            "Mint" to Mint,
            "NightDeep" to NightDeep,
            "NightCard" to NightCard,
            "DockCoral" to DockCoral,
        )
        swatches.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm)) {
                row.forEach { (name, color) ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(KuroSpacing.radiusSm))
                                .background(color)
                                .border(1.dp, tokens.cardBorder, RoundedCornerShape(KuroSpacing.radiusSm))
                        )
                        Text(name, style = KuroType.labelMeta, color = tokens.textMuted)
                    }
                }
            }
        }

        // Surface token pairs
        Text("Surface Tokens", style = KuroType.headingCard, color = tokens.textPrimary)
        val surfaceTokens = listOf(
            "pageBackground" to tokens.pageBackground,
            "cardFill" to tokens.cardFill,
            "activeCardFill" to tokens.activeCardFill,
            "cardBorder" to tokens.cardBorder,
            "divider" to tokens.divider,
            "accent" to tokens.accent,
            "accentSoft" to tokens.accentSoft,
            "interactiveFill" to tokens.interactiveFill,
            "textPrimary" to tokens.textPrimary,
            "textSecondary" to tokens.textSecondary,
            "textMuted" to tokens.textMuted,
            "textFaint" to tokens.textFaint,
        )
        surfaceTokens.forEach { (name, color) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(KuroSpacing.md),
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(KuroSpacing.radiusSm))
                        .background(color)
                        .border(1.dp, tokens.cardBorder, RoundedCornerShape(KuroSpacing.radiusSm))
                )
                Text(name, style = KuroType.bodySecondary, color = tokens.textPrimary)
            }
        }

        // Spacing scale
        Text("Spacing Scale", style = KuroType.headingCard, color = tokens.textPrimary)
        listOf(
            "xs=4" to KuroSpacing.xs,
            "sm=8" to KuroSpacing.sm,
            "md=16" to KuroSpacing.md,
            "lg=20" to KuroSpacing.lg,
            "xl=24" to KuroSpacing.xl,
            "xxl=32" to KuroSpacing.xxl,
            "xxxl=40" to KuroSpacing.xxxl,
        ).forEach { (label, size) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm)) {
                Box(modifier = Modifier.width(size).height(16.dp).clip(RoundedCornerShape(4.dp)).background(tokens.accent))
                Text(label, style = KuroType.labelMeta, color = tokens.textMuted)
            }
        }

        // Typography scale
        Text("Typography Scale", style = KuroType.headingCard, color = tokens.textPrimary)
        listOf(
            "displayTimer" to KuroType.displayTimer,
            "headingScreen" to KuroType.headingScreen,
            "headingCard" to KuroType.headingCard,
            "bodyPrimary" to KuroType.bodyPrimary,
            "bodySecondary" to KuroType.bodySecondary,
            "labelStrong" to KuroType.labelStrong,
            "labelMeta" to KuroType.labelMeta,
        ).forEach { (name, style) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(KuroSpacing.md),
            ) {
                Text(text = "Aa", style = style, color = tokens.textPrimary, modifier = Modifier.width(64.dp))
                Text(text = name, style = KuroType.labelMeta, color = tokens.textMuted)
            }
        }
    }
}

// ── Components ────────────────────────────────────────────────────────────────

@Composable
private fun ComponentsSection() {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    var chipSelected by remember { mutableIntStateOf(0) }
    var segSelected by remember { mutableIntStateOf(0) }
    var inputText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(KuroSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(KuroSpacing.xl),
    ) {
        // Atoms
        Text("Atoms", style = KuroType.headingCard, color = tokens.textPrimary)

        Text("KuroChip", style = KuroType.labelStrong, color = tokens.textMuted)
        Row(horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm)) {
            listOf("All", "Active", "Done").forEachIndexed { i, label ->
                KuroChip(text = label, selected = i == chipSelected, onClick = { chipSelected = i })
            }
        }

        Text("KuroPill", style = KuroType.labelStrong, color = tokens.textMuted)
        Row(horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm)) {
            KuroPill(label = "Playing", kind = KuroPillKind.Playing)
            KuroPill(label = "Locked", kind = KuroPillKind.Locked)
            KuroPill(label = "Default", kind = KuroPillKind.Default)
        }

        Text("KuroStepper", style = KuroType.labelStrong, color = tokens.textMuted)
        Row(horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm)) {
            KuroStepper(label = "\u2212", onClick = {})
            KuroStepper(label = "+", onClick = {})
        }

        Text("KuroDivider", style = KuroType.labelStrong, color = tokens.textMuted)
        KuroDivider(modifier = Modifier.fillMaxWidth())

        Text("KuroBadge", style = KuroType.labelStrong, color = tokens.textMuted)
        Row(horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm)) {
            KuroBadge(label = "Mock", fill = tokens.lockedFill, textColor = tokens.lockedText)
            KuroBadge(label = "Premium", fill = tokens.accentSoft, textColor = tokens.accent)
        }

        Text("KuroPremiumBadge", style = KuroType.labelStrong, color = tokens.textMuted)
        Row(horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm)) {
            KuroPremiumBadge()
            KuroPremiumBadgeCompact()
        }

        // Molecules
        Text("Molecules", style = KuroType.headingCard, color = tokens.textPrimary)

        Text("KuroListRow", style = KuroType.labelStrong, color = tokens.textMuted)
        KuroCard {
            KuroListRow(title = "Focus duration", subtitle = "25 minutes", trailing = {
                Text("25 min", style = KuroType.bodySecondary, color = tokens.textMuted)
            })
            KuroDivider(modifier = Modifier.fillMaxWidth())
            KuroListRow(title = "Short break", trailing = {
                Text("5 min", style = KuroType.bodySecondary, color = tokens.textMuted)
            })
        }

        Text("KuroSectionHeader", style = KuroType.labelStrong, color = tokens.textMuted)
        KuroSectionHeader(title = "TODAY", action = {
            Text("Add", style = KuroType.labelStrong, color = tokens.accent)
        })

        Text("KuroFilterBar", style = KuroType.labelStrong, color = tokens.textMuted)
        KuroFilterBar(
            options = listOf("All", "Work", "Personal", "Health"),
            selectedIndex = chipSelected,
            onSelect = { chipSelected = it },
        )

        Text("KuroInputField", style = KuroType.labelStrong, color = tokens.textMuted)
        KuroInputField(
            value = inputText,
            onValueChange = { inputText = it },
            placeholder = "Type something...",
        )

        Text("KuroSegmentedControl", style = KuroType.labelStrong, color = tokens.textMuted)
        KuroSegmentedControl(
            options = listOf("\u2600 Light", "\u263E Dark", "\u25D0 Auto"),
            selectedIndex = segSelected,
            onSelect = { segSelected = it },
        )

        // Organisms
        Text("Organisms", style = KuroType.headingCard, color = tokens.textPrimary)

        Text("KuroPrimaryButton", style = KuroType.labelStrong, color = tokens.textMuted)
        KuroPrimaryButton(text = "Start Focus", onClick = {}, modifier = Modifier.fillMaxWidth())
    }
}

// ── Patterns ─────────────────────────────────────────────────────────────────

@Composable
private fun PatternsSection() {
    var patternIndex by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        KuroSegmentedControl(
            options = listOf("List", "Settings", "Sheet"),
            selectedIndex = patternIndex,
            onSelect = { patternIndex = it },
            modifier = Modifier.padding(KuroSpacing.lg),
        )
        when (patternIndex) {
            0 -> ListPatternPreview()
            1 -> SettingsPatternPreview()
            2 -> SheetPatternPreview()
        }
    }
}

@Composable
private fun ListPatternPreview() {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.pageBackground)
            .verticalScroll(rememberScrollState())
            .padding(bottom = KuroSpacing.xxxl),
    ) {
        Text(
            text = "Tasks",
            style = KuroType.headingScreen,
            color = tokens.textPrimary,
            modifier = Modifier.padding(KuroSpacing.lg),
        )
        var chipSel by remember { mutableIntStateOf(0) }
        KuroFilterBar(
            options = listOf("All", "Work", "Personal"),
            selectedIndex = chipSel,
            onSelect = { chipSel = it },
        )
        KuroSectionHeader(title = "TODAY")
        KuroCard(modifier = Modifier.padding(horizontal = KuroSpacing.lg)) {
            listOf("Write design spec", "Review PR", "Update tests").forEachIndexed { i, t ->
                if (i > 0) KuroDivider(modifier = Modifier.fillMaxWidth())
                KuroListRow(title = t)
            }
        }
    }
}

@Composable
private fun SettingsPatternPreview() {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.pageBackground)
            .verticalScroll(rememberScrollState())
            .padding(KuroSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(KuroSpacing.md),
    ) {
        Text("Settings", style = KuroType.headingScreen, color = tokens.textPrimary)
        KuroCard {
            KuroListRow(title = "Focus duration", subtitle = "25 min", trailing = {
                Text("\u203A", style = KuroType.headingCard, color = tokens.textMuted)
            })
            KuroDivider(modifier = Modifier.fillMaxWidth())
            KuroListRow(title = "Short break", subtitle = "5 min", trailing = {
                Text("\u203A", style = KuroType.headingCard, color = tokens.textMuted)
            })
            KuroDivider(modifier = Modifier.fillMaxWidth())
            KuroListRow(title = "Long break", subtitle = "15 min", trailing = {
                Text("\u203A", style = KuroType.headingCard, color = tokens.textMuted)
            })
        }
        KuroCard {
            KuroListRow(
                title = "Delete all data",
                trailing = { Text("\u203A", style = KuroType.headingCard, color = tokens.accent) },
                onClick = {},
            )
        }
    }
}

@Composable
private fun SheetPatternPreview() {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    var text by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.pageBackground)
            .padding(KuroSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(KuroSpacing.md),
    ) {
        Text("Add Task (sheet preview)", style = KuroType.headingCard, color = tokens.textPrimary)
        KuroInputField(value = text, onValueChange = { text = it }, placeholder = "Task name")
        KuroListRow(title = "Category", subtitle = "Work", trailing = {
            Text("\u203A", style = KuroType.headingCard, color = tokens.textMuted)
        })
        KuroDivider(modifier = Modifier.fillMaxWidth())
        KuroListRow(title = "Pomodoros", subtitle = "4 sessions", trailing = {
            KuroStepper(label = "\u2212", onClick = {})
            Spacer(Modifier.width(KuroSpacing.sm))
            KuroStepper(label = "+", onClick = {})
        })
        Spacer(Modifier.weight(1f))
        KuroPrimaryButton(text = "Add Task", onClick = {}, modifier = Modifier.fillMaxWidth())
    }
}

// ── Audit ─────────────────────────────────────────────────────────────────────

@Composable
private fun AuditSection() {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val rules = listOf(
        "No Color(0xFF...) literals in screen files",
        "No fontSize/fontWeight literals in screen files",
        "No raw N.dp spacing in screen-level layout",
        "glassTokens() only used in Dashboard pattern",
        "No private Reference* or Home* color blocks in screens",
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(KuroSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
    ) {
        Text("Enforcement Rules", style = KuroType.headingCard, color = tokens.textPrimary)
        Spacer(Modifier.height(KuroSpacing.sm))
        rules.forEach { rule ->
            KuroListRow(
                title = rule,
                leading = { Text("\u2713", style = KuroType.labelStrong, color = tokens.accent) },
            )
            KuroDivider(modifier = Modifier.fillMaxWidth())
        }
    }
}
