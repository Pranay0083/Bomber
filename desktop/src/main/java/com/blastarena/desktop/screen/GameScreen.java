package com.blastarena.desktop.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.blastarena.core.board.Board;
import com.blastarena.core.board.MapGenerator;
import com.blastarena.core.control.Controller;
import com.blastarena.core.control.IdleController;
import com.blastarena.core.engine.GameEngine;
import com.blastarena.core.engine.GameWorld;
import com.blastarena.core.event.SynchronousEventPublisher;
import com.blastarena.core.model.GameConfig;
import com.blastarena.core.model.PlayerId;
import com.blastarena.desktop.input.KeyboardController;
import com.blastarena.desktop.render.BoardRenderer;
import com.blastarena.desktop.render.HudRenderer;
import com.blastarena.desktop.render.Layout;
import com.blastarena.desktop.render.Palette;
import com.blastarena.desktop.render.PlayerPlacement;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Plays rounds: runs the engine on a fixed 50 ms tick and draws the world every frame.
 * Player 1 is on the keyboard. R starts a new round once one is over; Esc quits.
 */
public final class GameScreen extends ScreenAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(GameScreen.class);
    private static final int PLAYER_COUNT = 4;

    private static final PlayerId HUMAN = new PlayerId(1);

    private final FixedStepClock clock = new FixedStepClock(1f / GameConfig.TICKS_PER_SECOND);
    private final Layout layout;
    private final OrthographicCamera camera = new OrthographicCamera();
    private final Viewport viewport;
    private final BoardRenderer boardRenderer;
    private final HudRenderer hudRenderer;
    private final InputAdapter screenKeys = new InputAdapter() {
        @Override
        public boolean keyDown(int keycode) {
            if (keycode == Keys.R && engine.isRoundOver()) {
                startRound();
                return true;
            }
            if (keycode == Keys.ESCAPE) {
                Gdx.app.exit();
                return true;
            }
            return false;
        }
    };
    private GameEngine engine;
    private KeyboardController keyboard;

    public GameScreen(Layout layout) {
        this.layout = layout;
        this.viewport = new FitViewport(layout.boardWidth(), layout.totalHeight(), camera);
        this.boardRenderer = new BoardRenderer(layout);
        this.hudRenderer = new HudRenderer(layout, HUMAN);
        startRound();
    }

    private void startRound() {
        GameConfig config = GameConfig.builder()
                .width(layout.columns())
                .height(layout.rows())
                .seed(System.nanoTime())
                .build();
        Board board = new MapGenerator().generate(config, new Random(config.seed()));
        GameWorld world = GameWorld.withPlayersOnSpawns(config, board, PLAYER_COUNT);

        keyboard = new KeyboardController(HUMAN);
        Map<PlayerId, Controller> controllers = new HashMap<>();
        controllers.put(HUMAN, keyboard);
        for (int id = 2; id <= PLAYER_COUNT; id++) {
            controllers.put(new PlayerId(id), new IdleController(new PlayerId(id)));
        }
        Gdx.input.setInputProcessor(new InputMultiplexer(screenKeys, keyboard));
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
        ScreenUtils.clear(Palette.BACKGROUND);
        viewport.apply();
        boardRenderer.draw(engine.view(), PlayerPlacement.onTiles(), camera.combined);
        hudRenderer.draw(engine.view(), camera.combined);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        boardRenderer.dispose();
        hudRenderer.dispose();
    }
}
