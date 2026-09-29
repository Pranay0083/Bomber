package com.blastarena.desktop.render;

/**
 * Where things go on screen. The core's y grows downwards from the top row;
 * LibGDX's y grows upwards from the bottom, so rows are flipped here and nowhere else.
 * The HUD strip sits above the board.
 */
public record Layout(int columns, int rows, float tileSize, float hudHeight) {

    public static final float DEFAULT_TILE_SIZE = 64f;
    public static final float DEFAULT_HUD_HEIGHT = 48f;

    public static Layout forBoard(int columns, int rows) {
        return new Layout(columns, rows, DEFAULT_TILE_SIZE, DEFAULT_HUD_HEIGHT);
    }

    public float boardWidth() {
        return columns * tileSize;
    }

    public float boardHeight() {
        return rows * tileSize;
    }

    public float totalHeight() {
        return boardHeight() + hudHeight;
    }

    /** Left edge of a (possibly fractional) column. */
    public float screenX(float column) {
        return column * tileSize;
    }

    /** Bottom edge of a (possibly fractional) row. */
    public float screenY(float row) {
        return (rows - 1 - row) * tileSize;
    }
}
