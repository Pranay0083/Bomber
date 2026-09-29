package com.blastarena.core.event;

import com.blastarena.core.model.Position;
import com.blastarena.core.powerup.PowerUpType;

/** A destroyed crate left a power-up behind. */
public record PowerUpDropped(Position position, PowerUpType type) implements GameEvent {
}
