package com.endiar.purrodoro.audio

import androidx.annotation.RawRes

/**
 * Where a soundscape track's audio bytes come from: shipped inside the APK
 * (`res/raw`) or downloaded from the cloud catalog to internal storage.
 * Value equality matters: [SoundscapeLoopPlayer] uses it to decide between
 * resuming the loaded track and restarting from scratch.
 */
sealed interface AudioSource {
    data class Bundled(@RawRes val resId: Int) : AudioSource
    data class LocalFile(val path: String) : AudioSource
}
