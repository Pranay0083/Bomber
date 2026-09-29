package com.blastarena.core.entity;

import com.blastarena.core.model.Position;
import java.util.Set;

/** The flames of one explosion. They burn on their tiles until the countdown runs out. */
public final class Fire implements Tickable {

    private final Set<Position> tiles;
    private int remainingTicks;

    public Fire(Set<Position> tiles, int durationTicks) {
        if (tiles.isEmpty()) {
            throw new IllegalArgumentException("Fire must cover at least one tile");
        }
        if (durationTicks < 1) {
            throw new IllegalArgumentException("Fire must last at least 1 tick, was " + durationTicks);
        }
        this.tiles = Set.copyOf(tiles);
        this.remainingTicks = durationTicks;
    }

    public Set<Position> tiles() {
        return tiles;
    }

    public int remainingTicks() {
        return remainingTicks;
    }

    public boolean covers(Position position) {
        return tiles.contains(position);
    }

    public boolean isExpired() {
        return remainingTicks == 0;
    }

    @Override
    public void tick() {
        if (remainingTicks > 0) {
            remainingTicks--;
        }
    }
}
