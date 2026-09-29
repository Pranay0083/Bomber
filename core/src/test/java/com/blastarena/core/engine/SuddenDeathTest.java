package com.blastarena.core.engine;

import static org.assertj.core.api.Assertions.assertThat;

import com.blastarena.core.board.SolidWall;
import com.blastarena.core.control.IdleController;
import com.blastarena.core.event.GameEvent;
import com.blastarena.core.event.PlayerDied;
import com.blastarena.core.event.RoundEnded;
import com.blastarena.core.event.SynchronousEventPublisher;
import com.blastarena.core.event.WallDropped;
import com.blastarena.core.model.GameConfig;
import com.blastarena.core.model.PlayerId;
import com.blastarena.core.model.Position;
import com.blastarena.core.testing.AsciiWorld;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SuddenDeathTest {

    private static final PlayerId ONE = new PlayerId(1);
    private static final PlayerId TWO = new PlayerId(2);

    private final List<GameEvent> events = new ArrayList<>();

    /** Sudden death starts straight after the countdown-free first tick, with a wall every 2 ticks. */
    private GameEngine engine(String... rows) {
        GameConfig config = GameConfig.builder()
                .countdownTicks(0).roundLengthTicks(1).suddenDeathIntervalTicks(2).build();
        GameWorld world = AsciiWorld.parse(config, rows);
        SynchronousEventPublisher publisher = new SynchronousEventPublisher();
        publisher.subscribe(events::add);
        return new GameEngine(world, Map.of(ONE, new IdleController(ONE), TWO, new IdleController(TWO)), publisher);
    }

    @Test
    void theSpiralRunsClockwiseFromTheOutsideIn() {
        assertThat(WallSpiral.order(5, 5)).containsExactly(
                new Position(1, 1), new Position(2, 1), new Position(3, 1),
                new Position(3, 2), new Position(3, 3), new Position(2, 3),
                new Position(1, 3), new Position(1, 2), new Position(2, 2));
        assertThat(WallSpiral.order(13, 11)).hasSize(11 * 9).doesNotHaveDuplicates();
    }

    @Test
    void wallsDropEveryIntervalAndSkipTilesThatAreAlreadyWalls() {
        GameEngine engine = engine(
                "#######",
                "#.1...#",
                "#.#.#.#",
                "#...2.#",
                "#######");

        engine.tick();
        assertThat(engine.phase()).isInstanceOf(SuddenDeath.class);
        assertThat(engine.view().ticksUntilNextWall()).isEqualTo(1);
        engine.tick();
        assertThat(events).contains(new WallDropped(new Position(1, 1)));
        assertThat(engine.view().ticksUntilNextWall()).isEqualTo(2);
        engine.tick();
        engine.tick();

        assertThat(events).filteredOn(WallDropped.class::isInstance)
                .containsExactly(new WallDropped(new Position(1, 1)), new WallDropped(new Position(2, 1)));
        assertThat(engine.view().upcomingWalls()).startsWith(new Position(3, 1), new Position(4, 1));
        assertThat(engine.view().upcomingWalls()).doesNotContain(new Position(2, 2));
    }

    @Test
    void aWallCrushesThePlayerUnderItAndEndsTheRound() {
        GameEngine engine = engine(
                "#######",
                "#.1...#",
                "#.#.#.#",
                "#...2.#",
                "#######");

        for (int i = 0; i < 4; i++) {
            engine.tick();
        }

        assertThat(events).contains(new PlayerDied(ONE, new Position(2, 1)));
        assertThat(engine.phase()).isEqualTo(new RoundOver(RoundEnded.won(TWO)));
    }

    @Test
    void aBombUnderAWallIsGoneAndItsOwnerGetsItBack() {
        GameEngine engine = engine(
                "#####",
                "#B..#",
                "#.#.#",
                "#1.2#",
                "#####");

        engine.tick();
        engine.tick();

        assertThat(engine.view().tileAt(new Position(1, 1))).isInstanceOf(SolidWall.class);
        assertThat(engine.view().bombs()).isEmpty();
        assertThat(engine.view().player(ONE).orElseThrow().activeBombs()).isZero();
    }

    @Test
    void theArenaEventuallyFillsSoEveryRoundEnds() {
        GameEngine engine = engine(
                "#######",
                "#1....#",
                "#.#.#.#",
                "#....2#",
                "#######");

        while (!engine.isRoundOver() && engine.tickCount() < 1000) {
            engine.tick();
        }

        assertThat(engine.isRoundOver()).isTrue();
    }

    @Test
    void theViewCountsDownToTheFirstWallBeforeSuddenDeath() {
        GameConfig config = GameConfig.builder().countdownTicks(0).roundLengthTicks(10).build();
        GameWorld world = AsciiWorld.parse(config, "#####", "#1.2#", "#####");
        GameEngine engine = new GameEngine(world,
                Map.of(ONE, new IdleController(ONE), TWO, new IdleController(TWO)), new SynchronousEventPublisher());

        engine.tick();
        int expected = engine.view().ticksUntilNextWall();
        for (int i = 0; i < expected - 1; i++) {
            engine.tick();
        }
        assertThat(engine.view().tileAt(new Position(1, 1))).isNotInstanceOf(SolidWall.class);
        engine.tick();
        assertThat(engine.isRoundOver()).isTrue();
    }
}
