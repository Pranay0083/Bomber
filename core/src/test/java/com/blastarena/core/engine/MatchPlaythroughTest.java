package com.blastarena.core.engine;

import static org.assertj.core.api.Assertions.assertThat;

import com.blastarena.core.board.MapGenerator;
import com.blastarena.core.bot.BotController;
import com.blastarena.core.bot.Difficulty;
import com.blastarena.core.control.Controller;
import com.blastarena.core.event.RoundEnded;
import com.blastarena.core.event.SynchronousEventPublisher;
import com.blastarena.core.level.RandomMapSource;
import com.blastarena.core.model.GameConfig;
import com.blastarena.core.model.PlayerId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** A whole best-of-3 match between bots, round after round, the way the desktop client plays one. */
class MatchPlaythroughTest {

    /** Sudden-death walls arrive in Phase 10; until then a round that runs this long is called a draw. */
    private static final int ROUND_TICK_LIMIT = 4000;
    private static final int MAX_ROUNDS = 15;

    @Test
    void aBestOfThreeMatchPlaysToAWinner() {
        List<PlayerId> players = List.of(new PlayerId(1), new PlayerId(2), new PlayerId(3));
        Match match = new Match(3, players);
        RandomMapSource maps = new RandomMapSource(new MapGenerator());

        for (long seed = 1; !match.isOver() && match.roundsPlayed() < MAX_ROUNDS; seed++) {
            GameConfig config = GameConfig.builder().seed(seed).build();
            GameWorld world = GameWorld.forArena(config, maps.createArena(config), players.size());
            Map<PlayerId, Controller> controllers = new HashMap<>();
            controllers.put(players.get(0), BotController.of(Difficulty.HARD, players.get(0), seed));
            controllers.put(players.get(1), BotController.of(Difficulty.EASY, players.get(1), seed + 1));
            controllers.put(players.get(2), BotController.of(Difficulty.EASY, players.get(2), seed + 2));
            GameEngine engine = new GameEngine(world, controllers, new SynchronousEventPublisher());
            while (!engine.isRoundOver() && engine.tickCount() < ROUND_TICK_LIMIT) {
                engine.tick();
            }
            match.recordRound(engine.phase() instanceof RoundOver over ? over.result() : RoundEnded.draw());
        }

        assertThat(match.isOver()).isTrue();
        assertThat(match.scores().get(match.winner().orElseThrow())).isEqualTo(2);
    }
}
