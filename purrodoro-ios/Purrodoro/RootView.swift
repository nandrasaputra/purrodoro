//
//  RootView.swift
//  Purrodoro
//
//  Top-level shell: the four primary destinations (Focus / Tasks / Journey /
//  Habits) hosted behind a custom warm bottom dock that mirrors the Android
//  app's navigation aesthetic.
//

import SwiftUI

enum AppTab: String, CaseIterable, Identifiable {
    case focus, tasks, journey, habits

    var id: String { rawValue }

    var label: String {
        switch self {
        case .focus: return "Focus"
        case .tasks: return "Tasks"
        case .journey: return "Journey"
        case .habits: return "Habits"
        }
    }

    var symbol: String {
        switch self {
        case .focus: return "timer"
        case .tasks: return "checklist"
        case .journey: return "map"
        case .habits: return "repeat"
        }
    }
}

struct RootView: View {
    @EnvironmentObject private var engine: TimerEngine
    @EnvironmentObject private var tasks: TaskStore
    @Environment(\.colorScheme) private var scheme
    @State private var tab: AppTab = .focus

    private var colors: PurrodoroColors { PurrodoroColors(scheme: scheme) }

    var body: some View {
        ZStack(alignment: .bottom) {
            colors.page.ignoresSafeArea()

            Group {
                switch tab {
                case .focus: HomeView()
                case .tasks:
                    TasksView { task in
                        tasks.setActive(task)
                        withAnimation(.spring(response: 0.32, dampingFraction: 0.78)) {
                            tab = .focus
                        }
                    }
                case .journey:
                    JourneyView()
                case .habits:
                    PlaceholderScreen(
                        title: "Habits",
                        subtitle: "Build gentle daily rituals and keep your streak alive.",
                        image: "CatCelebrate"
                    )
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)

            BottomDock(selected: $tab, colors: colors)
        }
        .environment(\.pColors, colors)
    }
}

// MARK: - Bottom dock

private struct BottomDock: View {
    @Binding var selected: AppTab
    let colors: PurrodoroColors

    private var dockFill: Color {
        colors.scheme == .dark ? Color(hex: 0x251E3A) : Color(hex: 0xEFE0CB)
    }
    private var dockBorder: Color {
        colors.scheme == .dark ? Color(hex: 0x3A3354) : Color(hex: 0xDFCBAC)
    }

    var body: some View {
        HStack(spacing: 4) {
            ForEach(AppTab.allCases) { tab in
                DockItem(tab: tab, isActive: tab == selected, colors: colors) {
                    withAnimation(.spring(response: 0.32, dampingFraction: 0.78)) {
                        selected = tab
                    }
                }
            }
        }
        .padding(6)
        .background(dockFill, in: Capsule())
        .overlay(Capsule().stroke(dockBorder, lineWidth: 1))
        .shadow(color: .black.opacity(colors.scheme == .dark ? 0.4 : 0.08), radius: 16, y: 6)
        .padding(.horizontal, 24)
        .padding(.bottom, 6)
    }
}

private struct DockItem: View {
    let tab: AppTab
    let isActive: Bool
    let colors: PurrodoroColors
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 6) {
                Image(systemName: tab.symbol)
                    .font(.system(size: 16, weight: .semibold))
                if isActive {
                    Text(tab.label)
                        .font(.system(.subheadline, design: .rounded).weight(.bold))
                        .lineLimit(1)
                        .fixedSize()
                }
            }
            .foregroundStyle(isActive ? .white : colors.textSecondary)
            .padding(.vertical, 12)
            .padding(.horizontal, isActive ? 16 : 12)
            .background {
                if isActive {
                    Capsule().fill(Brand.coral)
                }
            }
            .frame(maxWidth: isActive ? .infinity : nil)
        }
        .buttonStyle(.plain)
        .accessibilityLabel(tab.label)
    }
}

// MARK: - Placeholder

struct PlaceholderScreen: View {
    let title: String
    let subtitle: String
    let image: String
    @Environment(\.pColors) private var colors

    var body: some View {
        VStack(spacing: 18) {
            Spacer()
            Image(image)
                .resizable()
                .scaledToFit()
                .frame(width: 160, height: 160)
            Text(title)
                .font(.system(.title, design: .rounded).weight(.bold))
                .foregroundStyle(colors.textPrimary)
            Text(subtitle)
                .font(.system(.subheadline, design: .rounded))
                .foregroundStyle(colors.textSecondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 48)
            Text("Coming soon")
                .font(.system(.caption, design: .rounded).weight(.bold))
                .tracking(1)
                .foregroundStyle(Brand.cocoa)
                .padding(.vertical, 6)
                .padding(.horizontal, 14)
                .background(Brand.peach, in: Capsule())
            Spacer()
            Spacer()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

#Preview {
    RootView()
        .environmentObject(TimerEngine())
        .environmentObject(TaskStore())
}
