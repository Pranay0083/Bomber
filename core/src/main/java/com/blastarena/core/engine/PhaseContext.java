package com.blastarena.core.engine;

import com.blastarena.core.event.GameEvent;
import com.blastarena.core.rules.WinConditionChecker;
import java.util.function.Consumer;

/** What a phase may look at and change when deciding what comes next, and where it reports what it did. */
public record PhaseContext(GameWorld world, WinConditionChecker winConditionChecker, Consumer<GameEvent> events) {
}
