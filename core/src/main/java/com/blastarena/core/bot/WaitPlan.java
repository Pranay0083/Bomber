package com.blastarena.core.bot;

import com.blastarena.core.command.Command;
import com.blastarena.core.command.IdleCommand;
import com.blastarena.core.control.PlayerSnapshot;

/** Stay put rather than walk into danger; the bot looks again at its next rethink. */
public final class WaitPlan implements Plan {

    @Override
    public Command next(PlayerSnapshot me) {
        return new IdleCommand(me.id());
    }

    @Override
    public boolean isFinished(PlayerSnapshot me) {
        return false;
    }

    @Override
    public boolean isStillSafe(BotContext context) {
        return !context.danger().isDeadlyAt(context.me().position(), 1);
    }
}
