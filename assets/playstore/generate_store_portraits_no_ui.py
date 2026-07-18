#!/usr/bin/env python3
"""Generate six illustration-only Purrodoro Play Store portraits."""

from pathlib import Path

from PIL import Image, ImageDraw, ImageStat

import generate_store_portraits_final as final


ROOT = Path(__file__).resolve().parents[2]
OUTPUT_DIR = ROOT / "assets" / "playstore"
CANVAS = (1080, 1920)
FEATURE_CANVAS = (1024, 500)

CREAM = "#FFF8F2"
PAPER = "#FFFCF8"
BROWN = "#4A3128"
MUTED = "#80645A"
CORAL = "#FF7464"
PEACH = "#FFD8C7"
LAVENDER = "#E8DDFB"
PURPLE = "#7956A7"
PLUM = "#2D243A"
PLUM_2 = "#493757"
WHITE = "#FFFFFF"
GOLD = "#F6C75B"


def f(name: str, size: int):
    return final.f(name, size)


def campaign_header(
    canvas: Image.Image,
    *,
    index: int,
    headline_text: str,
    subtitle_text: str,
    dark: bool = False,
    headline_size: int = 101,
) -> ImageDraw.ImageDraw:
    draw = ImageDraw.Draw(canvas)
    primary = "#FFF8F1" if dark else BROWN
    secondary = "#D8CBE3" if dark else MUTED
    badge_fill = PLUM_2 if dark else WHITE
    draw.ellipse((68, 70, 100, 102), fill=CORAL)
    draw.text(
        (120, 86),
        "PURRODORO",
        font=f("extrabold", 30),
        fill=primary,
        anchor="lm",
    )
    draw.rounded_rectangle(
        (850, 58, 1012, 116),
        radius=29,
        fill=badge_fill,
    )
    draw.text(
        (931, 87),
        f"{index:02d} / 06",
        font=f("bold", 25),
        fill=primary,
        anchor="mm",
    )
    draw.multiline_text(
        (68, 180),
        headline_text,
        font=f("extrabold", headline_size),
        fill=primary,
        spacing=-8,
    )
    draw.multiline_text(
        (72, 420),
        subtitle_text,
        font=f("semibold", 40),
        fill=secondary,
        spacing=7,
    )
    return draw


def feature_pill(
    draw: ImageDraw.ImageDraw,
    box: tuple[int, int, int, int],
    text: str,
    *,
    fill: str,
    text_fill: str,
    outline: str | None = None,
    text_size: int = 25,
) -> None:
    x, y, width, height = box
    draw.rounded_rectangle(
        (x, y, x + width, y + height),
        radius=height // 2,
        fill=fill,
        outline=outline,
        width=4 if outline else 0,
    )
    draw.text(
        (x + width / 2, y + height / 2),
        text,
        font=f("extrabold", text_size),
        fill=text_fill,
        anchor="mm",
    )


def star(
    draw: ImageDraw.ImageDraw,
    center: tuple[int, int],
    radius: int,
    *,
    fill: str,
) -> None:
    x, y = center
    points = (
        (x, y - radius),
        (x + radius * 0.28, y - radius * 0.28),
        (x + radius, y),
        (x + radius * 0.28, y + radius * 0.28),
        (x, y + radius),
        (x - radius * 0.28, y + radius * 0.28),
        (x - radius, y),
        (x - radius * 0.28, y - radius * 0.28),
    )
    draw.polygon(points, fill=fill)


def check_card(
    draw: ImageDraw.ImageDraw,
    box: tuple[int, int, int, int],
    *,
    label: str,
    active: bool,
) -> None:
    x, y, width, height = box
    draw.rounded_rectangle(
        (x + 10, y + 14, x + width + 10, y + height + 14),
        radius=34,
        fill=(74, 49, 40, 25),
    )
    draw.rounded_rectangle(
        (x, y, x + width, y + height),
        radius=34,
        fill=WHITE,
        outline="#F0B6A8",
        width=4,
    )
    if active:
        final.draw_check(draw, (x + 58, y + height // 2), size=42, fill=CORAL)
    else:
        draw.ellipse(
            (
                x + 37,
                y + height // 2 - 21,
                x + 79,
                y + height // 2 + 21,
            ),
            outline=CORAL,
            width=4,
        )
    draw.text(
        (x + 104, y + height / 2),
        label,
        font=f("bold", 28),
        fill=BROWN,
        anchor="lm",
    )


def waveform(
    draw: ImageDraw.ImageDraw,
    *,
    center: tuple[int, int],
    color: str,
    scale: float = 1.0,
) -> None:
    x, y = center
    heights = (46, 84, 128, 74, 106, 54, 88, 42)
    for index, height in enumerate(heights):
        height = round(height * scale)
        bar_x = x + index * round(34 * scale)
        width = max(8, round(14 * scale))
        draw.rounded_rectangle(
            (bar_x, y - height // 2, bar_x + width, y + height // 2),
            radius=width // 2,
            fill=color,
        )


def build_overview() -> Image.Image:
    canvas = Image.new("RGBA", CANVAS, CREAM)
    draw = campaign_header(
        canvas,
        index=1,
        headline_text="Plan. Focus.\nFeel progress.",
        subtitle_text="Four tools. One calmer daily rhythm.",
    )
    draw.rounded_rectangle((-180, 1120, 1260, 2220), radius=480, fill=PEACH)
    draw.ellipse((730, 1280, 1270, 1820), fill=LAVENDER)

    feature_pill(draw, (74, 620, 340, 88), "FOCUS TIMER", fill=CORAL, text_fill=WHITE)
    feature_pill(
        draw,
        (470, 620, 250, 88),
        "TASKS",
        fill=WHITE,
        text_fill=BROWN,
        outline="#F1B8AA",
    )
    feature_pill(
        draw,
        (758, 620, 250, 88),
        "HABITS",
        fill=WHITE,
        text_fill=BROWN,
        outline="#F1B8AA",
    )
    feature_pill(
        draw,
        (74, 750, 310, 88),
        "CALM SOUNDS",
        fill=WHITE,
        text_fill=BROWN,
        outline="#F1B8AA",
    )

    final.place_pose(
        canvas,
        "purrodoro_pose_overview.png",
        (125, 760, 830, 940),
        fallback="cat_notify_hero.webp",
    )
    star(draw, (160, 1010), 28, fill=GOLD)
    star(draw, (880, 900), 24, fill=PURPLE)
    star(draw, (920, 1110), 18, fill=CORAL)
    draw.text(
        (72, 1780),
        "TIMER  ·  TASKS  ·  HABITS  ·  SOUNDS",
        font=f("extrabold", 28),
        fill=BROWN,
    )
    return canvas


def build_focus() -> Image.Image:
    canvas = Image.new("RGBA", CANVAS, PLUM)
    draw = campaign_header(
        canvas,
        index=2,
        headline_text="Focus without\nthe pressure.",
        subtitle_text="A quieter way to begin and keep going.",
        dark=True,
    )
    draw.ellipse((90, 630, 990, 1530), fill=PLUM_2, outline=CORAL, width=18)
    draw.arc((50, 590, 1030, 1570), 200, 338, fill="#8A6AA1", width=10)
    draw.text(
        (540, 820),
        "25:00",
        font=f("regular", 126),
        fill=WHITE,
        anchor="mm",
    )
    for index in range(4):
        x = 426 + index * 56
        fill = CORAL if index == 0 else PLUM_2
        draw.ellipse((x, 930, x + 26, 956), fill=fill, outline=CORAL, width=3)

    final.place_pose(
        canvas,
        "purrodoro_pose_focus.png",
        (160, 930, 760, 760),
        fallback="cat_focusing_1.webp",
    )
    feature_pill(
        draw,
        (140, 1650, 800, 94),
        "25 FOCUS  ·  5 BREAK  ·  REPEAT",
        fill=PEACH,
        text_fill=BROWN,
        text_size=27,
    )
    return canvas


def build_tasks() -> Image.Image:
    canvas = Image.new("RGBA", CANVAS, PAPER)
    draw = campaign_header(
        canvas,
        index=3,
        headline_text="Turn plans\ninto progress.",
        subtitle_text="Capture it. Choose it. Start.",
        headline_size=90,
    )
    draw.ellipse((-300, 1310, 530, 2170), fill=LAVENDER)
    draw.rounded_rectangle((680, -180, 1260, 720), radius=270, fill=PEACH)

    check_card(draw, (76, 650, 680, 150), label="Capture the task", active=True)
    check_card(draw, (126, 840, 760, 150), label="Choose what matters", active=False)
    check_card(draw, (176, 1030, 820, 150), label="Start one focus", active=False)

    final.place_pose(
        canvas,
        "purrodoro_pose_tasks.png",
        (330, 1110, 700, 760),
        fallback="cat_checklist.webp",
    )
    feature_pill(
        draw,
        (72, 1480, 380, 90),
        "ONE TASK AT A TIME",
        fill=CORAL,
        text_fill=WHITE,
        text_size=24,
    )
    return canvas


def build_habits() -> Image.Image:
    canvas = Image.new("RGBA", CANVAS, "#F2EBFC")
    draw = campaign_header(
        canvas,
        index=4,
        headline_text="Build habits\nthat stick.",
        subtitle_text="Tiny repetitions grow into visible momentum.",
    )
    draw.rounded_rectangle((-180, 1210, 1260, 2200), radius=460, fill=PEACH)
    draw.ellipse((720, 1300, 1260, 1840), fill=LAVENDER)

    days = ("M", "T", "W", "T", "F", "S", "S")
    for index, day in enumerate(days):
        x = 95 + index * 134
        y = 700 + index * 42
        complete = index < 5
        draw.ellipse(
            (x, y, x + 76, y + 76),
            fill=CORAL if complete else WHITE,
            outline=CORAL,
            width=4,
        )
        if complete:
            final.draw_check(draw, (x + 38, y + 38), size=46, fill=CORAL)
        draw.text(
            (x + 38, y + 110),
            day,
            font=f("extrabold", 22),
            fill=BROWN,
            anchor="mm",
        )

    final.place_pose(
        canvas,
        "purrodoro_pose_habits.png",
        (150, 1030, 790, 800),
        fallback="cat_celebrate.webp",
    )
    feature_pill(
        draw,
        (110, 1730, 530, 90),
        "SMALL WINS  ·  EVERY DAY",
        fill=WHITE,
        text_fill=BROWN,
        outline="#F1B8AA",
        text_size=24,
    )
    return canvas


def build_sounds() -> Image.Image:
    canvas = Image.new("RGBA", CANVAS, PLUM)
    draw = campaign_header(
        canvas,
        index=5,
        headline_text="Find your\nperfect sound.",
        subtitle_text="Rain, cafe ambience, and lo-fi—layered your way.",
        dark=True,
    )
    draw.ellipse((-160, 780, 1240, 2180), fill=PLUM_2)
    draw.ellipse((120, 760, 960, 1600), fill="#17131D")
    draw.ellipse((365, 1005, 715, 1355), fill=CORAL)
    draw.ellipse((515, 1155, 565, 1205), fill=PLUM)
    waveform(draw, center=(92, 1010), color=CORAL, scale=0.9)

    final.place_pose(
        canvas,
        "purrodoro_pose_sounds.png",
        (140, 830, 800, 850),
        fallback="cat_home_sleeping.webp",
    )
    feature_pill(draw, (82, 1680, 250, 84), "RAIN", fill=CORAL, text_fill=WHITE)
    feature_pill(
        draw,
        (362, 1680, 250, 84),
        "CAFE",
        fill="#5E466D",
        text_fill=WHITE,
        outline="#8A6AA1",
    )
    feature_pill(
        draw,
        (642, 1680, 250, 84),
        "LO-FI",
        fill="#5E466D",
        text_fill=WHITE,
        outline="#8A6AA1",
    )
    return canvas


def build_journey() -> Image.Image:
    canvas = Image.new("RGBA", CANVAS, CREAM)
    draw = campaign_header(
        canvas,
        index=6,
        headline_text="See progress,\nnot pressure.",
        subtitle_text="Every focused day tells your story.",
    )
    draw.ellipse((700, -230, 1260, 620), fill=PEACH)
    draw.rounded_rectangle((-180, 1320, 1260, 2200), radius=430, fill=LAVENDER)

    points = (
        (92, 1220),
        (250, 1100),
        (390, 1140),
        (560, 900),
        (720, 980),
        (930, 690),
    )
    draw.line(points, fill=CORAL, width=18, joint="curve")
    for x, y in points:
        draw.ellipse(
            (x - 18, y - 18, x + 18, y + 18),
            fill=WHITE,
            outline=CORAL,
            width=6,
        )
    draw.polygon(((930, 690), (890, 686), (920, 730)), fill=CORAL)
    star(draw, (835, 780), 32, fill=GOLD)
    star(draw, (910, 900), 24, fill=GOLD)
    star(draw, (740, 830), 20, fill=PURPLE)

    final.place_pose(
        canvas,
        "purrodoro_pose_journey.png",
        (260, 980, 760, 820),
        fallback="cat_celebrate.webp",
    )
    feature_pill(
        draw,
        (72, 750, 300, 92),
        "12 FOCUS DAYS",
        fill=WHITE,
        text_fill=BROWN,
        outline="#F1B8AA",
        text_size=23,
    )
    feature_pill(
        draw,
        (72, 870, 300, 92),
        "14 DAY STREAK",
        fill=CORAL,
        text_fill=WHITE,
        text_size=23,
    )
    return canvas


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


def main() -> None:
    outputs = (
        (build_overview(), "store_portrait_1_no_ui.png"),
        (build_focus(), "store_portrait_2_no_ui.png"),
        (build_tasks(), "store_portrait_3_no_ui.png"),
        (build_habits(), "store_portrait_4_no_ui.png"),
        (build_sounds(), "store_portrait_5_no_ui.png"),
        (build_journey(), "store_portrait_6_no_ui.png"),
    )
    for image, filename in outputs:
        save(image, filename)
    save(
        build_feature_graphic(),
        "feature_graphic.png",
        expected_size=FEATURE_CANVAS,
    )


if __name__ == "__main__":
    main()
