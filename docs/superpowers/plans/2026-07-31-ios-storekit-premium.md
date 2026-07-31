# iOS StoreKit Premium Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Port Android Purrodoro Premium to iOS with StoreKit 2 — entitlement, PremiumGate caps (10/10), Profile discovery, upsell sheets — mirroring Android behavior.

**Architecture:** Pure `PremiumGate` / `PremiumResolver` / `PremiumEntitlement`; `StoreKitManager` owns StoreKit 2 catalog + purchase + entitlement refresh; `PreferencesStore` caches `storePremium` + override flags; UI ports `PremiumView`, upsell, login gate, cap headers. Guest discovery requires sign-in.

**Tech Stack:** SwiftUI, StoreKit 2, Swift Testing (`PurrodoroTests`), `KeyValueStore` / `PreferencesStore`, existing `AuthStore` + `Localization`.

**Spec:** [`docs/superpowers/specs/2026-07-31-ios-storekit-premium-design.md`](../specs/2026-07-31-ios-storekit-premium-design.md)

---

## File structure

| File | Responsibility |
|------|----------------|
| `Purrodoro/StoreKitConfig.swift` | Product id constants |
| `Purrodoro/PremiumPlan.swift` | Monthly / Yearly enum |
| `Purrodoro/PremiumGate.swift` | Cap constants + helpers |
| `Purrodoro/PremiumResolver.swift` | Override ∪ store |
| `Purrodoro/PremiumEntitlement.swift` | Active entitlement check helpers |
| `Purrodoro/PremiumOverride.swift` | DEBUG force premium |
| `Purrodoro/SubscriptionOffers.swift` | Catalog state + plan offer details |
| `Purrodoro/PremiumUpsellModels.swift` | Reason / Trigger / AccessRequest |
| `Purrodoro/StoreKitManager.swift` | StoreKit 2 manager (`ObservableObject`) |
| `Purrodoro/PremiumView.swift` | Discovery screen |
| `Purrodoro/PremiumUpsellSheet.swift` | Cap / generic upsell |
| `Purrodoro/PremiumLoginRequiredSheet.swift` | Guest → login for discovery |
| `Purrodoro/PremiumCapHeader.swift` | `n/10` label |
| `Purrodoro/Purrodoro.storekit` | Local StoreKit config (simulator) |
| `PurrodoroTests/PremiumTests.swift` | Unit tests |
| Modify `PreferencesStore.swift` | Premium cache keys + apply override from sync |
| Modify `ProfileView.swift` | Live premium entry + chrome |
| Modify `TasksView.swift` / `HabitsView.swift` / `StartFocusSheet.swift` | Cap gates |
| Modify `PurrodoroApp.swift` / `RootView.swift` | Inject manager, sheets, refresh hooks |
| Modify `Localization.swift` | EN/ID premium strings |
| Modify sync profile apply path | Read override flags into prefs |

---

### Task 1: Pure premium logic + tests (TDD)

**Files:**
- Create: `purrodoro-ios/Purrodoro/PremiumGate.swift`
- Create: `purrodoro-ios/Purrodoro/PremiumResolver.swift`
- Create: `purrodoro-ios/Purrodoro/PremiumEntitlement.swift`
- Create: `purrodoro-ios/Purrodoro/PremiumPlan.swift`
- Create: `purrodoro-ios/Purrodoro/StoreKitConfig.swift`
- Create: `purrodoro-ios/Purrodoro/PremiumUpsellModels.swift`
- Create: `purrodoro-ios/Purrodoro/SubscriptionOffers.swift`
- Create: `purrodoro-ios/PurrodoroTests/PremiumTests.swift`

- [ ] **Step 1: Write failing tests**

```swift
import Testing
@testable import Purrodoro

struct PremiumGateTests {
    @Test func freeUserBlockedAtTenActiveTasks() {
        #expect(PremiumGate.canAddActiveTask(activeCount: 9, isPremium: false))
        #expect(!PremiumGate.canAddActiveTask(activeCount: 10, isPremium: false))
    }
    @Test func premiumBypassesTaskCap() {
        #expect(PremiumGate.canAddActiveTask(activeCount: 50, isPremium: true))
    }
    @Test func freeUserBlockedAtTenHabits() {
        #expect(PremiumGate.canAddHabit(habitCount: 9, isPremium: false))
        #expect(!PremiumGate.canAddHabit(habitCount: 10, isPremium: false))
    }
    @Test func activeTaskCountIgnoresCompleted() {
        let tasks = [
            TaskItem(title: "A", completed: false),
            TaskItem(title: "B", completed: true),
            TaskItem(title: "C", completed: false),
        ]
        #expect(PremiumGate.activeTaskCount(tasks) == 2)
    }
}

struct PremiumResolverTests {
    @Test func overrideEnabledUsesOverrideActive() {
        #expect(PremiumResolver.resolve(overrideEnabled: true, overrideActive: true, storePremium: false))
        #expect(!PremiumResolver.resolve(overrideEnabled: true, overrideActive: false, storePremium: true))
    }
    @Test func overrideDisabledUsesStorePremium() {
        #expect(PremiumResolver.resolve(overrideEnabled: false, overrideActive: false, storePremium: true))
        #expect(!PremiumResolver.resolve(overrideEnabled: false, overrideActive: true, storePremium: false))
    }
}

struct PremiumEntitlementTests {
    @Test func grantsWhenProductIdMatches() {
        #expect(PremiumEntitlement.grantsPremium(
            entitledProductIds: ["purrodoro-premium-monthly"],
            knownProductIds: StoreKitConfig.allProductIds
        ))
        #expect(!PremiumEntitlement.grantsPremium(
            entitledProductIds: ["other"],
            knownProductIds: StoreKitConfig.allProductIds
        ))
    }
}
```

- [ ] **Step 2: Run tests — expect FAIL** (types missing)

```bash
cd purrodoro-ios && xcodebuild test -scheme Purrodoro \
  -destination 'platform=iOS Simulator,name=iPhone 17' \
  -only-testing:PurrodoroTests/PremiumGateTests
```

- [ ] **Step 3: Implement pure types**

`StoreKitConfig.swift`:
```swift
enum StoreKitConfig {
    static let subscriptionGroupRef = "purrodoro_premium"
    static let monthlyProductId = "purrodoro-premium-monthly"
    static let yearlyProductId = "purrodoro-premium-yearly"
    static var allProductIds: [String] { [monthlyProductId, yearlyProductId] }
}
```

`PremiumPlan.swift`:
```swift
enum PremiumPlan: String, CaseIterable, Identifiable {
    case monthly, yearly
    var id: String { rawValue }
    var productId: String {
        switch self {
        case .monthly: StoreKitConfig.monthlyProductId
        case .yearly: StoreKitConfig.yearlyProductId
        }
    }
}
```

`PremiumGate.swift`:
```swift
enum PremiumGate {
    static let freeActiveTaskLimit = 10
    static let freeHabitLimit = 10
    static func activeTaskCount(_ tasks: [TaskItem]) -> Int { tasks.filter { !$0.completed }.count }
    static func canAddActiveTask(activeCount: Int, isPremium: Bool) -> Bool {
        isPremium || activeCount < freeActiveTaskLimit
    }
    static func canAddHabit(habitCount: Int, isPremium: Bool) -> Bool {
        isPremium || habitCount < freeHabitLimit
    }
}
```

`PremiumResolver.swift`:
```swift
enum PremiumResolver {
    static func resolve(overrideEnabled: Bool, overrideActive: Bool, storePremium: Bool) -> Bool {
        overrideEnabled ? overrideActive : storePremium
    }
}
```

`PremiumEntitlement.swift`:
```swift
enum PremiumEntitlement {
    static func grantsPremium(entitledProductIds: some Collection<String>, knownProductIds: [String]) -> Bool {
        entitledProductIds.contains { knownProductIds.contains($0) }
    }
}
```

`PremiumUpsellModels.swift` — port Android sealed reasons:
```swift
enum PremiumUpsellReason: Equatable {
    case lockedSound(String)
    case lockedScene(String)
    case activeTaskCap
    case habitCap
    case generic
}
struct PremiumUpsellTrigger: Equatable {
    var reason: PremiumUpsellReason = .generic
    static func lockedSound(_ name: String) -> Self { .init(reason: .lockedSound(name)) }
    static func lockedScene(_ name: String) -> Self { .init(reason: .lockedScene(name)) }
    static let activeTaskCap = Self(reason: .activeTaskCap)
    static let habitCap = Self(reason: .habitCap)
}
enum PremiumAccessRequest: Equatable {
    case discovery
    case upsell(PremiumUpsellTrigger)
}
```

`SubscriptionOffers.swift`:
```swift
enum BillingCatalogState: Equatable { case idle, loading, ready, failed }

struct PlanOfferDetails: Equatable {
    var priceLine: String? = nil
    var secondaryPriceLine: String? = nil
    var hasTrial: Bool = false
    var hasOffer: Bool = false
}

struct SubscriptionOffers: Equatable {
    var catalogState: BillingCatalogState = .idle
    var monthly = PlanOfferDetails()
    var yearly = PlanOfferDetails()
    var savingsPercent: Int? = nil
    var isReady: Bool { catalogState == .ready }
    func forPlan(_ plan: PremiumPlan) -> PlanOfferDetails {
        switch plan { case .monthly: monthly; case .yearly: yearly }
    }
}
```

- [ ] **Step 4: Run tests — expect PASS**

```bash
cd purrodoro-ios && xcodebuild test -scheme Purrodoro \
  -destination 'platform=iOS Simulator,name=iPhone 17' \
  -only-testing:PurrodoroTests/PremiumGateTests \
  -only-testing:PurrodoroTests/PremiumResolverTests \
  -only-testing:PurrodoroTests/PremiumEntitlementTests
```

- [ ] **Step 5: Commit**

```bash
git add purrodoro-ios/Purrodoro/StoreKitConfig.swift \
  purrodoro-ios/Purrodoro/PremiumPlan.swift \
  purrodoro-ios/Purrodoro/PremiumGate.swift \
  purrodoro-ios/Purrodoro/PremiumResolver.swift \
  purrodoro-ios/Purrodoro/PremiumEntitlement.swift \
  purrodoro-ios/Purrodoro/PremiumUpsellModels.swift \
  purrodoro-ios/Purrodoro/SubscriptionOffers.swift \
  purrodoro-ios/PurrodoroTests/PremiumTests.swift
git commit -m "feat(ios): add PremiumGate, resolver, and entitlement helpers"
```

---

### Task 2: Preferences premium cache + PremiumOverride + sync override apply

**Files:**
- Modify: `purrodoro-ios/Purrodoro/PreferencesStore.swift`
- Create: `purrodoro-ios/Purrodoro/PremiumOverride.swift`
- Modify sync profile pull to call `prefs.applyPremiumOverride(enabled:active:)` (find `ProfileTableSync` / `applySyncedProfile`)

- [ ] **Step 1: Add to PreferencesStore**

```swift
@Published private(set) var storePremium: Bool
@Published private(set) var premiumOverrideEnabled: Bool
@Published private(set) var premiumOverrideActive: Bool

var isPremium: Bool {
    PremiumOverride.overrideValue()
        ?? PremiumResolver.resolve(
            overrideEnabled: premiumOverrideEnabled,
            overrideActive: premiumOverrideActive,
            storePremium: storePremium
        )
}

func setStorePremium(_ value: Bool) {
    storePremium = value
    store.set(value, forKey: storePremiumKey)
}

func applyPremiumOverride(enabled: Bool, active: Bool) {
    premiumOverrideEnabled = enabled
    premiumOverrideActive = active
    store.set(enabled, forKey: overrideEnabledKey)
    store.set(active, forKey: overrideActiveKey)
}
```

Keys: `purrodoro.storePremium`, `purrodoro.premiumOverrideEnabled`, `purrodoro.premiumOverrideActive`.

- [ ] **Step 2: PremiumOverride (DEBUG)**

```swift
#if DEBUG
enum PremiumOverride {
    static var forcedValue: Bool?
    static func overrideValue() -> Bool? { forcedValue }
}
#else
enum PremiumOverride {
    static func overrideValue() -> Bool? { nil }
}
#endif
```

- [ ] **Step 3: Wire profile sync pull** so override flags from DTO update prefs (today push hardcodes `false` — pull path must read real DTO fields into `applyPremiumOverride`).

- [ ] **Step 4: Unit test prefs resolve path with InMemoryKeyValueStore if easy; else skip to Task 3.**

- [ ] **Step 5: Commit**

```bash
git commit -m "feat(ios): cache store premium and admin override flags"
```

---

### Task 3: StoreKitManager + .storekit config

**Files:**
- Create: `purrodoro-ios/Purrodoro/StoreKitManager.swift`
- Create: `purrodoro-ios/Purrodoro/Purrodoro.storekit` (Xcode StoreKit Configuration: monthly + yearly products with ids from `StoreKitConfig`)
- Modify: `purrodoro-ios/Purrodoro/PurrodoroApp.swift` — create manager, refresh on launch / scene phase / sign-in

- [ ] **Step 1: Implement StoreKitManager**

`ObservableObject` with:
- `@Published var offers: SubscriptionOffers`
- `@Published var purchaseInFlight: Bool`
- `@Published var lastError: String?` (optional)
- `func refreshCatalog() async`
- `func refreshEntitlements() async` → `prefs.setStorePremium(...)`
- `func purchase(_ plan: PremiumPlan) async`
- `func restore() async`
- Listen `Transaction.updates` in `Task` started from `start()`
- Map `Product` displayPrice → `PlanOfferDetails.priceLine`; detect introductory offer on yearly → `hasTrial`

- [ ] **Step 2: Add Purrodoro.storekit** with subscription group containing monthly + yearly auto-renewables (`purrodoro-premium-monthly`, `purrodoro-premium-yearly`). Document in scheme: Edit Scheme → Run → Options → StoreKit Configuration.

- [ ] **Step 3: Wire PurrodoroApp** — `@StateObject` / owned `StoreKitManager`, pass via environment; `scenePhase == .active` → refresh entitlements; on `AuthStore` signed-in change → refresh.

- [ ] **Step 4: Build**

```bash
cd purrodoro-ios && xcodebuild -scheme Purrodoro \
  -destination 'platform=iOS Simulator,name=iPhone 17' build
```

Expected: BUILD SUCCEEDED

- [ ] **Step 5: Commit**

```bash
git commit -m "feat(ios): add StoreKitManager and local StoreKit config"
```

---

### Task 4: Localization strings (EN + ID)

**Files:**
- Modify: `purrodoro-ios/Purrodoro/Localization.swift`

- [ ] **Step 1: Add keys** (mirror Android premium / free-tier strings):  
  `premium.screen.*`, `premium.sheet.*`, `premium.cta.*`, `premium.restore.*`, `premium.error.*`, `premium.login.*`, `free_tier.tasks`, `free_tier.habits`, `free_tier.cap_cd`, compare perk lines, plan labels monthly/yearly, maybe later, manage subscription.

- [ ] **Step 2: Commit**

```bash
git commit -m "feat(ios): add premium EN/ID localization strings"
```

---

### Task 5: Premium UI — View, Upsell, Login gate, Cap header

**Files:**
- Create: `PremiumView.swift`, `PremiumUpsellSheet.swift`, `PremiumLoginRequiredSheet.swift`, `PremiumCapHeader.swift`
- Port layout/copy structure from Android `PremiumScreen.kt`, `PremiumUpsellSheet.kt`, `PremiumLoginRequiredSheet.kt`, `FreeTierCapHeaderLabel`

- [ ] **Step 1: PremiumCapHeader** — `visible`, `current`, `limit`, label from loc; at-cap locked tint.

- [ ] **Step 2: PremiumLoginRequiredSheet** — title/body + Sign in button callback.

- [ ] **Step 3: PremiumUpsellSheet** — reason-driven title/body; plan picker; purchase CTA; guest → sign-in content; catalog retry.

- [ ] **Step 4: PremiumView** — header, compare card, plan picker, subscribe / restore / manage; uses `StoreKitManager` + `prefs.isPremium`.

- [ ] **Step 5: Build + commit**

```bash
git commit -m "feat(ios): add Premium discovery, upsell, and cap header UI"
```

---

### Task 6: Wire Profile + Tasks + Habits + Start Focus

**Files:**
- Modify: `ProfileView.swift` — replace stub banner; guest → login gate; signed-in → PremiumView sheet; identity chrome when premium
- Modify: `TasksView.swift` — cap header; + blocked → upsell
- Modify: `HabitsView.swift` — same
- Modify: `StartFocusSheet.swift` — create task/habit respects gate
- Modify: `RootView.swift` or `PurrodoroApp.swift` — host upsell sheet + pending `PremiumAccessRequest` after login

- [ ] **Step 1: Profile premium entry + sheets**

- [ ] **Step 2: Cap gates on add affordances**

```swift
if !PremiumGate.canAddActiveTask(activeCount: PremiumGate.activeTaskCount(tasks.tasks), isPremium: prefs.isPremium) {
    pendingUpsell = .activeTaskCap
    return
}
tasks.add(...)
```

- [ ] **Step 3: After guest signs in from premium gate, present pending discovery/upsell**

- [ ] **Step 4: Build + manual smoke on simulator**

- [ ] **Step 5: Commit**

```bash
git commit -m "feat(ios): wire premium paywall and free-tier caps into Profile and lists"
```

---

### Task 7: STATUS + full test run

**Files:**
- Modify: `purrodoro-ios/STATUS.md` — StoreKit row → 🔶 or ✅ for foundation; caps ✅

- [ ] **Step 1: Run full unit tests**

```bash
cd purrodoro-ios && xcodebuild test -scheme Purrodoro \
  -destination 'platform=iOS Simulator,name=iPhone 17' \
  -only-testing:PurrodoroTests
```

Expected: all pass

- [ ] **Step 2: Update STATUS.md parity matrix**

- [ ] **Step 3: Commit**

```bash
git commit -m "docs(ios): mark StoreKit premium foundation in STATUS"
```

---

## Spec coverage checklist

| Spec requirement | Task |
|------------------|------|
| StoreKitConfig IDs + two products | 1, 3 |
| PremiumGate 10/10 | 1, 6 |
| PremiumResolver + DEBUG override | 1, 2 |
| Entitlement refresh triggers | 3 |
| Device-local storePremium cache | 2, 3 |
| Sync override pull | 2 |
| PremiumView discovery | 5, 6 |
| Guest login gate | 5, 6 |
| Upsell caps + scaffold sound/scene reasons | 1, 5, 6 |
| Cap header UI | 5, 6 |
| EN/ID strings | 4 |
| .storekit simulator | 3 |
| Unit tests gate/resolver/entitlement | 1 |
| Soundscape/scenes UI | out of scope |

---

## Execution

User asked to **implement**. Use **subagent-driven-development** (or inline executing-plans). Prefer a feature branch off `main` before coding.
