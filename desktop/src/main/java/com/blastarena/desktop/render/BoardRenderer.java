package com.blastarena.desktop.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Disposable;
import com.blastarena.core.board.Crate;
import com.blastarena.core.board.Floor;
import com.blastarena.core.board.SolidWall;
import com.blastarena.core.control.BombSnapshot;
import com.blastarena.core.control.PlayerSnapshot;
import com.blastarena.core.control.WorldView;
import com.blastarena.core.model.Position;
import com.blastarena.core.powerup.PowerUpType;
import java.util.Map;

/** Draws the board and everything on it as coloured shapes. */
public final class BoardRenderer implements Disposable {

    private final ShapeRenderer shapes = new ShapeRenderer();
    private final SpriteBatch batch = new SpriteBatch();
    private final BitmapFont font = new BitmapFont();
    private final GlyphLayout glyphs = new GlyphLayout();
    private final Layout layout;

    public BoardRenderer(Layout layout) {
        this.layout = layout;
    }

    public void draw(WorldView view, PlayerPlacement placement, Matrix4 projection) {
        shapes.setProjectionMatrix(projection);
        batch.setProjectionMatrix(projection);

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        drawTiles(view);
        drawPowerUps(view);
        drawBombs(view);
        drawFire(view);
        drawPlayers(view, placement);
        shapes.end();

        batch.begin();
        drawPowerUpLetters(view);
        drawPlayerNumbers(view, placement);
        batch.end();
    }

    private void drawTiles(WorldView view) {
        float size = layout.tileSize();
        for (int y = 0; y < view.height(); y++) {
            for (int x = 0; x < view.width(); x++) {
                float left = layout.screenX(x);
                float bottom = layout.screenY(y);
                switch (view.tileAt(new Position(x, y))) {
                    case Floor floor -> TileArt.floor(shapes, left, bottom, size, x, y);
                    case SolidWall wall -> TileArt.wall(shapes, left, bottom, size);
                    case Crate crate -> TileArt.crate(shapes, left, bottom, size, x, y);
                }
            }
        }
    }

    private void drawPowerUps(WorldView view) {
        float size = layout.tileSize();
        for (Map.Entry<Position, PowerUpType> entry : view.powerUps().entrySet()) {
            TileArt.powerUp(shapes, layout.screenX(entry.getKey().x()), layout.screenY(entry.getKey().y()), size,
                    entry.getValue());
        }
    }

    private void drawBombs(WorldView view) {
        float size = layout.tileSize();
        for (BombSnapshot bomb : view.bombs()) {
            float cx = layout.screenX(bomb.position().x()) + size / 2;
            float cy = layout.screenY(bomb.position().y()) + size / 2;
            float pulse = 1f + 0.06f * (float) Math.sin(bomb.remainingFuse() * 0.6);
            boolean aboutToBlow = bomb.remainingFuse() <= 10 && bomb.remainingFuse() % 4 < 2;
            shapes.setColor(aboutToBlow ? Palette.BOMB_FLASH : Palette.BOMB);
            shapes.circle(cx, cy - 2, size * 0.34f * pulse, 32);
            shapes.setColor(Palette.BOMB_SHINE);
            shapes.circle(cx - size * 0.11f, cy + size * 0.1f, size * 0.07f, 16);
        }
    }

    private void drawFire(WorldView view) {
        float size = layout.tileSize();
        for (Position position : view.burningTiles()) {
            float left = layout.screenX(position.x());
            float bottom = layout.screenY(position.y());
            shapes.setColor(Palette.FIRE_OUTER);
            shapes.rect(left + 2, bottom + 2, size - 4, size - 4);
            shapes.setColor(Palette.FIRE_INNER);
            shapes.rect(left + size * 0.25f, bottom + size * 0.25f, size * 0.5f, size * 0.5f);
        }
    }

    private void drawPlayers(WorldView view, PlayerPlacement placement) {
        float size = layout.tileSize();
        for (PlayerSnapshot player : view.players()) {
            if (!player.alive()) {
                continue;
            }
            float[] at = placement.tileCoordinates(player);
            float cx = layout.screenX(at[0]) + size / 2;
            float cy = layout.screenY(at[1]) + size / 2;
            shapes.setColor(Color.BLACK);
            shapes.circle(cx, cy, size * 0.36f, 32);
            shapes.setColor(Palette.player(player.id()));
            shapes.circle(cx, cy, size * 0.32f, 32);
        }
    }

    private void drawPowerUpLetters(WorldView view) {
        float size = layout.tileSize();
        font.getData().setScale(1.3f);
        font.setColor(Palette.POWER_UP_BASE);
        for (Map.Entry<Position, PowerUpType> entry : view.powerUps().entrySet()) {
            drawCentred(TileArt.powerUpLetter(entry.getValue()), layout.screenX(entry.getKey().x()) + size / 2,
                    layout.screenY(entry.getKey().y()) + size / 2);
        }
    }

    private void drawPlayerNumbers(WorldView view, PlayerPlacement placement) {
        float size = layout.tileSize();
        font.getData().setScale(1.4f);
        font.setColor(Color.BLACK);
        for (PlayerSnapshot player : view.players()) {
            if (player.alive()) {
                float[] at = placement.tileCoordinates(player);
                drawCentred(Integer.toString(player.id().id()),
                        layout.screenX(at[0]) + size / 2, layout.screenY(at[1]) + size / 2);
            }
        }
    }

    private void drawCentred(String text, float cx, float cy) {
        glyphs.setText(font, text);
        font.draw(batch, glyphs, cx - glyphs.width / 2, cy + glyphs.height / 2);
    }

    @Override
    public void dispose() {
        shapes.dispose();
        batch.dispose();
        font.dispose();
    }
}
