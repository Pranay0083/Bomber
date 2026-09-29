package com.blastarena.core.entity;

import com.blastarena.core.model.Position;
import com.blastarena.core.powerup.PowerUp;
import java.util.Objects;

/** A power-up lying on a floor tile, waiting to be picked up or burned. */
public record PowerUpDrop(Position position, PowerUp powerUp) {

    public PowerUpDrop {
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(powerUp, "powerUp");
    }
}
