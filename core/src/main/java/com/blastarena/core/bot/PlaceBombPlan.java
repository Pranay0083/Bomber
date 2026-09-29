package com.blastarena.core.bot;

import com.blastarena.core.command.Command;
import com.blastarena.core.command.PlaceBombCommand;
import com.blastarena.core.control.PlayerSnapshot;

/** Drop one bomb, then think again (which normally means running). */
public final class PlaceBombPlan implements Plan {

    private boolean placed;

    @Override
    public Command next(PlayerSnapshot me) {
        placed = true;
        return new PlaceBombCommand(me.id());
    }

    @Override
    public boolean isFinished(PlayerSnapshot me) {
        return placed;
    }

    @Override
    public boolean isStillSafe(BotContext context) {
        return true;
    }
}
