package com.blastarena.core.powerup;

import com.blastarena.core.entity.Player;

/** One more tile of blast range, up to {@value #CAP}. */
public final class BlastRangePowerUp implements PowerUp {

    public static final int CAP = 8;

    @Override
    public PowerUpType type() {
        return PowerUpType.BLAST_RANGE;
    }

    @Override
    public void apply(Player player) {
        player.upgrade(stats -> stats.withRange(Math.min(CAP, stats.blastRange() + 1)));
    }
}
