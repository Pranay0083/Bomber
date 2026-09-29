package com.blastarena.core.event;

import com.blastarena.core.model.PlayerId;
import com.blastarena.core.model.Position;
import java.util.Set;

/** A bomb went off; {@code fireTiles} are the tiles its blast set alight. */
public record BombExploded(PlayerId owner, Position position, Set<Position> fireTiles) implements GameEvent {

    public BombExploded {
        fireTiles = Set.copyOf(fireTiles);
    }
}
