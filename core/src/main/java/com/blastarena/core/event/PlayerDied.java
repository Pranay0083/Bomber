package com.blastarena.core.event;

import com.blastarena.core.model.PlayerId;
import com.blastarena.core.model.Position;

public record PlayerDied(PlayerId player, Position position) implements GameEvent {
}
