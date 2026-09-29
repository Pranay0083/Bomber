package com.blastarena.desktop.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;

/** Saves what is on screen to a PNG. */
public final class Screenshots {

    private Screenshots() {
    }

    public static void save(String path) {
        // createFromFrameBuffer already flips OpenGL's bottom-up rows into top-down image order.
        Pixmap frame = Pixmap.createFromFrameBuffer(
                0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        PixmapIO.writePNG(Gdx.files.absolute(path), frame, -1, true);
        frame.dispose();
    }
}
