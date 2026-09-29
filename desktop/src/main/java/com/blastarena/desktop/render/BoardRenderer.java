package com.blastarena.desktop.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Disposable;
import com.blastarena.core.board.Crate;
import com.blastarena.core.board.Floor;
import com.blastarena.core.board.SolidWall;
import com.blastarena.core.board.Tile;
import com.blastarena.core.control.BombSnapshot;
import com.blastarena.core.control.PlayerSnapshot;
import com.blastarena.core.control.WorldView;
import com.blastarena.core.model.Direction;
import com.blastarena.core.model.Position;
import com.blastarena.core.powerup.PowerUpType;
import com.blastarena.desktop.render.Sprite.FirePiece;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Draws the board and everything on it with the pixel-art sprites: floor shaded under walls and crates, shadows
 * under everything that stands on the floor, blasts drawn as crosses with arms and rounded ends that play a
 * four-frame animation, bombs that flicker and flash before they go, bobbing power-ups, players facing the way
 * they walk, and short puffs and splinters where players fall and crates break.
 */
public final class BoardRenderer implements Disposable {

    /** Sudden-death walls this close are shown as a warning. */
    private static final int WARNING_TICKS = 20;

    private final SpriteBatch batch = new SpriteBatch();
    private final SpriteSheet sheet = new SpriteSheet();
    private final Matrix4 shaken = new Matrix4();
    private final Layout layout;
    private float clock;

    public BoardRenderer(Layout layout) {
        this.layout = layout;
    }

    public void draw(WorldView view, BoardAnimation animation, float alpha, float delta, Matrix4 projection) {
        clock += delta;
        float[] shake = animation.shake(alpha);
        shaken.set(projection).translate(shake[0] * layout.tileSize(), shake[1] * layout.tileSize(), 0);
        batch.setProjectionMatrix(shaken);
        batch.begin();
        drawTiles(view);
        drawWallWarnings(view);
        drawPowerUps(view);
        drawBombs(view);
        drawFire(view);
        drawEffects(animation.effects(alpha), BoardAnimation.Effect.Kind.DEBRIS);
        drawPlayers(view, animation, alpha);
        drawEffects(animation.effects(alpha), BoardAnimation.Effect.Kind.POOF);
        batch.setColor(Color.WHITE);
        batch.end();
    }

    private void draw(Sprite sprite, float tileX, float tileY) {
        batch.draw(sheet.get(sprite), layout.screenX(tileX), layout.screenY(tileY), layout.tileSize(), layout.tileSize());
    }

    private void drawTiles(WorldView view) {
        float size = layout.tileSize();
        for (int y = 0; y < view.height(); y++) {
            for (int x = 0; x < view.width(); x++) {
                float left = layout.screenX(x);
                float bottom = layout.screenY(y);
                switch (view.tileAt(new Position(x, y))) {
                    case Floor floor -> {
                        TileArt.floor(batch, sheet, left, bottom, size, x, y);
                        if (castsShadow(view, new Position(x, y - 1))) {
                            draw(Sprite.FLOOR_SHADOW, x, y);
                        }
                    }
                    case SolidWall wall -> TileArt.wall(batch, sheet, left, bottom, size);
                    case Crate crate -> TileArt.crate(batch, sheet, left, bottom, size);
                }
            }
        }
    }

    private static boolean castsShadow(WorldView view, Position above) {
        if (!view.isInside(above)) {
            return false;
        }
        Tile tile = view.tileAt(above);
        return tile instanceof SolidWall || tile instanceof Crate;
    }

    private void drawWallWarnings(WorldView view) {
        int next = view.ticksUntilNextWall();
        int interval = view.config().suddenDeathIntervalTicks();
        List<Position> walls = view.upcomingWalls();
        for (int i = 0; i < walls.size() && (long) next + (long) i * interval <= WARNING_TICKS; i++) {
            Position wall = walls.get(i);
            // Pulse faster as the wall gets closer.
            float urgency = 1f - (float) (next + i * interval) / WARNING_TICKS;
            batch.setColor(1f, 1f, 1f, 0.55f + 0.45f * (float) Math.abs(Math.sin(clock * (4 + urgency * 10))));
            draw(Sprite.WARNING, wall.x(), wall.y());
        }
        batch.setColor(Color.WHITE);
    }

    private void drawPowerUps(WorldView view) {
        float size = layout.tileSize();
        for (Map.Entry<Position, PowerUpType> entry : view.powerUps().entrySet()) {
            Position at = entry.getKey();
            float bob = size * 0.05f * (float) Math.sin(clock * 3 + at.x() + at.y());
            draw(Sprite.SHADOW, at.x(), at.y());
            TileArt.powerUp(batch, sheet, layout.screenX(at.x()), layout.screenY(at.y()) + size * 0.04f + bob, size,
                    entry.getValue());
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
            draw(Sprite.SHADOW, bomb.position().x(), bomb.position().y());
            batch.setColor(aboutToBlow ? Palette.BOMB_FLASH : Color.WHITE);
            batch.draw(sheet.get((fuse / 3) % 2 == 0 ? Sprite.BOMB_0 : Sprite.BOMB_1), left, bottom, drawn, drawn);
        }
        batch.setColor(Color.WHITE);
    }

    /**
     * Each burning tile is drawn as a centre, an arm or an end, turned to match its burning neighbours:
     * a line of fire with no neighbours beyond it ends in a rounded tip. The frame follows how old the fire is.
     */
    private void drawFire(WorldView view) {
        int lifetime = view.config().fireTicks();
        Set<Position> burning = view.burningTiles();
        float size = layout.tileSize();
        for (Position position : burning) {
            boolean left = burning.contains(position.step(Direction.LEFT));
            boolean right = burning.contains(position.step(Direction.RIGHT));
            boolean up = burning.contains(position.step(Direction.UP));
            boolean down = burning.contains(position.step(Direction.DOWN));
            boolean across = left || right;
            boolean along = up || down;
            FirePiece piece;
            float rotation;
            if (across == along) {
                piece = FirePiece.CENTRE;
                rotation = 0;
            } else if (across) {
                piece = left && right ? FirePiece.ARM : FirePiece.END;
                rotation = left ? 0 : 180;
            } else {
                piece = up && down ? FirePiece.ARM : FirePiece.END;
                // Screen y grows upwards, so a tip with fire below it points up the screen.
                rotation = piece == FirePiece.ARM || down ? 90 : 270;
            }
            int age = Math.max(0, lifetime - view.fireTicksLeft(position));
            int frame = Math.min(3, age * 4 / lifetime);
            TextureRegion region = sheet.get(Sprite.fire(piece, frame));
            batch.draw(region, layout.screenX(position.x()), layout.screenY(position.y()),
                    size / 2, size / 2, size, size, 1f, 1f, rotation);
        }
    }

    private void drawEffects(List<BoardAnimation.Effect> effects, BoardAnimation.Effect.Kind kind) {
        for (BoardAnimation.Effect effect : effects) {
            if (effect.kind() != kind) {
                continue;
            }
            int frame = Math.min(2, (int) (effect.age() * 3 / AnimationListener.EFFECT_TICKS));
            if (kind == BoardAnimation.Effect.Kind.POOF) {
                batch.setColor(effect.player() == null ? Color.WHITE : Palette.player(effect.player()));
                draw(Sprite.valueOf("POOF_" + frame), effect.position().x(), effect.position().y());
                batch.setColor(Color.WHITE);
            } else {
                draw(Sprite.valueOf("DEBRIS_" + frame), effect.position().x(), effect.position().y());
            }
        }
    }

    private void drawPlayers(WorldView view, BoardAnimation animation, float alpha) {
        // Players lower on the screen are drawn last, so they stand in front.
        List<PlayerSnapshot> players = view.players().stream()
                .filter(PlayerSnapshot::alive)
                .sorted((a, b) -> Float.compare(animation.pose(a, alpha).y(), animation.pose(b, alpha).y()))
                .toList();
        for (PlayerSnapshot player : players) {
            BoardAnimation.Pose pose = animation.pose(player, alpha);
            batch.setColor(Color.WHITE);
            draw(Sprite.SHADOW, pose.x(), pose.y());
            batch.setColor(Palette.player(player.id()));
            draw(Sprite.player(pose.facing(), pose.step()), pose.x(), pose.y());
        }
        batch.setColor(Color.WHITE);
    }

    @Override
    public void dispose() {
        batch.dispose();
        sheet.dispose();
    }
}
