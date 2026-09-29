package com.blastarena.desktop.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.utils.ScreenUtils;
import com.blastarena.core.board.Board;
import com.blastarena.core.board.MapGenerator;
import com.blastarena.core.control.Controller;
import com.blastarena.core.control.IdleController;
import com.blastarena.core.engine.GameEngine;
import com.blastarena.core.engine.GameWorld;
import com.blastarena.core.event.SynchronousEventPublisher;
import com.blastarena.core.model.GameConfig;
import com.blastarena.core.model.PlayerId;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Plays one round: runs the engine on a fixed 50 ms tick and draws the world every frame. */
public final class GameScreen extends ScreenAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(GameScreen.class);
    private static final int PLAYER_COUNT = 4;

    private final FixedStepClock clock = new FixedStepClock(1f / GameConfig.TICKS_PER_SECOND);
    private GameEngine engine;

    public GameScreen() {
        startRound();
    }

    private void startRound() {
        GameConfig config = GameConfig.builder().seed(System.nanoTime()).build();
        Board board = new MapGenerator().generate(config, new Random(config.seed()));
        GameWorld world = GameWorld.withPlayersOnSpawns(config, board, PLAYER_COUNT);

        Map<PlayerId, Controller> controllers = new HashMap<>();
        for (int id = 1; id <= PLAYER_COUNT; id++) {
            controllers.put(new PlayerId(id), new IdleController(new PlayerId(id)));
        }
        engine = new GameEngine(world, controllers, new SynchronousEventPublisher());
        clock.reset();
        LOG.info("New round, seed {}", config.seed());
    }

    @Override
    public void render(float delta) {
        int ticks = clock.advance(delta);
        for (int i = 0; i < ticks; i++) {
            engine.tick();
        }
        ScreenUtils.clear(0.12f, 0.14f, 0.18f, 1f);
    }
}
