package com.blastarena.core.engine;

import com.blastarena.core.model.GameConfig;

/**
 * The stage a round is in. Each phase decides what follows it at the end of every tick (the State pattern),
 * so the engine never switches on which phase it is in.
 */
public sealed interface GamePhase permits Countdown, Playing, SuddenDeath, RoundOver {

    /** Whether players act and bombs and fire count down during this phase. */
    boolean isRunning();

    /** The phase for the next tick. Returns {@code this} or a new phase; phases are immutable. */
    GamePhase next(PhaseContext context);

    static GamePhase initial(GameConfig config) {
        return config.countdownTicks() == 0 ? new Playing(0, config.roundLengthTicks()) : new Countdown(config);
    }
}
