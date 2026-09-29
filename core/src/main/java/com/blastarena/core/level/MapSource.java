package com.blastarena.core.level;

import com.blastarena.core.model.GameConfig;

/**
 * Where a round's arena comes from. The engine does not care whether it was generated or built by hand.
 * Randomness, such as crates in a level's crate zones, must come from the config's seed so rounds replay exactly.
 */
@FunctionalInterface
public interface MapSource {

    Arena createArena(GameConfig config);
}
