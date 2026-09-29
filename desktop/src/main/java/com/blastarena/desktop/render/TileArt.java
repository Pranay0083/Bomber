package com.blastarena.desktop.render;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.blastarena.core.powerup.PowerUpType;

/** How each kind of tile looks. Shared by the game and the editor so they always match. Needs an open batch. */
public final class TileArt {

    private TileArt() {
    }

    public static void draw(SpriteBatch batch, SpriteSheet sheet, Sprite sprite, float left, float bottom, float size) {
        batch.draw(sheet.get(sprite), left, bottom, size, size);
    }

    public static void floor(SpriteBatch batch, SpriteSheet sheet, float left, float bottom, float size, int x, int y) {
        draw(batch, sheet, (x * 7 + y * 3) % 5 == 0 ? Sprite.FLOOR_B : Sprite.FLOOR_A, left, bottom, size);
    }

    public static void wall(SpriteBatch batch, SpriteSheet sheet, float left, float bottom, float size) {
        draw(batch, sheet, Sprite.WALL, left, bottom, size);
    }

    public static void crate(SpriteBatch batch, SpriteSheet sheet, float left, float bottom, float size) {
        draw(batch, sheet, Sprite.CRATE, left, bottom, size);
    }

    public static void powerUp(SpriteBatch batch, SpriteSheet sheet, float left, float bottom, float size,
                               PowerUpType type) {
        Sprite sprite = switch (type) {
            case EXTRA_BOMB -> Sprite.POWER_UP_BOMB;
            case BLAST_RANGE -> Sprite.POWER_UP_RANGE;
            case SPEED -> Sprite.POWER_UP_SPEED;
        };
        draw(batch, sheet, sprite, left, bottom, size);
    }
}
