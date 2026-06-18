package com.endiar.purrodoro.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.endiar.purrodoro.R
import com.endiar.purrodoro.model.HomeScene
import com.endiar.purrodoro.model.canSelectHomeScene
import com.endiar.purrodoro.model.resolveHomeHeroDrawable
import com.endiar.purrodoro.ui.designsystem.KuroAdaptiveBottomSheet
import com.endiar.purrodoro.ui.designsystem.KuroPremiumBadge
import com.endiar.purrodoro.ui.designsystem.KuroSpacing
import com.endiar.purrodoro.ui.designsystem.KuroType
import com.endiar.purrodoro.ui.theme.kuroTokens
import java.time.LocalTime

@Composable
fun HomeScenePickerSheet(
    selected: HomeScene,
    isPremium: Boolean,
    onDismiss: () -> Unit,
    onSelect: (HomeScene) -> Unit,
) {
    KuroAdaptiveBottomSheet(
        onDismiss = onDismiss,
        horizontalPadding = KuroSpacing.lg,
        showCloseButton = true,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.home_scene_picker_title),
                style = KuroType.headingCard,
                color = MaterialTheme.colorScheme.kuroTokens.textPrimary,
            )
            Spacer(modifier = Modifier.height(KuroSpacing.xs))
            Text(
                text = stringResource(R.string.home_scene_picker_subtitle),
                style = KuroType.bodySecondary,
                color = MaterialTheme.colorScheme.kuroTokens.textSecondary,
            )
            Spacer(modifier = Modifier.height(KuroSpacing.lg))
            HomeScene.entries.forEachIndexed { index, scene ->
                if (index > 0) Spacer(modifier = Modifier.height(KuroSpacing.sm))
                HomeSceneOptionRow(
                    scene = scene,
                    selected = scene == selected,
                    locked = !canSelectHomeScene(scene, isPremium),
                    onClick = { onSelect(scene) },
                )
            }
            Spacer(modifier = Modifier.height(KuroSpacing.md))
        }
    }
}

@Composable
private fun HomeSceneOptionRow(
    scene: HomeScene,
    selected: Boolean,
    locked: Boolean,
    onClick: () -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val isDark = MaterialTheme.colorScheme.background.luminance() <= 0.5f
    val title = stringResource(scene.titleRes())
    val rowCd = stringResource(R.string.home_scene_option_cd, title)
    val shape = RoundedCornerShape(KuroSpacing.radiusSm)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) tokens.activeCardFill else tokens.cardFill)
            .border(
                width = 1.dp,
                color = if (selected) tokens.accent.copy(alpha = 0.45f) else tokens.cardBorder,
                shape = shape,
            )
            .clickable(onClick = onClick)
            .semantics { contentDescription = rowCd }
            .padding(KuroSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KuroSpacing.md),
    ) {
        Box(
            modifier = Modifier
                .weight(0.42f)
                .aspectRatio(1.48f)
                .clip(RoundedCornerShape(KuroSpacing.radiusSm)),
        ) {
            Image(
                painter = painterResource(resolveHomeHeroDrawable(scene, isDark, LocalTime.now())),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (locked) {
                KuroPremiumBadge(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(KuroSpacing.xs),
                )
            }
        }
        Column(modifier = Modifier.weight(0.58f)) {
            Text(
                text = title,
                style = KuroType.bodyPrimary.copy(fontWeight = FontWeight.SemiBold),
                color = tokens.textPrimary,
            )
            Spacer(modifier = Modifier.height(KuroSpacing.xs))
            Text(
                text = stringResource(scene.subtitleRes()),
                style = KuroType.bodySecondary,
                color = tokens.textSecondary,
            )
        }
        if (selected && !locked) {
            Icon(
                imageVector = Lucide.Check,
                contentDescription = null,
                tint = tokens.accent,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
