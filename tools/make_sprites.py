#!/usr/bin/env python3
"""Draws Blast Arena's pixel-art sprites into desktop/src/main/resources/sprites.png.

Every sprite is 16x16 and the sheet is 8 sprites wide. The order here must match
com.blastarena.desktop.render.Sprite. Standard library only, and deterministic,
so running it again gives the same file.
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


def fire(frame):
    """Four frames: a bright flash, a full blaze, dying down, embers."""
    rng = random.Random(100 + frame)
    radius = (5.5, 8.5, 7.5, 5.0)[frame]
    alpha = (255, 255, 220, 150)[frame]
    colours = [rgba("ffffff", alpha), rgba("fde047", alpha), rgba("f97316", alpha), rgba("dc2626", alpha)]
    if frame >= 2:
        colours = colours[1:] + [rgba("7f1d1d", alpha)]
    pixels = blank()
    for y in range(SIZE):
        for x in range(SIZE):
            distance = math.hypot(x + 0.5 - 8, y + 0.5 - 8) + rng.uniform(-1.2, 1.2)
            if distance <= radius:
                band = min(len(colours) - 1, int(distance / radius * len(colours)))
                pixels[y][x] = colours[band]
    return pixels


def player():
    """Drawn in white and greys so the game can tint it to each player's colour."""
    pixels = blank()
    disc(pixels, 8, 9, 6.2, rgba("9ca3af"))
    disc(pixels, 8, 8.4, 5.6, rgba("ffffff"))
    disc(pixels, 6.2, 6.4, 1.5, rgba("f3f4f6"))
    for x, y in ((6, 8), (6, 9), (10, 8), (10, 9)):
        pixels[y][x] = rgba("111827")
    rect(pixels, 4, 14, 6, 15, rgba("4b5563"))
    rect(pixels, 10, 14, 12, 15, rgba("4b5563"))
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
    floor(1), floor(2), wall(), crate(),
    bomb(0), bomb(1), fire(0), fire(1),
    fire(2), fire(3), player(), power_up_bomb(),
    power_up_range(), power_up_speed(), warning(), spawn_marker(),
    crate_zone(),
]


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
    for index, sprite in enumerate(SPRITES):
        left, top = (index % COLUMNS) * SIZE, (index // COLUMNS) * SIZE
        for y in range(SIZE):
            for x in range(SIZE):
                sheet[top + y][left + x] = sprite[y][x]
    write_png(OUT, COLUMNS * SIZE, rows * SIZE, sheet)
    print(f"Wrote {len(SPRITES)} sprites to {OUT}")


if __name__ == "__main__":
    main()
