package com.blastarena.core.level;

import com.blastarena.core.model.GameConfig;
import java.util.ArrayList;
import java.util.List;

/** Odd sizes from 9x7 to 21x17, so the checkerboard of pillars fits and every board is one the game can run. */
public final class SizeRule implements LevelRule {

    @Override
    public List<LevelProblem> check(LevelData level) {
        List<LevelProblem> problems = new ArrayList<>();
        if (!isOddBetween(level.width(), GameConfig.MIN_WIDTH, GameConfig.MAX_WIDTH)) {
            problems.add(LevelProblem.of("Width must be an odd number from "
                    + GameConfig.MIN_WIDTH + " to " + GameConfig.MAX_WIDTH + ", not " + level.width()));
        }
        if (!isOddBetween(level.height(), GameConfig.MIN_HEIGHT, GameConfig.MAX_HEIGHT)) {
            problems.add(LevelProblem.of("Height must be an odd number from "
                    + GameConfig.MIN_HEIGHT + " to " + GameConfig.MAX_HEIGHT + ", not " + level.height()));
        }
        return problems;
    }

    private static boolean isOddBetween(int value, int min, int max) {
        return value >= min && value <= max && value % 2 == 1;
    }
}
