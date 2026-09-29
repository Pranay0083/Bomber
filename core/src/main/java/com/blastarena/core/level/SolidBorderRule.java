package com.blastarena.core.level;

import com.blastarena.core.model.Position;
import java.util.ArrayList;
import java.util.List;

/** The outer edge must be solid wall all the way round. */
public final class SolidBorderRule implements LevelRule {

    @Override
    public List<LevelProblem> check(LevelData level) {
        List<LevelProblem> problems = new ArrayList<>();
        for (int y = 0; y < level.height(); y++) {
            for (int x = 0; x < level.width(); x++) {
                boolean edge = x == 0 || y == 0 || x == level.width() - 1 || y == level.height() - 1;
                Position position = new Position(x, y);
                if (edge && level.cellAt(position) != Cell.WALL) {
                    problems.add(LevelProblem.at(position, "The border must be solid wall"));
                }
            }
        }
        return problems;
    }
}
