package com.blastarena.core.level;

import java.util.List;

/** Two to four spawn points: one per player. */
public final class SpawnCountRule implements LevelRule {

    public static final int MIN_SPAWNS = 2;
    public static final int MAX_SPAWNS = 4;

    @Override
    public List<LevelProblem> check(LevelData level) {
        int spawns = level.spawns().size();
        if (spawns < MIN_SPAWNS || spawns > MAX_SPAWNS) {
            return List.of(LevelProblem.of(
                    "A level needs " + MIN_SPAWNS + " to " + MAX_SPAWNS + " spawn points, not " + spawns));
        }
        return List.of();
    }
}
