package com.blastarena.desktop.render;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.blastarena.core.control.IdleController;
import com.blastarena.core.control.PlayerSnapshot;
import com.blastarena.core.engine.GameEngine;
import com.blastarena.core.engine.GameWorld;
import com.blastarena.core.entity.Player;
import com.blastarena.core.event.SynchronousEventPublisher;
import com.blastarena.core.board.Board;
import com.blastarena.core.command.MoveCommand;
import com.blastarena.core.model.Direction;
import com.blastarena.core.model.GameConfig;
import com.blastarena.core.model.PlayerId;
import com.blastarena.core.model.Position;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AnimationListenerTest {

    private static final PlayerId ONE = new PlayerId(1);
    private static final PlayerId TWO = new PlayerId(2);

    private GameEngine engine;
    private AnimationListener animation;

    @BeforeEach
    void setUp() {
        GameConfig config = GameConfig.builder().countdownTicks(0).build();
        Board board = new Board(9, 3, List.of());
        GameWorld world = new GameWorld(config, board,
                List.of(new Player(ONE, new Position(0, 1)), new Player(TWO, new Position(8, 1))));
        SynchronousEventPublisher publisher = new SynchronousEventPublisher();
        engine = new GameEngine(world,
                Map.of(ONE, view -> new MoveCommand(ONE, Direction.RIGHT), TWO, new IdleController(TWO)),
                publisher);
        animation = new AnimationListener(engine.view());
        publisher.subscribe(animation);
    }

    private float drawnX(float alpha) {
        PlayerSnapshot one = engine.view().player(ONE).orElseThrow();
        return animation.placement(alpha).tileCoordinates(one)[0];
    }

    @Test
    void aMoveSlidesAcrossTheMoveDelay() {
        engine.tick();

        assertThat(drawnX(0f)).isZero();
        assertThat(drawnX(0.5f)).isCloseTo(0.1f, within(0.001f));
        engine.tick();
        engine.tick();
        assertThat(drawnX(0.5f)).isCloseTo(0.5f, within(0.001f));
    }

    @Test
    void walkingSteadilyNeverStopsOrJumps() {
        float previous = 0f;
        for (int tick = 0; tick < 20; tick++) {
            engine.tick();
            for (float alpha = 0f; alpha < 1f; alpha += 0.25f) {
                float x = drawnX(alpha);
                assertThat(x).isGreaterThanOrEqualTo(previous).isLessThanOrEqualTo(previous + 0.06f);
                previous = x;
            }
        }
        // Moves land on ticks 1, 6, 11 and 16; three-quarters into tick 20 the fourth slide is 95% done.
        assertThat(previous).isCloseTo(3.95f, within(0.001f));
    }

    @Test
    void playersThatHaveNotMovedAreDrawnOnTheirTile() {
        engine.tick();

        PlayerSnapshot two = engine.view().player(TWO).orElseThrow();
        assertThat(animation.placement(0.5f).tileCoordinates(two)).containsExactly(8f, 1f);
    }
}
