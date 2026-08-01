//
//  JourneyView.swift
//  Purrodoro
//
//  The Journey tab — Android `CalendarScreen` stacked period cards:
//  Today timeline → This week chart → This month heatmap → All time.
//  History (day drill-down + delete) opens from the header icon.
//

import SwiftUI

struct JourneyView: View {
    @EnvironmentObject private var history: SessionHistoryStore
    @EnvironmentObject private var loc: Localization
    @Environment(\.colorScheme) private var scheme
    @State private var showHistory = false
    @State private var timelineExpanded = false

    private var colors: PurrodoroColors { PurrodoroColors(scheme: scheme) }
    private var mondayCal: Calendar { Journey.mondayCalendar }
    private var today: Date { mondayCal.startOfDay(for: Date()) }

    private var summary: JourneyPeriodSummary {
        Journey.summary(records: history.records, today: today, calendar: mondayCal)
    }

    private var monthRollup: JourneyMonthSummary {
        Journey.monthSummary(records: history.records, month: today, calendar: mondayCal)
    }

    private var intensities: [Date: Int] {
        History.focusIntensitiesByDate(records: history.records, calendar: mondayCal)
    }

    private var monthStreak: Int {
        Journey.monthBestStreakDays(intensities: intensities, month: today, calendar: mondayCal)
    }

    private var allTime: JourneyAllTimeSummary {
        Journey.allTimeSummary(records: history.records, calendar: mondayCal)
    }

    var body: some View {
        ZStack {
            colors.page.ignoresSafeArea()
            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    journeyHeader

                    section(loc.t("journey.today")) {
                        todayCard
                    }
                    section(loc.t("journey.periodWeek")) {
                        weeklyCard
                    }
                    section(loc.t("journey.periodMonth")) {
                        monthCard
                    }
                    section(loc.t("journey.sectionAllTime")) {
                        allTimeCard
                    }
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 120)
            }
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

    private func section<Content: View>(_ label: String, @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(label.uppercased(with: .current))
                .font(.system(.caption, design: .rounded).weight(.bold))
                .foregroundStyle(colors.textSecondary)
                .tracking(0.6)
            content()
        }
    }

    // MARK: - Today

    private var todayCard: some View {
        let rows = summary.todayRows
        return Group {
            if rows.isEmpty {
                VStack(spacing: 12) {
                    Image("CatEmptyDay")
                        .resizable()
                        .scaledToFit()
                        .frame(width: 120, height: 90)
                    Text(loc.t("journey.noRecordsToday"))
                        .font(.system(.subheadline, design: .rounded))
                        .foregroundStyle(colors.textSecondary)
                        .multilineTextAlignment(.center)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)
            } else {
                let visible = timelineExpanded ? rows : Array(rows.prefix(3))
                let hidden = max(rows.count - 3, 0)
                VStack(spacing: 0) {
                    ForEach(Array(visible.enumerated()), id: \.element.id) { index, record in
                        todayTimelineRow(record, isLatest: index == 0)
                        if index < visible.count - 1 {
                            Rectangle()
                                .fill(colors.border.opacity(0.65))
                                .frame(height: 1)
                                .padding(.leading, 28)
                        }
                    }
                    if hidden > 0 {
                        Button {
                            withAnimation(.easeInOut(duration: 0.2)) {
                                timelineExpanded.toggle()
                            }
                        } label: {
                            Text(
                                timelineExpanded
                                    ? loc.t("journey.timelineShowLess")
                                    : loc.t("journey.timelineMore", hidden)
                            )
                            .font(.system(.caption, design: .rounded).weight(.semibold))
                            .foregroundStyle(Brand.coral)
                            .frame(maxWidth: .infinity)
                            .padding(.top, 12)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(colors.card, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: 22, style: .continuous).stroke(colors.border, lineWidth: 1))
    }

    private func todayTimelineRow(_ record: FocusSessionRecord, isLatest: Bool) -> some View {
        HStack(alignment: .top, spacing: 12) {
            timelineRail(isLatest: isLatest)
            VStack(alignment: .leading, spacing: 4) {
                HStack(alignment: .top, spacing: 8) {
                    Text(record.intentName ?? loc.t("journey.focusSession"))
                        .font(.system(.subheadline, design: .rounded).weight(.semibold))
                        .foregroundStyle(colors.textPrimary)
                        .lineLimit(2)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    Image(systemName: "checkmark.circle.fill")
                        .font(.system(size: 14))
                        .foregroundStyle(Color(hex: 0x8BA05E))
                }
                Text(loc.t(
                    "journey.timelineMeta",
                    Self.timeFormatter.string(from: History.startedAt(for: record)),
                    "\(max(record.minutes, 0)) min"
                ))
                .font(.system(.caption2, design: .rounded))
                .foregroundStyle(colors.textFaint)
            }
            .padding(.vertical, 10)
            .padding(.horizontal, 12)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(colors.page.opacity(0.55), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
        }
        .padding(.vertical, 4)
        .accessibilityElement(children: .combine)
    }

    private func timelineRail(isLatest: Bool) -> some View {
        VStack(spacing: 0) {
            Circle()
                .fill(isLatest ? Brand.coral : Brand.coral.opacity(0.35))
                .frame(width: isLatest ? 10 : 8, height: isLatest ? 10 : 8)
                .padding(.top, 14)
            Rectangle()
                .fill(Brand.coral.opacity(0.25))
                .frame(width: 2)
                .frame(maxHeight: .infinity)
        }
        .frame(width: 16)
    }

    // MARK: - Week

    private var weeklyCard: some View {
        let s = summary
        let wow = s.weekOverWeekPercent
        let wowText: String = {
            if let pct = wow {
                let prefix = pct >= 0 ? "+" : ""
                return loc.t("journey.weekOverWeek", prefix, pct)
            }
            return loc.t("journey.noLastWeek")
        }()
        let wowColor: Color = (wow ?? 0) > 0 ? Color(hex: 0x5B8C5A) : colors.textSecondary
        let avgLabel = s.dailyAverageMinutes > 0
            ? History.minutesLabel(s.dailyAverageMinutes)
            : loc.t("journey.statNone")
        let sessionsLabel = s.todayFocusSessions == 1
            ? loc.t("journey.todaySession", s.todayFocusSessions)
            : loc.t("journey.todaySessions", s.todayFocusSessions)

        return VStack(alignment: .leading, spacing: 14) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 8) {
                    Text(loc.t("journey.focusTimeWeek"))
                        .font(.system(.headline, design: .rounded).weight(.bold))
                        .foregroundStyle(colors.textPrimary)
                    Text(History.minutesLabel(s.weekFocusMinutes))
                        .font(.system(size: 34, weight: .bold, design: .rounded))
                        .foregroundStyle(colors.textPrimary)
                        .monospacedDigit()
                    Text(wowText)
                        .font(.system(.caption, design: .rounded).weight(.semibold))
                        .foregroundStyle(wowColor)
                }
                Spacer(minLength: 8)
                Image("CatReady")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 72, height: 72)
                    .accessibilityHidden(true)
            }

            HStack(spacing: 10) {
                weekChip(icon: "clock", label: loc.t("journey.statDailyAvg"), value: avgLabel, highlighted: false)
                weekChip(icon: "target", label: loc.t("journey.today"), value: sessionsLabel, highlighted: true)
            }

            weekChart(bars: s.weekBars)
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(colors.card, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: 22, style: .continuous).stroke(colors.border, lineWidth: 1))
    }

    private func weekChip(icon: String, label: String, value: String, highlighted: Bool) -> some View {
        HStack(spacing: 8) {
            Image(systemName: icon)
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(Brand.coral)
            VStack(alignment: .leading, spacing: 1) {
                Text(label)
                    .font(.system(.caption2, design: .rounded))
                    .foregroundStyle(colors.textSecondary)
                Text(value)
                    .font(.system(.caption, design: .rounded).weight(.bold))
                    .foregroundStyle(colors.textPrimary)
                    .lineLimit(1)
            }
            Spacer(minLength: 0)
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 10)
        .frame(maxWidth: .infinity)
        .background(
            highlighted ? Brand.coral.opacity(0.10) : colors.page,
            in: RoundedRectangle(cornerRadius: 12, style: .continuous)
        )
        .overlay(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .stroke(highlighted ? Brand.coral.opacity(0.35) : colors.border, lineWidth: 1)
        )
    }

    private func weekChart(bars: [JourneyDayBar]) -> some View {
        let maxMinutes = bars.map(\.focusMinutes).max() ?? 0
        let scale = Journey.chartScale(maxDayMinutes: maxMinutes)
        let chartHeight: CGFloat = 120

        return HStack(alignment: .bottom, spacing: 8) {
            VStack {
                Text(History.minutesLabel(scale.topMinutes))
                Spacer()
                Text(History.minutesLabel(scale.midMinutes))
                Spacer()
                Text("0")
            }
            .font(.system(size: 10, design: .rounded).weight(.semibold))
            .foregroundStyle(colors.textFaint)
            .frame(width: 36, height: chartHeight, alignment: .trailing)

            HStack(alignment: .bottom, spacing: 6) {
                ForEach(bars) { bar in
                    let isToday = mondayCal.isDate(bar.date, inSameDayAs: today)
                    let raw = scale.topMinutes > 0
                        ? CGFloat(bar.focusMinutes) / CGFloat(scale.topMinutes)
                        : 0
                    let ratio = bar.focusMinutes > 0 ? max(raw, 0.04) : 0
                    VStack(spacing: 6) {
                        ZStack(alignment: .bottom) {
                            if isToday {
                                RoundedRectangle(cornerRadius: 6, style: .continuous)
                                    .fill(Brand.coral.opacity(0.10))
                                    .frame(maxWidth: .infinity)
                                    .frame(height: chartHeight)
                            }
                            RoundedRectangle(cornerRadius: 4, style: .continuous)
                                .fill(Brand.coral)
                                .frame(height: max(chartHeight * ratio, bar.focusMinutes > 0 ? 4 : 0))
                                .frame(maxWidth: 22)
                        }
                        .frame(height: chartHeight, alignment: .bottom)

                        Text(String(bar.dayLabel.prefix(3)))
                            .font(.system(size: 10, design: .rounded).weight(isToday ? .bold : .semibold))
                            .foregroundStyle(isToday ? Brand.coral : colors.textSecondary)
                            .lineLimit(1)
                    }
                    .frame(maxWidth: .infinity)
                    .accessibilityLabel(
                        isToday
                            ? loc.t("journey.todayColumnCd", History.minutesLabel(bar.focusMinutes))
                            : "\(bar.dayLabel), \(History.minutesLabel(bar.focusMinutes))"
                    )
                }
            }
        }
    }

    // MARK: - Month

    private var monthCard: some View {
        let grid = HistoryMonthGrid(month: today, calendar: mondayCal)
        let headers = ["M", "T", "W", "T", "F", "S", "S"]

        return HStack(alignment: .center, spacing: 16) {
            VStack(spacing: 6) {
                HStack(spacing: 4) {
                    ForEach(Array(headers.enumerated()), id: \.offset) { _, day in
                        Text(day)
                            .font(.system(size: 10, design: .rounded).weight(.semibold))
                            .foregroundStyle(colors.textSecondary)
                            .frame(maxWidth: .infinity)
                    }
                }
                let rows = stride(from: 0, to: grid.days.count, by: 7).map {
                    Array(grid.days[$0..<min($0 + 7, grid.days.count)])
                }
                ForEach(Array(rows.enumerated()), id: \.offset) { _, row in
                    HStack(spacing: 4) {
                        ForEach(row, id: \.self) { date in
                            monthHeatCell(date: date, inMonth: grid.isInMonth(date, calendar: mondayCal))
                        }
                    }
                }
            }
            .frame(maxWidth: .infinity)

            VStack(alignment: .leading, spacing: 14) {
                monthStat(icon: "calendar", value: "\(monthRollup.focusDays)", label: loc.t("journey.statFocusDays"))
                Divider()
                monthStat(icon: "clock", value: monthRollup.focusTimeLabel, label: loc.t("journey.allTimeTotal"))
                Divider()
                monthStat(icon: "flame.fill", value: "\(monthStreak)", label: loc.t("journey.statStreak"))
            }
            .frame(width: 96, alignment: .leading)
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(colors.card, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: 22, style: .continuous).stroke(colors.border, lineWidth: 1))
        .accessibilityElement(children: .contain)
    }

    private func monthHeatCell(date: Date, inMonth: Bool) -> some View {
        let intensity = inMonth ? (intensities[date] ?? 0) : 0
        let isToday = inMonth && mondayCal.isDate(date, inSameDayAs: today)
        return ZStack(alignment: .topTrailing) {
            RoundedRectangle(cornerRadius: 4, style: .continuous)
                .fill(inMonth ? heatColor(intensity) : Color.clear)
                .aspectRatio(1, contentMode: .fit)
            if isToday {
                Circle()
                    .fill(intensity >= 3 ? Color.white : Brand.coral)
                    .frame(width: 5, height: 5)
                    .padding(2)
            }
        }
        .frame(maxWidth: .infinity)
        .accessibilityHidden(!inMonth)
        .accessibilityLabel(inMonth ? Self.dayA11yFormatter.string(from: date) : "")
    }

    private func monthStat(icon: String, value: String, label: String) -> some View {
        HStack(alignment: .top, spacing: 8) {
            Image(systemName: icon)
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(Brand.coral)
                .frame(width: 16)
            VStack(alignment: .leading, spacing: 2) {
                Text(value)
                    .font(.system(.subheadline, design: .rounded).weight(.bold))
                    .foregroundStyle(colors.textPrimary)
                    .monospacedDigit()
                Text(label)
                    .font(.system(.caption2, design: .rounded))
                    .foregroundStyle(colors.textSecondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(label)
        .accessibilityValue(value)
    }

    private func heatColor(_ level: Int) -> Color {
        switch level {
        case 1: return Brand.coral.opacity(0.22)
        case 2: return Brand.coral.opacity(0.45)
        case 3: return Brand.coral.opacity(0.70)
        case 4...: return Brand.coral
        default: return colors.border.opacity(0.45)
        }
    }

    // MARK: - All time

    private var allTimeCard: some View {
        HStack(alignment: .center, spacing: 12) {
            VStack(alignment: .leading, spacing: 14) {
                allTimeStat(icon: "clock", value: allTime.totalFocusLabel, label: loc.t("journey.allTimeTotal"))
                allTimeStat(icon: "flame.fill", value: "\(allTime.bestStreakDays)", label: loc.t("journey.allTimeStreak"))
                allTimeStat(icon: "target", value: "\(allTime.totalSessions)", label: loc.t("journey.allTimeSessions"))
            }
            Spacer(minLength: 8)
            Image("CatCelebrate")
                .resizable()
                .scaledToFit()
                .frame(width: 96, height: 96)
                .accessibilityHidden(true)
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(colors.card, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: 22, style: .continuous).stroke(colors.border, lineWidth: 1))
    }

    private func allTimeStat(icon: String, value: String, label: String) -> some View {
        HStack(spacing: 10) {
            Image(systemName: icon)
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(Brand.coral)
                .frame(width: 20)
            VStack(alignment: .leading, spacing: 2) {
                Text(value)
                    .font(.system(.title3, design: .rounded).weight(.bold))
                    .foregroundStyle(colors.textPrimary)
                    .monospacedDigit()
                Text(label)
                    .font(.system(.caption2, design: .rounded))
                    .foregroundStyle(colors.textSecondary)
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(label)
        .accessibilityValue(value)
    }

    private static let timeFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "h:mm a"
        return f
    }()

    private static let dayA11yFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateStyle = .medium
        return f
    }()
}

#Preview {
    JourneyView()
        .environmentObject(SessionHistoryStore())
        .environmentObject(Localization())
}
