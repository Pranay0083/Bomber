package com.blastarena.core.level;

import static org.assertj.core.api.Assertions.assertThat;

import com.blastarena.core.board.MapGenerator;
import com.blastarena.core.engine.GameWorld;
import com.blastarena.core.model.GameConfig;
import com.blastarena.core.testing.AsciiBoard;
import java.util.Random;
import org.junit.jupiter.api.Test;

class RandomMapSourceTest {

    @Test
    void givesTheSameBoardAsTheGeneratorForTheConfigSeed() {
        GameConfig config = GameConfig.builder().seed(12L).build();

        Arena arena = new RandomMapSource(new MapGenerator()).createArena(config);

        assertThat(AsciiBoard.render(arena.board()))
                .isEqualTo(AsciiBoard.render(new MapGenerator().generate(config, new Random(12L))));
        assertThat(arena.powerUps()).isEmpty();
    }

    @Test
    void aWorldBuiltFromAnArenaPutsPlayersOnItsSpawns() {
        GameConfig config = GameConfig.builder().build();
        Arena arena = new RandomMapSource(new MapGenerator()).createArena(config);

        GameWorld world = GameWorld.forArena(config, arena, 2);

        assertThat(world.players()).extracting(player -> player.position())
                .containsExactlyElementsOf(arena.board().spawns().subList(0, 2));
    }
}
