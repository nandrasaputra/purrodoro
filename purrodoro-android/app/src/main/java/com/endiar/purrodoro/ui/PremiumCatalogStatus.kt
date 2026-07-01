package com.endiar.purrodoro.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.CloudOff
import com.composables.icons.lucide.Lucide
import com.endiar.purrodoro.R
import com.endiar.purrodoro.ui.components.KuroCard
import com.endiar.purrodoro.ui.designsystem.KuroSpacing
import com.endiar.purrodoro.ui.designsystem.KuroType
import com.endiar.purrodoro.ui.theme.kuroTokens

/** Inline empty state when Play product details could not be loaded. */
@Composable
fun PremiumCatalogErrorPanel(
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    KuroCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = KuroSpacing.lg,
                    vertical = KuroSpacing.lg,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Lucide.CloudOff,
                contentDescription = null,
                tint = tokens.textMuted,
                modifier = Modifier.size(28.dp),
            )
            Spacer(modifier = Modifier.height(KuroSpacing.sm))
            Text(
                text = stringResource(R.string.premium_catalog_error_title),
                style = KuroType.bodyPrimary.copy(fontWeight = FontWeight.SemiBold),
                color = tokens.textPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(KuroSpacing.xs))
            Text(
                text = stringResource(R.string.premium_catalog_error_body),
                style = KuroType.bodySecondary,
                color = tokens.textSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}
