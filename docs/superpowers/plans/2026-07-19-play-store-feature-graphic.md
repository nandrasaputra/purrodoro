# Play Store Feature Graphic Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Generate a polished 1024×500 illustration-only Play Store feature graphic matching Purrodoro's portrait campaign.

**Architecture:** Extend the restored no-UI image generator with one focused builder function. Reuse its colors, fonts, mascot placement, symbols, and output validation rather than creating another generator.

**Tech Stack:** Python 3, Pillow, existing Android Nunito fonts and mascot assets.

---

### Task 1: Build and export the feature graphic

**Files:**
- Modify: `assets/playstore/generate_store_portraits_no_ui.py`
- Create: `assets/playstore/feature_graphic.png`

- [ ] **Step 1: Add a feature-graphic canvas constant**

Add beside `CANVAS`:

```python
FEATURE_CANVAS = (1024, 500)
```

- [ ] **Step 2: Add the feature graphic builder**

Add before `save()`:

```python
def build_feature_graphic() -> Image.Image:
    canvas = Image.new("RGBA", FEATURE_CANVAS, CREAM)
    draw = ImageDraw.Draw(canvas)

    draw.ellipse((650, -210, 1150, 410), fill=PEACH)
    draw.ellipse((820, 280, 1120, 600), fill=LAVENDER)
    draw.ellipse((670, 64, 950, 344), outline=CORAL, width=12)

    draw.ellipse((64, 64, 88, 88), fill=CORAL)
    draw.text(
        (102, 76),
        "PURRODORO",
        font=f("extrabold", 24),
        fill=BROWN,
        anchor="lm",
    )
    draw.multiline_text(
        (64, 126),
        "Plan. Focus.\nFeel progress.",
        font=f("extrabold", 58),
        fill=BROWN,
        spacing=-5,
    )
    draw.text(
        (66, 342),
        "TIMER  ·  TASKS  ·  HABITS  ·  SOUNDS",
        font=f("extrabold", 19),
        fill=MUTED,
    )

    final.place_pose(
        canvas,
        "purrodoro_pose_overview.png",
        (620, 60, 330, 376),
        fallback="cat_notify_hero.webp",
    )
    final.draw_check(draw, (905, 92), size=38, fill=CORAL)
    waveform(draw, center=(596, 410), color=CORAL, scale=0.45)
    star(draw, (610, 104), 15, fill=GOLD)
    star(draw, (940, 368), 13, fill=PURPLE)
    return canvas
```

- [ ] **Step 3: Generalize output validation**

Change `save()` so the expected dimensions can be supplied:

```python
def save(
    image: Image.Image,
    filename: str,
    *,
    expected_size: tuple[int, int] = CANVAS,
) -> Path:
    path = OUTPUT_DIR / filename
    rgb = image.convert("RGB")
    assert rgb.size == expected_size
    assert sum(ImageStat.Stat(rgb).var) > 100
    rgb.save(path, "PNG", optimize=True)
    print(path.relative_to(ROOT))
    return path
```

- [ ] **Step 4: Export the feature graphic from `main()`**

Keep the six portrait loop, then append:

```python
    save(
        build_feature_graphic(),
        "feature_graphic.png",
        expected_size=FEATURE_CANVAS,
    )
```

- [ ] **Step 5: Run the generator**

Run:

```bash
/tmp/purrodoro-playstore-venv/bin/python assets/playstore/generate_store_portraits_no_ui.py
```

Expected: seven output paths, ending with
`assets/playstore/feature_graphic.png`.

- [ ] **Step 6: Validate output**

Run:

```bash
/tmp/purrodoro-playstore-venv/bin/python -c "from PIL import Image,ImageStat; p='assets/playstore/feature_graphic.png'; im=Image.open(p); assert im.size==(1024,500); assert im.mode=='RGB'; assert sum(ImageStat.Stat(im).var)>100; print(p, im.size, im.mode)"
```

Expected:

```text
assets/playstore/feature_graphic.png (1024, 500) RGB
```

- [ ] **Step 7: Visually inspect**

Confirm:

- Headline remains readable at reduced size.
- No critical text or mascot detail crosses the 64-pixel safe area.
- Mascot is not clipped.
- Feature symbols stay secondary to headline and mascot.
- Composition matches the approved cream, peach, lavender, coral, and cocoa design.
