package com.blastarena.core.rules;

import com.blastarena.core.engine.GameWorld;
import com.blastarena.core.entity.Player;
import com.blastarena.core.event.RoundEnded;
import java.util.List;
import java.util.Optional;

/**
 * Decides whether the round is over. With two or more players, the last one standing wins,
 * and if the last players die in the same tick it is a draw. A solo round ends only when the player dies.
 */
public final class WinConditionChecker {

    public Optional<RoundEnded> check(GameWorld world) {
        List<Player> alive = world.livingPlayers();
        boolean solo = world.players().size() == 1;
        if (alive.isEmpty()) {
            return Optional.of(RoundEnded.draw());
        }
        if (!solo && alive.size() == 1) {
            return Optional.of(RoundEnded.won(alive.getFirst().id()));
        }
        return Optional.empty();
    }
}
