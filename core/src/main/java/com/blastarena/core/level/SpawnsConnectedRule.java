package com.blastarena.core.level;

import com.blastarena.core.bot.PathFinder;
import com.blastarena.core.model.Position;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Once every crate is gone, every spawn must be able to reach every other one. */
public final class SpawnsConnectedRule implements LevelRule {

    @Override
    public List<LevelProblem> check(LevelData level) {
        List<Position> spawns = level.spawns();
        if (spawns.size() < 2) {
            return List.of();
        }
        Set<Position> reachable = PathFinder.reachable(spawns.getFirst(),
                position -> level.isInside(position) && level.cellAt(position).isOpenWithoutCrates());
        List<LevelProblem> problems = new ArrayList<>();
        for (Position spawn : spawns.subList(1, spawns.size())) {
            if (!reachable.contains(spawn)) {
                problems.add(LevelProblem.at(spawn,
                        "Spawn at " + spawn.x() + "," + spawn.y() + " is walled off from the first spawn"));
            }
        }
        return problems;
    }
}
