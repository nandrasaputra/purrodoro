//
//  HabitsView.swift
//  Purrodoro
//
//  The Habits tab: gentle daily rituals with a per-day target, a 7-day
//  completion strip, a streak, tap-to-log, and a "focus on this" action that
//  routes a session toward the habit.
//

import SwiftUI

struct HabitsView: View {
    @EnvironmentObject private var store: HabitStore
    @Environment(\.colorScheme) private var scheme
    @State private var editing: HabitItem?
    @State private var showAdd = false

    var onFocusHabit: (HabitItem) -> Void = { _ in }

    private var colors: PurrodoroColors { PurrodoroColors(scheme: scheme) }

    var body: some View {
        ZStack(alignment: .bottomTrailing) {
            colors.page.ignoresSafeArea()
            content
            Button {
                showAdd = true
            } label: {
                Image(systemName: "plus")
                    .font(.system(size: 24, weight: .bold))
                    .foregroundStyle(.white)
                    .frame(width: 60, height: 60)
                    .background(Brand.coral, in: Circle())
                    .shadow(color: Brand.coral.opacity(0.4), radius: 12, y: 6)
            }
            .padding(.trailing, 24)
            .padding(.bottom, 96)
            .accessibilityLabel("Add habit")
        }
        .environment(\.pColors, colors)
        .sheet(isPresented: $showAdd) {
            HabitEditor(habit: nil) { store.add(name: $0, emoji: $1, colorHex: $2, target: $3) }
        }
        .sheet(item: $editing) { habit in
            HabitEditor(habit: habit) { name, emoji, color, target in
                var updated = habit
                updated.name = name
                updated.emoji = emoji
                updated.colorHex = color
                updated.targetPerDay = max(target, 1)
                store.update(updated)
            }
        }
    }

    @ViewBuilder
    private var content: some View {
        if store.habits.isEmpty {
            PlaceholderScreen(
                title: "Habits",
                subtitle: "Add a gentle daily ritual and keep your streak alive, one focus at a time.",
                image: "CatCelebrate"
            )
        } else {
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    Text("Habits")
                        .font(.system(.largeTitle, design: .rounded).weight(.bold))
                        .foregroundStyle(colors.textPrimary)
                        .padding(.top, 12)
                        .padding(.bottom, 4)

                    ForEach(store.sortedHabits) { habit in
                        HabitCard(
                            habit: habit,
                            isActive: store.activeHabitID == habit.id,
                            colors: colors,
                            onToggle: { store.toggleToday(habit) },
                            onFocus: { onFocusHabit(habit) }
                        )
                        .contextMenu {
                            Button { editing = habit } label: { Label("Edit", systemImage: "pencil") }
                            Button(role: .destructive) { store.delete(habit) } label: {
                                Label("Delete", systemImage: "trash")
                            }
                        }
                        .onTapGesture { editing = habit }
                    }
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 170)
            }
        }
    }
}

// MARK: - Card

private struct HabitCard: View {
    let habit: HabitItem
    let isActive: Bool
    let colors: PurrodoroColors
    let onToggle: () -> Void
    let onFocus: () -> Void

    var body: some View {
        VStack(spacing: 14) {
            HStack(spacing: 12) {
                Text(habit.emoji)
                    .font(.system(size: 26))
                    .frame(width: 48, height: 48)
                    .background(habit.color.opacity(0.35), in: RoundedRectangle(cornerRadius: 14, style: .continuous))

                VStack(alignment: .leading, spacing: 2) {
                    Text(habit.name)
                        .font(.system(.body, design: .rounded).weight(.semibold))
                        .foregroundStyle(colors.textPrimary)
                        .lineLimit(1)
                    HStack(spacing: 4) {
                        Image(systemName: "flame.fill")
                            .font(.system(size: 11))
                            .foregroundStyle(Brand.coral)
                        Text("\(habit.streak) day\(habit.streak == 1 ? "" : "s") · \(habit.todayCount)/\(habit.targetPerDay) today")
                            .font(.system(.caption2, design: .rounded))
                            .foregroundStyle(colors.textFaint)
                    }
                }

                Spacer()

                Button(action: onFocus) {
                    Image(systemName: isActive ? "play.circle.fill" : "play.circle")
                        .font(.system(size: 26))
                        .foregroundStyle(isActive ? Brand.coral : colors.textSecondary)
                }
                .buttonStyle(.plain)
                .accessibilityLabel(isActive ? "Active focus habit" : "Focus on this habit")

                Button(action: onToggle) {
                    Image(systemName: habit.isDoneToday ? "checkmark.circle.fill" : "circle")
                        .font(.system(size: 28))
                        .foregroundStyle(habit.isDoneToday ? habit.color : colors.textFaint)
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Log today")
            }

            weekStrip
        }
        .padding(14)
        .background(colors.card, in: RoundedRectangle(cornerRadius: 18, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .stroke(isActive ? Brand.coral : colors.border, lineWidth: isActive ? 2 : 1)
        )
        .contentShape(Rectangle())
    }

    private var weekStrip: some View {
        let cal = Journey.calendar
        let today = Date()
        let days: [Date] = (0..<7).reversed().compactMap {
            cal.date(byAdding: .day, value: -$0, to: today)
        }
        return HStack(spacing: 0) {
            ForEach(days, id: \.self) { date in
                let done = habit.meetsTarget(on: date)
                let isToday = cal.isDateInToday(date)
                VStack(spacing: 5) {
                    Text(shortWeekday(date))
                        .font(.system(size: 10, design: .rounded).weight(.semibold))
                        .foregroundStyle(colors.textFaint)
                    Circle()
                        .fill(done ? habit.color : colors.page)
                        .frame(width: 22, height: 22)
                        .overlay(Circle().stroke(isToday ? Brand.coral : colors.border, lineWidth: isToday ? 2 : 1))
                }
                .frame(maxWidth: .infinity)
            }
        }
    }

    private func shortWeekday(_ date: Date) -> String {
        let f = DateFormatter()
        f.dateFormat = "EEEEE"
        return f.string(from: date)
    }
}

// MARK: - Editor

private struct HabitEditor: View {
    let habit: HabitItem?
    let onSave: (String, String, UInt32, Int) -> Void

    @Environment(\.dismiss) private var dismiss
    @Environment(\.colorScheme) private var scheme
    @State private var name: String
    @State private var emoji: String
    @State private var colorHex: UInt32
    @State private var target: Int

    private var colors: PurrodoroColors { PurrodoroColors(scheme: scheme) }

    init(habit: HabitItem?, onSave: @escaping (String, String, UInt32, Int) -> Void) {
        self.habit = habit
        self.onSave = onSave
        _name = State(initialValue: habit?.name ?? "")
        _emoji = State(initialValue: habit?.emoji ?? "🌱")
        _colorHex = State(initialValue: habit?.colorHex ?? HabitStore.palette[0])
        _target = State(initialValue: habit?.targetPerDay ?? 1)
    }

    var body: some View {
        NavigationStack {
            ZStack {
                colors.page.ignoresSafeArea()
                ScrollView {
                    VStack(spacing: 16) {
                        TextField("Habit name", text: $name)
                            .font(.system(.body, design: .rounded))
                            .padding(16)
                            .background(colors.card, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                            .overlay(RoundedRectangle(cornerRadius: 16, style: .continuous).stroke(colors.border, lineWidth: 1))

                        card(title: "Icon") {
                            LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 6), spacing: 12) {
                                ForEach(HabitStore.emojis, id: \.self) { e in
                                    Text(e)
                                        .font(.system(size: 24))
                                        .frame(width: 40, height: 40)
                                        .background(emoji == e ? Color(hex: colorHex).opacity(0.4) : colors.page,
                                                    in: RoundedRectangle(cornerRadius: 10, style: .continuous))
                                        .overlay(RoundedRectangle(cornerRadius: 10, style: .continuous)
                                            .stroke(emoji == e ? Brand.coral : colors.border, lineWidth: emoji == e ? 2 : 1))
                                        .onTapGesture { emoji = e }
                                }
                            }
                        }

                        card(title: "Color") {
                            HStack(spacing: 12) {
                                ForEach(HabitStore.palette, id: \.self) { c in
                                    Circle()
                                        .fill(Color(hex: c))
                                        .frame(width: 34, height: 34)
                                        .overlay(Circle().stroke(colors.textPrimary, lineWidth: colorHex == c ? 2.5 : 0))
                                        .onTapGesture { colorHex = c }
                                }
                                Spacer()
                            }
                        }

                        card(title: nil) {
                            Stepper(value: $target, in: 1...8) {
                                HStack {
                                    Text("Daily target")
                                        .font(.system(.body, design: .rounded).weight(.semibold))
                                        .foregroundStyle(colors.textPrimary)
                                    Spacer()
                                    Text("\(target) / day")
                                        .font(.system(.body, design: .rounded).weight(.bold))
                                        .foregroundStyle(Brand.coral)
                                        .monospacedDigit()
                                }
                            }
                        }
                    }
                    .padding(20)
                }
            }
            .navigationTitle(habit == nil ? "New habit" : "Edit habit")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        onSave(name, emoji, colorHex, target)
                        dismiss()
                    }
                    .font(.system(.body, design: .rounded).weight(.bold))
                    .disabled(name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
            }
        }
    }

    private func card<Content: View>(title: String?, @ViewBuilder _ content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            if let title {
                Text(title)
                    .font(.system(.subheadline, design: .rounded).weight(.semibold))
                    .foregroundStyle(colors.textSecondary)
            }
            content()
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(colors.card, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: 16, style: .continuous).stroke(colors.border, lineWidth: 1))
    }
}

#Preview {
    HabitsView().environmentObject(HabitStore())
}
