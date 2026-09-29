package com.blastarena.desktop;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.blastarena.core.engine.Match;
import com.blastarena.core.level.EditableLevel;
import com.blastarena.core.level.LevelRepository;
import com.blastarena.core.level.MapSource;
import com.blastarena.desktop.render.Screenshots;
import com.blastarena.desktop.screen.EditorScreen;
import com.blastarena.desktop.screen.GameScreen;
import com.blastarena.desktop.screen.MatchSettings;
import com.blastarena.desktop.screen.MenuScreen;
import com.blastarena.desktop.screen.Navigator;
import com.blastarena.desktop.screen.ResultScreen;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The application: owns the level storage and moves between the menu, the editor and the game. */
public class BlastArenaGame extends Game implements Navigator {

    private static final Logger LOG = LoggerFactory.getLogger(BlastArenaGame.class);

    private final LevelRepository repository;
    private final Consumer<Navigator> firstScreen;
    private final String screenshotPath;
    private final float screenshotAfterSeconds;
    private float elapsedSeconds;
    /** Screens kept alive while another is shown, such as the editor during a test play. */
    private Screen parked;
    private MatchSettings settings;
    private final boolean autoplay;
    private final float speed;

    /**
     * @param firstScreen    opens the first screen, normally the menu
     * @param screenshotPath if not null, save a screenshot there after {@code screenshotAfterSeconds} and quit.
     *                       Lets you check the drawing without a person at the keyboard.
     */
    public BlastArenaGame(LevelRepository repository, MatchSettings settings, boolean autoplay, float speed,
                          Consumer<Navigator> firstScreen, String screenshotPath, float screenshotAfterSeconds) {
        this.speed = speed;
        this.repository = repository;
        this.settings = settings;
        this.autoplay = autoplay;
        this.firstScreen = firstScreen;
        this.screenshotPath = screenshotPath;
        this.screenshotAfterSeconds = screenshotAfterSeconds;
    }

    @Override
    public void create() {
        LOG.info("Blast Arena started");
        firstScreen.accept(this);
    }

    @Override
    public void showMenu() {
        switchTo(new MenuScreen(this, repository), false);
    }

    @Override
    public MatchSettings settings() {
        return settings;
    }

    @Override
    public void changeSettings(MatchSettings newSettings) {
        this.settings = newSettings;
    }

    @Override
    public void play(MapSource mapSource, MatchSettings matchSettings, Runnable onExit) {
        boolean fromEditor = getScreen() instanceof EditorScreen;
        switchTo(new GameScreen(this, mapSource, matchSettings, onExit, autoplay, speed), fromEditor);
    }

    @Override
    public void showResults(Match match, MapSource mapSource, MatchSettings matchSettings, Runnable onExit) {
        switchTo(new ResultScreen(this, match, mapSource, matchSettings, onExit), false);
        if (autoplay) {
            LOG.info("Match over: {} won, scores {}", match.winner().orElseThrow(), match.scores());
        }
    }

    @Override
    public void edit(EditableLevel level, String savedName) {
        switchTo(new EditorScreen(this, repository, level, savedName), false);
    }

    @Override
    public void resume(Screen screen) {
        Screen leaving = getScreen();
        parked = null;
        setScreen(screen);
        if (leaving != null && leaving != screen) {
            leaving.dispose();
        }
    }

    /** Shows the next screen and disposes the current one, unless it should be kept to come back to. */
    private void switchTo(Screen next, boolean keepCurrent) {
        Screen leaving = getScreen();
        setScreen(next);
        if (leaving == null) {
            return;
        }
        if (keepCurrent) {
            parked = leaving;
        } else {
            leaving.dispose();
            if (parked != null && parked != next) {
                parked.dispose();
                parked = null;
            }
        }
    }

    @Override
    public void render() {
        super.render();
        if (screenshotPath == null) {
            return;
        }
        elapsedSeconds += Gdx.graphics.getDeltaTime();
        if (elapsedSeconds >= screenshotAfterSeconds) {
            Screenshots.save(screenshotPath);
            LOG.info("Saved screenshot to {}", screenshotPath);
            Gdx.app.exit();
        }
    }

    @Override
    public void dispose() {
        if (getScreen() != null) {
            getScreen().dispose();
        }
        if (parked != null) {
            parked.dispose();
        }
    }
}
