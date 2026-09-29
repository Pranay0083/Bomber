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
import com.blastarena.core.bot.BotController;
import com.blastarena.core.bot.Difficulty;
import com.blastarena.core.control.Controller;
import com.blastarena.core.engine.GameEngine;
import com.blastarena.core.engine.GameWorld;
import com.blastarena.core.event.SynchronousEventPublisher;
import com.blastarena.core.level.Arena;
import com.blastarena.core.level.MapSource;
import com.blastarena.core.model.GameConfig;
import com.blastarena.core.model.PlayerId;
import com.blastarena.desktop.input.KeyboardController;
import com.blastarena.desktop.render.AnimationListener;
import com.blastarena.desktop.render.BoardRenderer;
import com.blastarena.desktop.render.HudRenderer;
import com.blastarena.desktop.render.Layout;
import com.blastarena.desktop.render.Palette;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Plays rounds on a map: runs the engine on a fixed 50 ms tick and draws the world every frame.
 * Player 1 is on the keyboard against Medium bots, one per remaining spawn up to three.
 * R starts a new round once one is over; Esc leaves.
 */
public final class GameScreen extends ScreenAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(GameScreen.class);
    private static final int MAX_PLAYERS = 4;
    private static final PlayerId HUMAN = new PlayerId(1);

    private final MapSource mapSource;
    private final Runnable onExit;
    private final FixedStepClock clock = new FixedStepClock(1f / GameConfig.TICKS_PER_SECOND);
    private final OrthographicCamera camera = new OrthographicCamera();
    private final InputAdapter screenKeys = new InputAdapter() {
        @Override
        public boolean keyDown(int keycode) {
            if (keycode == Keys.R && engine.isRoundOver()) {
                startRound();
                return true;
            }
            if (keycode == Keys.ESCAPE) {
                onExit.run();
                return true;
            }
            return false;
        }
    };
    private Layout layout;
    private Viewport viewport;
    private BoardRenderer boardRenderer;
    private HudRenderer hudRenderer;
    private GameEngine engine;
    private KeyboardController keyboard;
    private AnimationListener animation;

    public GameScreen(MapSource mapSource, Runnable onExit) {
        this.mapSource = mapSource;
        this.onExit = onExit;
        startRound();
    }

    private void startRound() {
        GameConfig config = GameConfig.builder().seed(System.nanoTime()).build();
        Arena arena = mapSource.createArena(config);
        useLayoutFor(arena);
        int players = Math.min(MAX_PLAYERS, arena.board().spawns().size());
        GameWorld world = GameWorld.forArena(config, arena, players);

        keyboard = new KeyboardController(HUMAN);
        Map<PlayerId, Controller> controllers = new HashMap<>();
        controllers.put(HUMAN, keyboard);
        for (int id = 2; id <= players; id++) {
            PlayerId bot = new PlayerId(id);
            controllers.put(bot, BotController.of(Difficulty.MEDIUM, bot, config.seed() + id));
        }
        Gdx.input.setInputProcessor(new InputMultiplexer(screenKeys, keyboard));
        SynchronousEventPublisher publisher = new SynchronousEventPublisher();
        engine = new GameEngine(world, controllers, publisher);
        animation = new AnimationListener(engine.view());
        publisher.subscribe(animation);
        clock.reset();
        LOG.info("New round, seed {}", config.seed());
    }

    /** Custom levels can be any size, so the drawing is set up for each board. */
    private void useLayoutFor(Arena arena) {
        Layout wanted = Layout.forBoard(arena.board().width(), arena.board().height());
        if (wanted.equals(layout)) {
            return;
        }
        disposeRenderers();
        layout = wanted;
        viewport = new FitViewport(layout.boardWidth(), layout.totalHeight(), camera);
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
        boardRenderer = new BoardRenderer(layout);
        hudRenderer = new HudRenderer(layout, HUMAN);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(new InputMultiplexer(screenKeys, keyboard));
    }

    @Override
    public void render(float delta) {
        int ticks = clock.advance(delta);
        for (int i = 0; i < ticks; i++) {
            engine.tick();
        }
        ScreenUtils.clear(Palette.BACKGROUND);
        viewport.apply();
        boardRenderer.draw(engine.view(), animation.placement(clock.alpha()), camera.combined);
        hudRenderer.draw(engine.view(), camera.combined);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        disposeRenderers();
    }

    private void disposeRenderers() {
        if (boardRenderer != null) {
            boardRenderer.dispose();
            hudRenderer.dispose();
        }
    }
}
