package com.blastarena.desktop.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
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
import java.util.List;
import java.util.Map;

/**
 * Draws the board and everything on it with the pixel-art sprites. Fire plays a four-frame explosion over its
 * lifetime, bombs flicker and flash red before they go, and tiles about to be walled in by sudden death are
 * striped red.
 */
public final class BoardRenderer implements Disposable {

    /** Sudden-death walls this close are shown as a warning. */
    private static final int WARNING_TICKS = 20;
    private static final Sprite[] FIRE_FRAMES = {Sprite.FIRE_0, Sprite.FIRE_1, Sprite.FIRE_2, Sprite.FIRE_3};

    private final SpriteBatch batch = new SpriteBatch();
    private final SpriteSheet sheet = new SpriteSheet();
    private final Layout layout;

    public BoardRenderer(Layout layout) {
        this.layout = layout;
    }

    public void draw(WorldView view, PlayerPlacement placement, Matrix4 projection) {
        batch.setProjectionMatrix(projection);
        batch.begin();
        drawTiles(view);
        drawWallWarnings(view);
        drawPowerUps(view);
        drawBombs(view);
        drawFire(view);
        drawPlayers(view, placement);
        batch.setColor(Color.WHITE);
        batch.end();
    }

    private void drawTiles(WorldView view) {
        float size = layout.tileSize();
        for (int y = 0; y < view.height(); y++) {
            for (int x = 0; x < view.width(); x++) {
                float left = layout.screenX(x);
                float bottom = layout.screenY(y);
                switch (view.tileAt(new Position(x, y))) {
                    case Floor floor -> TileArt.floor(batch, sheet, left, bottom, size, x, y);
                    case SolidWall wall -> TileArt.wall(batch, sheet, left, bottom, size);
                    case Crate crate -> TileArt.crate(batch, sheet, left, bottom, size);
                }
            }
        }
    }

    private void drawWallWarnings(WorldView view) {
        int next = view.ticksUntilNextWall();
        int interval = view.config().suddenDeathIntervalTicks();
        List<Position> walls = view.upcomingWalls();
        for (int i = 0; i < walls.size() && (long) next + (long) i * interval <= WARNING_TICKS; i++) {
            Position wall = walls.get(i);
            TileArt.draw(batch, sheet, Sprite.WARNING, layout.screenX(wall.x()), layout.screenY(wall.y()),
                    layout.tileSize());
        }
    }

    private void drawPowerUps(WorldView view) {
        for (Map.Entry<Position, PowerUpType> entry : view.powerUps().entrySet()) {
            TileArt.powerUp(batch, sheet, layout.screenX(entry.getKey().x()), layout.screenY(entry.getKey().y()),
                    layout.tileSize(), entry.getValue());
        }
    }

    private void drawBombs(WorldView view) {
        float size = layout.tileSize();
        for (BombSnapshot bomb : view.bombs()) {
            int fuse = bomb.remainingFuse();
            boolean aboutToBlow = fuse <= 10 && fuse % 4 < 2;
            float scale = 1f + 0.06f * (float) Math.sin(fuse * 0.6) + (aboutToBlow ? 0.08f : 0f);
            float drawn = size * scale;
            float left = layout.screenX(bomb.position().x()) - (drawn - size) / 2;
            float bottom = layout.screenY(bomb.position().y()) - (drawn - size) / 2;
            batch.setColor(aboutToBlow ? Palette.BOMB_FLASH : Color.WHITE);
            batch.draw(sheet.get((fuse / 3) % 2 == 0 ? Sprite.BOMB_0 : Sprite.BOMB_1), left, bottom, drawn, drawn);
        }
        batch.setColor(Color.WHITE);
    }

    /** The explosion animation: which frame depends on how far through its life the fire is. */
    private void drawFire(WorldView view) {
        int lifetime = view.config().fireTicks();
        float size = layout.tileSize();
        for (Position position : view.burningTiles()) {
            int age = Math.max(0, lifetime - view.fireTicksLeft(position));
            int frame = Math.min(FIRE_FRAMES.length - 1, age * FIRE_FRAMES.length / lifetime);
            // Fire slightly larger than its tile so neighbouring flames merge into one blast.
            float grow = size * 0.12f;
            batch.draw(sheet.get(FIRE_FRAMES[frame]), layout.screenX(position.x()) - grow / 2,
                    layout.screenY(position.y()) - grow / 2, size + grow, size + grow);
        }
    }

    private void drawPlayers(WorldView view, PlayerPlacement placement) {
        float size = layout.tileSize();
        for (PlayerSnapshot player : view.players()) {
            if (!player.alive()) {
                continue;
            }
            float[] at = placement.tileCoordinates(player);
            batch.setColor(Palette.player(player.id()));
            batch.draw(sheet.get(Sprite.PLAYER), layout.screenX(at[0]), layout.screenY(at[1]), size, size);
        }
        batch.setColor(Color.WHITE);
    }

    @Override
    public void dispose() {
        batch.dispose();
        sheet.dispose();
    }
}
