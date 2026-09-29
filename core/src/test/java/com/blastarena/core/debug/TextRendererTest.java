package com.blastarena.core.debug;

import static org.assertj.core.api.Assertions.assertThat;

import com.blastarena.core.control.IdleController;
import com.blastarena.core.engine.GameEngine;
import com.blastarena.core.engine.GameWorld;
import com.blastarena.core.event.SynchronousEventPublisher;
import com.blastarena.core.model.GameConfig;
import com.blastarena.core.model.PlayerId;
import com.blastarena.core.testing.AsciiWorld;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TextRendererTest {

    private static final PlayerId ONE = new PlayerId(1);
    private static final PlayerId TWO = new PlayerId(2);

    private final TextRenderer renderer = new TextRenderer();

    private static GameEngine engineFor(GameConfig config, String... rows) {
        GameWorld world = AsciiWorld.parse(config, rows);
        return new GameEngine(world, Map.of(ONE, new IdleController(ONE), TWO, new IdleController(TWO)),
                new SynchronousEventPublisher());
    }

    @Test
    void drawsTilesBombsAndPlayers() {
        GameEngine engine = engineFor(GameConfig.builder().build(),
                "#####",
                "#1Bx#",
                "#..2#",
                "#####");

        assertThat(renderer.render(engine.view())).isEqualTo("""
                #####
                #1Bx#
                #..2#
                #####
                """);
    }

    @Test
    void drawsPowerUps() {
        GameEngine engine = engineFor(GameConfig.builder().build(), "1brs2");

        assertThat(renderer.render(engine.view())).isEqualTo("1brs2\n");
    }

    @Test
    void drawsFireAroundAnExplodedBomb() {
        GameEngine engine = engineFor(GameConfig.builder().countdownTicks(0).fuseTicks(1).build(),
                "1.B.2",
                ".....");

        engine.tick();

        assertThat(renderer.render(engine.view())).isEqualTo("""
                1***2
                ..*..
                """);
    }

    @Test
    void playersKilledByFireDisappear() {
        GameEngine engine = engineFor(GameConfig.builder().countdownTicks(0).fuseTicks(1).build(),
                "1B.2");

        engine.tick();

        assertThat(renderer.render(engine.view())).isEqualTo("***2\n");
    }
}
