package com.blastarena.core.event;

import com.blastarena.core.model.PlayerId;
import com.blastarena.core.model.Position;
import com.blastarena.core.powerup.PowerUpType;

public record PowerUpCollected(PlayerId player, PowerUpType type, Position position) implements GameEvent {
}
