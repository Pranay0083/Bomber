package com.blastarena.core.entity;

import com.blastarena.core.model.PlayerId;
import com.blastarena.core.model.PlayerStats;
import com.blastarena.core.model.Position;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * A player on the board. Mutable, and changed only by the game world while it runs a tick.
 * Rules such as "is that tile walkable" live elsewhere; this class only guards its own invariants.
 */
public final class Player {

    private final PlayerId id;
    private Position position;
    private boolean alive = true;
    private PlayerStats stats = PlayerStats.DEFAULT;
    private int moveCooldown;
    private int activeBombs;

    public Player(PlayerId id, Position spawn) {
        this.id = Objects.requireNonNull(id, "id");
        this.position = Objects.requireNonNull(spawn, "spawn");
    }

    public PlayerId id() {
        return id;
    }

    public Position position() {
        return position;
    }

    public boolean isAlive() {
        return alive;
    }

    public PlayerStats stats() {
        return stats;
    }

    public int moveCooldown() {
        return moveCooldown;
    }

    public int activeBombs() {
        return activeBombs;
    }

    public boolean canMove() {
        return alive && moveCooldown == 0;
    }

    public boolean hasBombAvailable() {
        return alive && activeBombs < stats.bombCapacity();
    }

    /** Moves to a tile and waits {@code cooldownTicks} before the next move. */
    public void moveTo(Position target, int cooldownTicks) {
        requireAlive();
        if (moveCooldown > 0) {
            throw new IllegalStateException("Player " + id.id() + " is still cooling down");
        }
        if (cooldownTicks < 0) {
            throw new IllegalArgumentException("Cooldown must not be negative, was " + cooldownTicks);
        }
        this.position = Objects.requireNonNull(target, "target");
        this.moveCooldown = cooldownTicks;
    }

    public void tickCooldown() {
        if (moveCooldown > 0) {
            moveCooldown--;
        }
    }

    public void upgrade(UnaryOperator<PlayerStats> change) {
        this.stats = Objects.requireNonNull(change.apply(stats), "stats");
    }

    public void bombPlaced() {
        if (!hasBombAvailable()) {
            throw new IllegalStateException("Player " + id.id() + " has no bomb available");
        }
        activeBombs++;
    }

    /** Called when one of this player's bombs explodes, even if the player has died since placing it. */
    public void bombExploded() {
        if (activeBombs == 0) {
            throw new IllegalStateException("Player " + id.id() + " has no active bombs");
        }
        activeBombs--;
    }

    public void kill() {
        alive = false;
    }

    private void requireAlive() {
        if (!alive) {
            throw new IllegalStateException("Player " + id.id() + " is dead");
        }
    }

    @Override
    public String toString() {
        return "Player[" + id.id() + " at " + position + (alive ? "" : ", dead") + "]";
    }
}
