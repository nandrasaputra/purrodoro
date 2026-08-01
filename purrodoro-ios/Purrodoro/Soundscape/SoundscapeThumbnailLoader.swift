//
//  SoundscapeThumbnailLoader.swift
//  Purrodoro
//
//  HTTPS fetch + disk cache for R2 WebP thumbnails.
//  Mirrors Android `SoundscapeThumbnailLoader.kt`.
//

import Foundation
import UIKit

actor SoundscapeThumbnailLoader {
    static let shared = SoundscapeThumbnailLoader()

    private let cacheDir: URL
    private let publicBaseURL: String
    private var memory: [String: UIImage] = [:]

    init(
        publicBaseURL: String = SoundscapeConfig.r2PublicURL,
        directory: URL? = nil
    ) {
        self.publicBaseURL = publicBaseURL
        if let directory {
            self.cacheDir = directory
        } else {
            let support = FileManager.default.urls(
                for: .applicationSupportDirectory,
                in: .userDomainMask
            ).first ?? FileManager.default.temporaryDirectory
            self.cacheDir = support.appendingPathComponent("soundscape-thumbs", isDirectory: true)
        }
    }

    func loadImage(objectKey: String, cacheVersion: Int64 = 0) async -> UIImage? {
        let key = objectKey.trimmingCharacters(in: .whitespacesAndNewlines)
            .trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        guard !key.isEmpty else { return nil }

        let memKey = "\(key)#\(cacheVersion)"
        if let cached = memory[memKey] { return cached }

        let file = cacheFile(for: key, cacheVersion: cacheVersion)
        if let disk = decodeFile(file) {
            memory[memKey] = disk
            return disk
        }

        guard let image = await fetchAndCache(key: key, cacheVersion: cacheVersion, cacheFile: file) else {
            return nil
        }
        memory[memKey] = image
        return image
    }

    private func cacheFile(for objectKey: String, cacheVersion: Int64) -> URL {
        let safeName = objectKey.replacingOccurrences(of: "/", with: "_")
        let suffix = cacheVersion > 0 ? "-v\(cacheVersion)" : ""
        return cacheDir.appendingPathComponent("\(safeName)\(suffix).webp")
    }

    private func decodeFile(_ file: URL) -> UIImage? {
        guard FileManager.default.fileExists(atPath: file.path),
              let data = try? Data(contentsOf: file),
              !data.isEmpty
        else { return nil }
        return UIImage(data: data)
    }

    private func fetchAndCache(
        key: String,
        cacheVersion: Int64,
        cacheFile: URL
    ) async -> UIImage? {
        let base = publicBaseURL.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        guard let url = URL(string: "\(base)/\(key)") else { return nil }

        do {
            let (data, response) = try await URLSession.shared.data(from: url)
            if let http = response as? HTTPURLResponse, !(200..<300).contains(http.statusCode) {
                return nil
            }
            guard !data.isEmpty, let image = UIImage(data: data) else { return nil }

            try? FileManager.default.createDirectory(at: cacheDir, withIntermediateDirectories: true)
            pruneStaleVersions(objectKey: key, cacheVersion: cacheVersion)
            try? data.write(to: cacheFile, options: .atomic)
            return image
        } catch {
            return nil
        }
    }

    private func pruneStaleVersions(objectKey: String, cacheVersion: Int64) {
        let prefix = objectKey.replacingOccurrences(of: "/", with: "_")
        let keep = cacheFile(for: objectKey, cacheVersion: cacheVersion).lastPathComponent
        guard let files = try? FileManager.default.contentsOfDirectory(
            at: cacheDir,
            includingPropertiesForKeys: nil
        ) else { return }
        for file in files where file.lastPathComponent.hasPrefix(prefix) && file.lastPathComponent != keep {
            try? FileManager.default.removeItem(at: file)
        }
    }
}
