package com.blastarena.desktop;

import com.badlogic.gdx.Game;
import com.blastarena.desktop.screen.GameScreen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlastArenaGame extends Game {

    private static final Logger LOG = LoggerFactory.getLogger(BlastArenaGame.class);

    @Override
    public void create() {
        LOG.info("Blast Arena started");
        setScreen(new GameScreen());
    }

    @Override
    public void dispose() {
        if (getScreen() != null) {
            getScreen().dispose();
        }
    }
}
