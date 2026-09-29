package com.blastarena.core.entity;

import com.blastarena.core.model.PlayerId;
import com.blastarena.core.model.Position;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A bomb waiting to explode. Players standing on its tile when it was placed may walk off it;
 * once they leave, it blocks them like everyone else.
 */
public final class Bomb implements Tickable {

    private final PlayerId owner;
    private final Position position;
    private final int range;
    private final Set<PlayerId> mayWalkOff;
    private int remainingFuse;

    public Bomb(PlayerId owner, Position position, int range, int fuseTicks, Set<PlayerId> standingOnIt) {
        if (range < 1) {
            throw new IllegalArgumentException("Range must be at least 1, was " + range);
        }
        if (fuseTicks < 1) {
            throw new IllegalArgumentException("Fuse must be at least 1 tick, was " + fuseTicks);
        }
        this.owner = Objects.requireNonNull(owner, "owner");
        this.position = Objects.requireNonNull(position, "position");
        this.range = range;
        this.remainingFuse = fuseTicks;
        this.mayWalkOff = new HashSet<>(standingOnIt);
    }

    public PlayerId owner() {
        return owner;
    }

    public Position position() {
        return position;
    }

    public int range() {
        return range;
    }

    public int remainingFuse() {
        return remainingFuse;
    }

    public boolean isDue() {
        return remainingFuse == 0;
    }

    @Override
    public void tick() {
        if (remainingFuse > 0) {
            remainingFuse--;
        }
    }

    /** Whether this player may be on the bomb's tile, which is only true until they first step off it. */
    public boolean mayBeOccupiedBy(PlayerId player) {
        return mayWalkOff.contains(player);
    }

    public void playerLeft(PlayerId player) {
        mayWalkOff.remove(player);
    }

    @Override
    public String toString() {
        return "Bomb[" + owner.id() + " at " + position + ", fuse " + remainingFuse + "]";
    }
}
