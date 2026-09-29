package com.blastarena.desktop.screen;

/**
 * Turns variable frame times into a whole number of fixed game ticks.
 * Leftover time carries over to the next frame, and {@link #alpha()} says how far we are into the next tick,
 * so drawing can interpolate between ticks.
 */
public final class FixedStepClock {

    /** A long pause (dragging the window, a breakpoint) should not make the game race to catch up. */
    private static final float MAX_FRAME_SECONDS = 0.25f;

    private final float tickSeconds;
    private float accumulator;

    public FixedStepClock(float tickSeconds) {
        if (tickSeconds <= 0) {
            throw new IllegalArgumentException("Tick length must be positive, was " + tickSeconds);
        }
        this.tickSeconds = tickSeconds;
    }

    /** Adds a frame's elapsed time and returns how many ticks to run now. */
    public int advance(float frameSeconds) {
        accumulator += Math.min(Math.max(frameSeconds, 0f), MAX_FRAME_SECONDS);
        int ticks = 0;
        while (accumulator >= tickSeconds) {
            accumulator -= tickSeconds;
            ticks++;
        }
        return ticks;
    }

    /** How far into the next tick we are, from 0 up to (but not including) 1. */
    public float alpha() {
        return accumulator / tickSeconds;
    }

    public void reset() {
        accumulator = 0f;
    }
}
