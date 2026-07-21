package com.endiar.purrodoro.focusshield

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.view.KeyEvent
import android.view.WindowManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.endiar.purrodoro.MainActivity
import com.endiar.purrodoro.R
import com.endiar.purrodoro.ui.designsystem.KuroSpacing
import com.endiar.purrodoro.ui.designsystem.KuroType
import com.endiar.purrodoro.ui.theme.PurrodoroTheme
import com.endiar.purrodoro.ui.theme.kuroTokens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Soft interrupt overlay (Attuny-style) — SYSTEM_ALERT_WINDOW, dismissible.
 */
object FocusShieldOverlayController {

    private var overlayView: ComposeView? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null
    private var windowManager: WindowManager? = null
    private var showingPackage: String? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun isShowing(): Boolean = overlayView != null

    fun show(context: Context, blockedPackage: String, remainingSeconds: Int) {
        if (!FocusShieldOverlayAccess.isGranted(context)) return
        if (showingPackage == blockedPackage && overlayView != null) return

        scope.launch {
            hideInternal()
            val appContext = context.applicationContext
            val wm = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val owner = OverlayLifecycleOwner()
            val composeView = ComposeView(appContext)
            owner.attach(composeView)
            composeView.setContent {
                PurrodoroTheme {
                    FocusShieldOverlayContent(
                        remainingSeconds = remainingSeconds,
                        onReturn = {
                            openPurrodoro(appContext)
                            hideInternal()
                        },
                        onOpenAnyway = {
                            val until = System.currentTimeMillis() +
                                TimeUnit.SECONDS.toMillis(remainingSeconds.coerceAtLeast(60).toLong())
                                    .coerceAtMost(TimeUnit.HOURS.toMillis(2))
                            FocusShieldController.bypass(blockedPackage, until)
                            hideInternal()
                        },
                    )
                }
            }

            val layoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT,
            )

            composeView.isFocusable = true
            composeView.isFocusableInTouchMode = true
            composeView.requestFocus()
            composeView.setOnKeyListener { _, keyCode, _ ->
                keyCode == KeyEvent.KEYCODE_BACK
            }

            wm.addView(composeView, layoutParams)
            windowManager = wm
            overlayView = composeView
            lifecycleOwner = owner
            showingPackage = blockedPackage
        }
    }

    fun hide() {
        scope.launch { hideInternal() }
    }

    private fun hideInternal() {
        val view = overlayView ?: return
        val wm = windowManager
        lifecycleOwner?.destroy()
        lifecycleOwner = null
        overlayView = null
        showingPackage = null
        runCatching { wm?.removeView(view) }
        windowManager = null
    }

    private fun openPurrodoro(context: Context) {
        context.startActivity(
            Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
        )
    }

    private class OverlayLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner {
        private val lifecycleRegistry = LifecycleRegistry(this)
        private val savedStateController = SavedStateRegistryController.create(this)

        override val lifecycle: Lifecycle get() = lifecycleRegistry
        override val savedStateRegistry: SavedStateRegistry
            get() = savedStateController.savedStateRegistry

        fun attach(view: ComposeView) {
            savedStateController.performRestore(null)
            lifecycleRegistry.currentState = Lifecycle.State.RESUMED
            view.setViewTreeLifecycleOwner(this)
            view.setViewTreeSavedStateRegistryOwner(this)
        }

        fun destroy() {
            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        }
    }
}

@Composable
private fun FocusShieldOverlayContent(
    remainingSeconds: Int,
    onReturn: () -> Unit,
    onOpenAnyway: () -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val timeLabel = "%d:%02d".format(minutes, seconds)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.pageBackground)
            .padding(KuroSpacing.xl),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(KuroSpacing.md),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Image(
                painter = painterResource(id = R.drawable.cat_face),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape),
            )
            Text(
                text = stringResource(R.string.focus_shield_interrupt_title),
                style = KuroType.headingCard,
                color = tokens.textPrimary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.focus_shield_interrupt_body, timeLabel),
                style = KuroType.bodySecondary,
                color = tokens.textSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(KuroSpacing.lg))
            Button(
                onClick = onReturn,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = tokens.accent,
                    contentColor = tokens.accentOn,
                ),
            ) {
                Text(stringResource(R.string.focus_shield_interrupt_return))
            }
            OutlinedButton(
                onClick = onOpenAnyway,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.focus_shield_interrupt_anyway))
            }
        }
    }
}
