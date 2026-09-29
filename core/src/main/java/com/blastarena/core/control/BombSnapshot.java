package com.blastarena.core.control;

import com.blastarena.core.model.PlayerId;
import com.blastarena.core.model.Position;

/** A read-only copy of a bomb's state at the moment it was taken. */
public record BombSnapshot(PlayerId owner, Position position, int range, int remainingFuse) {
}
