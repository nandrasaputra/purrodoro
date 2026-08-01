//
//  TodayWidget.swift
//  PurrodoroWidgets
//
//  Home Screen widget mirroring the app's "today" focus stats (sessions,
//  minutes, current streak), read from the shared App Group store.
//

import SwiftUI
import WidgetKit

struct TodayEntry: TimelineEntry {
    let date: Date
    let snapshot: TodaySnapshot
}

struct TodayProvider: TimelineProvider {
    func placeholder(in context: Context) -> TodayEntry {
        TodayEntry(date: Date(), snapshot: TodaySnapshot(count: 3, minutes: 75, streak: 5))
    }

    func getSnapshot(in context: Context, completion: @escaping (TodayEntry) -> Void) {
        completion(TodayEntry(date: Date(), snapshot: SharedStore.readToday()))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<TodayEntry>) -> Void) {
        let now = Date()
        let entry = TodayEntry(date: now, snapshot: SharedStore.readToday(now: now))
        // Refresh after the next local midnight so the day rolls over, with an
        // hourly nudge in case the system coalesces our app-driven reloads.
        let nextHour = Calendar.current.date(byAdding: .hour, value: 1, to: now) ?? now.addingTimeInterval(3600)
        completion(Timeline(entries: [entry], policy: .after(nextHour)))
    }
}

struct TodayWidgetView: View {
    @Environment(\.widgetFamily) private var family
    let entry: TodayEntry

    private var snap: TodaySnapshot { entry.snapshot }

    var body: some View {
        switch family {
        case .systemSmall:
            smallBody
        default:
            mediumBody
        }
    }

    private var header: some View {
        HStack(spacing: 6) {
            Image(systemName: "timer")
                .font(.system(size: 13, weight: .bold))
                .foregroundStyle(WidgetBrand.coral)
            Text("Purrodoro")
                .font(.system(.caption, design: .rounded).weight(.bold))
                .foregroundStyle(.secondary)
        }
    }

    private var smallBody: some View {
        VStack(alignment: .leading, spacing: 6) {
            header
            Spacer(minLength: 0)
            Text("\(snap.count)")
                .font(.system(size: 40, design: .rounded).weight(.heavy))
                .foregroundStyle(WidgetBrand.cocoa)
            Text(snap.count == 1 ? "focus today" : "focuses today")
                .font(.system(.caption, design: .rounded).weight(.medium))
                .foregroundStyle(.secondary)
            Spacer(minLength: 0)
            HStack(spacing: 10) {
                Label("\(snap.minutes)m", systemImage: "clock.fill")
                Label("\(snap.streak)", systemImage: "flame.fill")
            }
            .font(.system(.caption2, design: .rounded).weight(.semibold))
            .foregroundStyle(WidgetBrand.coral)
        }
    }

    private var mediumBody: some View {
        VStack(alignment: .leading, spacing: 10) {
            header
            Spacer(minLength: 0)
            HStack(spacing: 12) {
                stat(value: "\(snap.count)", label: snap.count == 1 ? "focus" : "focuses", tint: WidgetBrand.coral)
                stat(value: "\(snap.minutes)", label: "minutes", tint: WidgetBrand.accent(forMode: "longBreak"))
                stat(value: "\(snap.streak)", label: "day streak", tint: WidgetBrand.accent(forMode: "shortBreak"))
            }
            Spacer(minLength: 0)
        }
    }

    private func stat(value: String, label: String, tint: Color) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(value)
                .font(.system(size: 30, design: .rounded).weight(.heavy))
                .foregroundStyle(WidgetBrand.cocoa)
            Text(label)
                .font(.system(.caption2, design: .rounded).weight(.semibold))
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.vertical, 8)
        .padding(.horizontal, 10)
        .background(tint.opacity(0.16), in: RoundedRectangle(cornerRadius: 14, style: .continuous))
    }
}

struct TodayWidget: Widget {
    let kind = "PurrodoroTodayWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: TodayProvider()) { entry in
            TodayWidgetView(entry: entry)
                .containerBackground(WidgetBrand.peach.opacity(0.12), for: .widget)
                // Deep link into the app (Android Today widget opens MainActivity;
                // medium also offers Start Focus via the same host URL family).
                .widgetURL(URL(string: "purrodoro://start-focus"))
        }
        .configurationDisplayName("Today")
        .description("Your focus sessions, minutes, and streak today.")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}
