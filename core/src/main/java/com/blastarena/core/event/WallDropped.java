package com.blastarena.core.event;

import com.blastarena.core.model.Position;

/** Sudden death put a solid wall on this tile. */
public record WallDropped(Position position) implements GameEvent {
}
