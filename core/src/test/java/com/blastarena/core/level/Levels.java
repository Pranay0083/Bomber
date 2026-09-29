package com.blastarena.core.level;

import com.blastarena.core.powerup.PowerUpType;
import java.util.List;

/** Ready-made levels for tests. */
final class Levels {

    private Levels() {
    }

    /** A valid 9x7 level with two spawns, a crate zone and a fixed power-up. */
    static LevelData small() {
        return new LevelData(1, "Small", 9, 7, List.of(
                "#########",
                "#S..x..S#",
                "#.#?#?#.#",
                "#x.???.x#",
                "#.#?#?#.#",
                "#...x...#",
                "#########"),
                List.of(new PlacedPowerUp(4, 3, PowerUpType.BLAST_RANGE)),
                0.5);
    }
}
