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
import com.blastarena.desktop.audio.SoundEffects;
import com.blastarena.desktop.audio.SoundListener;
import com.blastarena.core.bot.Difficulty;
import com.blastarena.core.control.Controller;
import com.blastarena.core.engine.GameEngine;
import com.blastarena.core.engine.GameWorld;
import com.blastarena.core.engine.Match;
import com.blastarena.core.engine.RoundOver;
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
import com.blastarena.desktop.render.MatchHud;
import com.blastarena.desktop.render.Palette;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Plays a best-of-N match on a map: player 1 on the keyboard against bots, one round after another.
 * Runs the engine on a fixed 50 ms tick and draws every frame.
 *
 * <p>P or Esc pauses; while paused R restarts the round and Q leaves. M mutes the sound. After a round, Enter starts the next one,
 * and once the match is decided Enter shows the results.
 */
public final class GameScreen extends ScreenAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(GameScreen.class);
    private static final int MAX_PLAYERS = 4;
    /** With autoplay on, how long a round result stays up before the next round starts by itself. */
    private static final float AUTOPLAY_PAUSE_SECONDS = 2.5f;
    private static final PlayerId HUMAN = new PlayerId(1);

    private final Navigator navigator;
    private final MapSource mapSource;
    private final MatchSettings settings;
    private final Runnable onExit;
    private final SoundEffects sounds;
    private final boolean autoplay;
    private final float speed;
    private final FixedStepClock clock = new FixedStepClock(1f / GameConfig.TICKS_PER_SECOND);
    private final OrthographicCamera camera = new OrthographicCamera();
    private final InputAdapter screenKeys = new InputAdapter() {
        @Override
        public boolean keyDown(int keycode) {
            return handleKey(keycode);
        }
    };
    private Match match;
    private Layout layout;
    private Viewport viewport;
    private BoardRenderer boardRenderer;
    private HudRenderer hudRenderer;
    private GameEngine engine;
    private KeyboardController keyboard;
    private AnimationListener animation;
    private boolean roundRecorded;
    private boolean paused;
    private float secondsSinceRoundOver;

    /**
     * @param autoplay put a Hard bot in player 1's seat and move on between rounds by itself,
     *                 so a whole match can be watched or checked without anyone at the keyboard
     * @param speed    how many times faster than real time the game runs; 1 for normal play
     */
    public GameScreen(Navigator navigator, MapSource mapSource, MatchSettings settings, Runnable onExit,
                      SoundEffects sounds, boolean autoplay, float speed) {
        this.sounds = sounds;
        this.navigator = navigator;
        this.mapSource = mapSource;
        this.settings = settings;
        this.onExit = onExit;
        this.autoplay = autoplay;
        this.speed = speed;
        startRound();
    }

    private void startRound() {
        GameConfig config = GameConfig.builder().seed(System.nanoTime()).build();
        Arena arena = mapSource.createArena(config);
        useLayoutFor(arena);
        int players = Math.min(Math.min(MAX_PLAYERS, settings.bots() + 1), arena.board().spawns().size());
        GameWorld world = GameWorld.forArena(config, arena, players);
        if (match == null) {
            List<PlayerId> ids = new ArrayList<>();
            for (int id = 1; id <= players; id++) {
                ids.add(new PlayerId(id));
            }
            match = new Match(settings.bestOf(), ids);
        }

        keyboard = new KeyboardController(HUMAN);
        Map<PlayerId, Controller> controllers = new HashMap<>();
        controllers.put(HUMAN, autoplay ? BotController.of(Difficulty.HARD, HUMAN, config.seed()) : keyboard);
        for (int id = 2; id <= players; id++) {
            PlayerId bot = new PlayerId(id);
            controllers.put(bot, BotController.of(settings.difficulty(), bot, config.seed() + id));
        }
        Gdx.input.setInputProcessor(new InputMultiplexer(screenKeys, keyboard));
        SynchronousEventPublisher publisher = new SynchronousEventPublisher();
        engine = new GameEngine(world, controllers, publisher);
        animation = new AnimationListener(engine.view());
        publisher.subscribe(animation);
        GameEngine current = engine;
        publisher.subscribe(new SoundListener(sounds, current::tickCount));
        clock.reset();
        roundRecorded = false;
        paused = false;
        secondsSinceRoundOver = 0f;
        LOG.info("Round {} of {}, seed {}", match.currentRound(), match.bestOf(), config.seed());
    }

    private void continueAfterRound() {
        if (match.isOver()) {
            navigator.showResults(match, mapSource, settings, onExit);
        } else {
            startRound();
        }
    }

    private boolean handleKey(int keycode) {
        if (engine.isRoundOver()) {
            if (keycode == Keys.ENTER || keycode == Keys.SPACE) {
                continueAfterRound();
                return true;
            }
            if (keycode == Keys.ESCAPE || keycode == Keys.Q) {
                onExit.run();
                return true;
            }
            return false;
        }
        if (keycode == Keys.M) {
            sounds.toggleMute();
            return true;
        }
        if (keycode == Keys.P || keycode == Keys.ESCAPE) {
            paused = !paused;
            clock.reset();
            return true;
        }
        if (paused && keycode == Keys.R) {
            startRound();
            return true;
        }
        if (paused && keycode == Keys.Q) {
            onExit.run();
            return true;
        }
        // While paused, keep the keys from reaching the player.
        return paused;
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
        if (!paused) {
            int ticks = clock.advance(delta * speed);
            for (int i = 0; i < ticks; i++) {
                engine.tick();
            }
        }
        if (engine.phase() instanceof RoundOver over && !roundRecorded) {
            match.recordRound(over.result());
            roundRecorded = true;
            LOG.info("Round over: {}. Scores {}", over.result(), match.scores());
        }
        if (autoplay && roundRecorded) {
            secondsSinceRoundOver += delta * speed;
            if (secondsSinceRoundOver >= AUTOPLAY_PAUSE_SECONDS) {
                continueAfterRound();
                return;
            }
        }
        ScreenUtils.clear(Palette.BACKGROUND);
        viewport.apply();
        boardRenderer.draw(engine.view(), animation.placement(paused ? 0f : clock.alpha()), camera.combined);
        hudRenderer.draw(engine.view(),
                new MatchHud(match.currentRound(), match.bestOf(), match.scores(), paused, match.winner(),
                        sounds.isMuted()),
                camera.combined);
    }

    @Override
    public void pause() {
        // The window lost focus: stop the round rather than let the bots play on.
        if (!engine.isRoundOver()) {
            paused = true;
        }
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
