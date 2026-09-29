package com.blastarena.core.command;

import com.blastarena.core.engine.GameWorld;
import com.blastarena.core.event.GameEvent;
import com.blastarena.core.rules.BombPlacer;
import com.blastarena.core.rules.MovementValidator;
import java.util.function.Consumer;

/** What a command needs to run: the world, the rules that guard it, and somewhere to report events. */
public record CommandContext(
        GameWorld world,
        MovementValidator movementValidator,
        BombPlacer bombPlacer,
        Consumer<GameEvent> events) {
}
