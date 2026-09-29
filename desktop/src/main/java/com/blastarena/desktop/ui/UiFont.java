package com.blastarena.desktop.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;

/**
 * The game's 5x7 pixel font (see tools/make_font.py), drawn at whole-number scales with nearest filtering
 * so every letter stays crisp.
 */
public final class UiFont {

    /** Small print: hints and details. 14 pixels tall. */
    public static final float SMALL = 2f;
    /** Normal text: list entries, labels. 21 pixels tall. */
    public static final float NORMAL = 3f;
    /** Headings and banners. */
    public static final float LARGE = 5f;
    /** The game title. */
    public static final float TITLE = 9f;

    private UiFont() {
    }

    public static BitmapFont create() {
        BitmapFont font = new BitmapFont(Gdx.files.classpath("font.fnt"));
        font.getRegion().getTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        font.setUseIntegerPositions(true);
        return font;
    }
}
