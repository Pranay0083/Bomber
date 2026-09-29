package com.blastarena.core.powerup;

import com.blastarena.core.entity.Player;

/** One more bomb on the board at once, up to {@value #CAP}. */
public final class ExtraBombPowerUp implements PowerUp {

    public static final int CAP = 8;

    @Override
    public PowerUpType type() {
        return PowerUpType.EXTRA_BOMB;
    }

    @Override
    public void apply(Player player) {
        player.upgrade(stats -> stats.withBombCapacity(Math.min(CAP, stats.bombCapacity() + 1)));
    }
}
