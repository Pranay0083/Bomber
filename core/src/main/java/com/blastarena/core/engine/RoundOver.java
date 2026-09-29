package com.blastarena.core.engine;

import com.blastarena.core.event.RoundEnded;
import java.util.Objects;

/** The round has a result. Nothing moves any more. */
public record RoundOver(RoundEnded result) implements GamePhase {

    public RoundOver {
        Objects.requireNonNull(result, "result");
    }

    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    public GamePhase next(PhaseContext context) {
        return this;
    }
}
