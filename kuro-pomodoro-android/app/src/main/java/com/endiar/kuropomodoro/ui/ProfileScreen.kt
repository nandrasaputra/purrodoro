package com.endiar.kuropomodoro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.endiar.kuropomodoro.model.TimerMode
import com.endiar.kuropomodoro.model.TimerSettings
import com.endiar.kuropomodoro.model.GlassReadabilityMode
import com.endiar.kuropomodoro.model.GlassTreatment
import com.endiar.kuropomodoro.ui.components.GlassCard
import com.endiar.kuropomodoro.ui.components.MockBadge

@Composable
fun ProfileScreen(
    settings: TimerSettings,
    darkModePreview: Boolean,
    glassReadabilityMode: GlassReadabilityMode,
    glassTreatment: GlassTreatment,
    onDarkModeChanged: (Boolean) -> Unit,
    onGlassReadabilityModeChanged: (GlassReadabilityMode) -> Unit,
    onDurationChanged: (TimerMode, Int) -> Unit,
    onMockFeature: (String) -> Unit,
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
            text = "Me",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        // Profile / avatar card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Focus User",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = "Pomodoro in progress",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.55f)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF8B6F)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "FU",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Timer settings card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "TIMER SETTINGS",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.45f),
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp
            )
            Spacer(Modifier.height(8.dp))
            DurationRow(
                label = TimerMode.Focus.label,
                minutes = settings.focusMinutes,
                onMinus = { onDurationChanged(TimerMode.Focus, (settings.focusMinutes - 1).coerceIn(1, 60)) },
                onPlus = { onDurationChanged(TimerMode.Focus, (settings.focusMinutes + 1).coerceIn(1, 60)) }
            )
            Spacer(Modifier.height(8.dp))
            DurationRow(
                label = TimerMode.ShortBreak.label,
                minutes = settings.shortBreakMinutes,
                onMinus = { onDurationChanged(TimerMode.ShortBreak, (settings.shortBreakMinutes - 1).coerceIn(1, 60)) },
                onPlus = { onDurationChanged(TimerMode.ShortBreak, (settings.shortBreakMinutes + 1).coerceIn(1, 60)) }
            )
            Spacer(Modifier.height(8.dp))
            DurationRow(
                label = TimerMode.LongBreak.label,
                minutes = settings.longBreakMinutes,
                onMinus = { onDurationChanged(TimerMode.LongBreak, (settings.longBreakMinutes - 1).coerceIn(1, 60)) },
                onPlus = { onDurationChanged(TimerMode.LongBreak, (settings.longBreakMinutes + 1).coerceIn(1, 60)) }
            )
        }

        // Dark mode toggle card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Dark Mode Preview",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                DarkModeToggle(
                    checked = darkModePreview,
                    onCheckedChange = onDarkModeChanged
                )
            }
        }

        // Mock features section
        Text(
            text = "FEATURES",
            fontSize = 10.sp,
            color = Color.White.copy(alpha = 0.30f),
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.5.sp
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MockFeatureSquare(
                name = "Focus Together",
                icon = "\uD83D\uDC65",
                description = "Co-focus with friends",
                onClick = { onMockFeature("Focus Together is a mock social focus room for this prototype.") },
                modifier = Modifier.weight(1f)
            )
            MockFeatureSquare(
                name = "Reminders",
                icon = "\uD83D\uDD14",
                description = "Smart break reminders",
                onClick = { onMockFeature("Reminders are mocked here; notifications are not scheduled yet.") },
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MockFeatureSquare(
                name = "D-Day Tracker",
                icon = "\uD83D\uDCC5",
                description = "Track important dates",
                onClick = { onMockFeature("D-Day Tracker is a placeholder for future goal countdowns.") },
                modifier = Modifier.weight(1f)
            )
            MockFeatureSquare(
                name = "Premium Sounds",
                icon = "\uD83C\uDFB5",
                description = "Unlock all sound packs",
                onClick = { onMockFeature("Premium Sounds is simulated in this clone and does not start a purchase.") },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DurationRow(
    label: String,
    minutes: Int,
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StepperButton(label = "−", onClick = onMinus)
            Text(
                text = "$minutes min",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.widthIn(min = 54.dp),
                textAlign = TextAlign.Center
            )
            StepperButton(label = "+", onClick = onPlus)
        }
    }
}

@Composable
private fun StepperButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.10f))
            .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Light)
    }
}

@Composable
private fun DarkModeToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val thumbOffset = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    Box(
        modifier = Modifier
            .width(44.dp)
            .height(24.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (checked) Color(0xFFFF8B6F) else Color.White.copy(alpha = 0.10f))
            .border(
                1.dp,
                if (checked) Color.Transparent else Color.White.copy(alpha = 0.20f),
                RoundedCornerShape(12.dp)
            )
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 3.dp)
                .size(18.dp)
                .clip(CircleShape)
                .background(Color.White)
                .align(thumbOffset)
        )
    }
}

@Composable
private fun MockFeatureSquare(
    name: String,
    icon: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier.clickable(role = Role.Button, onClickLabel = name, onClick = onClick)
    ) {
        Text(icon, fontSize = 24.sp)
        Spacer(Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            MockBadge()
        }
        Spacer(Modifier.height(2.dp))
        Text(description, color = Color.White.copy(alpha = 0.50f), fontSize = 11.sp)
    }
}
