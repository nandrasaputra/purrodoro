//
//  WidgetSnapshotBuilder.swift
//  Purrodoro
//
//  Pure derivation of `WidgetDashboardSnapshot` from app state — port of
//  Android `WidgetState.deriveData` / `computeNudge`.
//

import Foundation

enum WidgetSnapshotBuilder {
    private static let taskEmoji = "📝"
    private static let timeBlockOrder: [HabitTimeBlock] = [.morning, .afternoon, .evening, .any]

    static func build(
        stamps: [String: Int],
        minutesToday: Int,
        settings: TimerSettings,
        habits: [HabitItem],
        tasks: [TaskItem],
        timerState: TimerState,
        pendingCycleAck: Bool,
        today: Date = Date()
    ) -> WidgetDashboardSnapshot {
        let target = min(max(settings.focusSessionsPerCycle, 1), TimerSettings.maxFocusSessionsPerCycle)
        let focusToday = max(Journey.count(stamps, on: today), 0)
        let streak = Journey.currentStreak(stamps, today: today)
        let goalReached = focusToday >= target
        let streakAtRisk = streak > 0 && focusToday == 0

        let scheduledHabits = habits.filter { $0.isScheduled(on: today) }
        let dueHabits = scheduledHabits
            .filter { !$0.meetsTarget(on: today) }
            .sorted {
                let lhs = timeBlockOrder.firstIndex(of: $0.timeBlock) ?? 99
                let rhs = timeBlockOrder.firstIndex(of: $1.timeBlock) ?? 99
                if lhs != rhs { return lhs < rhs }
                return $0.createdAt < $1.createdAt
            }
        let pendingHabits = dueHabits.map { habit in
            WidgetChipSnapshot(
                emoji: habit.emoji.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                    ? "🌱" : habit.emoji,
                label: habit.name,
                target: .openHabits
            )
        }
        let habitsDoneToday = scheduledHabits.filter { $0.meetsTarget(on: today) }.count

        let pendingTaskItems = tasks.filter { !$0.completed }
        let cal = Journey.calendar
        let dayStart = cal.startOfDay(for: today)
        let dueTasks = pendingTaskItems
            .filter { task in
                guard let due = task.dueDate else { return false }
                return cal.startOfDay(for: due) <= dayStart
            }
            .sorted {
                let ld = $0.dueDate.map { cal.startOfDay(for: $0) } ?? .distantFuture
                let rd = $1.dueDate.map { cal.startOfDay(for: $0) } ?? .distantFuture
                if ld != rd { return ld < rd }
                return $0.priority.weight > $1.priority.weight
            }
        let orderedTasks = dueTasks + pendingTaskItems.filter { task in
            !dueTasks.contains(where: { $0.id == task.id })
        }
        let pendingTasks = orderedTasks.map { task in
            WidgetChipSnapshot(emoji: taskEmoji, label: task.title, target: .openTasks)
        }

        let nudge = computeNudge(
            timerState: timerState,
            pendingCycleAck: pendingCycleAck,
            goalReached: goalReached,
            streak: streak,
            streakAtRisk: streakAtRisk,
            firstHabit: pendingHabits.first,
            firstDueTask: dueTasks.first
        )

        return WidgetDashboardSnapshot(
            dayKey: Journey.dayKey(today),
            focusToday: focusToday,
            focusTarget: target,
            minutesToday: max(minutesToday, 0),
            streak: streak,
            streakAtRisk: streakAtRisk,
            goalReached: goalReached,
            habitsScheduledToday: scheduledHabits.count,
            habitsDoneToday: habitsDoneToday,
            pendingHabits: pendingHabits,
            pendingTasksCount: pendingTaskItems.count,
            pendingTasks: pendingTasks,
            timerPlaying: timerState == .playing,
            timerPaused: timerState == .paused,
            pendingCycleAck: pendingCycleAck,
            nudge: nudge
        )
    }

    private static func computeNudge(
        timerState: TimerState,
        pendingCycleAck: Bool,
        goalReached: Bool,
        streak: Int,
        streakAtRisk: Bool,
        firstHabit: WidgetChipSnapshot?,
        firstDueTask: TaskItem?
    ) -> WidgetNudgeSnapshot {
        switch timerState {
        case .playing:
            return WidgetNudgeSnapshot(
                kind: .focusing,
                title: "Purrodoro is focusing",
                line: "Stay with your session",
                ctaLabel: "Open Timer",
                target: .openTimer
            )
        case .paused:
            return WidgetNudgeSnapshot(
                kind: .paused,
                title: "Purrodoro is waiting",
                line: "Tap to resume",
                ctaLabel: "Resume",
                target: .openTimer
            )
        case .idle:
            break
        }

        if pendingCycleAck {
            return WidgetNudgeSnapshot(
                kind: .celebrating,
                title: "Nice work today",
                line: "Purrodoro is proud of you",
                ctaLabel: "Open Purrodoro",
                target: .openApp
            )
        }
        if goalReached {
            return WidgetNudgeSnapshot(
                kind: .goalReached,
                title: "Goal reached",
                line: "Purrodoro is so proud of you",
                ctaLabel: "One more?",
                target: .startFocus
            )
        }
        if streakAtRisk {
            return WidgetNudgeSnapshot(
                kind: .streakAtRisk,
                title: "Keep your \(streak)-day streak",
                line: "A small focus keeps it alive",
                ctaLabel: "Start Focus",
                target: .startFocus
            )
        }
        if let habit = firstHabit {
            return WidgetNudgeSnapshot(
                kind: .habitDue,
                title: "Time for \(habit.label)?",
                line: "Purrodoro will keep you company",
                ctaLabel: "Open Habits",
                target: .openHabits
            )
        }
        if let task = firstDueTask {
            return WidgetNudgeSnapshot(
                kind: .taskDue,
                title: "\(task.title) is waiting",
                line: "Want to focus on it together?",
                ctaLabel: "Open Tasks",
                target: .openTasks
            )
        }
        return WidgetNudgeSnapshot(
            kind: .freshFocus,
            title: "Purrodoro is ready",
            line: "Purrodoro's ready when you are",
            ctaLabel: "Start Focus",
            target: .startFocus
        )
    }
}
