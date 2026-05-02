package com.endiar.kuropomodoro.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.endiar.kuropomodoro.model.SoundOption
import com.endiar.kuropomodoro.ui.components.KuroCard
import com.endiar.kuropomodoro.ui.components.SectionTitle
import com.endiar.kuropomodoro.ui.theme.Cocoa

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
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionTitle(
            title = "Sound den",
            subtitle = "Pick a cozy background loop for Kuro's focus room"
        )

        sounds.forEach { sound ->
            SoundOptionCard(
                sound = sound,
                selected = sound.id == selectedSoundId,
                playing = isPlaying && sound.id == selectedSoundId,
                onClick = { onSoundSelected(sound) }
            )
        }
    }
}

@Composable
private fun SoundOptionCard(
    sound: SoundOption,
    selected: Boolean,
    playing: Boolean,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val status = when {
        sound.locked -> "Premium"
        playing -> "Playing"
        selected -> "Selected"
        else -> "Unlocked"
    }

    KuroCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .selectable(
                    selected = selected,
                    onClick = onClick,
                    role = Role.RadioButton
                )
                .semantics {
                    contentDescription = "${sound.name}, $status. ${sound.description}"
                },
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (selected && !sound.locked) colorScheme.primary else colorScheme.secondary.copy(alpha = 0.52f)
            ) {
                Text(
                    text = sound.symbol,
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected && !sound.locked) colorScheme.onPrimary else Cocoa,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sound.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = sound.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurface.copy(alpha = 0.72f)
                )
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (sound.locked) colorScheme.tertiary.copy(alpha = 0.44f) else colorScheme.primary.copy(alpha = 0.16f)
            ) {
                Text(
                    text = status,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
