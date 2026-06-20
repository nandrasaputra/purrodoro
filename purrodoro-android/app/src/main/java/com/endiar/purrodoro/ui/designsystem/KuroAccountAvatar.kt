package com.endiar.purrodoro.ui.designsystem

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.endiar.purrodoro.R
import com.endiar.purrodoro.auth.AccountAvatarStore
import com.endiar.purrodoro.ui.theme.kuroTokens
import androidx.compose.material3.MaterialTheme

@Composable
fun rememberAccountAvatarStore(): AccountAvatarStore {
    val context = LocalContext.current
    return remember { AccountAvatarStore.get(context) }
}

/** Photo when cached, otherwise the user's initial on the accent gradient. */
@Composable
fun KuroAccountAvatarContent(
    userId: String?,
    displayName: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val store = rememberAccountAvatarStore()
    val revision by store.revision.collectAsStateWithLifecycle()
    var bitmap by remember(userId, revision) { mutableStateOf<android.graphics.Bitmap?>(null) }
    val avatarDescription = contentDescription
        ?: stringResource(R.string.account_avatar_cd, displayName)

    LaunchedEffect(userId, revision) {
        bitmap = userId?.let { store.localBitmap(it) }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = avatarDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(CircleShape),
        )
    } else {
        val initial = displayName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        Box(
            modifier = modifier
                .clip(CircleShape)
                .semantics { this.contentDescription = avatarDescription }
                .background(
                    Brush.linearGradient(
                        colors = listOf(tokens.accent, tokens.interactiveFill),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initial,
                style = KuroType.headingCard.copy(fontWeight = FontWeight.SemiBold),
                color = tokens.interactiveText,
            )
        }
    }
}

/** Profile-style avatar — premium aurora frame or plain bordered circle. */
@Composable
fun KuroAccountAvatar(
    userId: String?,
    displayName: String,
    isPremium: Boolean,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens

    if (isPremium) {
        KuroPremiumAvatarFrame(size = size, modifier = modifier) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(CircleShape),
            ) {
                KuroAccountAvatarContent(
                    userId = userId,
                    displayName = displayName,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .border(1.dp, tokens.cardBorder, CircleShape),
        ) {
            KuroAccountAvatarContent(
                userId = userId,
                displayName = displayName,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
