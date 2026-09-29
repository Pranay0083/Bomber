package com.blastarena.desktop;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.blastarena.desktop.render.Layout;
import com.blastarena.desktop.render.Screenshots;
import com.blastarena.desktop.screen.GameScreen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlastArenaGame extends Game {

    private static final Logger LOG = LoggerFactory.getLogger(BlastArenaGame.class);

    /** Frames to wait before taking the screenshot: about four seconds, past the countdown. */
    private static final int SCREENSHOT_FRAME = 240;

    private final Layout layout;
    private final String screenshotPath;
    private int frames;

    /**
     * @param screenshotPath if not null, save a screenshot there after a few seconds and quit.
     *                       Lets you check the drawing without a person at the keyboard.
     */
    public BlastArenaGame(Layout layout, String screenshotPath) {
        this.layout = layout;
        this.screenshotPath = screenshotPath;
    }

    @Override
    public void create() {
        LOG.info("Blast Arena started");
        setScreen(new GameScreen(layout));
    }

    @Override
    public void render() {
        super.render();
        if (screenshotPath != null && ++frames == SCREENSHOT_FRAME) {
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
    }
}
