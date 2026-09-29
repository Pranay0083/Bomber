package com.blastarena.core.bot;

import com.blastarena.core.control.PlayerSnapshot;

/** Always flees, hunts crates and power-ups, and bombs opponents that come close. */
public final class MediumBot extends AbstractBotStrategy {

    @Override
    protected boolean shouldAttack(BotContext context, PlayerSnapshot opponent, DangerMap withMyBomb) {
        return withMyBomb.isThreatened(opponent.position())
                && opponent.position().manhattanDistanceTo(context.me().position()) <= 2;
    }

    @Override
    protected int collectRange() {
        return 8;
    }
}
