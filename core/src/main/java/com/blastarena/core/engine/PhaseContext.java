package com.blastarena.core.engine;

import com.blastarena.core.rules.WinConditionChecker;

/** What a phase may look at when deciding what comes next. */
public record PhaseContext(GameWorld world, WinConditionChecker winConditionChecker) {
}
