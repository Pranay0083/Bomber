package com.blastarena.core.control;

import com.blastarena.core.model.PlayerId;
import com.blastarena.core.model.PlayerStats;
import com.blastarena.core.model.Position;

/** A read-only copy of a player's state at the moment it was taken. */
public record PlayerSnapshot(
        PlayerId id,
        Position position,
        boolean alive,
        PlayerStats stats,
        int moveCooldown,
        int activeBombs) {
}
