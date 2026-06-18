package com.endiar.purrodoro.model

/**
 * Free-tier limits and pure helpers for premium gating.
 *
 * Caps apply only when [isPremium] is false. Premium users are unlimited.
 * Active tasks = tasks that are not completed.
 */
object PremiumGate {
    const val FREE_ACTIVE_TASK_LIMIT = 10
    const val FREE_HABIT_LIMIT = 10

    fun activeTaskCount(tasks: List<FocusTask>): Int =
        tasks.count { !it.completed }

    fun canAddActiveTask(activeCount: Int, isPremium: Boolean): Boolean =
        isPremium || activeCount < FREE_ACTIVE_TASK_LIMIT

    fun canAddHabit(habitCount: Int, isPremium: Boolean): Boolean =
        isPremium || habitCount < FREE_HABIT_LIMIT
}
