package com.blastarena.desktop.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
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
            Color fill = !button.enabled() ? Palette.BUTTON_DISABLED
                    : button.selected() ? Palette.BUTTON_SELECTED : Palette.BUTTON;
            Color border = button.selected() ? Palette.BUTTON_SELECTED_EDGE : Palette.BUTTON_EDGE;
            Ui.panel(shapes, button.x(), button.y(), button.width(), button.height(), fill, border);
        }
    }

    /** Labels at the small text size, or {@code scale} if given. */
    public void drawLabels(SpriteBatch batch, BitmapFont font) {
        drawLabels(batch, font, UiFont.SMALL);
    }

    public void drawLabels(SpriteBatch batch, BitmapFont font, float scale) {
        for (Button button : buttons) {
            Ui.text(batch, font, button.label(), scale, button.enabled() ? Palette.TEXT : Palette.TEXT_DIM,
                    button.x() + button.width() / 2, button.y() + button.height() / 2, Ui.Align.CENTRE);
        }
    }
}
