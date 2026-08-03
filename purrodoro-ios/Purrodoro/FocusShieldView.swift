//
//  FocusShieldView.swift
//  Purrodoro
//
//  Profile settings for Focus Shield (Android `FocusShieldScreen`). Uses
//  FamilyControls authorization + FamilyActivityPicker instead of Android
//  Usage Access / overlay + package list.
//

import FamilyControls
import SwiftUI

struct FocusShieldView: View {
    @EnvironmentObject private var shield: FocusShieldStore
    @EnvironmentObject private var loc: Localization
    @Environment(\.colorScheme) private var scheme
    @State private var showPicker = false
    @State private var isRequestingAuth = false

    private var colors: PurrodoroColors { PurrodoroColors(scheme: scheme) }

    var body: some View {
        ZStack {
            colors.page.ignoresSafeArea()
            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    Text(loc.t("focusShield.howBody"))
                        .font(.system(.subheadline, design: .rounded))
                        .foregroundStyle(colors.textSecondary)

                    sectionLabel(loc.t("focusShield.section.duringFocus"))
                    enableCard

                    sectionLabel(loc.t("focusShield.section.permission"))
                    permissionCard

                    sectionLabel(loc.t("focusShield.section.apps"))
                    appsCard

                    Text(loc.t("focusShield.deviceLocalHint"))
                        .font(.system(.caption2, design: .rounded))
                        .foregroundStyle(colors.textFaint)
                        .padding(.top, 4)
                }
                .padding(20)
                .padding(.bottom, 40)
            }
        }
        .navigationTitle(loc.t("focusShield.title"))
        .navigationBarTitleDisplayMode(.inline)
        .familyActivityPicker(
            isPresented: $showPicker,
            selection: $shield.selection
        )
        .onAppear { shield.refreshAuthorizationStatus() }
        .onReceive(NotificationCenter.default.publisher(for: UIApplication.willEnterForegroundNotification)) { _ in
            shield.refreshAuthorizationStatus()
        }
    }

    private var enableCard: some View {
        card {
            Toggle(isOn: Binding(
                get: { shield.enabled },
                set: { wantOn in
                    if wantOn, !shield.isAuthorized {
                        Task { await requestAuthThenEnable() }
                    } else if wantOn, !shield.hasSelection {
                        shield.enabled = true
                        showPicker = true
                    } else {
                        shield.enabled = wantOn
                    }
                }
            )) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(loc.t("focusShield.useDuringFocus"))
                        .font(.system(.headline, design: .rounded))
                        .foregroundStyle(colors.textPrimary)
                    Text(statusDetail)
                        .font(.system(.caption, design: .rounded))
                        .foregroundStyle(colors.textFaint)
                }
            }
            .tint(Brand.coral)
            .disabled(isRequestingAuth)
        }
    }

    private var permissionCard: some View {
        card {
            VStack(alignment: .leading, spacing: 12) {
                HStack(spacing: 12) {
                    Image(systemName: shield.isAuthorized ? "checkmark.shield.fill" : "hand.raised.fill")
                        .font(.system(size: 22, weight: .semibold))
                        .foregroundStyle(shield.isAuthorized ? Brand.mint : Brand.coral)
                    VStack(alignment: .leading, spacing: 2) {
                        Text(loc.t("focusShield.perm.screenTimeTitle"))
                            .font(.system(.subheadline, design: .rounded).weight(.semibold))
                            .foregroundStyle(colors.textPrimary)
                        Text(loc.t("focusShield.perm.screenTimeBody"))
                            .font(.system(.caption, design: .rounded))
                            .foregroundStyle(colors.textFaint)
                    }
                    Spacer()
                    Text(shield.isAuthorized ? loc.t("focusShield.perm.granted") : loc.t("focusShield.perm.needed"))
                        .font(.system(.caption, design: .rounded).weight(.semibold))
                        .foregroundStyle(shield.isAuthorized ? Brand.mint : Brand.coral)
                }

                if !shield.isAuthorized {
                    Button {
                        Task { await requestAuthThenEnable() }
                    } label: {
                        Text(isRequestingAuth ? loc.t("focusShield.perm.requesting") : loc.t("focusShield.perm.enable"))
                            .font(.system(.body, design: .rounded).weight(.bold))
                            .foregroundStyle(.white)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 12)
                            .background(Brand.coral, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                    }
                    .buttonStyle(.plain)
                    .disabled(isRequestingAuth)
                }

                if let message = shield.lastErrorMessage, !shield.isAuthorized {
                    Text(message)
                        .font(.system(.caption2, design: .rounded))
                        .foregroundStyle(Brand.coral)
                }
            }
        }
    }

    private var appsCard: some View {
        card {
            VStack(alignment: .leading, spacing: 12) {
                if shield.hasSelection {
                    Text(loc.t("focusShield.status.ready", shield.selectedAppCount))
                        .font(.system(.subheadline, design: .rounded).weight(.semibold))
                        .foregroundStyle(colors.textPrimary)
                } else {
                    Text(loc.t("focusShield.noApps"))
                        .font(.system(.subheadline, design: .rounded))
                        .foregroundStyle(colors.textSecondary)
                }

                HStack(spacing: 10) {
                    Button {
                        Task {
                            if !shield.isAuthorized {
                                let ok = await shield.requestAuthorization()
                                guard ok else { return }
                            }
                            showPicker = true
                        }
                    } label: {
                        Text(loc.t("focusShield.editApps"))
                            .font(.system(.subheadline, design: .rounded).weight(.semibold))
                            .foregroundStyle(.white)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 12)
                            .background(Brand.coral, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                    }
                    .buttonStyle(.plain)

                    if shield.hasSelection {
                        Button {
                            shield.clearSelection()
                        } label: {
                            Text(loc.t("focusShield.remove"))
                                .font(.system(.subheadline, design: .rounded).weight(.semibold))
                                .foregroundStyle(Brand.coral)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 12)
                                .background(colors.page, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                                .overlay(
                                    RoundedRectangle(cornerRadius: 14, style: .continuous)
                                        .stroke(colors.border, lineWidth: 1)
                                )
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
    }

    private var statusDetail: String {
        switch shield.readiness {
        case .off:
            return loc.t("focusShield.status.offHint")
        case .needAuthorization:
            return loc.t("focusShield.status.needAuth")
        case .needApps:
            return loc.t("focusShield.status.needApps")
        case .ready(let count):
            return loc.t("focusShield.status.ready", count)
        }
    }

    private func requestAuthThenEnable() async {
        isRequestingAuth = true
        defer { isRequestingAuth = false }
        let ok = await shield.requestAuthorization()
        if ok {
            shield.enabled = true
            if !shield.hasSelection {
                showPicker = true
            }
        }
    }

    private func sectionLabel(_ text: String) -> some View {
        Text(text.uppercased())
            .font(.system(.caption, design: .rounded).weight(.semibold))
            .foregroundStyle(colors.textSecondary)
            .tracking(0.6)
    }

    private func card<Content: View>(@ViewBuilder _ content: () -> Content) -> some View {
        content()
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(colors.card, in: RoundedRectangle(cornerRadius: 18, style: .continuous))
            .overlay(RoundedRectangle(cornerRadius: 18, style: .continuous).stroke(colors.border, lineWidth: 1))
    }
}

/// Compact Start Focus row (Android `FocusShieldToggleRow`).
struct FocusShieldStartFocusRow: View {
    @EnvironmentObject private var shield: FocusShieldStore
    @EnvironmentObject private var loc: Localization
    @Environment(\.colorScheme) private var scheme
    var onManage: () -> Void

    private var colors: PurrodoroColors { PurrodoroColors(scheme: scheme) }

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Toggle(isOn: Binding(
                get: { shield.enabled },
                set: { wantOn in
                    if wantOn, !shield.isAuthorized {
                        Task {
                            let ok = await shield.requestAuthorization()
                            if ok {
                                shield.enabled = true
                                if !shield.hasSelection { onManage() }
                            }
                        }
                    } else if wantOn, !shield.hasSelection {
                        shield.enabled = true
                        onManage()
                    } else {
                        shield.enabled = wantOn
                    }
                }
            )) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(loc.t("focusShield.sheetCopy"))
                        .font(.system(.subheadline, design: .rounded).weight(.semibold))
                        .foregroundStyle(colors.textPrimary)
                    Text(stateLine)
                        .font(.system(.caption2, design: .rounded))
                        .foregroundStyle(colors.textFaint)
                }
            }
            .tint(Brand.coral)

            Button(action: onManage) {
                Text(loc.t("focusShield.editApps"))
                    .font(.system(.caption, design: .rounded).weight(.semibold))
                    .foregroundStyle(Brand.coral)
            }
            .buttonStyle(.plain)
        }
        .padding(14)
        .background(colors.card, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: 16, style: .continuous).stroke(colors.border, lineWidth: 1))
        .onAppear { shield.refreshAuthorizationStatus() }
    }

    private var stateLine: String {
        if !shield.isAuthorized {
            return loc.t("focusShield.grantNeeded")
        }
        if shield.hasSelection {
            return loc.t("focusShield.sheetStateCount", shield.selectedAppCount)
        }
        return loc.t("focusShield.sheetStateNone")
    }
}
