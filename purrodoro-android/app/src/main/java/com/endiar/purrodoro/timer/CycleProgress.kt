package com.endiar.purrodoro.timer

import com.endiar.purrodoro.model.SessionItemType
import com.endiar.purrodoro.model.TimerSessionItem

enum class CycleSegmentKind { Focus, ShortBreak, LongBreak }

data class CycleProgressSegment(
    val kind: CycleSegmentKind,
    /** Cumulative end of this segment in seconds from cycle start. */
    val endSeconds: Int,
)

data class CycleProgress(
    val totalSeconds: Int,
    val elapsedSeconds: Int,
    val segments: List<CycleProgressSegment>,
)

/**
 * Maps a cycle to the single 0..[CycleProgress.totalSeconds] scale used by
 * `Notification.ProgressStyle`. [segmentRemainingSeconds] is the current
 * segment's remaining time as already resolved by the caller
 * (wall-clock `endsAtMillis` math while Playing, stored snapshot while Paused).
 */
fun cycleProgress(
    items: List<TimerSessionItem>,
    currentIndex: Int,
    segmentRemainingSeconds: Int,
): CycleProgress {
    var total = 0
    val segments = items.map { item ->
        total += item.type.seconds()
        CycleProgressSegment(kind = item.type.kind(), endSeconds = total)
    }
    var elapsedBefore = 0
    for (i in 0 until currentIndex.coerceIn(0, items.size)) {
        elapsedBefore += items[i].type.seconds()
    }
    val segmentTotal = items.getOrNull(currentIndex)?.type?.seconds() ?: 0
    val elapsed = (elapsedBefore + (segmentTotal - segmentRemainingSeconds)).coerceIn(0, total)
    return CycleProgress(
        totalSeconds = total,
        elapsedSeconds = elapsed,
        segments = segments,
    )
}

private fun SessionItemType.seconds(): Int = when (this) {
    is SessionItemType.Focus -> seconds
    is SessionItemType.ShortBreak -> seconds
    is SessionItemType.LongBreak -> seconds
}

private fun SessionItemType.kind(): CycleSegmentKind = when (this) {
    is SessionItemType.Focus -> CycleSegmentKind.Focus
    is SessionItemType.ShortBreak -> CycleSegmentKind.ShortBreak
    is SessionItemType.LongBreak -> CycleSegmentKind.LongBreak
}
