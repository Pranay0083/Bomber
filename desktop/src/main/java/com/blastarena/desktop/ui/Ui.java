package com.blastarena.desktop.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/** Small drawing helpers shared by the screens: text placed by its middle, drop shadows, and bordered panels. */
public final class Ui {

    public enum Align {
        LEFT,
        CENTRE,
        RIGHT
    }

    private static final Color SHADOW = new Color(0f, 0f, 0f, 0.6f);
    private static final GlyphLayout GLYPHS = new GlyphLayout();

    private Ui() {
    }

    /** Draws one line of text with its vertical middle at {@code centreY}. Returns its width. */
    public static float text(SpriteBatch batch, BitmapFont font, String text, float scale, Color colour,
                             float x, float centreY, Align align) {
        font.getData().setScale(scale);
        font.setColor(colour);
        GLYPHS.setText(font, text);
        float left = switch (align) {
            case LEFT -> x;
            case CENTRE -> x - GLYPHS.width / 2;
            case RIGHT -> x - GLYPHS.width;
        };
        font.draw(batch, GLYPHS, Math.round(left), Math.round(centreY + GLYPHS.height / 2));
        return GLYPHS.width;
    }

    /** As {@link #text}, with a dark copy one font-pixel down and to the right behind it. */
    public static float shadowText(SpriteBatch batch, BitmapFont font, String text, float scale, Color colour,
                                   float x, float centreY, Align align) {
        text(batch, font, text, scale, SHADOW, x + scale, centreY - scale, align);
        return text(batch, font, text, scale, colour, x, centreY, align);
    }

    public static float width(BitmapFont font, String text, float scale) {
        font.getData().setScale(scale);
        GLYPHS.setText(font, text);
        return GLYPHS.width;
    }

    /** A filled panel with a border, drawn inside a filled shape batch. */
    public static void panel(ShapeRenderer shapes, float x, float y, float width, float height,
                             Color fill, Color border) {
        shapes.setColor(border);
        shapes.rect(x, y, width, height);
        shapes.setColor(fill);
        shapes.rect(x + 3, y + 3, width - 6, height - 6);
    }
}
