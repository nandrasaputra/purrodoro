package com.endiar.purrodoro.audio

import android.content.Context
import android.media.MediaPlayer

/**
 * Loops a single track with independent volume control. Sources are either
 * bundled `res/raw` resources or downloaded files ([AudioSource]).
 *
 * Each player instance holds at most **one** track: pause/resume continues
 * that track mid-playback. Switching to a different source tears down the
 * previous track and always starts the new one from the beginning.
 */
class SoundscapeLoopPlayer(context: Context) {

    private val appContext = context.applicationContext
    private var player: MediaPlayer? = null
    private var loadedSource: AudioSource? = null

    fun play(source: AudioSource, volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        if (loadedSource == source) {
            player?.setVolume(clamped, clamped)
            if (player?.isPlaying != true) {
                player?.start()
            }
            return
        }
        tearDownPlayer()
        val mediaPlayer = createPlayer(source) ?: return
        mediaPlayer.isLooping = true
        mediaPlayer.setVolume(clamped, clamped)
        mediaPlayer.start()
        player = mediaPlayer
        loadedSource = source
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

    private fun createPlayer(source: AudioSource): MediaPlayer? = when (source) {
        is AudioSource.Bundled -> MediaPlayer.create(appContext, source.resId)
        is AudioSource.LocalFile -> runCatching {
            MediaPlayer().apply {
                setDataSource(source.path)
                prepare()
            }
        }.getOrElse { null }
    }

    private fun tearDownPlayer() {
        player?.release()
        player = null
        loadedSource = null
    }
}
