//
//  TrackDownloadManager.swift
//  Purrodoro
//
//  Downloads cloud soundscape tracks to Application Support/soundscapes/
//  as `{id}-v{updatedAtMillis}.mp3`. Mirrors Android `TrackDownloadManager.kt`.
//

import Foundation
import Combine

@MainActor
final class TrackDownloadManager: ObservableObject {
    @Published private(set) var states: [Int: DownloadState] = [:]

    private let directory: URL
    private let audioPublicBaseURL: String
    private var inFlight: Set<Int> = []

    init(
        audioPublicBaseURL: String = SoundscapeConfig.r2PublicURL,
        directory: URL? = nil
    ) {
        self.audioPublicBaseURL = audioPublicBaseURL
        if let directory {
            self.directory = directory
        } else {
            let support = FileManager.default.urls(
                for: .applicationSupportDirectory,
                in: .userDomainMask
            ).first ?? FileManager.default.temporaryDirectory
            self.directory = support.appendingPathComponent("soundscapes", isDirectory: true)
        }
    }

    func localFile(for track: RemoteCatalogRow) -> URL {
        directory.appendingPathComponent("\(track.id)-v\(track.updatedAtMillis).mp3")
    }

    func isDownloaded(_ track: RemoteCatalogRow) -> Bool {
        FileManager.default.fileExists(atPath: localFile(for: track).path)
    }

    func fileSizeBytes(_ track: RemoteCatalogRow) -> Int64 {
        let url = localFile(for: track)
        guard let attrs = try? FileManager.default.attributesOfItem(atPath: url.path),
              let size = attrs[.size] as? NSNumber
        else { return 0 }
        return size.int64Value
    }

    func localPath(for track: RemoteCatalogRow) -> String? {
        isDownloaded(track) ? localFile(for: track).path : nil
    }

    /// Recompute states from disk for the current catalog snapshot.
    func refreshStates(tracks: [RemoteCatalogRow]) {
        var next: [Int: DownloadState] = [:]
        for track in tracks {
            if case .downloading = states[track.id] {
                next[track.id] = states[track.id]
            } else {
                next[track.id] = isDownloaded(track) ? .downloaded : .notDownloaded
            }
        }
        states = next
    }

    /// Delete files that don't match any current catalog row (id + version).
    func prune(tracks: [RemoteCatalogRow]) {
        let valid = Set(tracks.map { localFile(for: $0).lastPathComponent })
        if let files = try? FileManager.default.contentsOfDirectory(
            at: directory,
            includingPropertiesForKeys: nil
        ) {
            for file in files where !valid.contains(file.lastPathComponent) {
                try? FileManager.default.removeItem(at: file)
            }
        }
        refreshStates(tracks: tracks)
    }

    @discardableResult
    func download(_ track: RemoteCatalogRow) async -> Bool {
        let target = localFile(for: track)
        if FileManager.default.fileExists(atPath: target.path) {
            setState(track.id, .downloaded)
            return true
        }
        guard !inFlight.contains(track.id) else { return false }
        guard let url = downloadURL(filePath: track.filePath) else {
            setState(track.id, .notDownloaded)
            return false
        }

        inFlight.insert(track.id)
        defer { inFlight.remove(track.id) }

        try? FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        let tmp = URL(fileURLWithPath: target.path + ".part")
        setState(track.id, .downloading(progress: 0))

        do {
            let expected = max(track.fileSizeBytes, 1)
            let (bytes, response) = try await URLSession.shared.bytes(from: url)
            if let http = response as? HTTPURLResponse, !(200..<300).contains(http.statusCode) {
                throw URLError(.badServerResponse)
            }

            if FileManager.default.fileExists(atPath: tmp.path) {
                try FileManager.default.removeItem(at: tmp)
            }
            FileManager.default.createFile(atPath: tmp.path, contents: nil)
            let handle = try FileHandle(forWritingTo: tmp)
            defer { try? handle.close() }

            var written: Int64 = 0
            var buffer: [UInt8] = []
            buffer.reserveCapacity(64 * 1024)

            for try await byte in bytes {
                buffer.append(byte)
                if buffer.count >= 64 * 1024 {
                    try handle.write(contentsOf: Data(buffer))
                    written += Int64(buffer.count)
                    buffer.removeAll(keepingCapacity: true)
                    let total = (response.expectedContentLength > 0)
                        ? response.expectedContentLength
                        : expected
                    let progress = Float(written) / Float(total)
                    setState(track.id, .downloading(progress: min(max(progress, 0), 1)))
                }
            }
            if !buffer.isEmpty {
                try handle.write(contentsOf: Data(buffer))
                written += Int64(buffer.count)
            }

            if FileManager.default.fileExists(atPath: target.path) {
                try FileManager.default.removeItem(at: target)
            }
            try FileManager.default.moveItem(at: tmp, to: target)
            setState(track.id, .downloaded)
            return true
        } catch {
            try? FileManager.default.removeItem(at: tmp)
            try? FileManager.default.removeItem(at: target)
            setState(track.id, .notDownloaded)
            return false
        }
    }

    func delete(_ track: RemoteCatalogRow) {
        try? FileManager.default.removeItem(at: localFile(for: track))
        setState(track.id, .notDownloaded)
    }

    func downloadURL(filePath: String) -> URL? {
        let base = audioPublicBaseURL.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        let path = filePath.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        guard !path.isEmpty else { return nil }
        return URL(string: "\(base)/\(path)")
    }

    private func setState(_ id: Int, _ state: DownloadState) {
        states[id] = state
    }
}
