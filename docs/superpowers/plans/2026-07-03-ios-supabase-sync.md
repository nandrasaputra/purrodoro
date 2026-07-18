# iOS Supabase Sync Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Port Android offline-first Supabase delta sync to iOS so signed-in users with matching `ownerId` sync the same seven tables as Android.

**Architecture:** Mirror `purrodoro-android/.../sync/*` — `SyncEngine` drives pull-then-push per table; `TableSync` adapters map Room-style sync entities to wire JSON; `SyncCoordinator` gates on `canSync(userId, ownerId)`, wires Claim/Keep/Drop hooks, debounced foreground triggers. iOS stores gain sync metadata (`dirty`, `deleted`, `locallyModifiedAt`, stable UUID string ids) before table adapters land.

**Tech Stack:** Swift 6, Supabase Swift SDK (PostgREST), Swift Testing, existing `KeyValueStore` persistence (SQLite later if needed).

---

## Phase 1 — Core engine (no store migration)

- [x] `SyncRules.swift` — `resolvePull`
- [x] `SyncJSON.swift` — row helpers (`rowId`, `maxUpdatedAt`, overlap window)
- [x] `SyncEngine.swift` — `CheckpointStore`, `TableSync`, `SyncEngine`
- [x] `TableSync.swift` — `BaseTableSync` generic skeleton
- [x] `SyncApi.swift` — `SyncApi` + `SupabaseSyncApi`
- [x] `SyncStatus.swift` — snapshot + UI status presentation
- [x] `SyncLogout.swift` — `logoutDecision`, `canSync`, `shouldSignOut`
- [x] `SyncEngineTests` in `SyncTests.swift` — port key cases from `SyncEngineTest.kt`

## Phase 2 — Sync entity layer

- [x] `SyncEntity.swift` — shared sync fields protocol
- [x] `SyncRepository.swift` — categories, tasks, habits, habit_logs, session_records, daily_stamps, profile rows in UserDefaults with dirty tracking
- [x] Migrate `TaskStore` / `HabitStore` / `TimerEngine` / `SessionHistoryStore` / `PreferencesStore` to read/write through `SyncRepository` (UI models stay; persistence becomes sync-aware)
- [x] Legacy id allocators + checkpoint keys in `KeyValueStore`

## Phase 3 — Table adapters + DTOs

- [x] `SyncDtoCodec.swift` — port `SyncDtos.kt` field names (snake_case via CodingKeys)
- [x] `TableSyncs.swift` — seven `TableSync` implementations (FK order: categories → tasks → habits → habit_logs → session_records → daily_stamps → profiles)
- [x] `ProfileTableSync` — apply pulled timer settings to `PreferencesStore` + `TimerEngine`

## Phase 4 — Coordinator + wiring

- [x] `SyncCoordinator.swift` — mirrors `SyncModule.kt`: `syncIfEligible`, `flushBeforeLogout`, Claim/Keep/Drop hooks
- [x] Wire `AccountManager` sync hooks (claim merge, delta, reset checkpoints)
- [x] Foreground + debounced local-write triggers (`scenePhase`, Combine publishers)
- [x] `ProfileView` sync status section
- [x] Sign-out unsynced warning alert

## Phase 5 — QA

- [ ] Two-device matrix (same as Android NAN-65) — manual
- [x] Claim merge with colliding `local_id` / habit log natural keys — `SyncIntegrationTests`
- [x] Startup ownership reconciliation — `StartupOwnershipTests`
- [x] Profile pull → prefs/`TimerEngine` apply — `ProfileSyncIntegrationTests`
- [x] MainActor deadlock fix — `MainActorGate` + sync `@MainActor` DAO hooks in `BaseTableSync` (no nested MainActor awaits)
- [x] Update `purrodoro-ios/STATUS.md` parity matrix

**Automated verification (2026-07-18):** 25 tests across `StartupOwnershipTests`, `NaturalKeyAdoptionTests`, `ProfileSyncIntegrationTests`, `SyncEngineTests`, `SyncRulesTests`, `SyncStatusTests`, `SyncLogoutTests` — all passing (`-parallel-testing-enabled NO`).

**Still open:** NAN-65 two-device manual matrix; no iOS periodic `SyncWorker` retry yet (foreground + debounced local-write triggers only).

**Spec refs:** `docs/superpowers/specs/2026-06-09-auth-and-account-identity.md`, `supabase/migrations/0001_sync_tables.sql`, Android `sync/SyncEngine.kt`.

**Done:** stores use UUID UI models + sync metadata (`dirty` / tombstones / checkpoints) via `SyncRepository`; wire DTOs + seven-table FK order match Android.
