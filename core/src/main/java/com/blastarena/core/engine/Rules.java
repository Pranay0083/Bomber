package com.blastarena.core.engine;

import com.blastarena.core.rules.BombPlacer;
import com.blastarena.core.rules.DeathChecker;
import com.blastarena.core.rules.ExplosionResolver;
import com.blastarena.core.rules.MovementValidator;
import com.blastarena.core.rules.WinConditionChecker;

/** The rule objects the engine runs each tick, bundled so they can be injected together. */
public record Rules(
        MovementValidator movementValidator,
        BombPlacer bombPlacer,
        ExplosionResolver explosionResolver,
        DeathChecker deathChecker,
        WinConditionChecker winConditionChecker) {

    public static Rules standard() {
        return new Rules(
                new MovementValidator(),
                new BombPlacer(),
                new ExplosionResolver(),
                new DeathChecker(),
                new WinConditionChecker());
    }
}
