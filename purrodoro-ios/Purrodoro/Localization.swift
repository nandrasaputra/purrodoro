//
//  Localization.swift
//  Purrodoro
//
//  Lightweight, runtime-switchable localization. The Android app supports
//  changing language on the fly (no Activity restart), so rather than rely on
//  the bundle's `.lproj` resolution (which is locked at launch) we keep an
//  in-app string table keyed by `AppLanguage`. Views observe `Localization`
//  via `@EnvironmentObject`, so flipping the language re-renders everything.
//

import Foundation
import Combine

enum AppLanguage: String, CaseIterable, Codable, Identifiable {
    case en
    case id

    var id: String { rawValue }

    /// Name shown in its own language (for the picker).
    var nativeLabel: String {
        switch self {
        case .en: return "English"
        case .id: return "Bahasa Indonesia"
        }
    }

    var flag: String {
        switch self {
        case .en: return "🇬🇧"
        case .id: return "🇮🇩"
        }
    }

    /// Best guess from the device locale, used the very first launch.
    static var systemDefault: AppLanguage {
        let code = Locale.preferredLanguages.first?.lowercased() ?? "en"
        return code.hasPrefix("id") ? .id : .en
    }
}

@MainActor
final class Localization: ObservableObject {

    @Published var language: AppLanguage {
        didSet { store.set(language.rawValue, forKey: key) }
    }

    private let store: KeyValueStore
    private let key = "purrodoro.language"

    init(store: KeyValueStore = UserDefaults.standard) {
        self.store = store
        if let raw = store.string(forKey: key), let lang = AppLanguage(rawValue: raw) {
            self.language = lang
        } else {
            self.language = AppLanguage.systemDefault
        }
    }

    /// Look up `key` for the active language, falling back to English then the
    /// raw key. `args` feed `String(format:)` for interpolated strings.
    func t(_ key: String, _ args: CVarArg...) -> String {
        let entry = Self.table[key]
        let template = entry?[language] ?? entry?[.en] ?? key
        return args.isEmpty ? template : String(format: template, locale: Locale.current, arguments: args)
    }

    // MARK: - String table

    private static let table: [String: [AppLanguage: String]] = [
        // Tabs
        "tab.focus": [.en: "Focus", .id: "Fokus"],
        "tab.tasks": [.en: "Tasks", .id: "Tugas"],
        "tab.journey": [.en: "Journey", .id: "Perjalanan"],
        "tab.habits": [.en: "Habits", .id: "Kebiasaan"],

        // Home greetings
        "greeting.morning": [.en: "Good morning", .id: "Selamat pagi"],
        "greeting.afternoon": [.en: "Good afternoon", .id: "Selamat siang"],
        "greeting.evening": [.en: "Good evening", .id: "Selamat malam"],
        "greeting.morning.name": [.en: "Good morning, %@", .id: "Selamat pagi, %@"],
        "greeting.afternoon.name": [.en: "Good afternoon, %@", .id: "Selamat siang, %@"],
        "greeting.evening.name": [.en: "Good evening, %@", .id: "Selamat malam, %@"],
        "encourage.morning": [.en: "Let's make this morning meaningful.", .id: "Mari buat pagi ini bermakna."],
        "encourage.afternoon": [.en: "Keep the afternoon moving gently.", .id: "Jalani siang ini dengan tenang."],
        "encourage.evening": [.en: "Let's make this evening meaningful.", .id: "Mari buat malam ini bermakna."],

        // Timer modes / chip
        "mode.focus": [.en: "Focus", .id: "Fokus"],
        "mode.shortBreak": [.en: "Short Break", .id: "Istirahat Singkat"],
        "mode.longBreak": [.en: "Long Break", .id: "Istirahat Panjang"],
        "mode.ready": [.en: "Ready", .id: "Siap"],

        // Home controls
        "home.startFocus": [.en: "Start Focus", .id: "Mulai Fokus"],
        "home.pause": [.en: "Pause", .id: "Jeda"],
        "home.resume": [.en: "Resume", .id: "Lanjut"],
        "home.stop": [.en: "Stop", .id: "Berhenti"],
        "home.skip": [.en: "Skip", .id: "Lewati"],
        "home.focusToday": [.en: "focus today", .id: "fokus hari ini"],
        "home.minutes": [.en: "minutes", .id: "menit"],
        "home.timerSettings": [.en: "Timer settings", .id: "Pengaturan timer"],
        "home.openProfile": [.en: "Profile", .id: "Profil"],
        "home.detach": [.en: "Detach", .id: "Lepas"],
        "home.focusingSessions": [.en: "Focusing · %ld/%ld sessions", .id: "Fokus · %ld/%ld sesi"],
        "home.habitToday": [.en: "Habit · %ld/%ld today", .id: "Kebiasaan · %ld/%ld hari ini"],
        "home.focusMode.chip": [.en: "Focus mode", .id: "Mode fokus"],
        "home.focusMode.open": [.en: "Open Focus Mode", .id: "Buka Mode Fokus"],
        "home.stopSession": [.en: "Stop session", .id: "Hentikan sesi"],
        "home.skipBreak": [.en: "Skip break", .id: "Lewati istirahat"],
        "home.stop.title": [.en: "Taking a break?", .id: "Istirahat dulu?"],
        "home.stop.body": [
            .en: "Your focus time so far will still be counted.",
            .id: "Waktu fokus sejauh ini tetap dihitung.",
        ],
        "home.stop.confirm": [.en: "Stop for now", .id: "Berhenti dulu"],
        "home.stop.dismiss": [.en: "Stay focused", .id: "Tetap fokus"],
        "home.scene.change": [.en: "Change home scene", .id: "Ganti latar beranda"],
        "home.scene.picker.title": [.en: "Home scene", .id: "Latar beranda"],
        "home.scene.picker.subtitle": [
            .en: "Pick where Kuro waits while you focus. Day and night match your time of day.",
            .id: "Pilih tempat Kuro menunggu saat kamu fokus. Siang dan malam mengikuti waktu lokal.",
        ],
        "home.scene.room.title": [.en: "Room", .id: "Kamar"],
        "home.scene.room.subtitle": [.en: "Desk by the city window", .id: "Meja dekat jendela kota"],
        "home.scene.cafe.title": [.en: "Café", .id: "Kafe"],
        "home.scene.cafe.subtitle": [.en: "Coffee shop corner table", .id: "Meja sudut coffee shop"],
        "home.scene.library.title": [.en: "Library", .id: "Perpustakaan"],
        "home.scene.library.subtitle": [.en: "Quiet desk among the stacks", .id: "Meja tenang di antara rak buku"],
        "home.scene.park.title": [.en: "Park", .id: "Taman"],
        "home.scene.park.subtitle": [.en: "Bench under the open sky", .id: "Bangku di bawah langit terbuka"],

        // Focus mode overlay
        "focus.mode.label.focus": [.en: "FOCUS", .id: "FOKUS"],
        "focus.mode.label.shortBreak": [.en: "SHORT BREAK", .id: "ISTIRAHAT SINGKAT"],
        "focus.mode.label.longBreak": [.en: "LONG BREAK", .id: "ISTIRAHAT PANJANG"],
        "focus.subtext.breakReady": [.en: "Break is ready", .id: "Istirahat siap"],
        "focus.subtext.breakRunning": [.en: "Rest with Purrodoro", .id: "Istirahat bersama Purrodoro"],
        "focus.subtext.paused": [.en: "Paused", .id: "Dijeda"],
        "focus.startBreak": [.en: "Start break", .id: "Mulai istirahat"],
        "focus.resumeFocus": [.en: "Resume focus", .id: "Lanjutkan fokus"],
        "focus.pauseFocus": [.en: "Pause", .id: "Jeda"],
        "focus.tapToExit": [.en: "tap anywhere to exit", .id: "ketuk di mana saja untuk keluar"],
        "focus.pauseBreak.cd": [.en: "Pause break", .id: "Jeda istirahat"],
        "focus.exit.cd": [.en: "Exit focus mode", .id: "Keluar mode fokus"],
        "focus.type.task": [.en: "Task", .id: "Tugas"],
        "focus.type.habit": [.en: "Habit", .id: "Kebiasaan"],

        // Interrupted focus / resume
        "resume.title": [.en: "Continue where you left off", .id: "Lanjutkan sesi sebelumnya"],
        "resume.subtitle": [.en: "%ld min of focus left", .id: "%ld menit fokus tersisa"],
        "resume.action": [.en: "Resume focus", .id: "Lanjutkan fokus"],

        // Start Focus sheet
        "start.title": [.en: "Start Focus", .id: "Mulai Fokus"],
        "start.whatWorkingOn": [.en: "What are you working on?", .id: "Sedang mengerjakan apa?"],
        "start.intentOptional": [.en: "Intent (optional)", .id: "Tujuan (opsional)"],
        "start.none": [.en: "None", .id: "Tanpa"],
        "start.task": [.en: "Task", .id: "Tugas"],
        "start.habit": [.en: "Habit", .id: "Kebiasaan"],
        "start.chooseTask": [.en: "Choose a task", .id: "Pilih tugas"],
        "start.chooseHabit": [.en: "Choose a habit", .id: "Pilih kebiasaan"],
        "start.noTasks": [.en: "No tasks yet — add one from the Tasks tab.", .id: "Belum ada tugas — tambah dari tab Tugas."],
        "start.noHabits": [.en: "No habits scheduled today.", .id: "Tidak ada kebiasaan terjadwal hari ini."],
        "start.config": [.en: "Session", .id: "Sesi"],
        "start.focusLength": [.en: "Focus length", .id: "Durasi fokus"],
        "start.sessions": [.en: "Focus sessions", .id: "Sesi fokus"],
        "start.change": [.en: "Change", .id: "Ubah"],
        "start.begin": [.en: "Start Focus", .id: "Mulai Fokus"],
        "start.minUnit": [.en: "%ld min", .id: "%ld mnt"],

        // Settings
        "settings.title": [.en: "Settings", .id: "Pengaturan"],
        "settings.profile": [.en: "Profile", .id: "Profil"],
        "settings.yourName": [.en: "Your name", .id: "Nama kamu"],
        "settings.addName": [.en: "Add your name", .id: "Tambahkan namamu"],
        "settings.timer": [.en: "Timer", .id: "Timer"],
        "settings.focus": [.en: "Focus", .id: "Fokus"],
        "settings.shortBreak": [.en: "Short break", .id: "Istirahat singkat"],
        "settings.longBreak": [.en: "Long break", .id: "Istirahat panjang"],
        "settings.focusSessions": [.en: "Focus sessions", .id: "Sesi fokus"],
        "settings.beforeLongBreak": [.en: "Before a long break", .id: "Sebelum istirahat panjang"],
        "settings.autoStart": [.en: "Auto-start next", .id: "Mulai otomatis"],
        "settings.autoStartDesc": [.en: "Flow from focus to break automatically", .id: "Lanjut dari fokus ke istirahat otomatis"],
        "settings.appearance": [.en: "Appearance", .id: "Tampilan"],
        "settings.theme": [.en: "Theme", .id: "Tema"],
        "settings.language": [.en: "Language", .id: "Bahasa"],
        "settings.done": [.en: "Done", .id: "Selesai"],

        // Profile
        "profile.title": [.en: "Profile", .id: "Profil"],
        "profile.friend": [.en: "Friend", .id: "Teman"],
        "profile.account": [.en: "Account", .id: "Akun"],
        "profile.guest": [.en: "Guest", .id: "Tamu"],
        "profile.guestHint": [
            .en: "Sign in to sync your tasks, habits, and journey across devices.",
            .id: "Masuk untuk menyinkronkan tugas, kebiasaan, dan perjalananmu di semua perangkat.",
        ],
        "profile.signIn": [.en: "Sign in", .id: "Masuk"],
        "profile.premium.title": [.en: "Purrodoro Premium", .id: "Purrodoro Premium"],
        "profile.premium.subtitle": [
            .en: "Soundscapes, home scenes, and more.",
            .id: "Soundscape, latar rumah, dan lainnya.",
        ],
        "profile.premium.explore": [.en: "Explore Premium", .id: "Jelajahi Premium"],
        "profile.notifications": [.en: "Notifications", .id: "Notifikasi"],
        "profile.notificationsOn": [.en: "Notifications enabled", .id: "Notifikasi aktif"],
        "profile.notificationsOff": [.en: "Notifications are off", .id: "Notifikasi nonaktif"],
        "profile.notificationsOffHint": [
            .en: "Enable them so Purrodoro can nudge you when a focus or break ends.",
            .id: "Aktifkan agar Purrodoro bisa mengingatkan saat fokus atau istirahat selesai.",
        ],
        "profile.openSettings": [.en: "Open Settings", .id: "Buka Pengaturan"],
        "profile.data": [.en: "Data", .id: "Data"],
        "profile.backup": [.en: "Backup & restore", .id: "Cadangan & pulihkan"],
        "profile.storage": [.en: "Storage", .id: "Penyimpanan"],
        "profile.comingSoon": [.en: "Coming soon", .id: "Segera hadir"],
        "profile.about": [.en: "About", .id: "Tentang"],
        "profile.version": [.en: "Version %@", .id: "Versi %@"],
        "profile.privacy": [.en: "Privacy policy", .id: "Kebijakan privasi"],

        // Account & login
        "account.loading": [.en: "Checking sign-in…", .id: "Memeriksa masuk…"],
        "account.signedInHint": [
            .en: "Your tasks, habits, and journey sync across devices.",
            .id: "Tugas, kebiasaan, dan perjalananmu tersinkron di semua perangkat.",
        ],
        "account.signOut": [.en: "Sign out", .id: "Keluar"],
        "account.signedInGoogle": [.en: "Signed in with Google", .id: "Masuk dengan Google"],
        "account.signedInApple": [.en: "Signed in with Apple", .id: "Masuk dengan Apple"],
        "account.signedInGeneric": [.en: "Signed in", .id: "Sudah masuk"],
        "account.switch.title": [.en: "Switch account?", .id: "Ganti akun?"],
        "account.switch.message": [
            .en: "This device has another account's data. Signing in will remove it. Continue?",
            .id: "Perangkat ini berisi data akun lain. Masuk akan menghapusnya. Lanjutkan?",
        ],
        "account.switch.confirm": [.en: "Remove and continue", .id: "Hapus dan lanjutkan"],
        "account.switch.cancel": [.en: "Cancel", .id: "Batal"],
        "sync.status.notYet": [.en: "Syncing soon", .id: "Segera tersimpan"],
        "sync.status.skipped": [.en: "Setting up backup", .id: "Menyiapkan backup"],
        "sync.status.upToDate": [.en: "Backed up", .id: "Sudah tersimpan"],
        "sync.status.incomplete": [.en: "Saving…", .id: "Sedang menyimpan…"],
        "sync.status.failed": [.en: "Couldn't back up", .id: "Gagal tersimpan"],
        "sync.status.lastSync": [.en: "Last backed up · %@", .id: "Terakhir disimpan · %@"],
        "sync.status.lastTried": [.en: "Last tried · %@", .id: "Terakhir dicoba · %@"],
        "sync.time.justNow": [.en: "just now", .id: "baru saja"],
        "sync.time.minutesAgo": [.en: "%d min ago", .id: "%d menit lalu"],
        "sync.time.hoursAgo": [.en: "%d hr ago", .id: "%d jam lalu"],
        "sync.time.stale": [.en: "earlier", .id: "sebelumnya"],
        "logout.unsynced.title": [
            .en: "Some changes aren't backed up yet",
            .id: "Keluar tanpa sinkronisasi?",
        ],
        "logout.unsynced.body": [
            .en: "We couldn't sync your latest updates. They'll stay on this device, but may not come with you if someone else signs in here.",
            .id: "Beberapa perubahan terbaru belum tersinkron ke cloud. Perubahan itu tetap ada di perangkat ini, tetapi bisa hilang jika akun lain masuk di sini.",
        ],
        "logout.unsynced.confirm": [.en: "Sign out anyway", .id: "Tetap keluar"],
        "logout.unsynced.dismiss": [.en: "Keep syncing", .id: "Lanjut sinkron"],
        "login.title": [.en: "Sign in", .id: "Masuk"],
        "login.subtitle": [
            .en: "Back up tasks, habits, and focus history across devices.",
            .id: "Cadangkan tugas, kebiasaan, dan riwayat fokus di semua perangkat.",
        ],
        "login.catA11y": [.en: "Purrodoro the cat waving hello", .id: "Purrodoro si kucing melambai"],
        "login.continueGoogle": [.en: "Continue with Google", .id: "Lanjutkan dengan Google"],
        "login.continueGuest": [.en: "Continue as guest", .id: "Lanjut sebagai tamu"],
        "login.guestHint": [
            .en: "Saved on this device only. Sign in to back it up.",
            .id: "Tersimpan di perangkat ini saja. Masuk untuk mencadangkannya.",
        ],
        "login.error.generic": [.en: "Couldn't sign in. Please try again.", .id: "Tidak bisa masuk. Coba lagi."],
        "login.error.notConfigured": [
            .en: "Google sign-in isn't set up yet.",
            .id: "Masuk dengan Google belum disiapkan.",
        ],

        // Backup & restore
        "backup.title": [.en: "Backup & restore", .id: "Cadangan & pulihkan"],
        "backup.summary": [
            .en: "Export your tasks, habits, journey stamps, and settings as a JSON file. Restore replaces your on-device data (guest only).",
            .id: "Ekspor tugas, kebiasaan, cap perjalanan, dan pengaturan sebagai file JSON. Pulihkan mengganti data di perangkat (tamu saja).",
        ],
        "backup.section.export": [.en: "Backup", .id: "Cadangkan"],
        "backup.section.restore": [.en: "Restore", .id: "Pulihkan"],
        "backup.export.label": [.en: "Export backup", .id: "Ekspor cadangan"],
        "backup.export.hint": [.en: "Save a JSON file to Files or iCloud", .id: "Simpan file JSON ke Files atau iCloud"],
        "backup.export.success": [.en: "Backup exported.", .id: "Cadangan diekspor."],
        "backup.export.error": [.en: "Couldn't export backup.", .id: "Gagal mengekspor cadangan."],
        "backup.import.label": [.en: "Restore from file", .id: "Pulihkan dari file"],
        "backup.import.hint": [.en: "Import a Purrodoro backup JSON", .id: "Impor file JSON cadangan Purrodoro"],
        "backup.working": [.en: "Working…", .id: "Memproses…"],
        "backup.confirm.title": [.en: "Restore backup?", .id: "Pulihkan cadangan?"],
        "backup.confirm.body": [
            .en: "This replaces your tasks, habits, and journey data on this device. Your current data is saved as a safety copy first.",
            .id: "Ini mengganti tugas, kebiasaan, dan data perjalanan di perangkat ini. Data saat ini disimpan sebagai salinan aman terlebih dahulu.",
        ],
        "backup.confirm.action": [.en: "Restore", .id: "Pulihkan"],
        "backup.restore.success": [.en: "Backup restored.", .id: "Cadangan dipulihkan."],
        "backup.restore.error.malformed": [.en: "That file doesn't look like a Purrodoro backup.", .id: "File itu bukan cadangan Purrodoro."],
        "backup.restore.error.version": [.en: "This backup needs a newer version of Purrodoro.", .id: "Cadangan ini membutuhkan versi Purrodoro yang lebih baru."],
        "backup.restore.error.tooLarge": [.en: "That file is too large to import.", .id: "File terlalu besar untuk diimpor."],
        "backup.restore.error.read": [.en: "Couldn't read that file.", .id: "Gagal membaca file."],
        "backup.restore.error.apply": [.en: "Restore failed — a safety copy was saved.", .id: "Gagal memulihkan — salinan aman tersimpan."],
        "backup.restore.error.timer": [.en: "Stop the timer before restoring.", .id: "Hentikan timer sebelum memulihkan."],
        "appearance.auto": [.en: "Auto", .id: "Otomatis"],
        "appearance.light": [.en: "Light", .id: "Terang"],
        "appearance.dark": [.en: "Dark", .id: "Gelap"],
        "unit.min": [.en: "%ld min", .id: "%ld mnt"],

        // Sounds / Soundscape
        "sounds.title": [.en: "Soundscape", .id: "Soundscape"],
        "sounds.subtitle": [.en: "Mix ambience + lo-fi for your focus", .id: "Campur ambien + lo-fi untuk fokusmu"],
        "sounds.tapToPlay": [.en: "Tap to play", .id: "Ketuk untuk putar"],
        "sounds.playing": [.en: "Playing", .id: "Diputar"],
        "sounds.selected": [.en: "Selected", .id: "Dipilih"],
        "sounds.premium": [.en: "Premium", .id: "Premium"],
        "sounds.none": [.en: "Silence", .id: "Hening"],
        "sounds.noneDesc": [.en: "No ambience", .id: "Tanpa ambien"],
        "soundscape.section.ambience": [.en: "Ambience", .id: "Ambien"],
        "soundscape.section.lofi": [.en: "Lo-fi music", .id: "Musik lo-fi"],
        "soundscape.music.none": [.en: "No music", .id: "Tanpa musik"],
        "soundscape.music.noneDesc": [.en: "Lo-fi deck silent", .id: "Deck lo-fi diam"],
        "soundscape.mute_all": [.en: "Mute all", .id: "Bisukan semua"],
        "soundscape.downloading": [.en: "Downloading…", .id: "Mengunduh…"],
        "soundscape.downloading_pct": [.en: "Downloading… %d%%", .id: "Mengunduh… %d%%"],
        "soundscape.download_size": [.en: "%@ MB · tap to download", .id: "%@ MB · ketuk untuk unduh"],
        "soundscape.home_hint": [
            .en: "Tap to mix ambience + lo-fi",
            .id: "Ketuk untuk campur ambien + lo-fi",
        ],
        "soundscape.compact_separator": [.en: " · ", .id: " · "],
        "soundscape.mini_open": [.en: "Open Soundscape", .id: "Buka Soundscape"],
        "soundscape.transport.play": [.en: "Play Soundscape", .id: "Putar Soundscape"],
        "soundscape.transport.pause": [.en: "Pause Soundscape", .id: "Jeda Soundscape"],

        // Storage
        "storage.title": [.en: "Storage", .id: "Penyimpanan"],
        "storage.used": [.en: "%@ used", .id: "%@ terpakai"],
        "storage.summary.subtitle": [
            .en: "Used by downloaded sounds",
            .id: "Dipakai oleh suara yang diunduh",
        ],
        "storage.section.downloads": [.en: "Downloads", .id: "Unduhan"],
        "storage.downloading": [.en: "Downloading… %d%%", .id: "Mengunduh… %d%%"],
        "storage.remove_one_cd": [.en: "Remove %@ download", .id: "Hapus unduhan %@"],
        "storage.remove_all": [.en: "Remove all downloads", .id: "Hapus semua unduhan"],
        "storage.remove_all.title": [.en: "Remove all downloads?", .id: "Hapus semua unduhan?"],
        "storage.remove_all.body": [
            .en: "This frees %@. You can download these sounds again anytime.",
            .id: "Ini mengosongkan %@. Kamu bisa mengunduh suara ini lagi kapan saja.",
        ],
        "storage.remove_all.confirm": [.en: "Remove all", .id: "Hapus semua"],
        "storage.empty": [.en: "No downloads yet", .id: "Belum ada unduhan"],
        "storage.empty.hint": [
            .en: "Sounds you download for offline play will show up here.",
            .id: "Suara yang kamu unduh untuk diputar offline akan muncul di sini.",
        ],

        "sound.rain.name": [.en: "Rainy Window", .id: "Jendela Hujan"],
        "sound.rain.desc": [.en: "Soft rain for deep reading", .id: "Hujan lembut untuk membaca dalam"],
        "sound.cafe.name": [.en: "Cafe Purr", .id: "Dengkuran Kafe"],
        "sound.cafe.desc": [.en: "Warm cafe room ambience", .id: "Suasana kafe yang hangat"],
        "sound.forest.name": [.en: "Forest Nap", .id: "Tidur Siang di Hutan"],
        "sound.forest.desc": [.en: "Leaves, birds, and slow wind", .id: "Daun, burung, dan angin pelan"],
        "sound.fire.name": [.en: "Fireplace", .id: "Perapian"],
        "sound.fire.desc": [.en: "Crackling fire with sleepy cat", .id: "Api berderak dengan kucing mengantuk"],
        "sound.sea_wave.name": [.en: "Ocean Shore", .id: "Pantai Tenang"],
        "sound.sea_wave.desc": [.en: "Gentle surf rolling onto sand", .id: "Ombak lembut menyapu pasir"],
        "sound.winter_wind.name": [.en: "Winter Wind", .id: "Angin Musim Dingin"],
        "sound.winter_wind.desc": [.en: "Cold breeze through a quiet night", .id: "Hembusan dingin di malam yang sunyi"],
        "sound.lofi.local_artist": [.en: "", .id: ""],
        "sound.lofi.desk_lamp_rain.name": [.en: "Desk Lamp Rain", .id: "Desk Lamp Rain"],
        "sound.lofi.mosswood_morning.name": [.en: "Mosswood Morning", .id: "Mosswood Morning"],
        "sound.lofi.paper_skies.name": [.en: "Paper Skies", .id: "Paper Skies"],
        "sound.lofi.quiet_window_code.name": [.en: "Quiet Window Code", .id: "Quiet Window Code"],
        "sound.lofi.sore_di_pendopo.name": [.en: "Sore di Pendopo", .id: "Sore di Pendopo"],
        "sound.lofi.sudirman_rain_loop.name": [.en: "Sudirman Rain Loop", .id: "Sudirman Rain Loop"],

        // Celebration
        "cycle.nice": [.en: "Nice", .id: "Mantap"],
        "cycle.v1.title": [.en: "Cycle complete!", .id: "Siklus selesai!"],
        "cycle.v1.body": [.en: "You did it — Purrodoro is proud of you.", .id: "Kamu berhasil — Purrodoro bangga padamu."],
        "cycle.v2.title": [.en: "Beautifully done", .id: "Kerja bagus"],
        "cycle.v2.body": [.en: "That's a full cycle of focus. Stretch and breathe.", .id: "Satu siklus fokus penuh. Regangkan badan dan tarik napas."],
        "cycle.v3.title": [.en: "Purr-fect work", .id: "Kerja yang purr-fect"],
        "cycle.v3.body": [.en: "The cat is impressed. Take a well-earned rest.", .id: "Si kucing terkesan. Nikmati istirahatmu."],
        "cycle.v4.title": [.en: "Focus complete", .id: "Fokus selesai"],
        "cycle.v4.body": [.en: "Another cycle in the books. You're on a roll.", .id: "Satu siklus lagi tercatat. Kamu makin lancar."],
        "cycle.v5.title": [.en: "Look at you go!", .id: "Lihat pencapaianmu!"],
        "cycle.v5.body": [.en: "A whole cycle done with care. Be proud.", .id: "Satu siklus penuh selesai dengan baik. Banggalah."],

        // Tasks
        "tasks.title": [.en: "Tasks", .id: "Tugas"],
        "tasks.active": [.en: "Active", .id: "Aktif"],
        "tasks.done": [.en: "Done", .id: "Selesai"],
        "tasks.emptyTitle": [.en: "No tasks yet", .id: "Belum ada tugas"],
        "tasks.emptySubtitle": [.en: "Add a task and attach it to a focus session to track your progress.", .id: "Tambahkan tugas dan kaitkan dengan sesi fokus untuk melacak kemajuanmu."],
        "tasks.new": [.en: "New task", .id: "Tugas baru"],
        "tasks.edit": [.en: "Edit task", .id: "Ubah tugas"],
        "tasks.add": [.en: "Add task", .id: "Tambah tugas"],
        "tasks.whatFocus": [.en: "What do you want to focus on?", .id: "Apa yang ingin kamu fokuskan?"],
        "tasks.priority": [.en: "Priority", .id: "Prioritas"],
        "tasks.plannedSessions": [.en: "Planned sessions", .id: "Sesi direncanakan"],
        "tasks.category": [.en: "Category", .id: "Kategori"],
        "tasks.categoryPlaceholder": [.en: "e.g. Work, Study", .id: "mis. Kerja, Belajar"],
        "tasks.dueDate": [.en: "Due date", .id: "Tenggat"],
        "tasks.noDue": [.en: "No due date", .id: "Tanpa tenggat"],
        "tasks.overdue": [.en: "Overdue", .id: "Terlambat"],
        "tasks.dueToday": [.en: "Due today", .id: "Tenggat hari ini"],
        "priority.low": [.en: "Low", .id: "Rendah"],
        "priority.normal": [.en: "Normal", .id: "Normal"],
        "priority.high": [.en: "High", .id: "Tinggi"],
        "common.edit": [.en: "Edit", .id: "Ubah"],
        "common.delete": [.en: "Delete", .id: "Hapus"],
        "common.cancel": [.en: "Cancel", .id: "Batal"],
        "common.save": [.en: "Save", .id: "Simpan"],
        "common.ok": [.en: "OK", .id: "OK"],
        "common.back": [.en: "Back", .id: "Kembali"],

        // Habits
        "habits.title": [.en: "Habits", .id: "Kebiasaan"],
        "habits.emptySubtitle": [.en: "Add a gentle daily ritual and keep your streak alive, one focus at a time.", .id: "Tambahkan ritual harian dan jaga rentetanmu, satu fokus setiap kali."],
        "habits.new": [.en: "New habit", .id: "Kebiasaan baru"],
        "habits.edit": [.en: "Edit habit", .id: "Ubah kebiasaan"],
        "habits.add": [.en: "Add habit", .id: "Tambah kebiasaan"],
        "habits.name": [.en: "Habit name", .id: "Nama kebiasaan"],
        "habits.icon": [.en: "Icon", .id: "Ikon"],
        "habits.color": [.en: "Color", .id: "Warna"],
        "habits.dailyTarget": [.en: "Daily target", .id: "Target harian"],
        "habits.perDay": [.en: "%ld / day", .id: "%ld / hari"],
        "habits.occurrence": [.en: "Repeat on", .id: "Ulangi pada"],
        "habits.everyDay": [.en: "Every day", .id: "Setiap hari"],
        "habits.timeBlock": [.en: "Time of day", .id: "Waktu"],
        "habits.restToday": [.en: "Rest day", .id: "Hari istirahat"],
        "habits.notToday": [.en: "Not scheduled today", .id: "Tidak terjadwal hari ini"],
        "habits.dayStreak": [.en: "%ld day", .id: "%ld hari"],
        "habits.daysStreak": [.en: "%ld days", .id: "%ld hari"],
        "timeblock.any": [.en: "Anytime", .id: "Kapan saja"],
        "timeblock.morning": [.en: "Morning", .id: "Pagi"],
        "timeblock.afternoon": [.en: "Afternoon", .id: "Siang"],
        "timeblock.evening": [.en: "Evening", .id: "Malam"],

        // Journey
        "journey.title": [.en: "Journey", .id: "Perjalanan"],
        "journey.emptySubtitle": [.en: "Finish your first focus session and your streak will start to bloom here.", .id: "Selesaikan sesi fokus pertamamu dan rentetanmu akan mulai mekar di sini."],
        "journey.startToday": [.en: "Start a session today", .id: "Mulai sesi hari ini"],
        "journey.currentStreak": [.en: "Current focus streak", .id: "Rentetan fokus saat ini"],
        "journey.thisWeek": [.en: "this week", .id: "minggu ini"],
        "journey.totalSessions": [.en: "total sessions", .id: "total sesi"],
        "journey.bestStreak": [.en: "best streak", .id: "rentetan terbaik"],
        "journey.recentSessions": [.en: "Recent sessions", .id: "Sesi terbaru"],
        "journey.focusSession": [.en: "Focus session", .id: "Sesi fokus"],
        "journey.less": [.en: "Less", .id: "Sedikit"],
        "journey.more": [.en: "More", .id: "Banyak"],
        "journey.openHistory": [.en: "Open history", .id: "Buka riwayat"],
        "journey.detail": [.en: "Journey detail", .id: "Detail perjalanan"],
        "journey.timeRange": [.en: "%@ - %@", .id: "%@ - %@"],
        "journey.statFocused": [.en: "Focused", .id: "Fokus"],
        "journey.statSessions": [.en: "Sessions", .id: "Sesi"],
        "journey.statRest": [.en: "Rest", .id: "Istirahat"],
        "journey.statusCompleted": [.en: "Completed", .id: "Selesai"],
        "journey.deleteRecord": [.en: "Delete Record", .id: "Hapus Catatan"],
        "journey.deleteTitle": [.en: "Delete this record?", .id: "Hapus catatan ini?"],
        "journey.deleteBody": [.en: "The session from %@ to %@ will be permanently removed.", .id: "Sesi dari %@ hingga %@ akan dihapus permanen."],

        // History
        "history.title": [.en: "History", .id: "Riwayat"],
        "history.emptyDay": [.en: "No focus sessions on this day.", .id: "Tidak ada sesi fokus pada hari ini."],
        "history.previousMonth": [.en: "Previous month", .id: "Bulan sebelumnya"],
        "history.nextMonth": [.en: "Next month", .id: "Bulan berikutnya"],

        "unit.minAbbrev": [.en: "%ldm", .id: "%ld mnt"],
        "date.today": [.en: "Today", .id: "Hari ini"],
        "date.yesterday": [.en: "Yesterday", .id: "Kemarin"],

        // Onboarding
        "onboard.skip": [.en: "Skip", .id: "Lewati"],
        "onboard.welcome.title": [.en: "Welcome to Purrodoro", .id: "Selamat datang di Purrodoro"],
        "onboard.welcome.subtitle": [.en: "A cozy focus companion. Work in gentle pomodoro cycles and let the cat cheer you on.", .id: "Teman fokus yang hangat. Bekerja dalam siklus pomodoro yang lembut dan biarkan si kucing menyemangatimu."],
        "onboard.name.title": [.en: "What should we call you?", .id: "Kami panggil kamu siapa?"],
        "onboard.name.subtitle": [.en: "We'll use it to greet you. You can change or skip this anytime.", .id: "Kami pakai untuk menyapamu. Bisa diubah atau dilewati kapan saja."],
        "onboard.notif.title": [.en: "Stay in the loop", .id: "Tetap terhubung"],
        "onboard.notif.subtitle": [.en: "Let Purrodoro nudge you when a focus block or break ends — even while the app is in the background.", .id: "Biarkan Purrodoro mengingatkanmu saat fokus atau istirahat selesai — bahkan saat aplikasi di latar belakang."],
        "onboard.getStarted": [.en: "Get started", .id: "Mulai"],
        "onboard.continue": [.en: "Continue", .id: "Lanjut"],
        "onboard.enableNotif": [.en: "Enable notifications", .id: "Aktifkan notifikasi"],
        "onboard.enabling": [.en: "Enabling…", .id: "Mengaktifkan…"],

        // Premium
        "premium.member.title": [.en: "You're Premium", .id: "Kamu Premium"],
        "premium.member.body": [
            .en: "Thanks for supporting Purrodoro. Enjoy unlimited tasks, habits, sounds, and scenes.",
            .id: "Terima kasih mendukung Purrodoro. Nikmati tugas, kebiasaan, suara, dan suasana tanpa batas.",
        ],
        "premium.screen.headline": [
            .en: "Go further with Premium",
            .id: "Lebih jauh dengan Premium",
        ],
        "premium.screen.whatYouGet": [.en: "What you get", .id: "Yang kamu dapat"],
        "premium.screen.pickPlan": [.en: "Pick a plan", .id: "Pilih paket"],
        "premium.screen.compareSummary": [
            .en: "Free stays generous. Premium unlocks more headroom and cozy extras.",
            .id: "Gratis tetap murah hati. Premium membuka kapasitas lebih dan ekstra yang nyaman.",
        ],
        "premium.compare.tasks.free": [.en: "10 active tasks", .id: "10 tugas aktif"],
        "premium.compare.tasks.premium": [.en: "Unlimited tasks", .id: "Tugas tanpa batas"],
        "premium.compare.habits.free": [.en: "10 habits", .id: "10 kebiasaan"],
        "premium.compare.habits.premium": [.en: "Unlimited habits", .id: "Kebiasaan tanpa batas"],
        "premium.compare.sounds.free": [.en: "Free ambience", .id: "Ambience gratis"],
        "premium.compare.sounds.premium": [.en: "Full soundscape library", .id: "Pustaka soundscape penuh"],
        "premium.compare.scenes.free": [.en: "Room scene", .id: "Suasana Room"],
        "premium.compare.scenes.premium": [.en: "All home scenes", .id: "Semua suasana rumah"],
        "premium.plan.monthly": [.en: "Monthly", .id: "Bulanan"],
        "premium.plan.yearly": [.en: "Yearly", .id: "Tahunan"],
        "premium.plan.savings": [.en: "Save %d%%", .id: "Hemat %d%%"],
        "premium.cta.subscribe": [.en: "Subscribe", .id: "Berlangganan"],
        "premium.cta.startTrial": [.en: "Start free trial", .id: "Mulai uji coba gratis"],
        "premium.cta.maybeLater": [.en: "Maybe later", .id: "Nanti saja"],
        "premium.cta.retry": [.en: "Retry", .id: "Coba lagi"],
        "premium.restore": [.en: "Restore purchases", .id: "Pulihkan pembelian"],
        "premium.manage": [.en: "Manage subscription", .id: "Kelola langganan"],
        "premium.restore.success": [.en: "Purchases restored.", .id: "Pembelian dipulihkan."],
        "premium.restore.none": [.en: "No active subscription found.", .id: "Tidak ada langganan aktif."],
        "premium.purchase.success": [.en: "Welcome to Premium!", .id: "Selamat datang di Premium!"],
        "premium.error.catalog_unavailable": [
            .en: "Subscription options aren't available right now.",
            .id: "Opsi langganan belum tersedia sekarang.",
        ],
        "premium.error.catalog_failed": [
            .en: "Couldn't load prices. Check your connection and try again.",
            .id: "Gagal memuat harga. Cek koneksi dan coba lagi.",
        ],
        "premium.error.product_unavailable": [
            .en: "That plan isn't available right now.",
            .id: "Paket itu belum tersedia sekarang.",
        ],
        "premium.error.purchase_failed": [
            .en: "Purchase didn't complete. Please try again.",
            .id: "Pembelian tidak selesai. Coba lagi.",
        ],
        "premium.error.verification_failed": [
            .en: "Couldn't verify the purchase.",
            .id: "Tidak bisa memverifikasi pembelian.",
        ],
        "premium.error.restore_failed": [
            .en: "Couldn't restore purchases. Please try again.",
            .id: "Gagal memulihkan pembelian. Coba lagi.",
        ],
        "premium.login.title": [
            .en: "Sign in to explore Premium",
            .id: "Masuk untuk menjelajahi Premium",
        ],
        "premium.login.body": [
            .en: "Create or sign in to your account to view plans and subscribe.",
            .id: "Buat atau masuk ke akunmu untuk melihat paket dan berlangganan.",
        ],
        "premium.login.signIn": [.en: "Sign in", .id: "Masuk"],
        "premium.upsell.taskCap.title": [
            .en: "You've reached 10 active tasks",
            .id: "Kamu mencapai 10 tugas aktif",
        ],
        "premium.upsell.taskCap.body": [
            .en: "Complete or delete a task to free a slot, or unlock unlimited tasks with Premium.",
            .id: "Selesaikan atau hapus tugas untuk slot baru, atau buka tugas tanpa batas dengan Premium.",
        ],
        "premium.upsell.habitCap.title": [
            .en: "You've reached 10 habits",
            .id: "Kamu mencapai 10 kebiasaan",
        ],
        "premium.upsell.habitCap.body": [
            .en: "Delete a habit to free a slot, or unlock unlimited habits with Premium.",
            .id: "Hapus kebiasaan untuk slot baru, atau buka kebiasaan tanpa batas dengan Premium.",
        ],
        "premium.upsell.generic.title": [
            .en: "Unlock Purrodoro Premium",
            .id: "Buka Purrodoro Premium",
        ],
        "premium.upsell.generic.body": [
            .en: "More tasks, habits, sounds, and home scenes — when you're ready.",
            .id: "Lebih banyak tugas, kebiasaan, suara, dan suasana rumah — saat kamu siap.",
        ],
        "premium.upsell.sound.title": [
            .en: "Unlock %@",
            .id: "Buka %@",
        ],
        "premium.upsell.scene.title": [
            .en: "Unlock %@ scene",
            .id: "Buka suasana %@",
        ],
        "premium.upsell.locked.body": [
            .en: "This is part of Purrodoro Premium. Subscribe to unlock it.",
            .id: "Ini bagian dari Purrodoro Premium. Berlangganan untuk membukanya.",
        ],
        "premium.guest.unlock.title": [
            .en: "Sign in to unlock Premium",
            .id: "Masuk untuk membuka Premium",
        ],
        "free_tier.tasks": [.en: "%d/%d active", .id: "%d/%d aktif"],
        "free_tier.habits": [.en: "%d/%d habits", .id: "%d/%d kebiasaan"],
        "free_tier.cap_cd": [
            .en: "%d of %d free slots used",
            .id: "%d dari %d slot gratis terpakai",
        ],
        "profile.premium.activeSubtitle": [
            .en: "Unlimited tasks, habits, and cozy extras.",
            .id: "Tugas, kebiasaan, dan ekstra nyaman tanpa batas.",
        ],

        // Accessibility
        "a11y.timer": [.en: "%@ timer", .id: "Timer %@"],
        "a11y.remaining": [.en: "%@ remaining", .id: "%@ tersisa"],
        "a11y.paused": [.en: "Paused", .id: "Dijeda"],
        "a11y.cycleProgress": [.en: "Cycle progress", .id: "Progres siklus"],
        "a11y.sessionsOf": [.en: "%ld of %ld focus sessions", .id: "%ld dari %ld sesi fokus"],
    ]
}
