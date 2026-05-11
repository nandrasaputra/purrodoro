package com.endiar.kuropomodoro.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.endiar.kuropomodoro.model.SoundOption
import com.endiar.kuropomodoro.ui.components.GlassCard
import com.endiar.kuropomodoro.ui.components.MockBadge
import com.endiar.kuropomodoro.ui.components.StatusPill
import com.endiar.kuropomodoro.ui.components.StatusPillKind

@Composable
fun SoundsScreen(
    sounds: List<SoundOption>,
    selectedSoundId: Int,
    isPlaying: Boolean,
    onSoundSelected: (SoundOption) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Sounds",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        sounds.forEach { sound ->
            val isSelected = sound.id == selectedSoundId
            val pillKind = when {
                isSelected && isPlaying -> StatusPillKind.Playing
                isSelected             -> StatusPillKind.Selected
                sound.locked           -> StatusPillKind.Locked
                else                   -> StatusPillKind.Unlocked
            }
            val pillLabel = when {
                isSelected && isPlaying -> "Playing"
                isSelected             -> "Selected"
                sound.locked           -> "Premium"
                else                   -> "Tap to play"
            }

            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        role = Role.Button,
                        onClickLabel = if (sound.locked) "Unlock ${sound.name}" else "Play ${sound.name}"
                    ) { onSoundSelected(sound) },
                active = isSelected
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = sound.symbol.take(2),
                            fontSize = 28.sp
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = sound.name,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (sound.locked) MockBadge()
                            }
                            Text(
                                text = sound.description,
                                color = Color.White.copy(alpha = 0.50f),
                                fontSize = 12.sp
                            )
                        }
                    }
                    StatusPill(label = pillLabel, kind = pillKind)
                }
            }
        }
    }
}
