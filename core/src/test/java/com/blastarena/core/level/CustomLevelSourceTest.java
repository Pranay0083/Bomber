package com.blastarena.core.level;

import static org.assertj.core.api.Assertions.assertThat;

import com.blastarena.core.bot.BotController;
import com.blastarena.core.bot.Difficulty;
import com.blastarena.core.control.Controller;
import com.blastarena.core.engine.GameEngine;
import com.blastarena.core.engine.GameWorld;
import com.blastarena.core.event.SynchronousEventPublisher;
import com.blastarena.core.model.GameConfig;
import com.blastarena.core.model.PlayerId;
import com.blastarena.core.model.Position;
import com.blastarena.core.powerup.PowerUpType;
import com.blastarena.core.testing.AsciiBoard;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CustomLevelSourceTest {

    private static Arena arena(LevelData level, long seed) {
        return new CustomLevelSource(level).createArena(GameConfig.builder().seed(seed).build());
    }

    private static LevelData withDensity(double density) {
        LevelData small = Levels.small();
        return new LevelData(1, small.name(), small.width(), small.height(), small.rows(), small.powerUps(), density);
    }

    @Test
    void wallsCratesAndSpawnsBecomeTheBoard() {
        Arena arena = arena(withDensity(0.0), 1L);

        assertThat(AsciiBoard.render(arena.board())).containsExactly(
                "#########",
                "#...x...#",
                "#.#.#.#.#",
                "#x.....x#",
                "#.#.#.#.#",
                "#...x...#",
                "#########");
        assertThat(arena.board().spawns()).containsExactly(new Position(1, 1), new Position(7, 1));
    }

    @Test
    void crateZonesFillAtTheLevelsDensity() {
        String full = String.join("", AsciiBoard.render(arena(withDensity(1.0), 1L).board()));
        String empty = String.join("", AsciiBoard.render(arena(withDensity(0.0), 1L).board()));

        // Four fixed crates plus seven zone cells.
        assertThat(full.chars().filter(c -> c == 'x').count()).isEqualTo(11);
        assertThat(empty.chars().filter(c -> c == 'x').count()).isEqualTo(4);
    }

    @Test
    void theSameSeedFillsTheZonesTheSameWay() {
        assertThat(AsciiBoard.render(arena(Levels.small(), 5L).board()))
                .isEqualTo(AsciiBoard.render(arena(Levels.small(), 5L).board()));
    }

    @Test
    void fixedPowerUpsLieOnTheFloorUnlessACrateCoversThem() {
        assertThat(arena(withDensity(0.0), 1L).powerUps()).containsExactly(Map.entry(new Position(4, 3), PowerUpType.BLAST_RANGE));
        assertThat(arena(withDensity(1.0), 1L).powerUps()).isEmpty();
    }

    @Test
    void botsCanPlayARoundOnACustomLevel() {
        GameConfig config = GameConfig.builder().countdownTicks(0).seed(3L).build();
        GameWorld world = GameWorld.forArena(config, new CustomLevelSource(Levels.small()).createArena(config), 2);
        PlayerId one = new PlayerId(1);
        PlayerId two = new PlayerId(2);
        Map<PlayerId, Controller> controllers = Map.of(
                one, BotController.of(Difficulty.HARD, one, 1L),
                two, BotController.of(Difficulty.MEDIUM, two, 2L));
        GameEngine engine = new GameEngine(world, controllers, new SynchronousEventPublisher());

        for (int i = 0; i < 2400 && !engine.isRoundOver(); i++) {
            engine.tick();
        }

        assertThat(engine.tickCount()).isPositive();
        assertThat(engine.view().players()).hasSize(2);
        assertThat(List.of(engine.view().width(), engine.view().height())).containsExactly(9, 7);
    }
}
