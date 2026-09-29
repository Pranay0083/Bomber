package com.blastarena.desktop.ui;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.blastarena.desktop.render.Palette;
import java.util.ArrayList;
import java.util.List;

/**
 * A set of buttons, rebuilt from the screen's state every frame so they never go stale.
 * Draw the boxes inside a filled shape batch and the labels inside a sprite batch.
 */
public final class ButtonBar {

    private final List<Button> buttons = new ArrayList<>();
    private final GlyphLayout glyphs = new GlyphLayout();

    public void clear() {
        buttons.clear();
    }

    public void add(Button button) {
        buttons.add(button);
    }

    /** Runs the button under the point, if any, and returns whether one was hit. */
    public boolean click(float x, float y) {
        for (Button button : buttons) {
            if (button.contains(x, y)) {
                if (button.enabled()) {
                    button.action().run();
                }
                return true;
            }
        }
        return false;
    }

    public void drawBoxes(ShapeRenderer shapes) {
        for (Button button : buttons) {
            shapes.setColor(!button.enabled() ? Palette.BUTTON_DISABLED
                    : button.selected() ? Palette.BUTTON_SELECTED : Palette.BUTTON);
            shapes.rect(button.x(), button.y(), button.width(), button.height());
        }
    }

    public void drawLabels(SpriteBatch batch, BitmapFont font) {
        for (Button button : buttons) {
            font.setColor(button.enabled() ? Palette.TEXT : Palette.TEXT_DIM);
            glyphs.setText(font, button.label());
            font.draw(batch, glyphs, button.x() + (button.width() - glyphs.width) / 2,
                    button.y() + (button.height() + glyphs.height) / 2);
        }
    }
}
