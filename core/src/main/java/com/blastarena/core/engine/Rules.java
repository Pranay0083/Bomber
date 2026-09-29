package com.blastarena.core.engine;

import com.blastarena.core.model.GameConfig;
import com.blastarena.core.powerup.PowerUpFactory;
import com.blastarena.core.rules.BombPlacer;
import com.blastarena.core.rules.DeathChecker;
import com.blastarena.core.rules.ExplosionResolver;
import com.blastarena.core.rules.MovementValidator;
import com.blastarena.core.rules.WinConditionChecker;
import java.util.Random;

/** The rule objects the engine runs each tick, bundled so they can be injected together. */
public record Rules(
        MovementValidator movementValidator,
        BombPlacer bombPlacer,
        ExplosionResolver explosionResolver,
        DeathChecker deathChecker,
        WinConditionChecker winConditionChecker,
        PowerUpFactory powerUpFactory) {

    /** The standard rules, with power-up drops seeded from the config so a round can be replayed. */
    public static Rules standard(GameConfig config) {
        return new Rules(
                new MovementValidator(),
                new BombPlacer(),
                new ExplosionResolver(),
                new DeathChecker(),
                new WinConditionChecker(),
                new PowerUpFactory(new Random(config.seed()), config.dropChance()));
    }
}
