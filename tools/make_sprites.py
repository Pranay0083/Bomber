#!/usr/bin/env python3
"""Draws Blast Arena's pixel-art sprites into desktop/src/main/resources/sprites.png.

Every sprite is 16x16 and the sheet is 8 sprites wide. sprites.txt lists each sprite's name and position;
the game looks sprites up by name (com.blastarena.desktop.render.Sprite) and checks every one is there.
Standard library only, and deterministic, so running it again gives the same files.
"""
import math
import random
import struct
import zlib
from pathlib import Path

SIZE = 16
COLUMNS = 8
OUT = Path(__file__).resolve().parent.parent / "desktop/src/main/resources/sprites.png"


def rgba(hex_colour, alpha=255):
    value = int(hex_colour, 16)
    return ((value >> 16) & 255, (value >> 8) & 255, value & 255, alpha)


CLEAR = (0, 0, 0, 0)


def blank():
    return [[CLEAR for _ in range(SIZE)] for _ in range(SIZE)]


def filled(colour):
    return [[colour for _ in range(SIZE)] for _ in range(SIZE)]


def disc(pixels, cx, cy, radius, colour):
    for y in range(SIZE):
        for x in range(SIZE):
            if (x + 0.5 - cx) ** 2 + (y + 0.5 - cy) ** 2 <= radius ** 2:
                pixels[y][x] = colour


def rect(pixels, x0, y0, x1, y1, colour):
    for y in range(max(0, y0), min(SIZE, y1 + 1)):
        for x in range(max(0, x0), min(SIZE, x1 + 1)):
            pixels[y][x] = colour


def floor(seed):
    rng = random.Random(seed)
    base, light, dark = rgba("3d6b4f"), rgba("47785a"), rgba("355e45")
    pixels = filled(base)
    for _ in range(14):
        x, y = rng.randrange(SIZE), rng.randrange(SIZE)
        pixels[y][x] = light if rng.random() < 0.5 else dark
    return pixels


def wall():
    pixels = filled(rgba("6b7280"))
    rect(pixels, 0, 0, 15, 1, rgba("9ca3af"))
    rect(pixels, 0, 14, 15, 15, rgba("374151"))
    rect(pixels, 0, 0, 0, 15, rgba("4b5563"))
    rect(pixels, 15, 0, 15, 15, rgba("4b5563"))
    mortar = rgba("565d6b")
    for y in (5, 10):
        rect(pixels, 1, y, 14, y, mortar)
    for x, y0, y1 in ((7, 2, 4), (4, 6, 9), (11, 6, 9), (8, 11, 13)):
        rect(pixels, x, y0, x, y1, mortar)
    return pixels


def crate():
    wood, dark, light = rgba("a16207"), rgba("713f12"), rgba("ca8a04")
    pixels = filled(wood)
    rect(pixels, 0, 0, 15, 0, dark)
    rect(pixels, 0, 15, 15, 15, dark)
    rect(pixels, 0, 0, 0, 15, dark)
    rect(pixels, 15, 0, 15, 15, dark)
    rect(pixels, 1, 1, 14, 1, light)
    for y in (5, 10):
        rect(pixels, 1, y, 14, y, dark)
    for i in range(2, 14):
        pixels[i][i] = dark
        pixels[i][15 - i] = dark
    return pixels


def bomb(frame):
    pixels = blank()
    disc(pixels, 8, 9.5, 5.6, rgba("111827"))
    disc(pixels, 6.3, 7.6, 1.6, rgba("6b7280"))
    for x, y in ((10, 4), (11, 4), (11, 3), (12, 2)):
        pixels[y][x] = rgba("d6d3d1")
    spark = rgba("fde047") if frame == 0 else rgba("f97316")
    for dx, dy in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1)) if frame == 0 else ((0, 0), (1, 1), (-1, -1), (1, -1), (-1, 1)):
        x, y = 13 + dx, 1 + dy
        if 0 <= x < SIZE and 0 <= y < SIZE:
            pixels[y][x] = spark
    return pixels


FIRE_THICKNESS = (4.0, 6.2, 5.2, 3.2)
FIRE_ALPHA = (255, 255, 235, 180)


def fire_colours(frame):
    alpha = FIRE_ALPHA[frame]
    if frame <= 1:
        return [rgba("ffffff", alpha), rgba("fde047", alpha), rgba("f97316", alpha), rgba("dc2626", alpha)]
    return [rgba("fde047", alpha), rgba("f97316", alpha), rgba("dc2626", alpha), rgba("7f1d1d", alpha)]


def fire_shape(frame, distance_of):
    """Paints fire wherever distance_of(x, y) is within the frame's thickness, in bands from a hot core out."""
    rng = random.Random(100 + frame)
    wobble = [rng.uniform(-0.9, 0.9) for _ in range(SIZE * SIZE)]
    half = FIRE_THICKNESS[frame]
    colours = fire_colours(frame)
    pixels = blank()
    for y in range(SIZE):
        for x in range(SIZE):
            distance = distance_of(x + 0.5, y + 0.5) + wobble[y * SIZE + x]
            if distance <= half:
                band = min(len(colours) - 1, int(max(0.0, distance) / half * len(colours)))
                pixels[y][x] = colours[band]
    return pixels


def fire_centre(frame):
    """Where the blast starts, or where its lines cross: arms out to all four edges round a hot core."""
    return fire_shape(frame, lambda x, y: min(abs(y - 8), abs(x - 8), math.hypot(x - 8, y - 8) * 0.55))


def fire_arm(frame):
    """A stretch of blast running left to right; turned a quarter for up and down."""
    return fire_shape(frame, lambda x, y: abs(y - 8))


def fire_end(frame):
    """The tip of a blast pointing right; turned for the other directions."""
    def distance(x, y):
        return abs(y - 8) if x <= 8 else math.hypot(x - 8, y - 8) * 1.15
    return fire_shape(frame, distance)


PLAYER_EYES = {
    "down": ((6, 8), (10, 8)),
    "left": ((4, 8), (7, 8)),
    "right": ((9, 8), (12, 8)),
    "up": (),
}


def player(facing="down", step=0):
    """Drawn in white and greys so the game can tint it to each player's colour. step 1 lifts a foot."""
    pixels = blank()
    bob = 1 if step == 1 else 0
    disc(pixels, 8, 9 - bob, 6.2, rgba("9ca3af"))
    disc(pixels, 8, 8.4 - bob, 5.6, rgba("ffffff"))
    if facing == "up":
        disc(pixels, 8, 9.6 - bob, 3.4, rgba("e5e7eb"))
    else:
        disc(pixels, 6.2, 6.4 - bob, 1.5, rgba("f3f4f6"))
    for x, y in PLAYER_EYES[facing]:
        for dy in (0, 1):
            pixels[y + dy - bob][x] = rgba("111827")
    left_foot, right_foot = (14, 15) if step == 0 else (13, 15)
    rect(pixels, 4, left_foot, 6, 15, rgba("4b5563"))
    rect(pixels, 10, 14 if step == 0 else 13, 12, 15 if step == 0 else 14, rgba("4b5563"))
    return pixels


def shadow():
    pixels = blank()
    for y in range(SIZE):
        for x in range(SIZE):
            if ((x + 0.5 - 8) / 6.5) ** 2 + ((y + 0.5 - 14) / 2.2) ** 2 <= 1:
                pixels[y][x] = (0, 0, 0, 80)
    return pixels


def floor_shadow():
    """The shade a wall or crate casts on the floor just below it."""
    pixels = blank()
    for y, alpha in enumerate((95, 70, 45, 20)):
        for x in range(SIZE):
            pixels[y][x] = (0, 0, 0, alpha)
    return pixels


def poof(frame):
    """A puff of smoke where a player was knocked out."""
    rng = random.Random(300 + frame)
    radius = (3.0, 5.0, 6.5)[frame]
    alpha = (240, 190, 110)[frame]
    pixels = blank()
    for i in range(7):
        angle = i / 7 * 2 * math.pi + rng.uniform(-0.3, 0.3)
        cx, cy = 8 + math.cos(angle) * radius, 8 + math.sin(angle) * radius
        disc(pixels, cx, cy, 2.4 - frame * 0.5, rgba("f3f4f6", alpha))
    disc(pixels, 8, 8, max(0.0, 2.5 - frame * 1.2), rgba("ffffff", alpha))
    return pixels


def debris(frame):
    """Splinters flying out of a broken crate."""
    rng = random.Random(400)
    spread = (2.0, 4.5, 6.5)[frame]
    alpha = (255, 220, 140)[frame]
    pixels = blank()
    for _ in range(9):
        angle = rng.uniform(0, 2 * math.pi)
        distance = spread * rng.uniform(0.6, 1.0)
        x, y = int(8 + math.cos(angle) * distance), int(8 + math.sin(angle) * distance + frame)
        colour = rgba("a16207", alpha) if rng.random() < 0.6 else rgba("713f12", alpha)
        rect(pixels, x, y, x + 1, y, colour)
    return pixels


def power_up_base():
    pixels = blank()
    rect(pixels, 2, 2, 13, 13, rgba("1f2937"))
    rect(pixels, 2, 2, 13, 2, rgba("374151"))
    return pixels


def power_up_bomb():
    pixels = power_up_base()
    disc(pixels, 8, 9, 3.6, rgba("a78bfa"))
    disc(pixels, 6.8, 7.8, 1.0, rgba("ede9fe"))
    rect(pixels, 9, 4, 10, 5, rgba("fde047"))
    return pixels


def flame(pixels, cx, bottom, height, width, colour):
    """A teardrop: round at the bottom, narrowing to a point at the top."""
    radius = width / 2
    centre_y = bottom - radius
    tip = bottom - height
    for y in range(SIZE):
        for x in range(SIZE):
            px, py = x + 0.5, y + 0.5
            if py >= centre_y:
                inside = (px - cx) ** 2 + (py - centre_y) ** 2 <= radius ** 2
            else:
                half = radius * (py - tip) / (centre_y - tip)
                inside = py >= tip and abs(px - cx) <= half
            if inside:
                pixels[y][x] = colour


def power_up_range():
    pixels = power_up_base()
    flame(pixels, 8, 12.5, 9, 7, rgba("ef4444"))
    flame(pixels, 8, 12.5, 6, 4.4, rgba("fb923c"))
    flame(pixels, 8, 12.5, 3.5, 2.2, rgba("fde047"))
    return pixels


def power_up_speed():
    pixels = power_up_base()
    bolt = [
        ".....####",
        "....####.",
        "...####..",
        "..######.",
        "....###..",
        "...###...",
        "..###....",
        ".##......",
    ]
    for row, line in enumerate(bolt):
        for column, char in enumerate(line):
            if char == "#":
                pixels[4 + row][3 + column] = rgba("22d3ee")
    return pixels


def warning():
    pixels = blank()
    for y in range(SIZE):
        for x in range(SIZE):
            if (x + y) % 6 < 3:
                pixels[y][x] = rgba("ef4444", 150)
    return pixels


def spawn_marker():
    pixels = blank()
    for y in range(SIZE):
        for x in range(SIZE):
            distance = math.hypot(x + 0.5 - 8, y + 0.5 - 8)
            if 4.2 <= distance <= 5.6:
                pixels[y][x] = rgba("e5e7eb")
    return pixels


def crate_zone():
    pixels = blank()
    colour = rgba("ca8a04", 170)
    for i in range(2, 14):
        pixels[i][i] = colour
        pixels[i][15 - i] = colour
    for i in range(2, 14):
        if i % 2 == 0:
            for x, y in ((i, 2), (i, 13), (2, i), (13, i)):
                pixels[y][x] = colour
    return pixels


SPRITES = [
    ("FLOOR_A", floor(1)), ("FLOOR_B", floor(2)), ("WALL", wall()), ("CRATE", crate()),
    ("BOMB_0", bomb(0)), ("BOMB_1", bomb(1)),
    ("POWER_UP_BOMB", power_up_bomb()), ("POWER_UP_RANGE", power_up_range()), ("POWER_UP_SPEED", power_up_speed()),
    ("WARNING", warning()), ("SPAWN_MARKER", spawn_marker()), ("CRATE_ZONE", crate_zone()),
    ("SHADOW", shadow()), ("FLOOR_SHADOW", floor_shadow()),
]
SPRITES += [(f"FIRE_CENTRE_{f}", fire_centre(f)) for f in range(4)]
SPRITES += [(f"FIRE_ARM_{f}", fire_arm(f)) for f in range(4)]
SPRITES += [(f"FIRE_END_{f}", fire_end(f)) for f in range(4)]
SPRITES += [(f"PLAYER_{facing.upper()}_{step}", player(facing, step))
            for facing in ("down", "up", "left", "right") for step in (0, 1)]
SPRITES += [(f"POOF_{f}", poof(f)) for f in range(3)]
SPRITES += [(f"DEBRIS_{f}", debris(f)) for f in range(3)]


def write_png(path, width, height, pixels):
    raw = b"".join(b"\x00" + bytes(channel for pixel in row for channel in pixel) for row in pixels)

    def chunk(kind, data):
        body = kind + data
        return struct.pack(">I", len(data)) + body + struct.pack(">I", zlib.crc32(body) & 0xFFFFFFFF)

    header = struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)
    path.write_bytes(b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", header)
                     + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))


def main():
    rows = (len(SPRITES) + COLUMNS - 1) // COLUMNS
    sheet = [[CLEAR for _ in range(COLUMNS * SIZE)] for _ in range(rows * SIZE)]
    index_lines = []
    for index, (name, sprite) in enumerate(SPRITES):
        left, top = (index % COLUMNS) * SIZE, (index // COLUMNS) * SIZE
        index_lines.append(f"{name} {left} {top} {SIZE} {SIZE}")
        for y in range(SIZE):
            for x in range(SIZE):
                sheet[top + y][left + x] = sprite[y][x]
    write_png(OUT, COLUMNS * SIZE, rows * SIZE, sheet)
    OUT.with_suffix(".txt").write_text("\n".join(index_lines) + "\n")
    print(f"Wrote {len(SPRITES)} sprites to {OUT}")


if __name__ == "__main__":
    main()
