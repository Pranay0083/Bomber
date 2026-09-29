package com.blastarena.desktop.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.blastarena.core.powerup.PowerUpType;

/** How each kind of tile looks. Shared by the game and the editor so they always match. Needs a filled batch. */
public final class TileArt {

    private TileArt() {
    }

    public static void floor(ShapeRenderer shapes, float left, float bottom, float size, int x, int y) {
        shapes.setColor((x + y) % 2 == 0 ? Palette.FLOOR : Palette.FLOOR_ALT);
        shapes.rect(left, bottom, size, size);
    }

    public static void wall(ShapeRenderer shapes, float left, float bottom, float size) {
        float edge = size * 0.05f;
        shapes.setColor(Palette.WALL_DARK);
        shapes.rect(left, bottom, size, size);
        shapes.setColor(Palette.WALL);
        shapes.rect(left + edge, bottom + edge * 2, size - edge * 2, size - edge * 3);
        shapes.setColor(Palette.WALL_LIGHT);
        shapes.rect(left + edge, bottom + size - edge * 3, size - edge * 2, edge * 2);
    }

    public static void crate(ShapeRenderer shapes, float left, float bottom, float size, int x, int y) {
        floor(shapes, left, bottom, size, x, y);
        float inset = size * 0.0625f;
        shapes.setColor(Palette.CRATE_DARK);
        shapes.rect(left + inset, bottom + inset, size - inset * 2, size - inset * 2);
        shapes.setColor(Palette.CRATE);
        shapes.rect(left + inset * 2, bottom + inset * 2, size - inset * 4, size - inset * 4);
        crateCross(shapes, left, bottom, size, Palette.CRATE_DARK);
    }

    /** A faint crate outline: this cell may get a crate when the level is played. */
    public static void crateZone(ShapeRenderer shapes, float left, float bottom, float size, int x, int y) {
        floor(shapes, left, bottom, size, x, y);
        crateCross(shapes, left, bottom, size, Palette.CRATE_ZONE);
    }

    private static void crateCross(ShapeRenderer shapes, float left, float bottom, float size, Color color) {
        float inset = size * 0.16f;
        float width = size * 0.08f;
        shapes.setColor(color);
        shapes.rectLine(left + inset, bottom + inset, left + size - inset, bottom + size - inset, width);
        shapes.rectLine(left + inset, bottom + size - inset, left + size - inset, bottom + inset, width);
    }

    public static void spawn(ShapeRenderer shapes, float left, float bottom, float size, int x, int y) {
        floor(shapes, left, bottom, size, x, y);
        shapes.setColor(Palette.SPAWN);
        shapes.circle(left + size / 2, bottom + size / 2, size * 0.32f, 28);
        floorDot(shapes, left, bottom, size, x, y);
    }

    private static void floorDot(ShapeRenderer shapes, float left, float bottom, float size, int x, int y) {
        shapes.setColor((x + y) % 2 == 0 ? Palette.FLOOR : Palette.FLOOR_ALT);
        shapes.circle(left + size / 2, bottom + size / 2, size * 0.24f, 28);
    }

    public static void powerUp(ShapeRenderer shapes, float left, float bottom, float size, PowerUpType type) {
        float cx = left + size / 2;
        float cy = bottom + size / 2;
        shapes.setColor(Palette.POWER_UP_BASE);
        shapes.rect(cx - size * 0.32f, cy - size * 0.32f, size * 0.64f, size * 0.64f);
        shapes.setColor(Palette.powerUp(type));
        shapes.circle(cx, cy, size * 0.24f, 24);
    }

    public static String powerUpLetter(PowerUpType type) {
        return switch (type) {
            case EXTRA_BOMB -> "B";
            case BLAST_RANGE -> "R";
            case SPEED -> "S";
        };
    }
}
