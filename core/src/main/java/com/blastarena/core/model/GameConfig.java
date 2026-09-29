package com.blastarena.core.model;

/**
 * Settings for one round. Create it with {@link #builder()}; every setting has a default from the game spec.
 *
 * @param width            board width in tiles; odd, so the checkerboard of walls fits
 * @param height           board height in tiles; odd, for the same reason
 * @param fuseTicks        ticks from placing a bomb until it explodes
 * @param fireTicks        ticks that fire stays on its tiles
 * @param dropChance       chance, from 0 to 1, that a destroyed crate drops a power-up
 * @param crateDensity     share, from 0 to 1, of free cells that start with a crate
 * @param roundLengthTicks ticks of play before sudden death starts
 * @param countdownTicks   ticks of countdown before play starts; 0 starts straight away
 * @param suddenDeathIntervalTicks ticks between sudden-death walls
 * @param seed             seed for every random choice, so a round can be replayed exactly
 */
public record GameConfig(
        int width,
        int height,
        int fuseTicks,
        int fireTicks,
        double dropChance,
        double crateDensity,
        int roundLengthTicks,
        int countdownTicks,
        int suddenDeathIntervalTicks,
        long seed) {

    public static final int TICKS_PER_SECOND = 20;
    public static final int MIN_WIDTH = 9;
    public static final int MAX_WIDTH = 21;
    public static final int MIN_HEIGHT = 7;
    public static final int MAX_HEIGHT = 17;

    public GameConfig {
        requireOddInRange("Width", width, MIN_WIDTH, MAX_WIDTH);
        requireOddInRange("Height", height, MIN_HEIGHT, MAX_HEIGHT);
        requirePositive("Fuse ticks", fuseTicks);
        requirePositive("Fire ticks", fireTicks);
        requirePositive("Round length ticks", roundLengthTicks);
        requirePositive("Sudden death interval ticks", suddenDeathIntervalTicks);
        if (countdownTicks < 0) {
            throw new IllegalArgumentException("Countdown ticks must not be negative, was " + countdownTicks);
        }
        requireFraction("Drop chance", dropChance);
        requireFraction("Crate density", crateDensity);
    }

    public static Builder builder() {
        return new Builder();
    }

    private static void requireOddInRange(String name, int value, int min, int max) {
        if (value < min || value > max || value % 2 == 0) {
            throw new IllegalArgumentException(
                    name + " must be an odd number from " + min + " to " + max + ", was " + value);
        }
    }

    private static void requirePositive(String name, int value) {
        if (value < 1) {
            throw new IllegalArgumentException(name + " must be positive, was " + value);
        }
    }

    private static void requireFraction(String name, double value) {
        if (!(value >= 0.0 && value <= 1.0)) {
            throw new IllegalArgumentException(name + " must be between 0 and 1, was " + value);
        }
    }

    public static final class Builder {

        private int width = 13;
        private int height = 11;
        private int fuseTicks = 50;
        private int fireTicks = 10;
        private double dropChance = 0.3;
        private double crateDensity = 0.7;
        private int roundLengthTicks = 2 * 60 * TICKS_PER_SECOND;
        private int countdownTicks = 3 * TICKS_PER_SECOND;
        private int suddenDeathIntervalTicks = 5;
        private long seed = 0L;

        private Builder() {
        }

        public Builder width(int width) {
            this.width = width;
            return this;
        }

        public Builder height(int height) {
            this.height = height;
            return this;
        }

        public Builder fuseTicks(int fuseTicks) {
            this.fuseTicks = fuseTicks;
            return this;
        }

        public Builder fireTicks(int fireTicks) {
            this.fireTicks = fireTicks;
            return this;
        }

        public Builder dropChance(double dropChance) {
            this.dropChance = dropChance;
            return this;
        }

        public Builder crateDensity(double crateDensity) {
            this.crateDensity = crateDensity;
            return this;
        }

        public Builder roundLengthTicks(int roundLengthTicks) {
            this.roundLengthTicks = roundLengthTicks;
            return this;
        }

        public Builder countdownTicks(int countdownTicks) {
            this.countdownTicks = countdownTicks;
            return this;
        }

        public Builder suddenDeathIntervalTicks(int suddenDeathIntervalTicks) {
            this.suddenDeathIntervalTicks = suddenDeathIntervalTicks;
            return this;
        }

        public Builder seed(long seed) {
            this.seed = seed;
            return this;
        }

        public GameConfig build() {
            return new GameConfig(
                    width, height, fuseTicks, fireTicks, dropChance, crateDensity, roundLengthTicks, countdownTicks,
                    suddenDeathIntervalTicks, seed);
        }
    }
}
