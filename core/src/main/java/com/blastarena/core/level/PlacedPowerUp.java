package com.blastarena.core.level;

import com.blastarena.core.model.Position;
import com.blastarena.core.powerup.PowerUpType;
import java.util.Objects;

/** A power-up a level puts on the floor from the start. */
public record PlacedPowerUp(int x, int y, PowerUpType type) {

    public PlacedPowerUp {
        Objects.requireNonNull(type, "type");
    }

    public PlacedPowerUp(Position position, PowerUpType type) {
        this(position.x(), position.y(), type);
    }

    public Position position() {
        return new Position(x, y);
    }
}
