# Play Store Screenshot Refresh Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Generate six finished 1080 × 1920 Play Store portraits with faithful static Android feature panels and custom Purrodoro mascot poses.

**Architecture:** Pillow composition scripts own shared campaign layout, typography, Android UI reconstruction, generated-pose extraction, and mascot placement. Final assets need no manual screenshot replacement and are validated for dimensions and image variance.

**Tech Stack:** Python 3, Pillow, existing Nunito font files and PNG/JPEG assets.

---

### Task 1: Build deterministic portrait compositor

**Files:**
- Create: `assets/playstore/generate_store_portraits_final.py`
- Create: `assets/playstore/store_portrait_1_final.png`
- Create: `assets/playstore/store_portrait_2_final.png`
- Create: `assets/playstore/store_portrait_3_final.png`
- Create: `assets/playstore/store_portrait_4_final.png`
- Create: `assets/playstore/store_portrait_5_final.png`
- Create: `assets/playstore/store_portrait_6_final.png`

- [ ] **Step 1: Install Pillow**

Run:

```bash
python3 -m pip install --user pillow
```

Expected: `Successfully installed pillow-<version>`.

- [ ] **Step 2: Implement shared composition primitives**

Create helpers for:

```python
CANVAS = (1080, 1920)

def cover_crop(image, size): ...
def contain(image, size): ...
def make_feature_ui(size): ...
def draw_heading(draw, text, xy, width, font, fill): ...
```

Use app-bundled Nunito fonts from
`purrodoro-android/app/src/main/res/font/`. Placeholders must remain visibly
empty and labeled for later editing.

- [ ] **Step 3: Compose three approved designs**

Generate six finished images covering overview, timer, tasks, habits,
soundscapes, and Journey. Use real Android copy, production color tokens,
representative feature data, one custom mascot pose, and short product copy.

- [ ] **Step 4: Run compositor**

Run:

```bash
python3 assets/playstore/generate_store_portraits_final.py
```

Expected: prints three output paths and no traceback.

- [ ] **Step 5: Validate outputs**

Run:

```bash
python3 - <<'PY'
from pathlib import Path
from PIL import Image, ImageStat

for path in sorted(Path("assets/playstore").glob("store_portrait_*_final.png")):
    image = Image.open(path).convert("RGB")
    assert image.size == (1080, 1920), (path, image.size)
    assert sum(ImageStat.Stat(image).var) > 100, path
    print(path, image.size)
PY
```

Expected: all six files print with `(1080, 1920)`.

- [ ] **Step 6: Review generated images visually**

Open each output and confirm:

- headline readable at thumbnail size;
- real UI remains sharp and dominant;
- mascot is consistent and subordinate;
- no accidental white extraction halos;
- no clipped text or misleading feature claim.
