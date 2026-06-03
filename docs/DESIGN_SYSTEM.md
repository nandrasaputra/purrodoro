# Purrodoro Design System

## Rules

These rules apply to all screen files (`ui/**Screen.kt`) and any new UI code.

### Colors
- No `Color(0xFF...)` literals in screen files.
- All colors via `KuroTokens.current`, `MaterialTheme.colorScheme`, or named constants from `ui/theme/Color.kt`.
- No private `val Reference*`, `val Home*`, or similar per-file color palettes.

### Typography
- No `fontSize = N.sp` or `fontWeight = FontWeight.X` literals in screen files.
- Use `KuroType.<role>` (e.g. `KuroType.bodyPrimary`) or `MaterialTheme.typography.<role>`.

### Spacing
- No raw `N.dp` values in screen-level layout. Use `KuroSpacing.<name>`.
- Raw `dp` values are allowed inside component-internal layout (inside `ui/designsystem/`).

### Glass tokens
- `glassTokens()` and `GlassCard` are only used in the **Dashboard pattern** (Home/Timer tab).
- All other screens use flat surface tokens via `KuroTokens.current`.

### New components
- New reusable Composables go in `ui/designsystem/` before being used in screens.
- Atoms → `KuroAtoms.kt`, Molecules → `KuroMolecules.kt`, Organisms → `KuroOrganisms.kt`.

## Package Map

| Package | Contents |
|---|---|
| `ui/designsystem/` | `KuroSpacing`, `KuroType`, `KuroAtoms`, `KuroMolecules`, `KuroOrganisms` |
| `ui/components/` | `KuroComponents.kt` (existing), `FocusModeMotion.kt` |
| `ui/theme/` | `Color.kt`, `SurfaceTokens.kt`, `Theme.kt` — do not add screen-specific values here |
| `src/debug/` | `ComponentCatalogScreen.kt` — debug only, never shipped |

## Screen Patterns

See `docs/superpowers/specs/2026-05-24-design-system-design.md` for the four named patterns:
**List Screen**, **Settings Screen**, **Detail Bottom Sheet**, **Dashboard Screen**.
