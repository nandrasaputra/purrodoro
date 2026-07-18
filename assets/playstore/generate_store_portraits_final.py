#!/usr/bin/env python3
"""Generate six finished Play Store portraits with faithful Purrodoro UI."""

from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont, ImageStat


ROOT = Path(__file__).resolve().parents[2]
OUTPUT_DIR = ROOT / "assets" / "playstore"
DRAWABLE_DIR = (
    ROOT
    / "purrodoro-android"
    / "app"
    / "src"
    / "main"
    / "res"
    / "drawable"
)
FONT_DIR = (
    ROOT
    / "purrodoro-android"
    / "app"
    / "src"
    / "main"
    / "res"
    / "font"
)
POSE_DIR = Path(
    "/Users/nandra.saputra/.cursor/projects/"
    "Users-nandra-saputra-Projects-nandra-purrodoro/assets"
)

CANVAS = (1080, 1920)
PAGE = "#FAF5F2"
WHITE = "#FFFFFF"
COCOA = "#5F4638"
COCOA_SOFT = "#8B6B5A"
CORAL = "#FF7F73"
DEEP_CORAL = "#C64432"
ACTIVE = "#FFE9DD"
BORDER = "#EBDFCC"
DOCK = "#EFE0CB"
DOCK_BORDER = "#DFCBAC"
MINT = "#CDEEDB"
LAVENDER = "#D8C8FF"
PURPLE = "#7653A6"
PLUM = "#2D243A"
GREEN = "#8BA05E"
SHADOW = (63, 38, 28, 35)
CAMPAIGN_CREAM = "#FFF8F2"
CAMPAIGN_PAPER = "#FFFCF8"
CAMPAIGN_BROWN = "#4A3128"
CAMPAIGN_MUTED = "#80645A"
CAMPAIGN_PEACH = "#FFD8C7"
CAMPAIGN_LAVENDER = "#E8DDFB"
CAMPAIGN_PLUM = "#2D243A"
CAMPAIGN_PLUM_2 = "#453550"


def f(name: str, size: int) -> ImageFont.FreeTypeFont:
    return ImageFont.truetype(str(FONT_DIR / f"nunito_{name}.ttf"), size)


def remove_white(image: Image.Image) -> Image.Image:
    image = image.convert("RGBA")
    pixels = image.load()
    for y in range(image.height):
        for x in range(image.width):
            r, g, b, _ = pixels[x, y]
            low = min(r, g, b)
            spread = max(r, g, b) - low
            if low > 230 and spread < 18:
                alpha = max(0, min(255, (245 - low) * 20))
            else:
                alpha = 255
            pixels[x, y] = (r, g, b, alpha)
    bbox = image.getchannel("A").getbbox()
    return image.crop(bbox) if bbox else image


def contain(image: Image.Image, size: tuple[int, int]) -> Image.Image:
    image = image.convert("RGBA")
    copy = image.copy()
    copy.thumbnail(size, Image.Resampling.LANCZOS)
    result = Image.new("RGBA", size, (0, 0, 0, 0))
    result.alpha_composite(
        copy,
        ((size[0] - copy.width) // 2, (size[1] - copy.height) // 2),
    )
    return result


def trim_alpha(image: Image.Image) -> Image.Image:
    image = image.convert("RGBA")
    bbox = image.getchannel("A").getbbox()
    return image.crop(bbox) if bbox else image


def cover(image: Image.Image, size: tuple[int, int]) -> Image.Image:
    image = image.convert("RGBA")
    scale = max(size[0] / image.width, size[1] / image.height)
    resized = image.resize(
        (round(image.width * scale), round(image.height * scale)),
        Image.Resampling.LANCZOS,
    )
    left = (resized.width - size[0]) // 2
    top = (resized.height - size[1]) // 2
    return resized.crop((left, top, left + size[0], top + size[1]))


def rounded_asset(
    image: Image.Image,
    size: tuple[int, int],
    radius: int,
) -> Image.Image:
    fitted = cover(image, size)
    mask = Image.new("L", size, 0)
    ImageDraw.Draw(mask).rounded_rectangle(
        (0, 0, size[0] - 1, size[1] - 1),
        radius=radius,
        fill=255,
    )
    fitted.putalpha(mask)
    return fitted


def panel_shadow(
    canvas: Image.Image,
    box: tuple[int, int, int, int],
    *,
    radius: int,
) -> None:
    x, y, width, height = box
    layer = Image.new("RGBA", CANVAS, (0, 0, 0, 0))
    draw = ImageDraw.Draw(layer)
    draw.rounded_rectangle(
        (x + 16, y + 24, x + width + 16, y + height + 24),
        radius=radius,
        fill=SHADOW,
    )
    canvas.alpha_composite(layer.filter(ImageFilter.GaussianBlur(24)))


def place_panel(
    canvas: Image.Image,
    panel: Image.Image,
    box: tuple[int, int, int, int],
    *,
    radius: int = 50,
    border: str = "#F2B6A7",
    border_width: int = 6,
) -> None:
    x, y, width, height = box
    panel_shadow(canvas, box, radius=radius)
    resized = panel.resize((width, height), Image.Resampling.LANCZOS)
    mask = Image.new("L", (width, height), 0)
    ImageDraw.Draw(mask).rounded_rectangle(
        (0, 0, width - 1, height - 1),
        radius=radius,
        fill=255,
    )
    resized.putalpha(mask)
    canvas.alpha_composite(resized, (x, y))
    ImageDraw.Draw(canvas).rounded_rectangle(
        (x, y, x + width, y + height),
        radius=radius,
        outline=border,
        width=border_width,
    )


def place_pose(
    canvas: Image.Image,
    filename: str,
    box: tuple[int, int, int, int],
    *,
    fallback: str,
) -> None:
    path = POSE_DIR / filename
    if path.exists():
        pose = remove_white(Image.open(path))
    else:
        pose = trim_alpha(Image.open(DRAWABLE_DIR / fallback))
    x, y, width, height = box
    canvas.alpha_composite(contain(pose, (width, height)), (x, y))


def draw_play(
    draw: ImageDraw.ImageDraw,
    center: tuple[int, int],
    *,
    size: int,
    fill: str,
) -> None:
    x, y = center
    draw.polygon(
        (
            (x - size // 3, y - size // 2),
            (x - size // 3, y + size // 2),
            (x + size // 2, y),
        ),
        fill=fill,
    )


def draw_check(
    draw: ImageDraw.ImageDraw,
    center: tuple[int, int],
    *,
    size: int,
    fill: str,
    line: str = WHITE,
) -> None:
    x, y = center
    draw.ellipse(
        (x - size // 2, y - size // 2, x + size // 2, y + size // 2),
        fill=fill,
    )
    draw.line(
        (
            x - size * 0.22,
            y,
            x - size * 0.05,
            y + size * 0.17,
            x + size * 0.25,
            y - size * 0.20,
        ),
        fill=line,
        width=max(3, size // 12),
        joint="curve",
    )


def card(
    draw: ImageDraw.ImageDraw,
    box: tuple[int, int, int, int],
    *,
    fill: str = WHITE,
    outline: str = BORDER,
    radius: int = 28,
    width: int = 2,
) -> None:
    x, y, w, h = box
    draw.rounded_rectangle(
        (x, y, x + w, y + h),
        radius=radius,
        fill=fill,
        outline=outline,
        width=width,
    )


def pill(
    draw: ImageDraw.ImageDraw,
    box: tuple[int, int, int, int],
    text: str,
    *,
    fill: str,
    text_fill: str,
    text_size: int = 22,
    outline: str | None = None,
) -> None:
    x, y, w, h = box
    draw.rounded_rectangle(
        (x, y, x + w, y + h),
        radius=h // 2,
        fill=fill,
        outline=outline,
        width=2 if outline else 0,
    )
    draw.text(
        (x + w / 2, y + h / 2),
        text,
        font=f("bold", text_size),
        fill=text_fill,
        anchor="mm",
    )


def draw_dock(
    draw: ImageDraw.ImageDraw,
    *,
    y: int,
    width: int,
    active: str,
    soundscape: bool = False,
) -> None:
    x = 38
    dock_width = width - 76
    dock_height = 180 if soundscape else 112
    card(
        draw,
        (x, y, dock_width, dock_height),
        fill=DOCK,
        outline=DOCK_BORDER,
        radius=48,
    )

    tab_y = y
    if soundscape:
        draw.rounded_rectangle(
            (x + 20, y + 16, x + 68, y + 56),
            radius=13,
            fill=ACTIVE,
        )
        for offset, bar_height in ((0, 10), (11, 20), (22, 30), (33, 16)):
            draw.rounded_rectangle(
                (
                    x + 27 + offset,
                    y + 36 - bar_height // 2,
                    x + 33 + offset,
                    y + 36 + bar_height // 2,
                ),
                radius=3,
                fill=CORAL,
            )
        draw.text(
            (x + 84, y + 36),
            "Rainy Window · Desk Lamp Rain",
            font=f("bold", 16),
            fill=COCOA,
            anchor="lm",
        )
        draw.ellipse(
            (x + dock_width - 112, y + 13, x + dock_width - 66, y + 59),
            fill=CORAL,
        )
        draw_play(
            draw,
            (x + dock_width - 89, y + 36),
            size=15,
            fill=WHITE,
        )
        draw.line(
            (
                x + dock_width - 45,
                y + 39,
                x + dock_width - 36,
                y + 30,
                x + dock_width - 27,
                y + 39,
            ),
            fill=COCOA_SOFT,
            width=3,
        )
        draw.line(
            (x + 18, y + 68, x + dock_width - 18, y + 68),
            fill=DOCK_BORDER,
            width=2,
        )
        tab_y = y + 68

    tabs = ("Home", "Tasks", "Habits", "Journey")
    slot_width = dock_width / 4
    centers = [x + slot_width * (index + 0.5) for index in range(4)]
    for label, center_x in zip(tabs, centers):
        selected = label == active
        if selected:
            draw.rounded_rectangle(
                (center_x - 46, tab_y + 10, center_x + 46, tab_y + 102),
                radius=40,
                fill=DEEP_CORAL,
            )
        icon_color = WHITE if selected else COCOA_SOFT
        if label == "Home":
            draw.line(
                (
                    center_x - 18,
                    tab_y + 51,
                    center_x,
                    tab_y + 34,
                    center_x + 18,
                    tab_y + 51,
                ),
                fill=icon_color,
                width=3,
                joint="curve",
            )
            draw.line(
                (
                    center_x - 14,
                    tab_y + 48,
                    center_x - 14,
                    tab_y + 69,
                    center_x + 14,
                    tab_y + 69,
                    center_x + 14,
                    tab_y + 48,
                ),
                fill=icon_color,
                width=3,
                joint="curve",
            )
            draw.rounded_rectangle(
                (
                    center_x - 5,
                    tab_y + 58,
                    center_x + 5,
                    tab_y + 69,
                ),
                radius=2,
                outline=icon_color,
                width=2,
            )
        elif label == "Tasks":
            for offset in (-10, 2, 14):
                draw.ellipse(
                    (
                        center_x - 19,
                        tab_y + 50 + offset - 2,
                        center_x - 15,
                        tab_y + 50 + offset + 2,
                    ),
                    fill=icon_color,
                )
                draw.line(
                    (
                        center_x - 8,
                        tab_y + 50 + offset,
                        center_x + 18,
                        tab_y + 50 + offset,
                    ),
                    fill=icon_color,
                    width=3,
                )
        elif label == "Habits":
            draw.ellipse(
                (
                    center_x - 17,
                    tab_y + 34,
                    center_x + 17,
                    tab_y + 68,
                ),
                outline=icon_color,
                width=3,
            )
            draw.line(
                (
                    center_x - 8,
                    tab_y + 52,
                    center_x - 1,
                    tab_y + 59,
                    center_x + 11,
                    tab_y + 43,
                ),
                fill=icon_color,
                width=3,
                joint="curve",
            )
        else:
            draw.rounded_rectangle(
                (
                    center_x - 18,
                    tab_y + 37,
                    center_x + 18,
                    tab_y + 69,
                ),
                radius=5,
                outline=icon_color,
                width=3,
            )
            draw.line(
                (
                    center_x - 18,
                    tab_y + 48,
                    center_x + 18,
                    tab_y + 48,
                ),
                fill=icon_color,
                width=3,
            )
            draw.line(
                (
                    center_x - 9,
                    tab_y + 33,
                    center_x - 9,
                    tab_y + 42,
                ),
                fill=icon_color,
                width=3,
            )
            draw.line(
                (
                    center_x + 9,
                    tab_y + 33,
                    center_x + 9,
                    tab_y + 42,
                ),
                fill=icon_color,
                width=3,
            )
        draw.text(
            (center_x, tab_y + 86),
            label,
            font=f("semibold", 16),
            fill=icon_color,
            anchor="mm",
        )


def make_home_ui(size: tuple[int, int] = (760, 1080)) -> Image.Image:
    width, height = size
    image = Image.new("RGBA", size, PAGE)
    draw = ImageDraw.Draw(image)

    draw.text((36, 34), "Good afternoon,", font=f("medium", 24), fill=COCOA_SOFT)
    draw.text((36, 72), "Purro Friend", font=f("extrabold", 46), fill=COCOA)
    draw.text(
        (36, 130),
        "Keep the afternoon moving gently.",
        font=f("medium", 22),
        fill=COCOA_SOFT,
    )
    draw.text((36, 166), "Jul 18", font=f("medium", 18), fill=COCOA_SOFT)
    draw.ellipse((660, 44, 716, 100), fill=ACTIVE, outline=BORDER, width=2)
    draw.ellipse((676, 59, 700, 83), fill="#F0D4BC")

    hero = Image.open(DRAWABLE_DIR / "cat_home_header_day.webp")
    hero_box = (36, 210, width - 72, 340)
    image.alpha_composite(
        rounded_asset(hero, (hero_box[2], hero_box[3]), 30),
        (hero_box[0], hero_box[1]),
    )
    draw.ellipse((width - 98, 228, width - 54, 272), fill=WHITE)
    draw.line(
        (width - 87, 242, width - 76, 234, width - 65, 242),
        fill=COCOA,
        width=3,
    )

    pill(
        draw,
        (218, 516, 324, 74),
        "Start Focus",
        fill=CORAL,
        text_fill=WHITE,
        text_size=26,
    )
    draw_play(draw, (254, 553), size=18, fill=WHITE)

    card(draw, (36, 618, width - 72, 146), radius=24)
    draw.text((62, 640), "READY WHEN YOU ARE", font=f("bold", 17), fill=COCOA_SOFT)
    draw.rounded_rectangle((62, 678, 116, 732), radius=14, fill=ACTIVE)
    draw_check(draw, (89, 705), size=29, fill=CORAL)
    draw.text((136, 676), "Design onboarding flow", font=f("bold", 25), fill=COCOA)
    draw.text(
        (136, 714),
        "Task · Suggested for now · 25 min focus",
        font=f("medium", 18),
        fill=COCOA_SOFT,
    )
    draw.text((width - 76, 705), "›", font=f("bold", 34), fill=COCOA_SOFT, anchor="mm")

    card(draw, (36, 786, width - 72, 130), radius=24)
    draw.text((62, 808), "TODAY'S FOCUS", font=f("bold", 17), fill=COCOA_SOFT)
    metrics = (("1h 15m", "Focus time"), ("3", "Sessions"), ("1", "Completed"))
    for index, (value, label) in enumerate(metrics):
        center_x = 145 + index * 235
        draw.text(
            (center_x, 854),
            value,
            font=f("extrabold", 28),
            fill=COCOA,
            anchor="mm",
        )
        draw.text(
            (center_x, 889),
            label,
            font=f("medium", 17),
            fill=COCOA_SOFT,
            anchor="mm",
        )

    draw_dock(draw, y=900, width=width, active="Home", soundscape=True)
    return image


def make_focus_ui(size: tuple[int, int] = (760, 960)) -> Image.Image:
    width, height = size
    image = Image.new("RGBA", size, PAGE)
    draw = ImageDraw.Draw(image)
    draw.text(
        (width / 2, 70),
        "F O C U S",
        font=f("bold", 19),
        fill=COCOA_SOFT,
        anchor="mm",
    )
    draw.text(
        (width / 2, 210),
        "25:00",
        font=f("regular", 98),
        fill=COCOA,
        anchor="mm",
    )
    pill(
        draw,
        (190, 280, 380, 58),
        "Task  ·  Design onboarding flow",
        fill=WHITE,
        text_fill=COCOA,
        text_size=18,
        outline=BORDER,
    )
    for index in range(4):
        x = 310 + index * 48
        fill = CORAL if index == 0 else PAGE
        draw.ellipse((x, 370, x + 24, 394), fill=fill, outline=CORAL, width=3)
    draw.text(
        (width / 2, 426),
        "1 / 4",
        font=f("bold", 18),
        fill=COCOA_SOFT,
        anchor="mm",
    )
    mascot = trim_alpha(Image.open(DRAWABLE_DIR / "cat_focusing_1.webp"))
    image.alpha_composite(contain(mascot, (400, 270)), (180, 450))
    card(draw, (112, 738, 536, 76), fill=WHITE, radius=38)
    draw.rounded_rectangle((136, 756, 184, 796), radius=14, fill=ACTIVE)
    for offset, bar_height in ((0, 12), (12, 22), (24, 31), (36, 18)):
        draw.rounded_rectangle(
            (144 + offset, 776 - bar_height // 2, 151 + offset, 776 + bar_height // 2),
            radius=3,
            fill=CORAL,
        )
    draw.text(
        (204, 776),
        "Rainy Window · Desk Lamp Rain",
        font=f("bold", 17),
        fill=COCOA,
        anchor="lm",
    )
    draw.ellipse((566, 754, 610, 798), fill=CORAL)
    draw.line((584, 768, 584, 784), fill=WHITE, width=4)
    draw.line((593, 768, 593, 784), fill=WHITE, width=4)
    pill(
        draw,
        (278, 840, 204, 62),
        "Pause",
        fill=WHITE,
        text_fill=COCOA_SOFT,
        text_size=20,
        outline=BORDER,
    )
    draw.text(
        (width / 2, 930),
        "tap anywhere to exit",
        font=f("medium", 16),
        fill="#B7A69D",
        anchor="mm",
    )
    return image


def make_tasks_ui(size: tuple[int, int] = (790, 1040)) -> Image.Image:
    width, height = size
    image = Image.new("RGBA", size, PAGE)
    draw = ImageDraw.Draw(image)
    draw.text((38, 42), "Tasks", font=f("extrabold", 48), fill=COCOA)
    draw.ellipse((width - 94, 30, width - 38, 86), fill=CORAL)
    draw.text((width - 66, 58), "+", font=f("regular", 38), fill=WHITE, anchor="mm")

    card(draw, (38, 116, width - 76, 70), fill=DOCK, radius=35)
    segments = (("All 5", True), ("Active 3", False), ("Done 2", False))
    for index, (label, selected) in enumerate(segments):
        x = 50 + index * 228
        if selected:
            draw.rounded_rectangle((x, 125, x + 212, 177), radius=26, fill=WHITE)
        draw.text(
            (x + 106, 151),
            label,
            font=f("bold", 19),
            fill=CORAL if selected else COCOA_SOFT,
            anchor="mm",
        )

    card(draw, (38, 216, width - 76, 280), fill=ACTIVE, radius=28)
    draw.text((66, 242), "NEXT FOCUS", font=f("extrabold", 17), fill=CORAL)
    draw.ellipse((64, 284, 96, 316), outline=CORAL, width=3)
    draw.text((116, 278), "Design onboarding flow", font=f("bold", 28), fill=COCOA)
    pill(
        draw,
        (116, 332, 120, 42),
        "Work",
        fill=WHITE,
        text_fill=COCOA_SOFT,
        text_size=16,
        outline=BORDER,
    )
    pill(
        draw,
        (250, 332, 150, 42),
        "Due today",
        fill=WHITE,
        text_fill=COCOA_SOFT,
        text_size=16,
        outline=BORDER,
    )
    draw.ellipse((68, 418, 90, 440), outline=COCOA_SOFT, width=2)
    draw.line((79, 422, 79, 430, 85, 434), fill=COCOA_SOFT, width=2)
    draw.text((102, 430), "25 min focus", font=f("bold", 19), fill=COCOA_SOFT)
    pill(
        draw,
        (width - 220, 406, 150, 58),
        "Start",
        fill=CORAL,
        text_fill=WHITE,
        text_size=20,
    )
    draw_play(draw, (width - 192, 435), size=15, fill=WHITE)

    draw.text((40, 532), "More tasks", font=f("bold", 25), fill=COCOA)
    tasks = (
        ("Review pull requests", "Work · Tomorrow", False),
        ("Write launch notes", "Personal · Jul 20", False),
        ("Book dentist appointment", "Personal · Done", True),
    )
    for index, (title, meta, done) in enumerate(tasks):
        y = 578 + index * 125
        card(draw, (38, y, width - 76, 102), radius=22)
        if done:
            draw_check(draw, (76, y + 51), size=34, fill=MINT, line=COCOA)
        else:
            draw.ellipse((59, y + 34, 93, y + 68), outline=CORAL, width=3)
        draw.text(
            (116, y + 29),
            title,
            font=f("bold", 23),
            fill=COCOA_SOFT if done else COCOA,
        )
        draw.text(
            (116, y + 64),
            meta,
            font=f("medium", 17),
            fill=COCOA_SOFT,
        )
    draw_dock(draw, y=914, width=width, active="Tasks")
    return image


def make_habits_ui(size: tuple[int, int] = (790, 1040)) -> Image.Image:
    width, height = size
    image = Image.new("RGBA", size, PAGE)
    draw = ImageDraw.Draw(image)
    draw.text((38, 42), "Habits", font=f("extrabold", 48), fill=COCOA)
    draw.ellipse((width - 94, 30, width - 38, 86), fill=CORAL)
    draw.text((width - 66, 58), "+", font=f("regular", 38), fill=WHITE, anchor="mm")
    draw.text((40, 126), "Today", font=f("bold", 27), fill=COCOA)
    pill(
        draw,
        (width - 176, 112, 136, 48),
        "2 to go",
        fill=ACTIVE,
        text_fill=DEEP_CORAL,
        text_size=17,
    )

    habits = (
        ("meditate", "Meditate", "Due today", "5 day streak", True, "#7FBFA3"),
        ("read", "Read", "Due today", "12 day streak", False, "#90CAF9"),
        ("water", "Drink water", "3 of 8 glasses", "8 day streak", False, "#8CC6DB"),
    )
    for index, (symbol, title, status, streak, done, accent) in enumerate(habits):
        y = 184 + index * 174
        card(draw, (38, y, width - 76, 148), radius=28)
        draw.rounded_rectangle((62, y + 42, 118, y + 98), radius=16, fill=accent)
        if symbol == "meditate":
            draw.arc((75, y + 54, 105, y + 84), 200, 340, fill=WHITE, width=3)
            draw.ellipse((85, y + 57, 95, y + 67), fill=WHITE)
        elif symbol == "read":
            draw.rounded_rectangle(
                (76, y + 55, 104, y + 84),
                radius=4,
                outline=WHITE,
                width=3,
            )
            draw.line((90, y + 55, 90, y + 84), fill=WHITE, width=2)
        else:
            draw.polygon(
                ((90, y + 53), (78, y + 74), (90, y + 87), (102, y + 74)),
                fill=WHITE,
            )
        draw.text((140, y + 34), title, font=f("bold", 25), fill=COCOA)
        draw.text((140, y + 76), status, font=f("medium", 18), fill=COCOA_SOFT)
        pill(
            draw,
            (140, y + 102, 92, 32),
            streak,
            fill=ACTIVE,
            text_fill=DEEP_CORAL,
            text_size=12,
        )
        if done:
            draw_check(draw, (width - 78, y + 74), size=44, fill=MINT)
        else:
            draw.ellipse(
                (width - 100, y + 52, width - 56, y + 96),
                outline=accent,
                width=4,
            )

    card(draw, (38, 728, width - 76, 154), fill=ACTIVE, radius=28)
    face = Image.open(DRAWABLE_DIR / "cat_face.webp")
    image.alpha_composite(contain(trim_alpha(face), (120, 120)), (55, 748))
    draw.text(
        (190, 755),
        "Consistency is the secret\nbehind all progress.",
        font=f("bold", 22),
        fill=COCOA,
        spacing=5,
    )
    draw.text((190, 828), "You're doing great!", font=f("medium", 18), fill=COCOA_SOFT)
    draw_dock(draw, y=914, width=width, active="Habits")
    return image


def image_tile(
    canvas: Image.Image,
    path: Path,
    box: tuple[int, int, int, int],
    *,
    radius: int,
    border: str | None = None,
) -> None:
    x, y, width, height = box
    tile = rounded_asset(Image.open(path), (width, height), radius)
    canvas.alpha_composite(tile, (x, y))
    if border:
        ImageDraw.Draw(canvas).rounded_rectangle(
            (x, y, x + width, y + height),
            radius=radius,
            outline=border,
            width=4,
        )


def make_sounds_ui(size: tuple[int, int] = (790, 1040)) -> Image.Image:
    width, height = size
    image = Image.new("RGBA", size, WHITE)
    draw = ImageDraw.Draw(image)
    draw.text((38, 40), "Soundscape", font=f("extrabold", 44), fill=COCOA)
    draw.text(
        (40, 96),
        "Mix ambience + lo-fi for your focus",
        font=f("medium", 20),
        fill=COCOA_SOFT,
    )
    draw.text((40, 154), "Ambience", font=f("bold", 26), fill=COCOA)
    draw.text((width - 104, 160), "See all ›", font=f("bold", 17), fill=COCOA_SOFT)

    ambience = (
        ("Rain", ROOT / "assets/ambience/thumbnails/rain.png"),
        ("Cafe Purr", ROOT / "assets/ambience/thumbnails/cafe.png"),
        ("Fireplace", ROOT / "assets/ambience/thumbnails/fireplace.png"),
        ("Forest Nap", ROOT / "assets/ambience/thumbnails/forest.png"),
    )
    for index, (label, path) in enumerate(ambience):
        x = 40 + index * 180
        if path.exists():
            image_tile(image, path, (x, 204, 142, 142), radius=24, border=CORAL if index == 0 else None)
        else:
            draw.rounded_rectangle((x, 204, x + 142, 346), radius=24, fill=ACTIVE)
        draw.text(
            (x + 71, 372),
            label,
            font=f("bold", 16),
            fill=COCOA,
            anchor="mm",
        )
        if index == 0:
            pill(
                draw,
                (x + 28, 392, 86, 32),
                "Playing",
                fill=ACTIVE,
                text_fill=CORAL,
                text_size=13,
            )

    draw.text((40, 462), "Lo-fi music", font=f("bold", 26), fill=COCOA)
    vinyls = (
        ("Desk Lamp Rain", ROOT / "assets/soundscape/soundscape_vinyl_study_beats.png"),
        ("Mosswood Morning", ROOT / "assets/soundscape/soundscape_vinyl_soft_piano.png"),
        ("Paper Skies", ROOT / "assets/soundscape/soundscape_vinyl_chill_hop.png"),
    )
    for index, (label, path) in enumerate(vinyls):
        x = 60 + index * 236
        if path.exists():
            vinyl = trim_alpha(Image.open(path))
            image.alpha_composite(contain(vinyl, (150, 150)), (x, 510))
        else:
            draw.ellipse((x, 510, x + 150, 660), fill="#161616")
            draw.ellipse((x + 56, 566, x + 94, 604), fill=CORAL)
        draw.text(
            (x + 75, 690),
            label,
            font=f("bold", 15),
            fill=COCOA,
            anchor="mm",
        )

    card(draw, (24, 750, width - 48, 250), fill=PAGE, radius=32)
    draw.text((54, 780), "YOUR MIX", font=f("extrabold", 16), fill=COCOA_SOFT)
    for index, (label, value) in enumerate((("Ambience", 0.35), ("Music", 0.68))):
        y = 834 + index * 66
        draw.text((58, y), label, font=f("bold", 20), fill=COCOA)
        draw.rounded_rectangle((220, y + 7, 606, y + 19), radius=6, fill=DOCK)
        draw.rounded_rectangle(
            (220, y + 7, 220 + round(386 * value), y + 19),
            radius=6,
            fill=CORAL,
        )
        draw.ellipse(
            (
                212 + round(386 * value),
                y - 2,
                236 + round(386 * value),
                y + 28,
            ),
            fill=CORAL,
        )
    draw.ellipse((668, 842, 734, 908), fill=CORAL)
    draw.line((693, 862, 693, 889), fill=WHITE, width=5)
    draw.line((708, 862, 708, 889), fill=WHITE, width=5)
    return image


def make_journey_ui(size: tuple[int, int] = (790, 1080)) -> Image.Image:
    width, height = size
    image = Image.new("RGBA", size, PAGE)
    draw = ImageDraw.Draw(image)
    draw.text((38, 40), "Journey", font=f("extrabold", 48), fill=COCOA)
    draw.ellipse((width - 88, 38, width - 40, 86), fill=WHITE, outline=BORDER, width=2)
    draw.rounded_rectangle((width - 75, 52, width - 53, 72), radius=4, outline=COCOA, width=2)

    draw.text((40, 126), "TODAY", font=f("extrabold", 17), fill=COCOA_SOFT)
    card(draw, (38, 164, width - 76, 184), radius=28)
    draw.line((80, 204, 80, 310), fill=BORDER, width=4)
    draw.ellipse((68, 212, 92, 236), fill=CORAL)
    draw.text((112, 202), "Design onboarding flow", font=f("bold", 23), fill=COCOA)
    draw.text((112, 240), "2:15 PM · 25 min", font=f("medium", 18), fill=COCOA_SOFT)
    draw_check(draw, (width - 86, 228), size=36, fill=MINT, line=COCOA)
    draw.ellipse((71, 280, 89, 298), fill=ACTIVE, outline=CORAL, width=2)
    draw.text((112, 272), "Review pull requests", font=f("bold", 21), fill=COCOA)
    draw.text((112, 305), "1:20 PM · 25 min", font=f("medium", 17), fill=COCOA_SOFT)

    draw.text((40, 390), "THIS WEEK", font=f("extrabold", 17), fill=COCOA_SOFT)
    card(draw, (38, 428, width - 76, 278), radius=28)
    draw.text((64, 452), "Focus time this week", font=f("bold", 21), fill=COCOA_SOFT)
    draw.text((64, 492), "4h 30m", font=f("extrabold", 45), fill=COCOA)
    draw.text((244, 514), "+18% vs last week", font=f("bold", 17), fill=GREEN)
    values = (38, 82, 54, 100, 70, 46, 88)
    labels = ("M", "T", "W", "T", "F", "S", "S")
    for index, (value, label) in enumerate(zip(values, labels)):
        x = 74 + index * 88
        draw.rounded_rectangle(
            (x, 646 - value, x + 38, 646),
            radius=12,
            fill=CORAL if index == 5 else ACTIVE,
        )
        draw.text((x + 19, 674), label, font=f("bold", 15), fill=COCOA_SOFT, anchor="mm")

    draw.text((40, 744), "THIS MONTH", font=f("extrabold", 17), fill=COCOA_SOFT)
    card(draw, (38, 782, width - 76, 176), radius=28)
    values = (0, 1, 2, 0, 3, 1, 0, 1, 2, 3, 1, 0, 0, 2, 4, 2, 1, 0, 3, 2, 1)
    for index, value in enumerate(values):
        row = index // 7
        col = index % 7
        alpha_colors = ("#F8EDE8", "#FFD8D1", "#FFB7AE", "#FF948A", CORAL)
        x = 66 + col * 54
        y = 824 + row * 42
        draw.rounded_rectangle(
            (x, y, x + 30, y + 30),
            radius=8,
            fill=alpha_colors[value],
        )
    draw.text((480, 826), "12", font=f("extrabold", 35), fill=COCOA)
    draw.text((480, 866), "focus days", font=f("medium", 17), fill=COCOA_SOFT)
    draw.text((620, 826), "14", font=f("extrabold", 35), fill=COCOA)
    draw.text((620, 866), "day streak", font=f("medium", 17), fill=COCOA_SOFT)
    draw_dock(draw, y=968, width=width, active="Journey")
    return image


def campaign_header(
    canvas: Image.Image,
    *,
    index: int,
    headline_text: str,
    subtitle_text: str,
    dark: bool = False,
) -> None:
    draw = ImageDraw.Draw(canvas)
    fill = "#FFF8F1" if dark else CAMPAIGN_BROWN
    badge_fill = "#4A3958" if dark else WHITE
    badge_text = "#FFF8F1" if dark else CAMPAIGN_BROWN
    draw.ellipse((68, 70, 100, 102), fill="#FF7464")
    draw.text(
        (120, 86),
        "PURRODORO",
        font=f("extrabold", 30),
        fill=fill,
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
        fill=badge_text,
        anchor="mm",
    )
    draw.multiline_text(
        (68, 180),
        headline_text,
        font=f("extrabold", 101),
        fill=fill,
        spacing=-8,
    )
    draw.multiline_text(
        (72, 420),
        subtitle_text,
        font=f("semibold", 40),
        fill="#D9CBE3" if dark else CAMPAIGN_MUTED,
        spacing=7,
    )


def build_overview() -> Image.Image:
    canvas = Image.new("RGBA", CANVAS, CAMPAIGN_CREAM)
    draw = ImageDraw.Draw(canvas)
    draw.ellipse((-280, 1300, 780, 2320), fill=CAMPAIGN_PEACH)
    draw.ellipse((760, 1360, 1270, 1870), fill=CAMPAIGN_LAVENDER)
    campaign_header(
        canvas,
        index=1,
        headline_text="Plan. Focus.\nFeel progress.",
        subtitle_text="Your whole focus day in one calm place.",
    )
    place_panel(canvas, make_home_ui(), (82, 590, 800, 1135), radius=58)
    place_pose(
        canvas,
        "purrodoro_pose_overview.png",
        (640, 1320, 410, 470),
        fallback="cat_notify_hero.webp",
    )
    pill(
        draw,
        (615, 1020, 390, 76),
        "TASKS  ·  HABITS  ·  SOUNDS",
        fill=WHITE,
        text_fill=COCOA,
        text_size=20,
        outline="#F2B6A7",
    )
    return canvas


def build_focus() -> Image.Image:
    canvas = Image.new("RGBA", CANVAS, "#EEE6FC")
    draw = ImageDraw.Draw(canvas)
    draw.ellipse((710, -280, 1280, 520), fill=CAMPAIGN_PEACH)
    draw.rounded_rectangle(
        (-220, 1400, 1300, 2200),
        radius=420,
        fill=CAMPAIGN_CREAM,
    )
    campaign_header(
        canvas,
        index=2,
        headline_text="Focus without\nthe pressure.",
        subtitle_text="A calm timer that keeps everything else quiet.",
    )
    place_panel(canvas, make_focus_ui(), (154, 610, 772, 980), radius=66)
    place_pose(
        canvas,
        "purrodoro_pose_focus.png",
        (430, 1370, 560, 500),
        fallback="cat_focusing_1.webp",
    )
    draw.arc((76, 560, 1004, 1490), 198, 342, fill=CORAL, width=18)
    return canvas


def build_tasks() -> Image.Image:
    canvas = Image.new("RGBA", CANVAS, CAMPAIGN_PAPER)
    draw = ImageDraw.Draw(canvas)
    draw.rounded_rectangle(
        (640, -220, 1280, 780),
        radius=300,
        fill=CAMPAIGN_PEACH,
    )
    draw.ellipse((-300, 1390, 520, 2200), fill=CAMPAIGN_LAVENDER)
    campaign_header(
        canvas,
        index=3,
        headline_text="Turn plans into\nprogress.",
        subtitle_text="Capture it. Choose it. Start focusing.",
    )
    place_panel(canvas, make_tasks_ui(), (90, 590, 820, 1080), radius=58)
    place_pose(
        canvas,
        "purrodoro_pose_tasks.png",
        (590, 1280, 460, 550),
        fallback="cat_checklist.webp",
    )
    pill(
        draw,
        (70, 1460, 410, 80),
        "NEXT FOCUS  ·  25 MIN",
        fill=CORAL,
        text_fill=WHITE,
        text_size=22,
    )
    return canvas


def build_habits() -> Image.Image:
    canvas = Image.new("RGBA", CANVAS, CAMPAIGN_CREAM)
    draw = ImageDraw.Draw(canvas)
    draw.rounded_rectangle(
        (-120, 560, 1200, 2040),
        radius=310,
        fill=CAMPAIGN_PLUM,
    )
    draw.ellipse((730, 1380, 1280, 1980), fill=CAMPAIGN_PLUM_2)
    campaign_header(
        canvas,
        index=4,
        headline_text="Build habits\nthat stick.",
        subtitle_text="Small wins become visible momentum.",
    )
    place_panel(
        canvas,
        make_habits_ui(),
        (82, 620, 820, 1080),
        radius=58,
        border="#8A6AA1",
    )
    place_pose(
        canvas,
        "purrodoro_pose_habits.png",
        (610, 1320, 440, 480),
        fallback="cat_celebrate.webp",
    )
    pill(
        draw,
        (92, 1510, 360, 78),
        "12 DAY STREAK",
        fill="#5A4568",
        text_fill=WHITE,
        text_size=22,
        outline="#8A6AA1",
    )
    return canvas


def build_sounds() -> Image.Image:
    canvas = Image.new("RGBA", CANVAS, CAMPAIGN_PLUM)
    draw = ImageDraw.Draw(canvas)
    draw.rounded_rectangle(
        (-180, 1370, 1260, 2200),
        radius=420,
        fill=CAMPAIGN_PEACH,
    )
    draw.ellipse((730, 1230, 1290, 1810), fill="#4A3858")
    campaign_header(
        canvas,
        index=5,
        headline_text="Find your\nperfect sound.",
        subtitle_text="Layer ambience with lo-fi, your way.",
        dark=True,
    )
    place_panel(canvas, make_sounds_ui(), (78, 590, 830, 1090), radius=58)
    place_pose(
        canvas,
        "purrodoro_pose_sounds.png",
        (560, 1320, 500, 520),
        fallback="cat_home_sleeping.webp",
    )
    for x, height in ((80, 54), (108, 94), (136, 138), (164, 92), (192, 60)):
        draw.rounded_rectangle(
            (x, 1710 - height // 2, x + 14, 1710 + height // 2),
            radius=7,
            fill=CORAL,
        )
    return canvas


def build_journey() -> Image.Image:
    canvas = Image.new("RGBA", CANVAS, "#F2EBFC")
    draw = ImageDraw.Draw(canvas)
    draw.ellipse((720, -240, 1260, 640), fill=CAMPAIGN_PEACH)
    draw.rounded_rectangle(
        (-180, 1340, 1260, 2200),
        radius=430,
        fill=CAMPAIGN_PEACH,
    )
    campaign_header(
        canvas,
        index=6,
        headline_text="See progress,\nnot pressure.",
        subtitle_text="Every focused day becomes part of your story.",
    )
    place_panel(canvas, make_journey_ui(), (76, 590, 830, 1135), radius=58)
    place_pose(
        canvas,
        "purrodoro_pose_journey.png",
        (590, 1320, 470, 520),
        fallback="cat_celebrate.webp",
    )
    pill(
        draw,
        (80, 1500, 400, 78),
        "+18%  VS LAST WEEK",
        fill=WHITE,
        text_fill=GREEN,
        text_size=22,
        outline="#F2B6A7",
    )
    return canvas


def save(image: Image.Image, filename: str) -> Path:
    path = OUTPUT_DIR / filename
    rgb = image.convert("RGB")
    assert rgb.size == CANVAS
    assert sum(ImageStat.Stat(rgb).var) > 100
    rgb.save(path, "PNG", optimize=True)
    print(path.relative_to(ROOT))
    return path


def main() -> None:
    outputs = (
        (build_overview(), "store_portrait_1_final.png"),
        (build_focus(), "store_portrait_2_final.png"),
        (build_tasks(), "store_portrait_3_final.png"),
        (build_habits(), "store_portrait_4_final.png"),
        (build_sounds(), "store_portrait_5_final.png"),
        (build_journey(), "store_portrait_6_final.png"),
    )
    for image, filename in outputs:
        save(image, filename)


if __name__ == "__main__":
    main()
