package com.blastarena.core.event;

import com.blastarena.core.model.PlayerId;
import java.util.Optional;

/** The round is over. An empty winner means a draw. */
public record RoundEnded(Optional<PlayerId> winner) implements GameEvent {

    public static RoundEnded won(PlayerId winner) {
        return new RoundEnded(Optional.of(winner));
    }

    public static RoundEnded draw() {
        return new RoundEnded(Optional.empty());
    }

    public boolean isDraw() {
        return winner.isEmpty();
    }
}
