//
//  BackupManager.swift
//  Purrodoro
//
//  File IO + safety snapshots around `BackupCodec`. Mirrors Android
//  `BackupManager.kt`.
//

import Foundation

@MainActor
final class BackupManager {
    private let safetyDirectory: URL
    private let appVersionName: String

    init(appVersionName: String, safetyDirectory: URL? = nil) {
        self.appVersionName = appVersionName
        if let safetyDirectory {
            self.safetyDirectory = safetyDirectory
        } else {
            let base = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first!
            self.safetyDirectory = base.appendingPathComponent("backups", isDirectory: true)
        }
    }

    func makeExportJSON(
        displayName: String,
        timerSettings: TimerSettings,
        appearance: AppearanceMode,
        language: AppLanguage,
        selectedSoundId: Int,
        homeScene: HomeScene = .room,
        notificationSound: Bool = true,
        tasks: [TaskItem],
        habits: [HabitItem],
        sessionRecords: [FocusSessionRecord],
        stamps: [String: Int]
    ) throws -> String {
        try BackupCodec.encode(
            displayName: displayName,
            timerSettings: timerSettings,
            appearance: appearance,
            language: language,
            selectedSoundId: selectedSoundId,
            homeScene: homeScene,
            notificationSound: notificationSound,
            tasks: tasks,
            habits: habits,
            sessionRecords: sessionRecords,
            stamps: stamps,
            appVersionName: appVersionName
        )
    }

    func applyDecoded(_ text: String, applicator: BackupRestoreApplicator) async -> RestoreOutcome {
        switch BackupCodec.decode(text) {
        case .malformed:
            return .malformed
        case .unsupportedVersion(let version):
            return .unsupportedVersion(version)
        case .success(let bundle):
            await writeSafetyBackup(applicator: applicator)
            do {
                try applicator.apply(bundle)
                return .success
            } catch BackupApplyError.timerRunning {
                return .timerRunning
            } catch {
                return .applyError
            }
        }
    }

    func applyData(_ data: Data, applicator: BackupRestoreApplicator) async -> RestoreOutcome {
        switch BackupCodec.readCapped(data) {
        case .failure(.tooLarge):
            return .tooLarge
        case .failure(.invalidData):
            return .readError
        case .success(let text):
            return await applyDecoded(text, applicator: applicator)
        }
    }

    private func writeSafetyBackup(applicator: BackupRestoreApplicator) async {
        do {
            try FileManager.default.createDirectory(at: safetyDirectory, withIntermediateDirectories: true)
            let json = try applicator.currentExportJSON(manager: self)
            let name = "autobackup-\(Int(Date().timeIntervalSince1970 * 1000)).json"
            let url = safetyDirectory.appendingPathComponent(name)
            try json.write(to: url, atomically: true, encoding: .utf8)
            pruneSafetyBackups()
        } catch {
            // Non-fatal — restore still proceeds.
        }
    }

    private func pruneSafetyBackups() {
        guard let files = try? FileManager.default.contentsOfDirectory(at: safetyDirectory, includingPropertiesForKeys: [.contentModificationDateKey]) else { return }
        let backups = files
            .filter { $0.lastPathComponent.hasPrefix("autobackup-") }
            .sorted { lhs, rhs in
                let l = (try? lhs.resourceValues(forKeys: [.contentModificationDateKey]).contentModificationDate) ?? .distantPast
                let r = (try? rhs.resourceValues(forKeys: [.contentModificationDateKey]).contentModificationDate) ?? .distantPast
                return l > r
            }
        for url in backups.dropFirst(3) {
            try? FileManager.default.removeItem(at: url)
        }
    }
}

/// Applies a validated [RestoreBundle] to the live stores.
@MainActor
struct BackupRestoreApplicator {
    let engine: TimerEngine
    let tasks: TaskStore
    let habits: HabitStore
    let history: SessionHistoryStore
    let prefs: PreferencesStore
    let loc: Localization
    let sounds: SoundsStore
    let isGuest: Bool

    func currentExportJSON(manager: BackupManager) throws -> String {
        try manager.makeExportJSON(
            displayName: prefs.userName,
            timerSettings: engine.settings,
            appearance: prefs.appearanceMode,
            language: loc.language,
            selectedSoundId: sounds.exportSoundId,
            homeScene: prefs.homeScene,
            notificationSound: prefs.notificationSoundEnabled,
            tasks: tasks.tasks,
            habits: habits.habits,
            sessionRecords: history.records,
            stamps: engine.focusStampsByDay
        )
    }

    func apply(_ bundle: RestoreBundle) throws {
        if engine.isStarted {
            throw BackupApplyError.timerRunning
        }

        tasks.replaceAll(bundle.tasks)
        habits.replaceAll(bundle.habits)
        history.unionRecords(bundle.sessionRecords)
        engine.applyRestored(stamps: bundle.stamps, settings: bundle.timerSettings)
        prefs.userName = bundle.displayName
        prefs.appearanceMode = bundle.prefs.appearance
        prefs.homeScene = bundle.prefs.homeScene
        prefs.notificationSoundEnabled = bundle.prefs.notificationSound
        loc.language = bundle.prefs.language
        sounds.applyRestoredSoundId(bundle.prefs.selectedSoundId)
    }
}

enum BackupApplyError: Error {
    case timerRunning
}
