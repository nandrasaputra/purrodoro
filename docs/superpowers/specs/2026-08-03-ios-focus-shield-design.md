# Focus Shield on iOS (Screen Time / FamilyControls)

_Design for the Android Focus Shield port. Product rules stay the same; APIs do not._

**Android reference:** `purrodoro-android/.../focusshield/*`, `FocusShieldScreen.kt`  
**Status:** Phase 1 (UI + prefs + pure rules + ManagedSettings apply/clear) ships in app code. Distribution still needs the Family Controls entitlement approved in App Store Connect / the Apple Developer portal.

---

## Product (parity)

While a **Focus** segment is playing or paused, Purrodoro can gently keep the user out of selected distraction apps and invite them back to the session.

| Rule | Android | iOS |
|------|---------|-----|
| Pref on | DataStore `focus_shield_enabled` | `PreferencesStore` / `FocusShieldStore.enabled` |
| Selection nonempty | Package name set | `FamilyActivitySelection` (apps / categories / web domains) |
| Permission | Usage Access + Overlay | Screen Time via `AuthorizationCenter` (`.individual`) |
| Armed only on Focus | Yes (not breaks / idle) | Same (`FocusShield.isArmed`) |
| Interrupt UX | Soft Compose overlay | System **ManagedSettings** shield (harder OS block; softer copy in settings) |
| Bypass | Per-package until segment end | Clear shield until segment end / bypass window expires |
| Device-local | Not in backup/sync | Same — do not sync selection tokens |

Tone: invite back, not punish. Settings live under **Profile → Timer** and a compact row on **Start Focus**.

---

## Why not a 1:1 port

- No UsageStats foreground polling and no `SYSTEM_ALERT_WINDOW` overlays on iOS.
- No launcher enumeration of third-party package names — use `FamilyActivityPicker`.
- Do **not** use Accessibility APIs for blocking (Android also avoided this).

---

## Architecture

```
FocusShield.isArmed(...)          pure rules (unit-tested)
FocusShieldStore                  prefs, selection, auth status, apply/clear
FocusShieldView                   Profile settings + FamilyActivityPicker
StartFocusSheet row               enable + deep link to settings
TimerEngine observation           arm/disarm ManagedSettingsStore on segment change
```

`ManagedSettingsStore(named: .focusShield)` is written only while armed; cleared on break, idle, stop, pref off, or user bypass.

Optional later: `ShieldConfiguration` / `ShieldAction` app extensions for branded shield chrome and an explicit “Open anyway” button. Phase 1 uses the system default shield UI.

---

## Entitlement

Add `com.apple.developer.family-controls` to `Purrodoro.entitlements` and enable the **Family Controls** capability on App ID `com.endiar.Purrodoro`. Request distribution approval from Apple before App Store release; development builds can exercise the authorization sheet once the capability is on the provisioning profile.

---

## Phases

1. **Shipped in this slice:** pure `isArmed`, settings UI, Start Focus row, selection persistence, authorization request, ManagedSettings apply/clear wired to the timer.
2. **Follow-up:** ShieldConfiguration/Action extensions, DeviceActivityMonitor if background re-apply is needed, App Store entitlement approval, device QA matrix.
