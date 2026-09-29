package com.blastarena.core.event;

import com.blastarena.core.model.PlayerId;
import com.blastarena.core.model.Position;

public record BombPlaced(PlayerId owner, Position position) implements GameEvent {
}
