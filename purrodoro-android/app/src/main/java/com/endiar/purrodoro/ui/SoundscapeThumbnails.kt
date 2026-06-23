package com.endiar.purrodoro.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.endiar.purrodoro.R
import com.endiar.purrodoro.soundscape.SoundscapeConfig
import com.endiar.purrodoro.soundscape.SoundscapeModule
import com.endiar.purrodoro.soundscape.SoundscapeThumbnailLoader
import com.endiar.purrodoro.ui.theme.Coral
import kotlin.math.min

/** Center label diameter as a fraction of the full disc. */
private const val VINYL_LABEL_FRACTION = 0.52f
/** Spindle cap diameter as a fraction of the full disc. */
private const val VINYL_SPINDLE_FRACTION = 0.07f

private val VinylEdge = Color(0xFF0C0C0C)
private val VinylMid = Color(0xFF181818)
private val VinylHighlight = Color(0xFF2C2C2C)
private val VinylSpindleWell = Color(0xFF241818)

@DrawableRes
fun bundledAmbienceThumbnailRes(trackId: Int): Int = when (trackId) {
    1 -> R.drawable.soundscape_ambience_rain
    2 -> R.drawable.soundscape_ambience_cafe
    3 -> R.drawable.soundscape_ambience_fireplace
    4 -> R.drawable.soundscape_ambience_forest
    5 -> R.drawable.soundscape_ambience_sea_wave
    6 -> R.drawable.soundscape_ambience_winter_wind
    else -> R.drawable.soundscape_ambience_fallback
}

@DrawableRes
fun bundledMusicCoverRes(trackId: Int): Int = when (trackId) {
    1 -> R.drawable.soundscape_music_desk_lamp_rain
    2 -> R.drawable.soundscape_music_mosswood_morning
    3 -> R.drawable.soundscape_music_paper_skies
    4 -> R.drawable.soundscape_music_quiet_window_code
    5 -> R.drawable.soundscape_music_sore_di_pendopo
    6 -> R.drawable.soundscape_music_sudirman_rain_loop
    else -> R.drawable.soundscape_lofi_fallback
}

fun remoteThumbnailObjectKey(thumbnailPath: String?, audioFilePath: String? = null): String {
    val path = thumbnailPath.orEmpty()
    val audio = audioFilePath.orEmpty()
    return SoundscapeConfig.thumbnailKey(path, audio)
}

@Composable
private fun rememberSoundscapeThumbnailLoader(): SoundscapeThumbnailLoader {
    val context = LocalContext.current
    return remember { SoundscapeModule.get(context).thumbnailLoader }
}

@Composable
private fun SoundscapeRemoteThumbnail(
    objectKey: String,
    cacheVersion: Long,
    @DrawableRes placeholderRes: Int,
    @DrawableRes fallbackRes: Int,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val loader = rememberSoundscapeThumbnailLoader()
    var bitmap by remember(objectKey, cacheVersion) { mutableStateOf<android.graphics.Bitmap?>(null) }
    var failed by remember(objectKey, cacheVersion) { mutableStateOf(false) }

    LaunchedEffect(objectKey, cacheVersion) {
        failed = false
        bitmap = null
        if (objectKey.isEmpty()) {
            failed = true
            return@LaunchedEffect
        }
        val loaded = loader.loadBitmap(objectKey, cacheVersion)
        if (loaded != null) {
            bitmap = loaded
        } else {
            failed = true
        }
    }

    when {
        bitmap != null -> Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = null,
            modifier = modifier,
            contentScale = contentScale,
        )
        failed -> Image(
            painter = painterResource(fallbackRes),
            contentDescription = null,
            modifier = modifier,
            contentScale = contentScale,
        )
        else -> Image(
            painter = painterResource(placeholderRes),
            contentDescription = null,
            modifier = modifier,
            contentScale = contentScale,
        )
    }
}

@Composable
fun SoundscapeAmbienceThumbnail(
    trackId: Int,
    thumbnailPath: String?,
    audioFilePath: String? = null,
    cacheVersion: Long = 0L,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val objectKey = remoteThumbnailObjectKey(thumbnailPath, audioFilePath)
    if (objectKey.isEmpty()) {
        Image(
            painter = painterResource(bundledAmbienceThumbnailRes(trackId)),
            contentDescription = null,
            modifier = modifier,
            contentScale = contentScale,
        )
        return
    }
    SoundscapeRemoteThumbnail(
        objectKey = objectKey,
        cacheVersion = cacheVersion,
        placeholderRes = R.drawable.soundscape_ambience_placeholder,
        fallbackRes = R.drawable.soundscape_ambience_fallback,
        modifier = modifier,
        contentScale = contentScale,
    )
}

@Composable
fun SoundscapeMusicCover(
    trackId: Int,
    thumbnailPath: String?,
    audioFilePath: String? = null,
    cacheVersion: Long = 0L,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val objectKey = remoteThumbnailObjectKey(thumbnailPath, audioFilePath)
    if (objectKey.isEmpty()) {
        Image(
            painter = painterResource(bundledMusicCoverRes(trackId)),
            contentDescription = null,
            modifier = modifier,
            contentScale = contentScale,
        )
        return
    }
    SoundscapeRemoteThumbnail(
        objectKey = objectKey,
        cacheVersion = cacheVersion,
        placeholderRes = R.drawable.soundscape_lofi_placeholder,
        fallbackRes = R.drawable.soundscape_lofi_fallback,
        modifier = modifier,
        contentScale = contentScale,
    )
}

/** Charcoal grooved vinyl plate drawn behind the center label. */
@Composable
private fun VinylGroovePlate(modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    Canvas(modifier = modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = min(cx, cy)
        drawCircle(
            brush = Brush.radialGradient(
                0f to VinylHighlight,
                0.55f to VinylMid,
                1f to VinylEdge,
                center = Offset(cx, cy),
                radius = radius,
            ),
            radius = radius,
            center = Offset(cx, cy),
        )
        val labelRadius = radius * VINYL_LABEL_FRACTION * 0.5f
        val grooveInset = with(density) { 2.dp.toPx() }
        val grooveStart = labelRadius + grooveInset
        val grooveEnd = radius - grooveInset
        val grooveCount = 6
        repeat(grooveCount) { index ->
            val t = (index + 1f) / (grooveCount + 1f)
            val grooveRadius = grooveStart + (grooveEnd - grooveStart) * t
            drawCircle(
                color = Color.White.copy(alpha = if (index % 2 == 0) 0.075f else 0.04f),
                radius = grooveRadius,
                center = Offset(cx, cy),
                style = Stroke(width = with(density) { 1.dp.toPx() }),
            )
        }
        drawCircle(
            color = Color.Black.copy(alpha = 0.4f),
            radius = radius - 0.5f,
            center = Offset(cx, cy),
            style = Stroke(width = with(density) { 1.5.dp.toPx() }),
        )
    }
}

/**
 * Lo-fi vinyl tile: grooved charcoal disc, small center cover label, coral spindle.
 */
@Composable
fun SoundscapeVinylDisc(
    trackId: Int,
    thumbnailPath: String?,
    audioFilePath: String? = null,
    cacheVersion: Long = 0L,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        VinylGroovePlate(Modifier.matchParentSize())
        Box(
            modifier = Modifier
                .fillMaxWidth(VINYL_LABEL_FRACTION)
                .aspectRatio(1f)
                .clip(CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            SoundscapeMusicCover(
                trackId = trackId,
                thumbnailPath = thumbnailPath,
                audioFilePath = audioFilePath,
                cacheVersion = cacheVersion,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth(VINYL_SPINDLE_FRACTION)
                .aspectRatio(1f)
                .clip(CircleShape)
                .background(Coral)
                .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize(0.38f)
                    .aspectRatio(1f)
                    .clip(CircleShape)
                    .background(VinylSpindleWell),
            )
        }
    }
}
