//
//  JourneyView.swift
//  Purrodoro
//
//  The Journey tab: stacked Today / This week / This month / All time sections
//  (Android `CalendarScreen`), driven by focus session records + stamps.
//

import SwiftUI

struct JourneyView: View {
    @EnvironmentObject private var engine: TimerEngine
    @EnvironmentObject private var history: SessionHistoryStore
    @EnvironmentObject private var loc: Localization
    @Environment(\.colorScheme) private var scheme
    @State private var showHistory = false
    @State private var timelineExpanded = false

    private var colors: PurrodoroColors { PurrodoroColors(scheme: scheme) }
    private var records: [FocusSessionRecord] { history.records }
    private var summary: JourneyPeriodSummary {
        Journey.journeySummary(records: records)
    }
    private var month: JourneyMonthSummary {
        Journey.monthSummary(records: records)
    }
    private var intensities: [Date: Int] {
        History.focusIntensitiesByDate(records: records)
    }
    private var monthStreak: Int {
        Journey.monthBestStreakDays(intensities: intensities, month: Date())
    }
    private var allTime: JourneyAllTimeSummary {
        Journey.allTimeSummary(records: records)
    }

    var body: some View {
        ZStack {
            colors.page.ignoresSafeArea()
            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    journeyHeader
                    periodSection(loc.t("journey.today")) {
                        todayCard
                    }
                    periodSection(loc.t("journey.periodWeek")) {
                        weeklyCard
                    }
                    periodSection(loc.t("journey.periodMonth")) {
                        monthCard
                    }
                    periodSection(loc.t("journey.sectionAllTime")) {
                        allTimeCard
                    }
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 120)
            }
            .readableContentWidth()
        }
        .environment(\.pColors, colors)
        .fullScreenCover(isPresented: $showHistory) {
            HistoryView()
                .environmentObject(history)
                .environmentObject(loc)
        }
    }

    private var journeyHeader: some View {
        HStack(alignment: .center) {
            Text(loc.t("journey.title"))
                .font(.system(.largeTitle, design: .rounded).weight(.bold))
                .foregroundStyle(colors.textPrimary)
            Spacer()
            Button {
                showHistory = true
            } label: {
                Image(systemName: "clock.arrow.circlepath")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundStyle(colors.textPrimary)
                    .frame(width: 40, height: 40)
                    .contentShape(Circle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(loc.t("journey.openHistory"))
        }
        .padding(.top, 12)
    }

    private func periodSection<Content: View>(_ label: String, @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(label.uppercased())
                .font(.system(.caption, design: .rounded).weight(.semibold))
                .foregroundStyle(colors.textSecondary)
                .tracking(0.6)
            content()
        }
    }

    // MARK: Today

    private var todayCard: some View {
        let rows = summary.todayRows
        return Group {
            if rows.isEmpty {
                HStack(spacing: 14) {
                    Image("CatEmptyDay")
                        .resizable()
                        .scaledToFit()
                        .frame(width: 72, height: 72)
                    Text(loc.t("journey.noRecordsToday"))
                        .font(.system(.subheadline, design: .rounded))
                        .foregroundStyle(colors.textSecondary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
                .padding(18)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(colors.card, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
                .overlay(RoundedRectangle(cornerRadius: 22, style: .continuous).stroke(colors.border, lineWidth: 1))
            } else {
                VStack(spacing: 0) {
                    let collapsed = 3
                    let visible = timelineExpanded || rows.count <= collapsed
                        ? rows
                        : Array(rows.prefix(collapsed))
                    ForEach(Array(visible.enumerated()), id: \.element.id) { index, record in
                        timelineRow(record, isLatest: index == 0)
                        if index < visible.count - 1 {
                            Rectangle().fill(colors.border).frame(height: 1)
                        }
                    }
                    if rows.count > collapsed {
                        Button {
                            withAnimation(.easeInOut(duration: 0.2)) {
                                timelineExpanded.toggle()
                            }
                        } label: {
                            Text(
                                timelineExpanded
                                    ? loc.t("journey.timelineShowLess")
                                    : loc.t("journey.timelineMore", rows.count - collapsed)
                            )
                            .font(.system(.caption, design: .rounded).weight(.semibold))
                            .foregroundStyle(Brand.coral)
                            .frame(maxWidth: .infinity)
                            .padding(.top, 12)
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(18)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(colors.card, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
                .overlay(RoundedRectangle(cornerRadius: 22, style: .continuous).stroke(colors.border, lineWidth: 1))
            }
        }
    }

    private func timelineRow(_ record: FocusSessionRecord, isLatest: Bool) -> some View {
        HStack(spacing: 12) {
            Circle()
                .fill(isLatest ? Brand.coral : colors.border)
                .frame(width: 10, height: 10)
            VStack(alignment: .leading, spacing: 2) {
                Text(record.intentName ?? loc.t("journey.focusSession"))
                    .font(.system(.subheadline, design: .rounded).weight(.semibold))
                    .foregroundStyle(colors.textPrimary)
                    .lineLimit(1)
                Text(loc.t("journey.timelineMeta", timeLabel(record), loc.t("journey.statusCompleted")))
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

    // MARK: Week

    private var weeklyCard: some View {
        let note: String = {
            if let pct = summary.weekOverWeekPercent {
                let sign = pct >= 0 ? "+" : ""
                return loc.t("journey.weekOverWeek", sign, pct)
            }
            return loc.t("journey.noLastWeek")
        }()
        let noteColor: Color = (summary.weekOverWeekPercent ?? 0) > 0 ? Color(hex: 0x3D9A6A) : colors.textSecondary
        let avgLabel = summary.dailyAverageMinutes > 0
            ? History.minutesLabel(summary.dailyAverageMinutes)
            : loc.t("journey.statNone")
        let todaySessionsLabel = loc.t(
            summary.todayFocusSessions == 1 ? "journey.todaySessions.one" : "journey.todaySessions.other",
            summary.todayFocusSessions
        )
        let maxDay = summary.weekBars.map(\.focusMinutes).max() ?? 0
        let scale = Journey.chartScale(maxDayMinutes: maxDay)
        let today = Journey.calendar.startOfDay(for: Date())

        return VStack(alignment: .leading, spacing: 14) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 6) {
                    Text(loc.t("journey.focusTimeWeek"))
                        .font(.system(.headline, design: .rounded).weight(.bold))
                        .foregroundStyle(colors.textPrimary)
                    Text(History.minutesLabel(summary.weekFocusMinutes))
                        .font(.system(.largeTitle, design: .rounded).weight(.bold))
                        .foregroundStyle(colors.textPrimary)
                        .monospacedDigit()
                    Text(note)
                        .font(.system(.caption, design: .rounded))
                        .foregroundStyle(noteColor)
                }
                Spacer()
                Image("CatReady")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 72, height: 72)
            }

            HStack(spacing: 10) {
                weekChip(
                    icon: "clock",
                    label: loc.t("journey.statDailyAvg"),
                    value: avgLabel,
                    highlighted: false
                )
                weekChip(
                    icon: "target",
                    label: loc.t("journey.today"),
                    value: todaySessionsLabel,
                    highlighted: true
                )
            }

            weekChart(scale: scale, today: today)
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(colors.card, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: 22, style: .continuous).stroke(colors.border, lineWidth: 1))
    }

    private func weekChip(icon: String, label: String, value: String, highlighted: Bool) -> some View {
        HStack(spacing: 10) {
            Image(systemName: icon)
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(Brand.coral)
            VStack(alignment: .leading, spacing: 2) {
                Text(label)
                    .font(.system(.caption2, design: .rounded))
                    .foregroundStyle(colors.textFaint)
                Text(value)
                    .font(.system(.subheadline, design: .rounded).weight(.semibold))
                    .foregroundStyle(colors.textPrimary)
            }
            Spacer(minLength: 0)
        }
        .padding(12)
        .frame(maxWidth: .infinity)
        .background(
            highlighted ? Brand.coral.opacity(0.12) : colors.page,
            in: RoundedRectangle(cornerRadius: 14, style: .continuous)
        )
        .overlay(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .stroke(highlighted ? Brand.coral.opacity(0.35) : colors.border, lineWidth: 1)
        )
    }

    private func weekChart(scale: JourneyChartScale, today: Date) -> some View {
        let top = max(scale.topMinutes, 1)
        return HStack(alignment: .bottom, spacing: 8) {
            VStack(alignment: .trailing) {
                Text(History.minutesLabel(scale.topMinutes))
                Spacer()
                Text(History.minutesLabel(scale.midMinutes))
                Spacer()
                Text("0")
            }
            .font(.system(.caption2, design: .rounded))
            .foregroundStyle(colors.textFaint)
            .frame(width: 36, height: 120)

            HStack(alignment: .bottom, spacing: 4) {
                ForEach(Array(summary.weekBars.enumerated()), id: \.offset) { _, bar in
                    let isToday = Journey.calendar.isDate(bar.date, inSameDayAs: today)
                    let ratio: CGFloat = bar.focusMinutes <= 0
                        ? 0
                        : CGFloat(min(max(Double(bar.focusMinutes) / Double(top), 0.04), 1))
                    VStack(spacing: 6) {
                        ZStack(alignment: .bottom) {
                            if isToday {
                                RoundedRectangle(cornerRadius: 8, style: .continuous)
                                    .fill(Brand.coral.opacity(0.12))
                                    .frame(maxWidth: .infinity)
                                    .frame(height: 120)
                            }
                            RoundedRectangle(cornerRadius: 4, style: .continuous)
                                .fill(Brand.coral)
                                .frame(width: 14, height: max(4, 100 * ratio))
                                .opacity(bar.focusMinutes > 0 ? 1 : 0)
                        }
                        .frame(height: 120)
                        Text(String(bar.dayLabel.prefix(3)))
                            .font(.system(.caption2, design: .rounded).weight(isToday ? .bold : .regular))
                            .foregroundStyle(isToday ? Brand.coral : colors.textFaint)
                    }
                    .frame(maxWidth: .infinity)
                    .accessibilityLabel(
                        isToday
                            ? loc.t("journey.todayColumnA11y", History.minutesLabel(bar.focusMinutes))
                            : "\(bar.dayLabel), \(History.minutesLabel(bar.focusMinutes))"
                    )
                }
            }
        }
    }

    // MARK: Month

    private var monthCard: some View {
        let grid = MonthGrid(month: Date(), mondayFirst: true)
        return HStack(alignment: .center, spacing: 16) {
            VStack(spacing: 8) {
                HStack(spacing: 0) {
                    ForEach(Array(grid.mondayWeekdaySymbols.enumerated()), id: \.offset) { _, sym in
                        Text(sym)
                            .font(.system(size: 10, design: .rounded).weight(.semibold))
                            .foregroundStyle(colors.textFaint)
                            .frame(maxWidth: .infinity)
                    }
                }
                let columns = Array(repeating: GridItem(.flexible(), spacing: 4), count: 7)
                LazyVGrid(columns: columns, spacing: 4) {
                    ForEach(grid.days) { cell in
                        monthHeatCell(cell)
                    }
                }
            }
            .frame(maxWidth: .infinity)

            VStack(alignment: .leading, spacing: 14) {
                monthStat(icon: "calendar", value: "\(month.focusDays)", label: loc.t("journey.statFocusDays"))
                Divider()
                monthStat(icon: "clock", value: month.focusTimeLabel, label: loc.t("journey.allTimeTotal"))
                Divider()
                monthStat(icon: "flame.fill", value: "\(monthStreak)", label: loc.t("journey.statStreak"))
            }
            .frame(width: 110)
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(colors.card, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: 22, style: .continuous).stroke(colors.border, lineWidth: 1))
    }

    @ViewBuilder
    private func monthHeatCell(_ cell: MonthDay) -> some View {
        if let date = cell.date {
            let level = intensities[Journey.calendar.startOfDay(for: date)] ?? 0
            let isToday = Journey.calendar.isDateInToday(date)
            ZStack(alignment: .topTrailing) {
                RoundedRectangle(cornerRadius: 5, style: .continuous)
                    .fill(heatColor(level))
                    .aspectRatio(1, contentMode: .fit)
                if isToday {
                    Circle()
                        .fill(level >= 3 ? Color.white : Brand.coral)
                        .frame(width: 5, height: 5)
                        .padding(2)
                }
            }
        } else {
            Color.clear.aspectRatio(1, contentMode: .fit)
        }
    }

    private func heatColor(_ level: Int) -> Color {
        switch level {
        case 1: return Brand.coral.opacity(0.22)
        case 2: return Brand.coral.opacity(0.45)
        case 3: return Brand.coral.opacity(0.70)
        case 4: return Brand.coral
        default: return colors.border.opacity(0.45)
        }
    }

    private func monthStat(icon: String, value: String, label: String) -> some View {
        HStack(spacing: 8) {
            Image(systemName: icon)
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(Brand.coral)
                .frame(width: 16)
            VStack(alignment: .leading, spacing: 1) {
                Text(value)
                    .font(.system(.subheadline, design: .rounded).weight(.bold))
                    .foregroundStyle(colors.textPrimary)
                    .monospacedDigit()
                Text(label)
                    .font(.system(.caption2, design: .rounded))
                    .foregroundStyle(colors.textFaint)
            }
        }
    }

    // MARK: All time

    private var allTimeCard: some View {
        HStack(spacing: 8) {
            allTimeStat(icon: "clock", value: allTime.totalFocusLabel, label: loc.t("journey.allTimeTotal"))
            allTimeStat(icon: "flame.fill", value: "\(allTime.bestStreakDays)", label: loc.t("journey.allTimeStreak"))
            allTimeStat(icon: "target", value: "\(allTime.totalSessions)", label: loc.t("journey.allTimeSessions"))
            Image("CatCelebrate")
                .resizable()
                .scaledToFit()
                .frame(width: 64, height: 72)
        }
        .padding(18)
        .frame(maxWidth: .infinity)
        .background(colors.card, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: 22, style: .continuous).stroke(colors.border, lineWidth: 1))
    }

    private func allTimeStat(icon: String, value: String, label: String) -> some View {
        VStack(spacing: 8) {
            ZStack {
                Circle().fill(Brand.coral.opacity(0.15)).frame(width: 36, height: 36)
                Image(systemName: icon)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(Brand.coral)
            }
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
    }

    private func timeLabel(_ record: FocusSessionRecord) -> String {
        Self.timeFormatter.string(from: History.startedAt(for: record))
    }

    private static let timeFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "h:mm a"
        return f
    }()
}

#Preview {
    JourneyView()
        .environmentObject(TimerEngine())
        .environmentObject(SessionHistoryStore())
        .environmentObject(Localization())
}
