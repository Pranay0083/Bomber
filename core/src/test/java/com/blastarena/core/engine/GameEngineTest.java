package com.blastarena.core.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.blastarena.core.command.IdleCommand;
import com.blastarena.core.command.MoveCommand;
import com.blastarena.core.control.BombSnapshot;
import com.blastarena.core.control.Controller;
import com.blastarena.core.control.IdleController;
import com.blastarena.core.control.PlayerSnapshot;
import com.blastarena.core.event.BombExploded;
import com.blastarena.core.event.BombPlaced;
import com.blastarena.core.event.CrateDestroyed;
import com.blastarena.core.event.EventPublisher;
import com.blastarena.core.event.GameEvent;
import com.blastarena.core.event.PlayerDied;
import com.blastarena.core.event.PlayerMoved;
import com.blastarena.core.event.RoundEnded;
import com.blastarena.core.event.SynchronousEventPublisher;
import com.blastarena.core.model.Direction;
import com.blastarena.core.model.GameConfig;
import com.blastarena.core.model.PlayerId;
import com.blastarena.core.model.Position;
import com.blastarena.core.testing.AsciiWorld;
import com.blastarena.core.testing.ScriptedController;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GameEngineTest {

    private static final PlayerId ONE = new PlayerId(1);
    private static final PlayerId TWO = new PlayerId(2);
    private static final GameConfig NO_COUNTDOWN = GameConfig.builder().countdownTicks(0).build();

    private final List<GameEvent> events = new ArrayList<>();

    private GameEngine engine(GameWorld world, Map<PlayerId, Controller> controllers) {
        SynchronousEventPublisher publisher = new SynchronousEventPublisher();
        publisher.subscribe(events::add);
        return new GameEngine(world, controllers, publisher);
    }

    private static void runUntilOver(GameEngine engine, int maxTicks) {
        while (!engine.isRoundOver() && engine.tickCount() < maxTicks) {
            engine.tick();
        }
    }

    @Test
    void aScriptedRoundPlaysFromStartToFinish() {
        GameWorld world = AsciiWorld.parse(NO_COUNTDOWN,
                "...",
                "1.2",
                "...");
        ScriptedController one = new ScriptedController(ONE)
                .move(Direction.RIGHT, 1)
                .bomb()
                .move(Direction.UP, 10)
                .move(Direction.RIGHT, 10);
        GameEngine engine = engine(world, Map.of(ONE, one, TWO, new IdleController(TWO)));

        runUntilOver(engine, 500);

        assertThat(engine.isRoundOver()).isTrue();
        // The bomb lands on tick 2 and its 50-tick fuse runs out on tick 51.
        assertThat(engine.tickCount()).isEqualTo(51);
        assertThat(events).containsSubsequence(
                new PlayerMoved(ONE, new Position(0, 1), new Position(1, 1)),
                new BombPlaced(ONE, new Position(1, 1)),
                new PlayerMoved(ONE, new Position(1, 1), new Position(1, 0)),
                new PlayerMoved(ONE, new Position(1, 0), new Position(2, 0)),
                new PlayerDied(TWO, new Position(2, 1)),
                RoundEnded.won(ONE));
        assertThat(events.getLast()).isEqualTo(RoundEnded.won(ONE));
        assertThat(engine.view().player(ONE)).map(PlayerSnapshot::alive).contains(true);
        assertThat(engine.phase()).isEqualTo(new RoundOver(RoundEnded.won(ONE)));
    }

    @Test
    void playersDyingInTheSameTickIsADraw() {
        GameWorld world = AsciiWorld.parse(NO_COUNTDOWN, "1B2");
        GameEngine engine = engine(world, Map.of(ONE, new IdleController(ONE), TWO, new IdleController(TWO)));

        runUntilOver(engine, 500);

        assertThat(events).filteredOn(PlayerDied.class::isInstance).hasSize(2);
        assertThat(events.getLast()).isEqualTo(RoundEnded.draw());
    }

    @Test
    void aBombExplodesOnItsFuseTickCountingTheTickItWasPlaced() {
        GameConfig config = GameConfig.builder().countdownTicks(0).fuseTicks(4).build();
        GameWorld world = AsciiWorld.parse(config, "1....2");
        GameEngine engine = engine(world, Map.of(ONE, new ScriptedController(ONE).bomb().move(Direction.RIGHT, 20),
                TWO, new IdleController(TWO)));

        for (int i = 0; i < 3; i++) {
            engine.tick();
        }
        assertThat(engine.view().bombs()).hasSize(1);
        engine.tick();
        assertThat(engine.view().bombs()).isEmpty();
        assertThat(events).filteredOn(BombExploded.class::isInstance).hasSize(1);
    }

    @Test
    void fireBurnsForExactlyTheConfiguredTicks() {
        GameConfig config = GameConfig.builder().countdownTicks(0).fuseTicks(1).fireTicks(3).build();
        GameWorld world = AsciiWorld.parse(config, "1..B..2");
        GameEngine engine = engine(world, Map.of(ONE, new IdleController(ONE), TWO, new IdleController(TWO)));

        int burningTicks = 0;
        for (int i = 0; i < 30 && !engine.isRoundOver(); i++) {
            engine.tick();
            if (!engine.view().burningTiles().isEmpty()) {
                burningTicks++;
            }
        }

        assertThat(burningTicks).isEqualTo(3);
    }

    @Test
    void movesRunBeforeBombsSoAPlayerCanStepOntoATileAsABombLandsThere() {
        GameWorld world = AsciiWorld.parse(NO_COUNTDOWN, "1.2.");
        world.movePlayer(world.player(ONE), new Position(1, 0));
        coolDown(world);
        GameEngine engine = engine(world, Map.of(
                ONE, new ScriptedController(ONE).bomb(),
                TWO, new ScriptedController(TWO).move(Direction.LEFT, 1).idle(4).move(Direction.RIGHT, 1)));

        engine.tick();

        assertThat(engine.view().player(TWO)).map(PlayerSnapshot::position).contains(new Position(1, 0));
        assertThat(engine.view().bombAt(new Position(1, 0))).map(BombSnapshot::owner).contains(ONE);

        for (int i = 0; i < 5; i++) {
            engine.tick();
        }
        assertThat(engine.view().player(TWO)).map(PlayerSnapshot::position).contains(new Position(2, 0));
    }

    @Test
    void commandsRunInPlayerIdOrder() {
        GameWorld world = AsciiWorld.parse(NO_COUNTDOWN, ".2.1.");
        GameEngine engine = engine(world, Map.of(
                TWO, new ScriptedController(TWO).move(Direction.LEFT, 1),
                ONE, new ScriptedController(ONE).move(Direction.RIGHT, 1)));

        engine.tick();

        assertThat(events).extracting(event -> ((PlayerMoved) event).player()).containsExactly(ONE, TWO);
    }

    @Test
    void blastsDestroyCratesAndReportThem() {
        GameWorld world = AsciiWorld.parse(NO_COUNTDOWN,
                "x1x...",
                ".....2");
        GameEngine engine = engine(world, Map.of(
                ONE, new ScriptedController(ONE).bomb().move(Direction.DOWN, 1).idle(4).move(Direction.RIGHT, 20),
                TWO, new IdleController(TWO)));

        for (int i = 0; i < 60; i++) {
            engine.tick();
        }

        assertThat(events).contains(new CrateDestroyed(new Position(0, 0)), new CrateDestroyed(new Position(2, 0)));
        assertThat(engine.view().tileAt(new Position(0, 0)).isWalkable()).isTrue();
        assertThat(engine.isRoundOver()).isFalse();
    }

    @Test
    void controllersAreNotAskedDuringTheCountdown() {
        GameConfig config = GameConfig.builder().countdownTicks(3).build();
        GameWorld world = AsciiWorld.parse(config, "1.2");
        Controller one = mock(Controller.class);
        when(one.nextCommand(any())).thenReturn(new IdleCommand(ONE));
        GameEngine engine = engine(world, Map.of(ONE, one, TWO, new IdleController(TWO)));

        for (int i = 0; i < 3; i++) {
            engine.tick();
        }
        verify(one, never()).nextCommand(any());
        assertThat(engine.phase()).isInstanceOf(Playing.class);

        engine.tick();
        verify(one, times(1)).nextCommand(any());
    }

    @Test
    void aCommandForAnotherPlayerIsIgnored() {
        GameWorld world = AsciiWorld.parse(NO_COUNTDOWN, "1.2.");
        Controller cheater = view -> new MoveCommand(TWO, Direction.RIGHT);
        GameEngine engine = engine(world, Map.of(ONE, cheater, TWO, new IdleController(TWO)));

        engine.tick();

        assertThat(engine.view().player(TWO)).map(PlayerSnapshot::position).contains(new Position(2, 0));
        assertThat(events).isEmpty();
    }

    @Test
    void publishesThroughTheInjectedPublisherAndStopsOnceTheRoundIsOver() {
        GameWorld world = AsciiWorld.parse(NO_COUNTDOWN, "1B2");
        EventPublisher publisher = mock(EventPublisher.class);
        GameEngine engine = new GameEngine(world, Map.of(ONE, new IdleController(ONE), TWO, new IdleController(TWO)), publisher);

        runUntilOver(engine, 500);
        long ticks = engine.tickCount();
        engine.tick();

        verify(publisher).publish(RoundEnded.draw());
        assertThat(engine.tickCount()).isEqualTo(ticks);
    }

    @Test
    void everyPlayerNeedsAController() {
        GameWorld world = AsciiWorld.parse(NO_COUNTDOWN, "1.2");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> engine(world, Map.of(ONE, new IdleController(ONE))));
    }

    @Test
    void theViewHandsOutCopiesNotLiveObjects() {
        GameWorld world = AsciiWorld.parse(NO_COUNTDOWN, "1.2");
        GameEngine engine = engine(world, Map.of(ONE, new ScriptedController(ONE).move(Direction.RIGHT, 1),
                TWO, new IdleController(TWO)));
        PlayerSnapshot before = engine.view().player(ONE).orElseThrow();

        engine.tick();

        assertThat(before.position()).isEqualTo(new Position(0, 0));
        assertThat(engine.view().player(ONE)).map(PlayerSnapshot::position).contains(new Position(1, 0));
    }

    private static void coolDown(GameWorld world) {
        for (int i = 0; i < 10; i++) {
            world.tickCooldowns();
        }
    }
}
