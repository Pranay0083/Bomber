package com.blastarena.core.model;

/**
 * A player's upgradeable stats. Immutable: the {@code with...} methods return new copies.
 * Caps are enforced by the power-ups that raise these values, not here.
 *
 * @param bombCapacity how many bombs the player may have on the board at once
 * @param blastRange   how many tiles a blast reaches in each direction
 * @param speedLevel   0 is base speed; each level shortens the delay between tile moves
 */
public record PlayerStats(int bombCapacity, int blastRange, int speedLevel) {

    public static final PlayerStats DEFAULT = new PlayerStats(1, 1, 0);

    public PlayerStats {
        if (bombCapacity < 1) {
            throw new IllegalArgumentException("Bomb capacity must be at least 1, was " + bombCapacity);
        }
        if (blastRange < 1) {
            throw new IllegalArgumentException("Blast range must be at least 1, was " + blastRange);
        }
        if (speedLevel < 0) {
            throw new IllegalArgumentException("Speed level must not be negative, was " + speedLevel);
        }
    }

    public PlayerStats withBombCapacity(int newBombCapacity) {
        return new PlayerStats(newBombCapacity, blastRange, speedLevel);
    }

    public PlayerStats withRange(int newBlastRange) {
        return new PlayerStats(bombCapacity, newBlastRange, speedLevel);
    }

    public PlayerStats withSpeedLevel(int newSpeedLevel) {
        return new PlayerStats(bombCapacity, blastRange, newSpeedLevel);
    }
}
