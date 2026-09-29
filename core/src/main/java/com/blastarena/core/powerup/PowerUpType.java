package com.blastarena.core.powerup;

import java.util.function.Supplier;

/** The kinds of power-up, used for drops, level files and drawing. Each knows how to create its power-up. */
public enum PowerUpType {
    EXTRA_BOMB(ExtraBombPowerUp::new),
    BLAST_RANGE(BlastRangePowerUp::new),
    SPEED(SpeedPowerUp::new);

    private final Supplier<PowerUp> constructor;

    PowerUpType(Supplier<PowerUp> constructor) {
        this.constructor = constructor;
    }

    public PowerUp create() {
        return constructor.get();
    }
}
