//
//  HistoryView.swift
//  Purrodoro
//
//  History — full-screen day drill-down entered from the Journey tab history
//  icon. Interactive monthly heatmap + per-day session timeline with inspect /
//  delete. Ported from Android `HistoryScreen.kt`, adapted to iOS per-segment
//  `FocusSessionRecord`s (no rest totals / partial status).
//

import SwiftUI

struct HistoryView: View {
    @EnvironmentObject private var history: SessionHistoryStore
    @EnvironmentObject private var loc: Localization
    @Environment(\.colorScheme) private var scheme
    @Environment(\.dismiss) private var dismiss

    @State private var selectedDate: Date = Calendar.current.startOfDay(for: Date())
    @State private var displayMonth: Date = Date()
    @State private var selectedRecord: FocusSessionRecord?
    @State private var recordPendingDelete: FocusSessionRecord?

    private var colors: PurrodoroColors { PurrodoroColors(scheme: scheme) }
    private var intensities: [Date: Int] {
        History.focusIntensitiesByDate(records: history.records)
    }
    private var detail: HistoryDayDetail {
        History.dayDetail(records: history.records, date: selectedDate)
    }

    var body: some View {
        NavigationStack {
            ZStack {
                colors.page.ignoresSafeArea()
                ScrollView {
                    VStack(alignment: .leading, spacing: 16) {
                        calendarCard
                        dayDetailCard
                    }
                    .padding(.horizontal, 20)
                    .padding(.top, 8)
                    .padding(.bottom, 40)
                }
            }
            .navigationTitle(loc.t("history.title"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button {
                        dismiss()
                    } label: {
                        Image(systemName: "chevron.left")
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundStyle(colors.textPrimary)
                    }
                    .accessibilityLabel(loc.t("common.back"))
                }
            }
            .sheet(item: $selectedRecord) { record in
                HistoryRecordSheet(
                    record: record,
                    onDelete: { recordPendingDelete = record }
                )
                .environmentObject(loc)
                .environment(\.pColors, colors)
            }
            .alert(
                loc.t("journey.deleteTitle"),
                isPresented: Binding(
                    get: { recordPendingDelete != nil },
                    set: { if !$0 { recordPendingDelete = nil } }
                )
            ) {
                Button(loc.t("common.cancel"), role: .cancel) {
                    recordPendingDelete = nil
                }
                Button(loc.t("common.delete"), role: .destructive) {
                    if let record = recordPendingDelete {
                        history.delete(record.id)
                        selectedRecord = nil
                        recordPendingDelete = nil
                    }
                }
            } message: {
                if let record = recordPendingDelete {
                    let start = Self.timeFormatter.string(from: History.startedAt(for: record))
                    let end = Self.timeFormatter.string(from: record.endedAt)
                    Text(loc.t("journey.deleteBody", start, end))
                }
            }
        }
        .environment(\.pColors, colors)
    }

    // MARK: Calendar

    private var calendarCard: some View {
        let grid = HistoryMonthGrid(month: displayMonth)
        return VStack(spacing: 14) {
            HStack {
                Text(grid.title)
                    .font(.system(.headline, design: .rounded).weight(.bold))
                    .foregroundStyle(colors.textPrimary)
                Spacer()
                monthButton(systemName: "chevron.left", label: loc.t("history.previousMonth")) {
                    shiftMonth(-1)
                }
                monthButton(systemName: "chevron.right", label: loc.t("history.nextMonth")) {
                    shiftMonth(1)
                }
            }

            HStack(spacing: 0) {
                ForEach(Array(grid.weekdaySymbols.enumerated()), id: \.offset) { _, sym in
                    Text(sym)
                        .font(.system(.caption2, design: .rounded).weight(.semibold))
                        .foregroundStyle(colors.textFaint)
                        .frame(maxWidth: .infinity)
                }
            }

            let columns = Array(repeating: GridItem(.flexible(), spacing: 4), count: 7)
            LazyVGrid(columns: columns, spacing: 6) {
                ForEach(grid.days, id: \.self) { date in
                    dayCell(
                        date: date,
                        inMonth: grid.isInMonth(date),
                        intensity: intensities[date] ?? 0
                    )
                }
            }
        }
        .padding(18)
        .background(colors.card, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: 22, style: .continuous).stroke(colors.border, lineWidth: 1))
    }

    private func dayCell(date: Date, inMonth: Bool, intensity: Int) -> some View {
        let cal = Journey.calendar
        let selected = cal.isDate(date, inSameDayAs: selectedDate)
        let isToday = cal.isDateInToday(date)
        let dayNumber = cal.component(.day, from: date)

        return Button {
            selectedDate = date
            if !inMonth {
                withAnimation(.easeInOut(duration: 0.2)) { displayMonth = date }
            }
        } label: {
            Text("\(dayNumber)")
                .font(.system(.caption, design: .rounded).weight(selected || isToday ? .bold : .regular))
                .foregroundStyle(dayTextColor(selected: selected, inMonth: inMonth))
                .frame(width: 36, height: 36)
                .background(dayBackground(selected: selected, inMonth: inMonth, intensity: intensity), in: Circle())
                .overlay(
                    Circle()
                        .stroke(Brand.coral, lineWidth: isToday && !selected ? 1.5 : 0)
                )
        }
        .buttonStyle(.plain)
        .frame(maxWidth: .infinity)
        .frame(height: 42)
        .accessibilityLabel(Self.dayA11yFormatter.string(from: date))
    }

    private func dayBackground(selected: Bool, inMonth: Bool, intensity: Int) -> Color {
        if selected { return Brand.coral }
        if !inMonth { return .clear }
        return heatColor(intensity)
    }

    private func dayTextColor(selected: Bool, inMonth: Bool) -> Color {
        if selected { return .white }
        if !inMonth { return colors.textFaint }
        return colors.textPrimary
    }

    private func heatColor(_ level: Int) -> Color {
        switch level {
        case 1: return Brand.coral.opacity(0.22)
        case 2: return Brand.coral.opacity(0.45)
        case 3: return Brand.coral.opacity(0.70)
        case 4...: return Brand.coral
        default: return .clear
        }
    }

    // MARK: Day detail

    private var dayDetailCard: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text(Self.dayHeaderFormatter.string(from: detail.date))
                .font(.system(.headline, design: .rounded).weight(.bold))
                .foregroundStyle(colors.textPrimary)

            HStack(spacing: 18) {
                dayStat(icon: "clock", value: History.minutesLabel(detail.focusMinutes), label: loc.t("journey.statFocused"))
                dayStat(icon: "target", value: "\(detail.focusSessions)", label: loc.t("journey.statSessions"))
                dayStat(icon: "cup.and.saucer", value: History.minutesLabel(detail.restMinutes), label: loc.t("journey.statRest"))
            }

            if detail.rows.isEmpty {
                emptyDay
            } else {
                VStack(spacing: 0) {
                    ForEach(Array(detail.rows.enumerated()), id: \.element.id) { index, record in
                        timelineRow(record, isLatest: index == 0)
                        if index < detail.rows.count - 1 {
                            Rectangle()
                                .fill(colors.border.opacity(0.65))
                                .frame(height: 1)
                                .padding(.leading, 28)
                        }
                    }
                }
            }
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(colors.card, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: 22, style: .continuous).stroke(colors.border, lineWidth: 1))
    }

    private var emptyDay: some View {
        VStack(spacing: 10) {
            Image("CatEmptyDay")
                .resizable()
                .scaledToFit()
                .frame(width: 120, height: 90)
            Text(loc.t("history.emptyDay"))
                .font(.system(.subheadline, design: .rounded))
                .foregroundStyle(colors.textSecondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 8)
    }

    private func dayStat(icon: String, value: String, label: String) -> some View {
        HStack(spacing: 8) {
            Image(systemName: icon)
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(Brand.coral)
            VStack(alignment: .leading, spacing: 1) {
                Text(value)
                    .font(.system(.subheadline, design: .rounded).weight(.bold))
                    .foregroundStyle(colors.textPrimary)
                    .monospacedDigit()
                Text(label)
                    .font(.system(.caption2, design: .rounded))
                    .foregroundStyle(colors.textSecondary)
            }
        }
    }

    private func timelineRow(_ record: FocusSessionRecord, isLatest: Bool) -> some View {
        Button {
            selectedRecord = record
        } label: {
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
                    Text(timelineMeta(record))
                        .font(.system(.caption2, design: .rounded))
                        .foregroundStyle(colors.textFaint)
                }
                .padding(.vertical, 10)
                .padding(.horizontal, 12)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(colors.page.opacity(0.55), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
            }
            .padding(.vertical, 4)
        }
        .buttonStyle(.plain)
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

    private func timelineMeta(_ record: FocusSessionRecord) -> String {
        let time = Self.timeFormatter.string(from: History.startedAt(for: record))
        let duration = loc.t("unit.min", record.minutes)
        return "\(time) · \(duration)"
    }

    // MARK: Helpers

    private func monthButton(systemName: String, label: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: systemName)
                .font(.system(size: 14, weight: .bold))
                .foregroundStyle(colors.textSecondary)
                .frame(width: 34, height: 34)
                .background(colors.page, in: Circle())
                .overlay(Circle().stroke(colors.border, lineWidth: 1))
        }
        .buttonStyle(.plain)
        .accessibilityLabel(label)
    }

    private func shiftMonth(_ delta: Int) {
        if let next = Journey.calendar.date(byAdding: .month, value: delta, to: displayMonth) {
            withAnimation(.easeInOut(duration: 0.2)) { displayMonth = next }
        }
    }

    private static let timeFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "h:mm a"
        return f
    }()

    private static let dayHeaderFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "EEEE, MMMM d"
        return f
    }()

    private static let dayA11yFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateStyle = .full
        f.timeStyle = .none
        return f
    }()
}

// MARK: - Record detail sheet

private struct HistoryRecordSheet: View {
    @EnvironmentObject private var loc: Localization
    @Environment(\.pColors) private var colors
    @Environment(\.dismiss) private var dismiss

    let record: FocusSessionRecord
    let onDelete: () -> Void

    private var startLabel: String {
        Self.timeFormatter.string(from: History.startedAt(for: record))
    }

    private var endLabel: String {
        Self.timeFormatter.string(from: record.endedAt)
    }

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: 20) {
                HStack {
                    Text(loc.t("journey.timeRange", startLabel, endLabel))
                        .font(.system(.subheadline, design: .rounded).weight(.semibold))
                        .foregroundStyle(colors.textSecondary)
                    Spacer()
                    Text(loc.t("journey.statusCompleted"))
                        .font(.system(.caption, design: .rounded).weight(.bold))
                        .foregroundStyle(Color(hex: 0x8BA05E))
                        .padding(.horizontal, 10)
                        .padding(.vertical, 5)
                        .background(Color(hex: 0x8BA05E).opacity(0.14), in: Capsule())
                }

                HStack {
                    sheetStat(label: loc.t("journey.statFocused"), value: History.minutesLabel(record.minutes))
                    sheetDivider
                    sheetStat(label: loc.t("journey.statSessions"), value: "1/1")
                    sheetDivider
                    sheetStat(label: loc.t("journey.statRest"), value: History.minutesLabel(0))
                }
                .frame(maxWidth: .infinity)

                if let name = record.intentName, !name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                    Text(name)
                        .font(.system(.body, design: .rounded))
                        .foregroundStyle(colors.textPrimary)
                        .padding(.horizontal, 14)
                        .padding(.vertical, 10)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(colors.card, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                        .overlay(
                            RoundedRectangle(cornerRadius: 12, style: .continuous)
                                .stroke(colors.border, lineWidth: 1)
                        )
                }

                Button {
                    dismiss()
                    // Let the sheet dismiss before presenting the confirm alert.
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.35) {
                        onDelete()
                    }
                } label: {
                    Text(loc.t("journey.deleteRecord"))
                        .font(.system(.body, design: .rounded).weight(.semibold))
                        .foregroundStyle(Color(hex: 0xE53935))
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(Color(hex: 0xE53935).opacity(0.10), in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                        .overlay(
                            RoundedRectangle(cornerRadius: 16, style: .continuous)
                                .stroke(Color(hex: 0xE53935).opacity(0.30), lineWidth: 1)
                        )
                }
                .buttonStyle(.plain)

                Spacer()
            }
            .padding(20)
            .background(colors.page.ignoresSafeArea())
            .navigationTitle(loc.t("journey.detail"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button(loc.t("common.cancel")) { dismiss() }
                }
            }
        }
        .presentationDetents([.medium])
    }

    private var sheetDivider: some View {
        Rectangle()
            .fill(colors.border)
            .frame(width: 1, height: 36)
    }

    private func sheetStat(label: String, value: String) -> some View {
        VStack(spacing: 4) {
            Text(label)
                .font(.system(.caption2, design: .rounded))
                .foregroundStyle(colors.textSecondary)
            Text(value)
                .font(.system(.headline, design: .rounded).weight(.bold))
                .foregroundStyle(colors.textPrimary)
                .monospacedDigit()
        }
        .frame(maxWidth: .infinity)
    }

    private static let timeFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "h:mm a"
        return f
    }()
}

#Preview {
    HistoryView()
        .environmentObject(SessionHistoryStore())
        .environmentObject(Localization())
}
