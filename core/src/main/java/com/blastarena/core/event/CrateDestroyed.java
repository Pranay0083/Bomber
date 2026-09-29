package com.blastarena.core.event;

import com.blastarena.core.model.Position;

public record CrateDestroyed(Position position) implements GameEvent {
}
