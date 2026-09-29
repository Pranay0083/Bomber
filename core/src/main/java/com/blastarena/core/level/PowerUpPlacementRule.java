package com.blastarena.core.level;

import com.blastarena.core.model.Position;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Fixed power-ups lie on floor or in a crate zone, one per tile. In a crate zone the power-up is lost if a crate
 * lands there. Not on a spawn, where it would be picked up the moment the round starts.
 */
public final class PowerUpPlacementRule implements LevelRule {

    @Override
    public List<LevelProblem> check(LevelData level) {
        List<LevelProblem> problems = new ArrayList<>();
        Set<Position> seen = new HashSet<>();
        for (PlacedPowerUp powerUp : level.powerUps()) {
            Position position = powerUp.position();
            Cell cell = level.cellAt(position);
            if (cell != Cell.FLOOR && cell != Cell.CRATE_ZONE) {
                problems.add(LevelProblem.at(position, "A power-up must lie on floor or in a crate zone"));
            }
            if (!seen.add(position)) {
                problems.add(LevelProblem.at(position, "Only one power-up per tile"));
            }
        }
        return problems;
    }
}
