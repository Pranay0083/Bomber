package com.blastarena.core.engine;

import static org.assertj.core.api.Assertions.assertThat;

import com.blastarena.core.board.Board;
import com.blastarena.core.board.MapGenerator;
import com.blastarena.core.control.Controller;
import com.blastarena.core.event.GameEvent;
import com.blastarena.core.event.SynchronousEventPublisher;
import com.blastarena.core.model.GameConfig;
import com.blastarena.core.model.PlayerId;
import com.blastarena.core.testing.RandomController;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DeterminismTest {

    private record Outcome(List<GameEvent> events, List<Object> finalPlayers, long ticks) {
    }

    @ParameterizedTest
    @ValueSource(longs = {1L, 7L, 42L})
    void sameSeedAndCommandsGiveTheSameGame(long seed) {
        Outcome first = play(seed);
        Outcome second = play(seed);

        assertThat(first.events()).isNotEmpty();
        assertThat(second).isEqualTo(first);
    }

    private static Outcome play(long seed) {
        GameConfig config = GameConfig.builder().seed(seed).countdownTicks(0).build();
        Board board = new MapGenerator().generate(config, new Random(config.seed()));
        GameWorld world = GameWorld.withPlayersOnSpawns(config, board, 4);
        Map<PlayerId, Controller> controllers = new HashMap<>();
        for (int id = 1; id <= 4; id++) {
            controllers.put(new PlayerId(id), new RandomController(new PlayerId(id), seed * 31 + id));
        }
        SynchronousEventPublisher publisher = new SynchronousEventPublisher();
        List<GameEvent> events = new ArrayList<>();
        publisher.subscribe(events::add);
        GameEngine engine = new GameEngine(world, controllers, publisher);

        while (!engine.isRoundOver() && engine.tickCount() < 3000) {
            engine.tick();
        }
        return new Outcome(events, List.copyOf(engine.view().players()), engine.tickCount());
    }
}
