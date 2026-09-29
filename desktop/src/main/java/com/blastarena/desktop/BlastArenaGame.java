package com.blastarena.desktop;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.utils.ScreenUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlastArenaGame extends ApplicationAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(BlastArenaGame.class);

    @Override
    public void create() {
        LOG.info("Blast Arena started");
    }

    @Override
    public void render() {
        ScreenUtils.clear(0.12f, 0.14f, 0.18f, 1f);
    }
}
