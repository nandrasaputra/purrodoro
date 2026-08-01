//
//  HomeScenePickerSheet.swift
//  Purrodoro
//
//  Bottom sheet to pick a home hero backdrop — mirrors Android
//  `HomeScenePickerSheet.kt`. Locked premium scenes open the upsell.
//

import SwiftUI

struct HomeScenePickerSheet: View {
    let selected: HomeScene
    let isPremium: Bool
    let onSelect: (HomeScene) -> Void
    let onPremiumSceneAttempt: (String) -> Void

    @EnvironmentObject private var loc: Localization
    @Environment(\.dismiss) private var dismiss
    @Environment(\.colorScheme) private var scheme

    private var colors: PurrodoroColors { PurrodoroColors(scheme: scheme) }
    private var isDark: Bool { scheme == .dark }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    Text(loc.t("home.scene.picker.subtitle"))
                        .font(.system(.subheadline, design: .rounded))
                        .foregroundStyle(colors.textSecondary)
                        .padding(.bottom, 4)

                    ForEach(HomeScene.allCases) { scene in
                        let locked = !canSelectHomeScene(scene, isPremium: isPremium)
                        let title = loc.t(scene.titleKey)
                        HomeSceneOptionRow(
                            scene: scene,
                            title: title,
                            subtitle: loc.t(scene.subtitleKey),
                            selected: scene == selected,
                            locked: locked,
                            imageName: resolveHomeHeroImageName(scene: scene, isDarkTheme: isDark),
                            colors: colors
                        ) {
                            if locked {
                                onPremiumSceneAttempt(title)
                                dismiss()
                            } else {
                                onSelect(scene)
                                dismiss()
                            }
                        }
                    }
                }
                .padding(20)
            }
            .background(colors.page.ignoresSafeArea())
            .navigationTitle(loc.t("home.scene.picker.title"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button(loc.t("settings.done")) { dismiss() }
                        .font(.system(.body, design: .rounded).weight(.semibold))
                }
            }
        }
        .presentationDetents([.medium, .large])
    }
}

private struct HomeSceneOptionRow: View {
    let scene: HomeScene
    let title: String
    let subtitle: String
    let selected: Bool
    let locked: Bool
    let imageName: String
    let colors: PurrodoroColors
    let onClick: () -> Void

    var body: some View {
        Button(action: onClick) {
            HStack(spacing: 14) {
                ZStack(alignment: .topTrailing) {
                    Image(imageName)
                        .resizable()
                        .scaledToFill()
                        .frame(maxWidth: .infinity)
                        .aspectRatio(1.48, contentMode: .fit)
                        .clipped()
                        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))

                    if scene.isPremiumOnly {
                        Text("PREMIUM")
                            .font(.system(size: 9, weight: .heavy, design: .rounded))
                            .tracking(0.6)
                            .foregroundStyle(.white)
                            .padding(.horizontal, 7)
                            .padding(.vertical, 4)
                            .background(
                                LinearGradient(
                                    colors: [Brand.premiumPurpleTop, Brand.premiumPurpleBottom],
                                    startPoint: .top,
                                    endPoint: .bottom
                                ),
                                in: Capsule()
                            )
                            .padding(6)
                    }
                }
                .frame(width: 118)

                VStack(alignment: .leading, spacing: 4) {
                    Text(title)
                        .font(.system(.body, design: .rounded).weight(.semibold))
                        .foregroundStyle(colors.textPrimary)
                    Text(subtitle)
                        .font(.system(.caption, design: .rounded))
                        .foregroundStyle(colors.textSecondary)
                        .fixedSize(horizontal: false, vertical: true)
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                if selected && !locked {
                    Image(systemName: "checkmark")
                        .font(.system(size: 16, weight: .bold))
                        .foregroundStyle(Brand.coral)
                }
            }
            .padding(12)
            .background(
                selected ? Brand.peach.opacity(0.18) : colors.card,
                in: RoundedRectangle(cornerRadius: 16, style: .continuous)
            )
            .overlay(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .stroke(selected ? Brand.coral.opacity(0.45) : colors.border, lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
        .accessibilityLabel(title)
    }
}
