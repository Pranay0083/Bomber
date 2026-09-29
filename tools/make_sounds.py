#!/usr/bin/env python3
"""Synthesises Blast Arena's sound effects into desktop/src/main/resources/sounds/.

16-bit mono WAV at 22 050 Hz. Standard library only, and seeded, so running it again gives the same files.
"""
import math
import random
import struct
import wave
from pathlib import Path

RATE = 22050
OUT = Path(__file__).resolve().parent.parent / "desktop/src/main/resources/sounds"


def envelope(t, length, attack=0.005, curve=3.0):
    if t < attack:
        return t / attack
    return max(0.0, 1.0 - (t - attack) / (length - attack)) ** curve


def render(length, sample):
    return [sample(i / RATE) for i in range(int(length * RATE))]


def square(frequency, t):
    return 1.0 if math.sin(2 * math.pi * frequency * t) >= 0 else -1.0


def place():
    length = 0.12
    return render(length, lambda t: 0.5 * square(220 - 500 * t, t) * envelope(t, length, curve=2))


def explosion():
    rng = random.Random(7)
    length = 0.75
    low = [0.0]

    def sample(t):
        # Low-passed noise for the roar plus a falling sine for the thump.
        low[0] += (rng.uniform(-1, 1) - low[0]) * (0.35 - 0.3 * t / length)
        thump = math.sin(2 * math.pi * (90 - 60 * t) * t)
        return (0.9 * low[0] * 2.2 + 0.6 * thump) * envelope(t, length, attack=0.002, curve=2.5)

    return render(length, sample)


def pickup():
    notes = [523.25, 659.25, 783.99, 1046.5]
    step = 0.06
    length = step * len(notes) + 0.08

    def sample(t):
        note = notes[min(len(notes) - 1, int(t / step))]
        return 0.35 * square(note, t) * envelope(t, length, curve=1.5)

    return render(length, sample)


def death():
    length = 0.6
    return render(length, lambda t: 0.45 * square(440 * (1 - t / length * 0.7), t)
                  * envelope(t, length, curve=1.2) * (0.6 + 0.4 * math.sin(2 * math.pi * 12 * t)))


def wall():
    rng = random.Random(11)
    length = 0.25
    return render(length, lambda t: (0.7 * math.sin(2 * math.pi * (70 - 80 * t) * t) + 0.3 * rng.uniform(-1, 1))
                  * envelope(t, length, attack=0.001, curve=4))


def win():
    notes = [(523.25, 0.12), (659.25, 0.12), (783.99, 0.12), (1046.5, 0.35)]
    samples = []
    for frequency, length in notes:
        samples += render(length, lambda t, f=frequency, n=length: 0.35 * (square(f, t) * 0.6 + math.sin(2 * math.pi * f * t) * 0.4)
                          * envelope(t, n, curve=1.0))
    return samples


def write(name, samples):
    peak = max(1e-9, max(abs(s) for s in samples))
    scale = min(1.0, 0.9 / peak)
    with wave.open(str(OUT / f"{name}.wav"), "wb") as out:
        out.setnchannels(1)
        out.setsampwidth(2)
        out.setframerate(RATE)
        out.writeframes(b"".join(struct.pack("<h", int(max(-1.0, min(1.0, s * scale)) * 32767)) for s in samples))


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    sounds = {"place": place(), "explosion": explosion(), "pickup": pickup(),
              "death": death(), "wall": wall(), "win": win()}
    for name, samples in sounds.items():
        write(name, samples)
    print(f"Wrote {len(sounds)} sounds to {OUT}")


if __name__ == "__main__":
    main()
