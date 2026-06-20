package com.endiar.purrodoro.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.endiar.purrodoro.R
import com.endiar.purrodoro.soundscape.SoundscapeConfig
import com.endiar.purrodoro.soundscape.SoundscapeModule
import com.endiar.purrodoro.soundscape.SoundscapeThumbnailLoader

/** Fraction of the vinyl disc diameter used for the cover art circle. */
private const val VINYL_COVER_FRACTION = 0.86f
private val VinylRingWidth = 5.dp
private val VinylRingColor = Color(0xFF141414)

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

/** Layered lo-fi vinyl: large center cover + thin black ring. */
@Composable
fun SoundscapeVinylDisc(
    trackId: Int,
    thumbnailPath: String?,
    audioFilePath: String? = null,
    cacheVersion: Long = 0L,
    modifier: Modifier = Modifier,
    ringWidth: Dp = VinylRingWidth,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        SoundscapeMusicCover(
            trackId = trackId,
            thumbnailPath = thumbnailPath,
            audioFilePath = audioFilePath,
            cacheVersion = cacheVersion,
            modifier = Modifier
                .fillMaxSize(VINYL_COVER_FRACTION)
                .clip(CircleShape),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .border(width = ringWidth, color = VinylRingColor, shape = CircleShape),
        )
    }
}
