//
//  BackupRestoreView.swift
//  Purrodoro
//
//  Export / import JSON backups — mirrors Android `BackupRestoreScreen.kt`.
//  Restore is guest-only (always guest on iOS until auth lands).
//

import SwiftUI
import UniformTypeIdentifiers

struct BackupRestoreView: View {
    @EnvironmentObject private var engine: TimerEngine
    @EnvironmentObject private var tasks: TaskStore
    @EnvironmentObject private var habits: HabitStore
    @EnvironmentObject private var history: SessionHistoryStore
    @EnvironmentObject private var prefs: PreferencesStore
    @EnvironmentObject private var loc: Localization
    @EnvironmentObject private var sounds: SoundsStore
    @Environment(\.colorScheme) private var scheme

    @State private var working = false
    @State private var statusMessage: String?
    @State private var showImporter = false
    @State private var showExporter = false
    @State private var exportDocument = BackupFileDocument(data: Data())
    @State private var pendingImportURL: URL?
    @State private var showConfirmRestore = false

    private var colors: PurrodoroColors { PurrodoroColors(scheme: scheme) }

    private var applicator: BackupRestoreApplicator {
        BackupRestoreApplicator(
            engine: engine,
            tasks: tasks,
            habits: habits,
            history: history,
            prefs: prefs,
            loc: loc,
            sounds: sounds
        )
    }

    private var manager: BackupManager {
        BackupManager(appVersionName: appVersion)
    }

    private var appVersion: String {
        Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0"
    }

    private var defaultFileName: String {
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM-dd"
        f.locale = Locale(identifier: "en_US_POSIX")
        return "purrodoro-backup-\(f.string(from: Date())).json"
    }

    var body: some View {
        ZStack {
            colors.page.ignoresSafeArea()
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    summaryCard

                    sectionHeader(loc.t("backup.section.export"))
                    actionCard(
                        symbol: "square.and.arrow.up",
                        title: loc.t("backup.export.label"),
                        hint: loc.t("backup.export.hint"),
                        enabled: !working
                    ) {
                        prepareExport()
                    }

                    sectionHeader(loc.t("backup.section.restore"))
                    actionCard(
                        symbol: "square.and.arrow.down",
                        title: loc.t("backup.import.label"),
                        hint: loc.t("backup.import.hint"),
                        enabled: !working && applicator.isGuest
                    ) {
                        showImporter = true
                    }

                    if working {
                        HStack(spacing: 10) {
                            ProgressView()
                            Text(loc.t("backup.working"))
                                .font(.system(.subheadline, design: .rounded))
                                .foregroundStyle(colors.textSecondary)
                        }
                    }

                    if let statusMessage {
                        Text(statusMessage)
                            .font(.system(.subheadline, design: .rounded))
                            .foregroundStyle(colors.textSecondary)
                    }
                }
                .padding(20)
            }
        }
        .navigationTitle(loc.t("backup.title"))
        .navigationBarTitleDisplayMode(.inline)
        .fileExporter(
            isPresented: $showExporter,
            document: exportDocument,
            contentType: .json,
            defaultFilename: defaultFileName
        ) { result in
            switch result {
            case .success:
                statusMessage = loc.t("backup.export.success")
            case .failure:
                statusMessage = loc.t("backup.export.error")
            }
        }
        .fileImporter(
            isPresented: $showImporter,
            allowedContentTypes: [.json, .data, .plainText],
            allowsMultipleSelection: false
        ) { result in
            switch result {
            case .success(let urls):
                pendingImportURL = urls.first
                showConfirmRestore = true
            case .failure:
                statusMessage = loc.t("backup.restore.error.read")
            }
        }
        .alert(loc.t("backup.confirm.title"), isPresented: $showConfirmRestore) {
            Button(loc.t("common.cancel"), role: .cancel) {
                pendingImportURL = nil
            }
            Button(loc.t("backup.confirm.action"), role: .destructive) {
                if let url = pendingImportURL {
                    Task { await runRestore(from: url) }
                }
                pendingImportURL = nil
            }
        } message: {
            Text(loc.t("backup.confirm.body"))
        }
    }

    private var summaryCard: some View {
        card {
            HStack(alignment: .top, spacing: 12) {
                Image(systemName: "shield.checkered")
                    .font(.system(size: 22, weight: .semibold))
                    .foregroundStyle(Brand.coral)
                Text(loc.t("backup.summary"))
                    .font(.system(.subheadline, design: .rounded))
                    .foregroundStyle(colors.textSecondary)
            }
        }
    }

    private func prepareExport() {
        statusMessage = nil
        do {
            let json = try applicator.currentExportJSON(manager: manager)
            exportDocument = BackupFileDocument(data: Data(json.utf8))
            showExporter = true
        } catch {
            statusMessage = loc.t("backup.export.error")
        }
    }

    private func runRestore(from url: URL) async {
        working = true
        statusMessage = nil
        defer { working = false }

        let accessed = url.startAccessingSecurityScopedResource()
        defer { if accessed { url.stopAccessingSecurityScopedResource() } }

        guard let data = try? Data(contentsOf: url) else {
            statusMessage = loc.t("backup.restore.error.read")
            return
        }

        let outcome = await manager.applyData(data, applicator: applicator)
        statusMessage = message(for: outcome)
    }

    private func message(for outcome: RestoreOutcome) -> String {
        switch outcome {
        case .success: return loc.t("backup.restore.success")
        case .malformed: return loc.t("backup.restore.error.malformed")
        case .unsupportedVersion: return loc.t("backup.restore.error.version")
        case .tooLarge: return loc.t("backup.restore.error.tooLarge")
        case .readError: return loc.t("backup.restore.error.read")
        case .applyError: return loc.t("backup.restore.error.apply")
        case .timerRunning: return loc.t("backup.restore.error.timer")
        }
    }

    private func sectionHeader(_ title: String) -> some View {
        Text(title.uppercased())
            .font(.system(.caption, design: .rounded).weight(.bold))
            .tracking(1.2)
            .foregroundStyle(colors.textFaint)
    }

    private func actionCard(symbol: String, title: String, hint: String, enabled: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            card {
                HStack(spacing: 12) {
                    Image(systemName: symbol)
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundStyle(enabled ? Brand.coral : colors.textFaint)
                        .frame(width: 28)
                    VStack(alignment: .leading, spacing: 2) {
                        Text(title)
                            .font(.system(.body, design: .rounded).weight(.semibold))
                            .foregroundStyle(enabled ? colors.textPrimary : colors.textFaint)
                        Text(hint)
                            .font(.system(.caption, design: .rounded))
                            .foregroundStyle(colors.textFaint)
                    }
                    Spacer()
                    Image(systemName: "chevron.right")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(colors.textFaint)
                }
            }
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
    }

    private func card<Content: View>(@ViewBuilder _ content: () -> Content) -> some View {
        content()
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(colors.card, in: RoundedRectangle(cornerRadius: 18, style: .continuous))
            .overlay(RoundedRectangle(cornerRadius: 18, style: .continuous).stroke(colors.border, lineWidth: 1))
    }
}

// MARK: - File document

struct BackupFileDocument: FileDocument {
    static var readableContentTypes: [UTType] { [.json] }

    var data: Data

    init(data: Data) {
        self.data = data
    }

    init(configuration: ReadConfiguration) throws {
        data = configuration.file.regularFileContents ?? Data()
    }

    func fileWrapper(configuration: WriteConfiguration) throws -> FileWrapper {
        FileWrapper(regularFileWithContents: data)
    }
}

#Preview {
    NavigationStack {
        BackupRestoreView()
            .environmentObject(TimerEngine())
            .environmentObject(TaskStore())
            .environmentObject(HabitStore())
            .environmentObject(SessionHistoryStore())
            .environmentObject(PreferencesStore())
            .environmentObject(Localization())
            .environmentObject(SoundsStore())
    }
}
