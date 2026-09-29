package com.blastarena.desktop.ui;

/** A clickable box with a label. {@code selected} draws it highlighted; a disabled button ignores clicks. */
public record Button(String label, float x, float y, float width, float height,
                     boolean selected, boolean enabled, Runnable action) {

    public static Button of(String label, float x, float y, float width, float height, Runnable action) {
        return new Button(label, x, y, width, height, false, true, action);
    }

    public Button selected(boolean isSelected) {
        return new Button(label, x, y, width, height, isSelected, enabled, action);
    }

    public Button enabled(boolean isEnabled) {
        return new Button(label, x, y, width, height, selected, isEnabled, action);
    }

    public boolean contains(float pointX, float pointY) {
        return pointX >= x && pointX <= x + width && pointY >= y && pointY <= y + height;
    }
}
