package com.blastarena.core.level;

import com.blastarena.core.model.Position;
import java.util.ArrayList;
import java.util.List;

/**
 * Each spawn needs at least two open neighbours at the start, so nobody is trapped when the round begins.
 * Crate zones count as blocked, since they may be filled.
 */
public final class SpawnExitsRule implements LevelRule {

    public static final int MIN_EXITS = 2;

    @Override
    public List<LevelProblem> check(LevelData level) {
        List<LevelProblem> problems = new ArrayList<>();
        for (Position spawn : level.spawns()) {
            long exits = spawn.neighbours().stream()
                    .filter(level::isInside)
                    .filter(next -> level.cellAt(next).isOpenAtStart())
                    .count();
            if (exits < MIN_EXITS) {
                problems.add(LevelProblem.at(spawn,
                        "Spawn at " + spawn.x() + "," + spawn.y() + " needs " + MIN_EXITS + " open neighbours"));
            }
        }
        return problems;
    }
}
