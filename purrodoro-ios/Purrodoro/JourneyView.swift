//
//  JourneyView.swift
//  Purrodoro
//
//  The Journey tab: focus streaks, lightweight stats, and a month calendar
//  heatmap driven by the per-day focus stamps the engine records.
//

import SwiftUI

struct JourneyView: View {
    @EnvironmentObject private var engine: TimerEngine
    @EnvironmentObject private var history: SessionHistoryStore
    @EnvironmentObject private var loc: Localization
    @Environment(\.colorScheme) private var scheme
    @State private var month: Date = Date()

    private var colors: PurrodoroColors { PurrodoroColors(scheme: scheme) }
    private var stamps: [String: Int] { engine.focusStampsByDay }
    private var minutes: [String: Int] { engine.focusMinutesByDay }

    var body: some View {
        ZStack {
            colors.page.ignoresSafeArea()
            content
        }
        .environment(\.pColors, colors)
    }

    @ViewBuilder
    private var content: some View {
        if Journey.total(stamps) == 0 {
            PlaceholderScreen(
                title: loc.t("journey.title"),
                subtitle: loc.t("journey.emptySubtitle"),
                image: "CatEmptyMonth"
            )
        } else {
            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    Text(loc.t("journey.title"))
                        .font(.system(.largeTitle, design: .rounded).weight(.bold))
                        .foregroundStyle(colors.textPrimary)
                        .padding(.top, 12)

                    streakHero
                    statsRow
                    calendarCard
                    recentSessionsCard
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 120)
            }
        }
    }

    // MARK: Streak hero

    private var streakHero: some View {
        let streak = Journey.currentStreak(stamps)
        return HStack(spacing: 16) {
            ZStack {
                Circle().fill(Brand.coral.opacity(0.15)).frame(width: 64, height: 64)
                Image(systemName: "flame.fill")
                    .font(.system(size: 28))
                    .foregroundStyle(Brand.coral)
            }
            VStack(alignment: .leading, spacing: 2) {
                Text(loc.t(streak == 1 ? "habits.dayStreak" : "habits.daysStreak", streak))
                    .font(.system(.title, design: .rounded).weight(.bold))
                    .foregroundStyle(colors.textPrimary)
                Text(streak == 0 ? loc.t("journey.startToday") : loc.t("journey.currentStreak"))
                    .font(.system(.subheadline, design: .rounded))
                    .foregroundStyle(colors.textSecondary)
            }
            Spacer()
        }
        .padding(18)
        .frame(maxWidth: .infinity)
        .background(colors.card, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: 22, style: .continuous).stroke(colors.border, lineWidth: 1))
    }

    // MARK: Stats

    private var statsRow: some View {
        HStack(spacing: 12) {
            statCard(value: loc.t("unit.minAbbrev", Journey.sum(minutes, lastDays: 7)), label: loc.t("journey.thisWeek"))
            statCard(value: "\(Journey.total(stamps))", label: loc.t("journey.totalSessions"))
            statCard(value: "\(Journey.bestStreak(stamps))", label: loc.t("journey.bestStreak"))
        }
    }

    private func statCard(value: String, label: String) -> some View {
        VStack(spacing: 4) {
            Text(value)
                .font(.system(.title3, design: .rounded).weight(.bold))
                .foregroundStyle(colors.textPrimary)
                .monospacedDigit()
            Text(label)
                .font(.system(.caption2, design: .rounded))
                .foregroundStyle(colors.textFaint)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 16)
        .background(colors.card, in: RoundedRectangle(cornerRadius: 18, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: 18, style: .continuous).stroke(colors.border, lineWidth: 1))
    }

    // MARK: Calendar

    private var calendarCard: some View {
        let grid = MonthGrid(month: month)
        return VStack(spacing: 14) {
            HStack {
                Text(grid.title)
                    .font(.system(.headline, design: .rounded).weight(.bold))
                    .foregroundStyle(colors.textPrimary)
                Spacer()
                monthButton(systemName: "chevron.left") { shiftMonth(-1) }
                monthButton(systemName: "chevron.right") { shiftMonth(1) }
            }

            HStack(spacing: 0) {
                ForEach(Array(grid.weekdaySymbols.enumerated()), id: \.offset) { _, sym in
                    Text(sym)
                        .font(.system(.caption2, design: .rounded).weight(.semibold))
                        .foregroundStyle(colors.textFaint)
                        .frame(maxWidth: .infinity)
                }
            }

            let columns = Array(repeating: GridItem(.flexible(), spacing: 6), count: 7)
            LazyVGrid(columns: columns, spacing: 6) {
                ForEach(grid.days) { cell in
                    dayCell(cell)
                }
            }

            legend
        }
        .padding(18)
        .background(colors.card, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: 22, style: .continuous).stroke(colors.border, lineWidth: 1))
    }

    @ViewBuilder
    private func dayCell(_ cell: MonthDay) -> some View {
        if let date = cell.date {
            let count = Journey.count(stamps, on: date)
            let isToday = Journey.calendar.isDateInToday(date)
            Text("\(Journey.calendar.component(.day, from: date))")
                .font(.system(.caption, design: .rounded).weight(.semibold))
                .foregroundStyle(count > 0 ? Brand.cocoa : colors.textFaint)
                .frame(maxWidth: .infinity)
                .frame(height: 38)
                .background(intensity(count), in: RoundedRectangle(cornerRadius: 10, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: 10, style: .continuous)
                        .stroke(Brand.coral, lineWidth: isToday ? 2 : 0)
                )
        } else {
            Color.clear.frame(height: 38)
        }
    }

    private func intensity(_ count: Int) -> Color {
        switch count {
        case 0: return colors.scheme == .dark ? Brand.nightDeep : Brand.pageWhite
        case 1: return Brand.peach.opacity(0.45)
        case 2: return Brand.peach
        default: return Brand.coral
        }
    }

    private var legend: some View {
        HStack(spacing: 8) {
            Text(loc.t("journey.less"))
                .font(.system(.caption2, design: .rounded))
                .foregroundStyle(colors.textFaint)
            ForEach([0, 1, 2, 3], id: \.self) { c in
                RoundedRectangle(cornerRadius: 4)
                    .fill(intensity(c))
                    .frame(width: 16, height: 16)
                    .overlay(RoundedRectangle(cornerRadius: 4).stroke(colors.border, lineWidth: c == 0 ? 1 : 0))
            }
            Text(loc.t("journey.more"))
                .font(.system(.caption2, design: .rounded))
                .foregroundStyle(colors.textFaint)
            Spacer()
        }
    }

    // MARK: Recent sessions

    @ViewBuilder
    private var recentSessionsCard: some View {
        if !history.records.isEmpty {
            VStack(alignment: .leading, spacing: 12) {
                Text(loc.t("journey.recentSessions"))
                    .font(.system(.headline, design: .rounded).weight(.bold))
                    .foregroundStyle(colors.textPrimary)

                VStack(spacing: 0) {
                    let recent = Array(history.records.prefix(8))
                    ForEach(Array(recent.enumerated()), id: \.element.id) { index, record in
                        sessionRow(record)
                        if index < recent.count - 1 {
                            Rectangle()
                                .fill(colors.border)
                                .frame(height: 1)
                        }
                    }
                }
            }
            .padding(18)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(colors.card, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
            .overlay(RoundedRectangle(cornerRadius: 22, style: .continuous).stroke(colors.border, lineWidth: 1))
        }
    }

    private func sessionRow(_ record: FocusSessionRecord) -> some View {
        HStack(spacing: 12) {
            ZStack {
                Circle().fill(Brand.coral.opacity(0.15)).frame(width: 38, height: 38)
                Image(systemName: record.intentName == nil ? "timer" : "target")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(Brand.coral)
            }
            VStack(alignment: .leading, spacing: 2) {
                Text(record.intentName ?? loc.t("journey.focusSession"))
                    .font(.system(.subheadline, design: .rounded).weight(.semibold))
                    .foregroundStyle(colors.textPrimary)
                    .lineLimit(1)
                Text(relativeStamp(record.endedAt))
                    .font(.system(.caption2, design: .rounded))
                    .foregroundStyle(colors.textFaint)
            }
            Spacer()
            Text(loc.t("unit.min", record.minutes))
                .font(.system(.subheadline, design: .rounded).weight(.semibold))
                .foregroundStyle(colors.textSecondary)
                .monospacedDigit()
        }
        .padding(.vertical, 10)
    }

    /// "Today · 2:30 PM" / "Yesterday · 2:30 PM" / "Jun 3 · 2:30 PM".
    private func relativeStamp(_ date: Date) -> String {
        let cal = Journey.calendar
        let time = Self.timeFormatter.string(from: date)
        if cal.isDateInToday(date) { return "\(loc.t("date.today")) · \(time)" }
        if cal.isDateInYesterday(date) { return "\(loc.t("date.yesterday")) · \(time)" }
        return "\(Self.dayFormatter.string(from: date)) · \(time)"
    }

    private static let timeFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "h:mm a"
        return f
    }()

    private static let dayFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "MMM d"
        return f
    }()

    private func monthButton(systemName: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: systemName)
                .font(.system(size: 14, weight: .bold))
                .foregroundStyle(colors.textSecondary)
                .frame(width: 34, height: 34)
                .background(colors.page, in: Circle())
                .overlay(Circle().stroke(colors.border, lineWidth: 1))
        }
        .buttonStyle(.plain)
    }

    private func shiftMonth(_ delta: Int) {
        if let next = Journey.calendar.date(byAdding: .month, value: delta, to: month) {
            withAnimation(.easeInOut(duration: 0.2)) { month = next }
        }
    }
}

#Preview {
    JourneyView()
        .environmentObject(TimerEngine())
        .environmentObject(SessionHistoryStore())
        .environmentObject(Localization())
}
