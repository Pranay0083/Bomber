package com.blastarena.core.level;

import java.util.ArrayList;
import java.util.List;

/** Runs every registered rule and collects what they find. Adding a check never changes this class. */
public final class LevelValidator {

    private final List<LevelRule> rules;

    public LevelValidator(List<LevelRule> rules) {
        this.rules = List.copyOf(rules);
    }

    public static LevelValidator standard() {
        return new LevelValidator(List.of(
                new SizeRule(),
                new SolidBorderRule(),
                new SpawnCountRule(),
                new SpawnExitsRule(),
                new SpawnsConnectedRule(),
                new PowerUpPlacementRule()));
    }

    public List<LevelProblem> validate(LevelData level) {
        List<LevelProblem> problems = new ArrayList<>();
        for (LevelRule rule : rules) {
            problems.addAll(rule.check(level));
        }
        return problems;
    }

    public boolean isPlayable(LevelData level) {
        return validate(level).isEmpty();
    }
}
