package com.blastarena.core.engine;

import com.blastarena.core.model.GameConfig;

/** Players wait at their spawns while the countdown runs. */
public record Countdown(int ticksLeft, int roundLengthTicks) implements GamePhase {

    public Countdown(GameConfig config) {
        this(config.countdownTicks(), config.roundLengthTicks());
    }

    public Countdown {
        if (ticksLeft < 1) {
            throw new IllegalArgumentException("Countdown needs at least 1 tick left, was " + ticksLeft);
        }
    }

    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    public GamePhase next(PhaseContext context) {
        return ticksLeft == 1 ? new Playing(0, roundLengthTicks) : new Countdown(ticksLeft - 1, roundLengthTicks);
    }
}
