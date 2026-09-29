package com.blastarena.core.bot;

import java.util.Optional;

/** Slow to react, flees only some of the time, wanders about, and rarely attacks. */
public final class EasyBot extends AbstractBotStrategy {

    private static final double FLEE_CHANCE = 0.6;
    private static final double ATTACK_CHANCE = 0.15;
    private static final double WANDER_CHANCE = 0.5;

    @Override
    protected Optional<Plan> flee(BotContext context) {
        return context.random().nextDouble() < FLEE_CHANCE ? super.flee(context) : Optional.empty();
    }

    @Override
    protected Optional<Plan> attack(BotContext context) {
        return context.random().nextDouble() < ATTACK_CHANCE ? super.attack(context) : Optional.empty();
    }

    @Override
    protected int collectRange() {
        return 0;
    }

    @Override
    protected Optional<Plan> explore(BotContext context) {
        if (context.random().nextDouble() < WANDER_CHANCE) {
            return wander(context);
        }
        return super.explore(context);
    }
}
