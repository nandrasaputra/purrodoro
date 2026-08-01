//
//  StorageView.swift
//  Purrodoro
//
//  Downloaded cloud soundscape management. Mirrors Android `StorageScreen.kt`.
//  Opened from Profile → Storage.
//

import SwiftUI

struct StorageView: View {
    @EnvironmentObject private var sounds: SoundsStore
    @EnvironmentObject private var loc: Localization
    @Environment(\.colorScheme) private var scheme

    @State private var showRemoveAllConfirm = false

    private var colors: PurrodoroColors { PurrodoroColors(scheme: scheme) }
    private var summary: StorageSummary { sounds.storageSummary }
    private var removableCount: Int { summary.items.filter(\.isRemovable).count }

    var body: some View {
        ZStack {
            colors.page.ignoresSafeArea()
            ScrollView {
                VStack(spacing: 16) {
                    summaryCard

                    if summary.items.isEmpty {
                        emptyState
                    } else {
                        sectionHeader(loc.t("storage.section.downloads"))
                        downloadsCard

                        if removableCount > 0 {
                            removeAllButton
                        }
                    }
                }
                .padding(20)
            }
        }
        .navigationTitle(loc.t("storage.title"))
        .navigationBarTitleDisplayMode(.inline)
        .alert(loc.t("storage.remove_all.title"), isPresented: $showRemoveAllConfirm) {
            Button(loc.t("storage.remove_all.confirm"), role: .destructive) {
                sounds.removeAllDownloads()
            }
            Button(loc.t("common.cancel"), role: .cancel) {}
        } message: {
            Text(loc.t("storage.remove_all.body", formatBytes(summary.totalBytes)))
        }
    }

    // MARK: - Summary

    private var summaryCard: some View {
        HStack(spacing: 14) {
            ZStack {
                Circle()
                    .fill(Brand.coral.opacity(0.14))
                    .frame(width: 48, height: 48)
                Image(systemName: "internaldrive")
                    .font(.system(size: 20, weight: .semibold))
                    .foregroundStyle(Brand.coral)
            }
            VStack(alignment: .leading, spacing: 2) {
                Text(formatBytes(summary.totalBytes))
                    .font(.system(.title3, design: .rounded).weight(.bold))
                    .foregroundStyle(colors.textPrimary)
                Text(loc.t("storage.summary.subtitle"))
                    .font(.system(.caption, design: .rounded))
                    .foregroundStyle(colors.textSecondary)
            }
            Spacer(minLength: 0)
        }
        .padding(16)
        .background(colors.card, in: RoundedRectangle(cornerRadius: 18, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .stroke(colors.border, lineWidth: 1)
        )
    }

    // MARK: - List

    private var downloadsCard: some View {
        VStack(spacing: 0) {
            ForEach(Array(summary.items.enumerated()), id: \.element.id) { index, item in
                if index > 0 {
                    Divider().background(colors.border)
                }
                storageRow(item)
            }
        }
        .background(colors.card, in: RoundedRectangle(cornerRadius: 18, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .stroke(colors.border, lineWidth: 1)
        )
    }

    private func storageRow(_ item: StorageItem) -> some View {
        HStack(spacing: 14) {
            Image(systemName: item.kind == "music" ? "headphones" : "cloud.rain.fill")
                .font(.system(size: 18, weight: .semibold))
                .foregroundStyle(Brand.coral)
                .frame(width: 28)

            VStack(alignment: .leading, spacing: 2) {
                Text(item.name)
                    .font(.system(.body, design: .rounded).weight(.medium))
                    .foregroundStyle(colors.textPrimary)
                    .lineLimit(1)
                Text(subtitle(for: item))
                    .font(.system(.caption, design: .rounded))
                    .foregroundStyle(colors.textSecondary)
            }

            Spacer(minLength: 0)

            if item.isRemovable {
                Button {
                    sounds.removeDownload(id: item.id)
                } label: {
                    Image(systemName: "trash")
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundStyle(Brand.deepCoral)
                        .frame(width: 36, height: 36)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityLabel(loc.t("storage.remove_one_cd", item.name))
            } else {
                ProgressView(value: Double(item.progress ?? 0))
                    .frame(width: 28, height: 28)
                    .tint(Brand.coral)
            }
        }
        .padding(16)
    }

    private func subtitle(for item: StorageItem) -> String {
        if item.isRemovable {
            return formatBytes(item.sizeBytes)
        }
        let pct = Int(((item.progress ?? 0) * 100).rounded())
        return loc.t("storage.downloading", pct)
    }

    // MARK: - Remove all / empty

    private var removeAllButton: some View {
        Button {
            showRemoveAllConfirm = true
        } label: {
            Text(loc.t("storage.remove_all"))
                .font(.system(.body, design: .rounded).weight(.semibold))
                .foregroundStyle(Brand.deepCoral)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
                .background(
                    Brand.deepCoral.opacity(0.10),
                    in: RoundedRectangle(cornerRadius: 16, style: .continuous)
                )
                .overlay(
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .stroke(Brand.deepCoral.opacity(0.30), lineWidth: 1)
                )
        }
        .buttonStyle(.plain)
    }

    private var emptyState: some View {
        VStack(spacing: 12) {
            Image("CatEmptyMonth")
                .resizable()
                .scaledToFit()
                .frame(width: 140, height: 140)
            Text(loc.t("storage.empty"))
                .font(.system(.body, design: .rounded).weight(.semibold))
                .foregroundStyle(colors.textPrimary)
                .multilineTextAlignment(.center)
            Text(loc.t("storage.empty.hint"))
                .font(.system(.subheadline, design: .rounded))
                .foregroundStyle(colors.textSecondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 28)
        .padding(.horizontal, 16)
        .background(colors.card, in: RoundedRectangle(cornerRadius: 18, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .stroke(colors.border, lineWidth: 1)
        )
    }

    private func sectionHeader(_ title: String) -> some View {
        Text(title.uppercased())
            .font(.system(.caption, design: .rounded).weight(.bold))
            .tracking(1)
            .foregroundStyle(colors.textFaint)
            .frame(maxWidth: .infinity, alignment: .leading)
    }
}
