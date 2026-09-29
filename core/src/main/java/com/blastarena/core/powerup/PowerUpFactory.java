package com.blastarena.core.powerup;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

/**
 * Decides whether a destroyed crate drops a power-up, and which one. Every kind is equally likely.
 * All randomness comes from the injected {@link Random}, so a seeded factory always rolls the same drops.
 */
public final class PowerUpFactory {

    private final Random random;
    private final double dropChance;
    private final List<PowerUpType> types;

    public PowerUpFactory(Random random, double dropChance) {
        this(random, dropChance, List.of(PowerUpType.values()));
    }

    public PowerUpFactory(Random random, double dropChance, List<PowerUpType> types) {
        if (!(dropChance >= 0.0 && dropChance <= 1.0)) {
            throw new IllegalArgumentException("Drop chance must be between 0 and 1, was " + dropChance);
        }
        if (types.isEmpty()) {
            throw new IllegalArgumentException("At least one power-up type is needed");
        }
        this.random = Objects.requireNonNull(random, "random");
        this.dropChance = dropChance;
        this.types = List.copyOf(types);
    }

    /** Rolls for one crate: empty most of the time, otherwise a random power-up. */
    public Optional<PowerUp> rollDrop() {
        if (random.nextDouble() >= dropChance) {
            return Optional.empty();
        }
        return Optional.of(types.get(random.nextInt(types.size())).create());
    }
}
