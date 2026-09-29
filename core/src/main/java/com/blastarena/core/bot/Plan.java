package com.blastarena.core.bot;

import com.blastarena.core.command.Command;
import com.blastarena.core.control.PlayerSnapshot;

/** What a bot has decided to do; it keeps following the plan until it finishes or needs rethinking. */
public sealed interface Plan permits PlaceBombPlan, PathPlan, WaitPlan {

    Command next(PlayerSnapshot me);

    boolean isFinished(PlayerSnapshot me);

    /** Whether carrying on is still safe given what the bot sees now. */
    boolean isStillSafe(BotContext context);
}
