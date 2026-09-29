package com.blastarena.desktop.render;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.blastarena.core.board.Board;
import com.blastarena.core.board.Crate;
import com.blastarena.core.board.Floor;
import com.blastarena.core.board.SolidWall;
import com.blastarena.core.level.LevelData;
import com.blastarena.core.model.Position;

/** Draws a whole board or level small, fitted and centred in a box, with the same sprites as the game. */
public final class MiniMap {

    private MiniMap() {
    }

    public static void draw(SpriteBatch batch, SpriteSheet sheet, LevelData level,
                            float x, float y, float width, float height) {
        float size = tileSize(level.width(), level.height(), width, height);
        float left = x + (width - size * level.width()) / 2;
        float bottom = y + (height - size * level.height()) / 2;
        for (int row = 0; row < level.height(); row++) {
            for (int column = 0; column < level.width(); column++) {
                Position position = new Position(column, row);
                float cellLeft = left + column * size;
                float cellBottom = bottom + (level.height() - 1 - row) * size;
                switch (level.cellAt(position)) {
                    case WALL -> TileArt.wall(batch, sheet, cellLeft, cellBottom, size);
                    case CRATE -> TileArt.crate(batch, sheet, cellLeft, cellBottom, size);
                    case FLOOR -> TileArt.floor(batch, sheet, cellLeft, cellBottom, size, column, row);
                    case SPAWN -> {
                        TileArt.floor(batch, sheet, cellLeft, cellBottom, size, column, row);
                        TileArt.draw(batch, sheet, Sprite.SPAWN_MARKER, cellLeft, cellBottom, size);
                    }
                    case CRATE_ZONE -> {
                        TileArt.floor(batch, sheet, cellLeft, cellBottom, size, column, row);
                        TileArt.draw(batch, sheet, Sprite.CRATE_ZONE, cellLeft, cellBottom, size);
                    }
                }
            }
        }
        level.powerUps().forEach(powerUp -> TileArt.powerUp(batch, sheet, left + powerUp.x() * size,
                bottom + (level.height() - 1 - powerUp.y()) * size, size, powerUp.type()));
    }

    public static void draw(SpriteBatch batch, SpriteSheet sheet, Board board,
                            float x, float y, float width, float height) {
        float size = tileSize(board.width(), board.height(), width, height);
        float left = x + (width - size * board.width()) / 2;
        float bottom = y + (height - size * board.height()) / 2;
        for (int row = 0; row < board.height(); row++) {
            for (int column = 0; column < board.width(); column++) {
                float cellLeft = left + column * size;
                float cellBottom = bottom + (board.height() - 1 - row) * size;
                switch (board.tileAt(new Position(column, row))) {
                    case SolidWall wall -> TileArt.wall(batch, sheet, cellLeft, cellBottom, size);
                    case Crate crate -> TileArt.crate(batch, sheet, cellLeft, cellBottom, size);
                    case Floor floor -> TileArt.floor(batch, sheet, cellLeft, cellBottom, size, column, row);
                }
            }
        }
        for (Position spawn : board.spawns()) {
            TileArt.draw(batch, sheet, Sprite.SPAWN_MARKER, left + spawn.x() * size,
                    bottom + (board.height() - 1 - spawn.y()) * size, size);
        }
    }

    private static float tileSize(int columns, int rows, float width, float height) {
        return (float) Math.floor(Math.min(width / columns, height / rows));
    }
}
