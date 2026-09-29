package com.blastarena.core.powerup;

import com.blastarena.core.entity.Player;

/** One speed level, shortening the delay between tile moves, up to level {@value #CAP}. */
public final class SpeedPowerUp implements PowerUp {

    public static final int CAP = 4;

    @Override
    public PowerUpType type() {
        return PowerUpType.SPEED;
    }

    @Override
    public void apply(Player player) {
        player.upgrade(stats -> stats.withSpeedLevel(Math.min(CAP, stats.speedLevel() + 1)));
    }
}
