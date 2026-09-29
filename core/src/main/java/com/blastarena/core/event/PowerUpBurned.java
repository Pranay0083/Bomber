package com.blastarena.core.event;

import com.blastarena.core.model.Position;
import com.blastarena.core.powerup.PowerUpType;

/** A blast destroyed a power-up lying on the floor. */
public record PowerUpBurned(Position position, PowerUpType type) implements GameEvent {
}
