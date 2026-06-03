# Todo Feature Improvements — Design Spec
**Date:** 2026-05-15  
**Status:** Approved

---

## Overview

Improve the Todo screen with a flat task model, category-based filtering, inline category management, a task detail sheet, and a completed tasks section. Two pre-existing bugs are also fixed as part of this work.

---

## Section 1: Data Model

### New `Category` model
```kotlin
data class Category(val id: Int, val name: String)
```

### Revised `FocusTask`
```kotlin
data class FocusTask(
    val id: Int,
    val title: String,
    val minutes: Int,
    val completed: Boolean = false,
    val plannedSessions: Int = 1,
    val completedSessions: Int = 0,
    val categoryId: Int? = null   // null = uncategorized; appears under "All"
)
```

### Flat task list
Tasks are no longer grouped inside `TodoListState` containers. The model becomes:
- `tasks: List<FocusTask>` — single flat list of all tasks
- `categories: List<Category>` — available category labels
- `selectedCategoryId: Int?` — `null` means "All" is selected

Categories are optional tags on tasks. Deleting a category sets affected tasks' `categoryId` to `null` (uncategorized). No tasks are ever deleted when a category is deleted.

### DataStore changes
| Key | Change |
|-----|--------|
| `KEY_TODOS` | **Removed** — replaced by `KEY_TASKS` |
| `KEY_TASKS` | **New** — serialized flat `List<FocusTask>` |
| `KEY_CATEGORIES` | **New** — serialized `List<Category>` |
| `KEY_NEXT_TASK_ID` | **Kept** — unchanged |
| `KEY_NEXT_CATEGORY_ID` | **New** — auto-incrementing counter for category IDs, initialized to `4` (seeds occupy 1–3) |
| `KEY_SELECTED_LIST` | **Removed** — replaced by `KEY_SELECTED_CATEGORY_ID` |
| `KEY_SELECTED_CATEGORY_ID` | **New** — persists selected category id; `null` = All |

### Seed data migration
The existing seed data (Study, Home, Ideas lists) becomes three seed `Category` objects:
- `Category(1, "Study")`
- `Category(2, "Home")`
- `Category(3, "Ideas")`

Existing seed tasks are migrated to the flat task list with their `categoryId` set accordingly. `KEY_NEXT_CATEGORY_ID` is initialized to `4`.

### AppRepository new operations
- `setTasks(tasks: List<FocusTask>)`
- `setCategories(categories: List<Category>)`
- `setNextCategoryId(id: Int)`
- `setSelectedCategoryId(id: Int?)`
- `addCategory(name: String)` — creates category with `nextCategoryId`, increments counter
- `renameCategory(id: Int, name: String)`
- `deleteCategory(id: Int)` — nulls `categoryId` on affected tasks, removes category

---

## Section 2: UI / Interaction Design

### Filter pill row
- First pill is always **"All"** — non-deletable, non-editable
- Remaining pills are one per `Category`
- Selecting a pill filters the task list to tasks with that `categoryId`
- Uncategorized tasks (`categoryId = null`) appear only under "All"
- An **"Edit"** text button sits in the header row next to the date label

### Edit mode (Apple-style inline)
- Tapping "Edit" → pill row enters edit mode; "Edit" button becomes "Done"
- Each category pill gains a `×` badge in the top-left corner
- A `+ New` pill appears at the end of the scrollable row
- **Tapping `×`** → confirmation bottom sheet:
  > "Delete [Name]? Tasks in this category will become uncategorized."
  > [Cancel] [Delete]
  - On confirm: category deleted, affected tasks become uncategorized
- **Tapping `+ New`** → bottom sheet with a text field; confirm creates the category
- **Tapping a pill name** in edit mode → bottom sheet with text field pre-filled for rename
- Tapping "Done" exits edit mode

### Adding a task
- `+` button always visible regardless of active filter tab
- Opens inline input row (same as current implementation)
- `categoryId` is pre-filled with the currently selected category if one is active; otherwise `null`
- Optional category selector chips shown below the text input when adding, allowing override. The chips are always rendered but selecting one is optional — the task can be saved without a category.

### Task row — interaction changes
| Interaction | Action |
|---|---|
| Tap circle checkmark | Toggle done / undone |
| Tap anywhere else on row | Open `TaskDetailSheet` |
| Swipe right | "Focus with Purrodoro" → `PreSessionSheet` (unchanged) |
| Swipe left | Delete task (unchanged) |

- In "All" view: show category name as a dim subtitle below task title (blank if uncategorized)

### TaskDetailSheet (ModalBottomSheet)
Fields:
- **Title** — editable text field
- **Category** — chip/dropdown selector showing all categories + "None"
- **Minutes** — stepper (same style as session stepper in `PreSessionSheet`)
- **Planned sessions** — stepper (1–8)
- **Completed sessions** — read-only display: "2 of 4 sessions done"
- **"Focus with Purrodoro"** button at the bottom → same flow as swipe-right

Behaviour:
- Changes auto-save on sheet dismiss (no explicit Save button)
- Sheet follows existing `ModalBottomSheet` + `rememberModalBottomSheetState` pattern

### Completed tasks section
- Rendered below the active task list within the same scrollable column
- Section header: **"COMPLETED (N)"** — collapsible, collapsed by default
- Expanding reveals completed tasks in same `TaskGlassRow` style but visually dimmed
- **"Delete All Completed"** destructive button appears at the bottom of the expanded section
  - Scoped to the current filter view (deletes completed tasks in current category, or all completed if "All" is selected)
- Tapping circle on a completed task → marks it active again (moves back up to active section)

---

## Section 3: Bug Fixes

### Bug 1 — `completedSessions` never increments
**Location:** `TimerStateMachine.applyTimerComplete`
**Fix:** After a Focus segment completes, if `activeTask != null`, increment `task.completedSessions` by 1 and persist the updated task list via `AppRepository`.

### Bug 2 — `addTask` hardcodes `minutes = 25`
**Location:** `PurrodoroApp.kt` `addTask` lambda
**Fix:** Replace `FocusTask(nextTaskId, title, 25)` with `FocusTask(nextTaskId, title, settings.focusMinutes)`.

---

## Out of Scope
- Task reordering (drag-and-drop)
- Due dates or priority levels
- Task notes/description field
- Cross-device sync
