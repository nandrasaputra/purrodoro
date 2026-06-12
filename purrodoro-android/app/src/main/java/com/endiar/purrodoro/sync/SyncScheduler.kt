package com.endiar.purrodoro.sync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

/**
 * Coalesces sync triggers. Local-write signals are debounced ([debounceMs] of
 * quiet before [sync] runs) so a burst of edits costs one network pass;
 * [requestImmediate] skips the debounce (e.g. app-foreground). Both paths
 * funnel through one collector, so triggers can never run [sync] concurrently
 * from here (and the engine's mutex guards everything else).
 */
class SyncScheduler(
    scope: CoroutineScope,
    debounceMs: Long,
    private val sync: suspend () -> Unit,
) {

    private val localWrites = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val immediate = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    init {
        scope.launch {
            @OptIn(FlowPreview::class)
            merge(localWrites.debounce(debounceMs), immediate).collect {
                sync()
            }
        }
    }

    /** Signals that account-owned local data changed; syncs after a quiet period. */
    fun onLocalWrite() {
        localWrites.tryEmit(Unit)
    }

    /** Requests a sync as soon as possible, without the write debounce. */
    fun requestImmediate() {
        immediate.tryEmit(Unit)
    }
}
