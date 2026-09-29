package com.blastarena.core.engine;

import static org.assertj.core.api.Assertions.assertThat;

import com.blastarena.core.control.Controller;
import com.blastarena.core.control.IdleController;
import com.blastarena.core.control.PlayerSnapshot;
import com.blastarena.core.entity.Player;
import com.blastarena.core.entity.PowerUpDrop;
import com.blastarena.core.event.GameEvent;
import com.blastarena.core.event.PlayerMoved;
import com.blastarena.core.event.PowerUpBurned;
import com.blastarena.core.event.PowerUpCollected;
import com.blastarena.core.event.PowerUpDropped;
import com.blastarena.core.event.SynchronousEventPublisher;
import com.blastarena.core.model.Direction;
import com.blastarena.core.model.GameConfig;
import com.blastarena.core.model.PlayerId;
import com.blastarena.core.model.Position;
import com.blastarena.core.powerup.PowerUp;
import com.blastarena.core.powerup.PowerUpFactory;
import com.blastarena.core.powerup.PowerUpType;
import com.blastarena.core.rules.BombPlacer;
import com.blastarena.core.rules.DeathChecker;
import com.blastarena.core.rules.ExplosionResolver;
import com.blastarena.core.rules.MovementValidator;
import com.blastarena.core.rules.WinConditionChecker;
import com.blastarena.core.testing.AsciiWorld;
import com.blastarena.core.testing.ScriptedController;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

class PowerUpEngineTest {

    private static final PlayerId ONE = new PlayerId(1);
    private static final PlayerId TWO = new PlayerId(2);
    private static final GameConfig CONFIG = GameConfig.builder().countdownTicks(0).fuseTicks(1).fireTicks(3).build();

    private final List<GameEvent> events = new ArrayList<>();

    private GameEngine engine(GameWorld world, Controller one, PowerUpFactory factory) {
        Rules rules = new Rules(new MovementValidator(), new BombPlacer(), new ExplosionResolver(),
                new DeathChecker(), new WinConditionChecker(), factory);
        SynchronousEventPublisher publisher = new SynchronousEventPublisher();
        publisher.subscribe(events::add);
        return new GameEngine(world, Map.of(ONE, one, TWO, new IdleController(TWO)), publisher, rules);
    }

    private static PowerUpFactory alwaysDrops(PowerUpType type) {
        return new PowerUpFactory(new Random(1L), 1.0, List.of(type));
    }

    private static PowerUpFactory neverDrops() {
        return new PowerUpFactory(new Random(1L), 0.0);
    }

    @Test
    void aDestroyedCrateCanDropAPowerUpThatOutlastsTheFireThatRevealedIt() {
        GameWorld world = AsciiWorld.parse(CONFIG, "1.Bx..2");
        GameEngine engine = engine(world, new IdleController(ONE), alwaysDrops(PowerUpType.SPEED));

        engine.tick();

        assertThat(events).contains(new PowerUpDropped(new Position(3, 0), PowerUpType.SPEED));
        for (int i = 0; i < 10; i++) {
            engine.tick();
        }
        assertThat(engine.view().powerUpAt(new Position(3, 0))).contains(PowerUpType.SPEED);
        assertThat(events).noneMatch(PowerUpBurned.class::isInstance);
    }

    @Test
    void noDropWhenTheRollFails() {
        GameWorld world = AsciiWorld.parse(CONFIG, "1.Bx..2");
        GameEngine engine = engine(world, new IdleController(ONE), neverDrops());

        engine.tick();

        assertThat(engine.view().powerUps()).isEmpty();
    }

    @Test
    void walkingOntoAPowerUpCollectsIt() {
        GameWorld world = AsciiWorld.parse(CONFIG, "1r..2");
        GameEngine engine = engine(world, new ScriptedController(ONE).move(Direction.RIGHT, 1), neverDrops());

        engine.tick();

        assertThat(events).containsExactly(
                new PlayerMoved(ONE, new Position(0, 0), new Position(1, 0)),
                new PowerUpCollected(ONE, PowerUpType.BLAST_RANGE, new Position(1, 0)));
        assertThat(engine.view().player(ONE)).map(PlayerSnapshot::stats).map(stats -> stats.blastRange()).contains(2);
        assertThat(engine.view().powerUps()).isEmpty();
    }

    @Test
    void aBlastBurnsPowerUpsAlreadyOnTheFloor() {
        GameWorld world = AsciiWorld.parse(CONFIG, "1.Bb..2");
        GameEngine engine = engine(world, new IdleController(ONE), neverDrops());

        engine.tick();

        assertThat(events).contains(new PowerUpBurned(new Position(3, 0), PowerUpType.EXTRA_BOMB));
        assertThat(engine.view().powerUps()).isEmpty();
    }

    @Test
    void powerUpsOutOfReachOfTheBlastSurvive() {
        GameWorld world = AsciiWorld.parse(CONFIG, "1.B.s.2");
        GameEngine engine = engine(world, new IdleController(ONE), neverDrops());

        engine.tick();

        assertThat(engine.view().powerUpAt(new Position(4, 0))).contains(PowerUpType.SPEED);
    }

    @Test
    void anExtraBombPowerUpLetsThePlayerPlaceTwoBombs() {
        GameConfig config = GameConfig.builder().countdownTicks(0).build();
        GameWorld world = AsciiWorld.parse(config, "1b....2");
        Controller one = new ScriptedController(ONE)
                .move(Direction.RIGHT, 1).bomb()
                .move(Direction.RIGHT, 5).bomb();
        GameEngine engine = engine(world, one, neverDrops());

        // Move and collect on tick 1, bomb on tick 2, the next move lands on tick 6, the second bomb on tick 8.
        for (int i = 0; i < 8; i++) {
            engine.tick();
        }

        assertThat(engine.view().bombs()).extracting(bomb -> bomb.position())
                .containsExactly(new Position(1, 0), new Position(2, 0));
    }

    /** A power-up the engine has never heard of: it only needs to implement {@link PowerUp}. */
    private static final class GiantBlastPowerUp implements PowerUp {

        @Override
        public PowerUpType type() {
            return PowerUpType.BLAST_RANGE;
        }

        @Override
        public void apply(Player player) {
            player.upgrade(stats -> stats.withRange(8));
        }
    }

    @Test
    void aNewKindOfPowerUpWorksWithoutChangingTheEngine() {
        GameWorld world = AsciiWorld.parse(CONFIG, "1...2");
        world.addDrop(new PowerUpDrop(new Position(1, 0), new GiantBlastPowerUp()));
        GameEngine engine = engine(world, new ScriptedController(ONE).move(Direction.RIGHT, 1), neverDrops());

        engine.tick();

        assertThat(engine.view().player(ONE)).map(PlayerSnapshot::stats).map(stats -> stats.blastRange()).contains(8);
    }
}
