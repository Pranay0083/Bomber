package com.blastarena.core.event;

import com.blastarena.core.model.PlayerId;
import com.blastarena.core.model.Position;

public record PlayerMoved(PlayerId player, Position from, Position to) implements GameEvent {
}
