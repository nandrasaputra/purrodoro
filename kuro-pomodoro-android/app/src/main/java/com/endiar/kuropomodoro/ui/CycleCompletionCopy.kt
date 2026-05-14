package com.endiar.kuropomodoro.ui

data class CycleCompletionCopyVariant(
    val title: String,
    val body: String
)

object CycleCompletionCopy {
    const val buttonLabel: String = "Done"

    val variants: List<CycleCompletionCopyVariant> = listOf(
        CycleCompletionCopyVariant(
            title = "Cycle complete",
            body = "That's a full cycle behind you. Take a moment — you earned it."
        ),
        CycleCompletionCopyVariant(
            title = "Nicely done",
            body = "You finished a full Pomodoro cycle. Rest before the next one."
        ),
        CycleCompletionCopyVariant(
            title = "That's a wrap",
            body = "Four focused sessions, all done. Step away with a clear head."
        ),
        CycleCompletionCopyVariant(
            title = "Good work",
            body = "A complete cycle is no small thing. Take the win."
        ),
        CycleCompletionCopyVariant(
            title = "All done",
            body = "One full cycle behind you. Breathe, then come back when you're ready."
        )
    )
}
