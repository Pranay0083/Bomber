package com.blastarena.core.level;

import java.util.List;

/** One check a level must pass (the Specification pattern). A new check is a new class. */
@FunctionalInterface
public interface LevelRule {

    List<LevelProblem> check(LevelData level);
}
