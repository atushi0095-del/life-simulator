#!/usr/bin/env python3
"""
Regenerates the Play Store assets in this folder.

The screenshots are rendered mockups of the real screens, not captures from a
device - there is no emulator in the build environment. They follow the actual
layout, copy (res/values/strings.xml) and colour tokens (ui/theme/Theme.kt), so
they are accurate, but they should be replaced with real device captures before
the listing goes live. See AGENT_HANDOFF_LOG.md.

    python3 generate_assets.py
"""

import os
from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
SHOTS = os.path.join(HERE, "screenshots")

JP = "/usr/share/fonts/truetype/fonts-japanese-gothic.ttf"

# Tokens mirrored from ui/theme/Theme.kt (light scheme).
TEAL = (0x00, 0x69, 0x6E)
TEAL_CONTAINER = (0x9C, 0xF1, 0xF7)
ON_TEAL_CONTAINER = (0x00, 0x20, 0x22)
SURFACE = (0xFF, 0xFB, 0xFE)
SURFACE_VARIANT = (0xDD, 0xE4, 0xE5)
ON_SURFACE = (0x1B, 0x1C, 0x1E)
ON_SURFACE_VARIANT = (0x5C, 0x5F, 0x63)
WHITE = (0xFF, 0xFF, 0xFF)

W, H = 1080, 1920


def font(size):
    return ImageFont.truetype(JP, size)


def centre(draw, y, text, f, fill):
    w = draw.textbbox((0, 0), text, font=f)[2]
    draw.text(((W - w) // 2, y), text, font=f, fill=fill)


def rounded(draw, box, radius, fill):
    draw.rounded_rectangle(box, radius=radius, fill=fill)


def chrome(draw, title="WorkLog"):
    """Status bar and top app bar."""
    draw.rectangle([0, 0, W, 120], fill=SURFACE)
    draw.text((56, 44), "9:41", font=font(34), fill=ON_SURFACE)
    draw.text((W - 150, 44), "100%", font=font(30), fill=ON_SURFACE_VARIANT)
    draw.text((56, 150), title, font=font(52), fill=ON_SURFACE)
    # History + settings icons, drawn as simple glyph stand-ins.
    draw.ellipse([W - 250, 150, W - 190, 210], outline=ON_SURFACE_VARIANT, width=5)
    draw.ellipse([W - 150, 150, W - 90, 210], outline=ON_SURFACE_VARIANT, width=5)


def totals_card(draw, y, today, week, month):
    rounded(draw, [56, y, W - 56, y + 230], 40, SURFACE_VARIANT)
    cols = [("今日", today), ("今週", week), ("今月", month)]
    span = (W - 112) / 3
    for i, (label, value) in enumerate(cols):
        cx = 56 + span * i + span / 2
        lf, vf = font(30), font(44)
        lw = draw.textbbox((0, 0), label, font=lf)[2]
        vw = draw.textbbox((0, 0), value, font=vf)[2]
        draw.text((cx - lw / 2, y + 60), label, font=lf, fill=ON_SURFACE_VARIANT)
        draw.text((cx - vw / 2, y + 115), value, font=vf, fill=ON_SURFACE)


def big_button(draw, y, label, fill, text_fill):
    rounded(draw, [56, y, W - 56, y + 200], 100, fill)
    f = font(62)
    w = draw.textbbox((0, 0), label, font=f)[2]
    draw.text(((W - w) // 2, y + 62), label, font=f, fill=text_fill)


def outlined_button(draw, y, label):
    draw.rounded_rectangle([56, y, W - 56, y + 120], radius=60, outline=TEAL, width=4)
    f = font(44)
    w = draw.textbbox((0, 0), label, font=f)[2]
    draw.text(((W - w) // 2, y + 36), label, font=f, fill=TEAL)


def shot_home_off():
    img = Image.new("RGB", (W, H), SURFACE)
    d = ImageDraw.Draw(img)
    chrome(d)
    centre(d, 400, "勤務外", font(46), ON_SURFACE_VARIANT)
    centre(d, 480, "0時間00分", font(120), ON_SURFACE)
    centre(d, 640, "今日", font(36), ON_SURFACE_VARIANT)
    big_button(d, 780, "出勤", TEAL, WHITE)
    totals_card(d, 1080, "0時間00分", "32時間10分", "126時間32分")
    centre(d, 1400, "出勤・休憩・退勤を押すだけで", font(34), ON_SURFACE_VARIANT)
    centre(d, 1455, "勤務時間を記録できます。", font(34), ON_SURFACE_VARIANT)
    img.save(os.path.join(SHOTS, "01-home-off.png"))


def shot_home_working():
    img = Image.new("RGB", (W, H), SURFACE)
    d = ImageDraw.Draw(img)
    chrome(d)
    centre(d, 400, "勤務中", font(46), TEAL)
    centre(d, 480, "2時間17分", font(120), ON_SURFACE)
    centre(d, 640, "開始 09:03", font(36), ON_SURFACE_VARIANT)
    big_button(d, 780, "退勤", SURFACE_VARIANT, ON_SURFACE)
    outlined_button(d, 1010, "休憩する")
    totals_card(d, 1200, "2時間17分", "34時間27分", "128時間49分")
    img.save(os.path.join(SHOTS, "02-home-working.png"))


def shot_home_break():
    img = Image.new("RGB", (W, H), SURFACE)
    d = ImageDraw.Draw(img)
    chrome(d)
    centre(d, 400, "休憩中", font(46), TEAL)
    centre(d, 480, "2時間57分", font(120), ON_SURFACE)
    centre(d, 640, "12:03から休憩中", font(36), ON_SURFACE_VARIANT)
    big_button(d, 780, "休憩終了", TEAL, WHITE)
    totals_card(d, 1080, "2時間57分", "35時間07分", "129時間29分")
    img.save(os.path.join(SHOTS, "03-home-break.png"))


def shot_history():
    img = Image.new("RGB", (W, H), SURFACE)
    d = ImageDraw.Draw(img)
    chrome(d, "履歴")

    d.text((110, 250), "◀", font=font(44), fill=TEAL)
    centre(d, 245, "2026/9", font(54), ON_SURFACE)
    d.text((W - 150, 250), "▶", font=font(44), fill=TEAL)

    rounded(d, [56, 350, W - 56, 560], 40, SURFACE_VARIANT)
    cells = [("勤務日数", "20日"), ("総勤務時間", "162時間24分"), ("総休憩", "20時間00分")]
    span = (W - 112) / 3
    for i, (label, value) in enumerate(cells):
        cx = 56 + span * i + span / 2
        lf, vf = font(28), font(38)
        lw = d.textbbox((0, 0), label, font=lf)[2]
        vw = d.textbbox((0, 0), value, font=vf)[2]
        d.text((cx - lw / 2, 400), label, font=lf, fill=ON_SURFACE_VARIANT)
        d.text((cx - vw / 2, 455), value, font=vf, fill=ON_SURFACE)

    rows = [
        ("9/20", "09:03 → 18:17", "8時間14分"),
        ("9/19", "08:54 → 17:58", "8時間04分"),
        ("9/18", "09:12 → 20:31", "10時間19分"),
        ("9/17", "09:00 → 18:00", "8時間00分"),
        ("9/16", "22:00 → 06:00", "8時間00分"),
        ("9/15", "09:05 → 17:45", "7時間40分"),
    ]
    y = 640
    for date, span_text, total in rows:
        d.text((70, y + 30), date, font=font(42), fill=ON_SURFACE)
        d.text((260, y + 32), span_text, font=font(40), fill=ON_SURFACE)
        tw = d.textbbox((0, 0), total, font=font(42))[2]
        d.text((W - 70 - tw, y + 30), total, font=font(42), fill=ON_SURFACE)
        d.line([56, y + 110, W - 56, y + 110], fill=SURFACE_VARIANT, width=3)
        y += 130

    rounded(d, [W - 480, H - 260, W - 56, H - 130], 65, TEAL_CONTAINER)
    d.text((W - 430, H - 225), "＋ 勤務を追加", font=font(42), fill=ON_TEAL_CONTAINER)
    img.save(os.path.join(SHOTS, "04-history.png"))


def shot_summary():
    """The clock-out result screen."""
    img = Image.new("RGB", (W, H), SURFACE)
    d = ImageDraw.Draw(img)
    chrome(d)
    centre(d, 330, "今日もお疲れさまでした", font(52), ON_SURFACE)

    rounded(d, [56, 460, W - 56, 900], 40, SURFACE_VARIANT)
    pairs = [("出勤", "09:03"), ("退勤", "18:17"), ("休憩", "1時間00分")]
    y = 520
    for label, value in pairs:
        d.text((110, y), label, font=font(40), fill=ON_SURFACE_VARIANT)
        vw = d.textbbox((0, 0), value, font=font(44))[2]
        d.text((W - 110 - vw, y - 2), value, font=font(44), fill=ON_SURFACE)
        y += 120

    centre(d, 980, "実働", font(40), ON_SURFACE_VARIANT)
    centre(d, 1040, "8時間14分", font(120), TEAL)
    centre(d, 1230, "今月", font(36), ON_SURFACE_VARIANT)
    centre(d, 1290, "126時間32分", font(64), ON_SURFACE)
    img.save(os.path.join(SHOTS, "05-clock-out.png"))


def clock_face(d, cx, cy, r, colour, width):
    d.ellipse([cx - r, cy - r, cx + r, cy + r], outline=colour, width=width)
    d.line([cx, cy, cx, cy - r * 0.55], fill=colour, width=width)
    d.line([cx, cy, cx + r * 0.42, cy + r * 0.28], fill=colour, width=width)


def icon_512():
    img = Image.new("RGB", (512, 512), TEAL)
    d = ImageDraw.Draw(img)
    clock_face(d, 256, 256, 150, WHITE, 26)
    img.save(os.path.join(HERE, "icon-512.png"))


def feature_graphic():
    img = Image.new("RGB", (1024, 500), TEAL)
    d = ImageDraw.Draw(img)
    clock_face(d, 170, 250, 110, WHITE, 18)
    d.text((340, 170), "WorkLog", font=font(92), fill=WHITE)
    d.text((345, 290), "今日何時間働いた？", font=font(48), fill=TEAL_CONTAINER)
    img.save(os.path.join(HERE, "feature-graphic-1024x500.png"))


if __name__ == "__main__":
    os.makedirs(SHOTS, exist_ok=True)
    icon_512()
    feature_graphic()
    shot_home_off()
    shot_home_working()
    shot_home_break()
    shot_history()
    shot_summary()
    print("Wrote store assets to", HERE)
