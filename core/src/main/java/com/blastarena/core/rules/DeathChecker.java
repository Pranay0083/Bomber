package com.blastarena.core.rules;

import com.blastarena.core.engine.GameWorld;
import com.blastarena.core.entity.Player;
import java.util.List;

/** Finds the living players standing in fire. Fire is fire: a bomb's owner is not safe from it. */
public final class DeathChecker {

    /** Players to kill this tick, in player-id order. */
    public List<Player> playersInFire(GameWorld world) {
        return world.livingPlayers().stream()
                .filter(player -> world.isBurning(player.position()))
                .toList();
    }
}
