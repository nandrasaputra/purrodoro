package com.endiar.purrodoro.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.View.MeasureSpec
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import com.endiar.purrodoro.model.AppTab
import com.endiar.purrodoro.model.AppearanceMode
import com.endiar.purrodoro.model.HomeScene
import com.endiar.purrodoro.model.TimerMode
import com.endiar.purrodoro.model.TimerSettings
import com.endiar.purrodoro.model.TimerState
import com.endiar.purrodoro.ui.components.DashboardDock
import com.endiar.purrodoro.ui.theme.PurrodoroTheme
import com.endiar.purrodoro.ui.theme.kuroTokens
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import android.os.Looper
import java.io.File
import java.io.FileOutputStream
import java.time.LocalTime

/**
 * Renders the idle Home dashboard (Compose) to a PNG via Robolectric.
 *
 * Run manually when marketing assets or home layout change:
 *   EXPORT_HOME_SCREENSHOT=1 ./gradlew :app:testDebugUnitTest \
 *     --tests com.endiar.purrodoro.ui.HomeTimerScreenScreenshotTest --rerun-tasks
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomeTimerScreenScreenshotTest {

    @Test
    fun exportIdleHomeDashboardPng() {
        if (System.getenv("EXPORT_HOME_SCREENSHOT") != "1") return

        val widthPx = 411 * 3
        val heightPx = 891 * 3
        val backdropTime = LocalTime.of(10, 30)
        val greeting = homeGreetingFor(backdropTime)
        val dockHeight = 168.dp

        val activity = Robolectric.buildActivity(ComponentActivity::class.java)
            .create()
            .start()
            .resume()
            .visible()
            .get()

        activity.setContent {
            PurrodoroTheme(appearanceMode = AppearanceMode.Light) {
                val tokens = MaterialTheme.colorScheme.kuroTokens
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(tokens.pageBackground),
                ) {
                    HomeTimerScreen(
                        timerMode = TimerMode.Focus,
                        remainingSeconds = TimerMode.Focus.defaultSeconds,
                        isRunning = false,
                        isStarted = false,
                        activeTask = null,
                        suggestion = HomeSuggestion.Empty,
                        suggestionFocusMinutes = TimerSettings().focusMinutes,
                        greeting = greeting,
                        userName = "",
                        accountUserId = null,
                        todayMetrics = TodayFocusMetrics(
                            focusTimeLabel = "0m",
                            sessionsLabel = "0",
                            completedLabel = "0",
                        ),
                        cycleItems = emptyList(),
                        cycleIndex = 0,
                        currentTimerState = TimerState.Idle,
                        timerSettings = TimerSettings(),
                        backdropTime = backdropTime,
                        homeScene = HomeScene.Room,
                        isPremium = false,
                        contentPadding = PaddingValues(bottom = dockHeight),
                        onSkip = {},
                        onStartPause = {},
                        onRequestStartFocus = {},
                        onContinueFocus = {},
                        onStop = {},
                    )
                    DashboardDock(
                        isRunning = false,
                        onNavigate = {},
                        selectedTab = AppTab.Timer,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .navigationBarsPadding(),
                    )
                }
            }
        }

        Shadows.shadowOf(Looper.getMainLooper()).idle()

        val composeView = findComposeView(activity.window.decorView)
            ?: error("ComposeView not found in activity hierarchy")

        measureAndLayout(composeView, widthPx, heightPx)

        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        composeView.draw(Canvas(bitmap))

        val outDir = File(System.getenv("HOME_SCREENSHOT_DIR") ?: "/opt/cursor/artifacts/assets")
            .also { it.mkdirs() }
        val outFile = File(outDir, "purrodoro-home-dashboard-robolectric.png")
        FileOutputStream(outFile).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
    }

    private fun findComposeView(root: View): ComposeView? {
        if (root is ComposeView) return root
        if (root is android.view.ViewGroup) {
            for (i in 0 until root.childCount) {
                findComposeView(root.getChildAt(i))?.let { return it }
            }
        }
        return null
    }

    private fun measureAndLayout(view: View, widthPx: Int, heightPx: Int) {
        val widthSpec = MeasureSpec.makeMeasureSpec(widthPx, MeasureSpec.EXACTLY)
        val heightSpec = MeasureSpec.makeMeasureSpec(heightPx, MeasureSpec.EXACTLY)
        view.measure(widthSpec, heightSpec)
        view.layout(0, 0, widthPx, heightPx)
    }
}
