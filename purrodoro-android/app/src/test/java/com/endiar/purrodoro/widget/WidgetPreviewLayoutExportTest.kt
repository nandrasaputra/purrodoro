package com.endiar.purrodoro.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.LayoutInflater
import android.view.View
import android.view.View.MeasureSpec
import androidx.test.core.app.ApplicationProvider
import com.endiar.purrodoro.R
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream

/**
 * Renders [previewLayout] XML to square PNGs for `android:previewImage` fallbacks.
 *
 * Run manually when preview layouts change:
 *   EXPORT_WIDGET_PREVIEWS=1 ./gradlew :app:testDebugUnitTest \
 *     --tests com.endiar.purrodoro.widget.WidgetPreviewLayoutExportTest --rerun-tasks
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WidgetPreviewLayoutExportTest {

    @Test
    fun exportPreviewPngs() {
        if (System.getenv("EXPORT_WIDGET_PREVIEWS") != "1") return

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val drawableDir = File(System.getProperty("user.dir"), "src/main/res/drawable")
            .absoluteFile
            .also { check(it.isDirectory) { "Missing drawable dir: $it" } }

        val exports = listOf(
            R.layout.widget_preview_daily_plan to "widget_preview_daily_plan.png",
            R.layout.widget_preview_stats_grid to "widget_preview_stats_grid.png",
            R.layout.widget_preview_dashboard to "widget_preview_dashboard.png",
        )

        exports.forEach { (layoutRes, fileName) ->
            // Only re-export picker PNGs whose preview-only layouts changed.
            if (fileName !in PREVIEW_ONLY_PNGS) return@forEach
            val bitmap = renderLayout(context, layoutRes, PREVIEW_PX, PREVIEW_PX)
            FileOutputStream(File(drawableDir, fileName)).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        }
    }

    private fun renderLayout(
        context: android.content.Context,
        layoutRes: Int,
        widthPx: Int,
        heightPx: Int,
    ): Bitmap {
        val view = LayoutInflater.from(context).inflate(layoutRes, null, false)
        measureAndLayout(view, widthPx, heightPx)
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        return bitmap
    }

    private fun measureAndLayout(view: View, widthPx: Int, heightPx: Int) {
        val widthSpec = MeasureSpec.makeMeasureSpec(widthPx, MeasureSpec.EXACTLY)
        val heightSpec = MeasureSpec.makeMeasureSpec(heightPx, MeasureSpec.EXACTLY)
        view.measure(widthSpec, heightSpec)
        view.layout(0, 0, widthPx, heightPx)
    }

    private companion object {
        /** 180dp at xxhdpi (3×) — matches a 3×3 widget cell preview. */
        const val PREVIEW_PX = 540

        val PREVIEW_ONLY_PNGS = setOf(
            "widget_preview_daily_plan.png",
            "widget_preview_dashboard.png",
        )
    }
}
