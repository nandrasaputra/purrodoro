package com.endiar.purrodoro.sync

/** What to do with an incoming remote row during a pull. */
enum class PullAction { Apply, KeepLocal }

/**
 * Row-level last-write-wins conflict rule for the pull side of a sync pass.
 *
 * A dirty local row always survives the pull and re-pushes on the next push;
 * that push then makes it the newest server state — so the server acts as the
 * tie-breaker and no client clock comparison is ever needed. Everything else
 * (no local row, or a clean local row) simply applies the remote row,
 * including remote tombstones.
 *
 * @param localDirty `null` when no local row exists for the incoming id.
 */
fun resolvePull(localDirty: Boolean?, remoteDeleted: Boolean): PullAction =
    if (localDirty == true) PullAction.KeepLocal else PullAction.Apply
