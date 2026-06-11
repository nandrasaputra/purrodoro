package com.endiar.purrodoro.audio

import android.content.Context
import android.media.MediaPlayer
import androidx.annotation.RawRes

/**
 * Loops a single bundled `res/raw` track with independent volume control.
 *
 * Each player instance holds at most **one** track: pause/resume continues that
 * track mid-playback. Switching to a different resource tears down the previous
 * track and always starts the new one from the beginning.
 */
class SoundscapeLoopPlayer(context: Context) {

    private val appContext = context.applicationContext
    private var player: MediaPlayer? = null
    private var loadedResId: Int? = null

    fun play(@RawRes resId: Int, volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        if (loadedResId == resId) {
            player?.setVolume(clamped, clamped)
            if (player?.isPlaying != true) {
                player?.start()
            }
            return
        }
        tearDownPlayer()
        val mediaPlayer = MediaPlayer.create(appContext, resId) ?: return
        mediaPlayer.isLooping = true
        mediaPlayer.setVolume(clamped, clamped)
        mediaPlayer.start()
        player = mediaPlayer
        loadedResId = resId
    }

    fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        player?.setVolume(clamped, clamped)
    }

    /** Pause playback but retain loaded media and progress for [play]. */
    fun pause() {
        val mediaPlayer = player ?: return
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
        }
    }

    fun release() {
        tearDownPlayer()
    }

    private fun tearDownPlayer() {
        player?.release()
        player = null
        loadedResId = null
    }
}
