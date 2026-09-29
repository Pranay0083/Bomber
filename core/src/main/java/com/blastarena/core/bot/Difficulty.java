package com.blastarena.core.bot;

import java.util.function.Supplier;

/** The three bot levels and how quickly each reacts. */
public enum Difficulty {
    EASY(8, EasyBot::new),
    MEDIUM(4, MediumBot::new),
    HARD(1, HardBot::new);

    private final int reactionDelayTicks;
    private final Supplier<BotStrategy> strategy;

    Difficulty(int reactionDelayTicks, Supplier<BotStrategy> strategy) {
        this.reactionDelayTicks = reactionDelayTicks;
        this.strategy = strategy;
    }

    public int reactionDelayTicks() {
        return reactionDelayTicks;
    }

    public BotStrategy newStrategy() {
        return strategy.get();
    }
}
