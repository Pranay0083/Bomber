package com.blastarena.core.powerup;

import com.blastarena.core.entity.Player;

/**
 * An upgrade a player collects by walking onto it. Each kind is its own class (the Strategy pattern),
 * so a new power-up is a new class and the engine does not change.
 */
public interface PowerUp {

    PowerUpType type();

    /** Upgrades the player, never past this power-up's cap. Collecting one at the cap does nothing. */
    void apply(Player player);
}
