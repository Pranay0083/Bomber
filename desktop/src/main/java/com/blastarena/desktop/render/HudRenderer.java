package com.blastarena.desktop.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Disposable;
import com.blastarena.core.control.PlayerSnapshot;
import com.blastarena.core.control.WorldView;
import com.blastarena.core.engine.Countdown;
import com.blastarena.core.engine.GamePhase;
import com.blastarena.core.engine.Playing;
import com.blastarena.core.engine.RoundOver;
import com.blastarena.core.engine.SuddenDeath;
import com.blastarena.core.model.GameConfig;
import com.blastarena.core.model.PlayerId;

/** The strip above the board, plus the big messages for the countdown and the end of a round. */
public final class HudRenderer implements Disposable {

    private final ShapeRenderer shapes = new ShapeRenderer();
    private final SpriteBatch batch = new SpriteBatch();
    private final BitmapFont font = new BitmapFont();
    private final GlyphLayout glyphs = new GlyphLayout();
    private final Layout layout;
    private final PlayerId humanPlayer;

    public HudRenderer(Layout layout, PlayerId humanPlayer) {
        this.layout = layout;
        this.humanPlayer = humanPlayer;
    }

    public void draw(WorldView view, Matrix4 projection) {
        shapes.setProjectionMatrix(projection);
        batch.setProjectionMatrix(projection);
        float top = layout.totalHeight();
        float stripBottom = layout.boardHeight();

        String banner = bannerFor(view.phase());
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(Palette.HUD);
        shapes.rect(0, stripBottom, layout.boardWidth(), layout.hudHeight());
        drawAliveMarkers(view, stripBottom);
        if (banner != null) {
            shapes.setColor(Palette.OVERLAY);
            shapes.rect(0, layout.boardHeight() / 2 - 50, layout.boardWidth(), 100);
        }
        shapes.end();

        batch.begin();
        font.getData().setScale(1.3f);
        view.player(humanPlayer).ifPresent(player -> {
            font.setColor(player.alive() ? Palette.TEXT : Palette.TEXT_DIM);
            font.draw(batch, statsLine(player), 16, top - 16);
        });
        font.setColor(Palette.TEXT);
        glyphs.setText(font, statusFor(view.phase()));
        font.draw(batch, glyphs, layout.boardWidth() / 2 - glyphs.width / 2, top - 16);

        if (banner != null) {
            font.getData().setScale(2.6f);
            glyphs.setText(font, banner);
            font.draw(batch, glyphs, layout.boardWidth() / 2 - glyphs.width / 2,
                    layout.boardHeight() / 2 + glyphs.height / 2);
        }
        batch.end();
    }

    private void drawAliveMarkers(WorldView view, float stripBottom) {
        float x = layout.boardWidth() - 24;
        for (int i = view.players().size() - 1; i >= 0; i--) {
            PlayerSnapshot player = view.players().get(i);
            shapes.setColor(player.alive() ? Palette.player(player.id()) : Palette.WALL_DARK);
            shapes.circle(x, stripBottom + layout.hudHeight() / 2, 10, 20);
            x -= 30;
        }
    }

    private static String statsLine(PlayerSnapshot player) {
        return "P" + player.id().id()
                + "   bombs " + player.stats().bombCapacity()
                + "   range " + player.stats().blastRange()
                + "   speed " + player.stats().speedLevel();
    }

    private static String statusFor(GamePhase phase) {
        return switch (phase) {
            case Countdown countdown -> "Get ready";
            case Playing playing -> clock(playing.roundLengthTicks() - playing.elapsedTicks());
            case SuddenDeath suddenDeath -> "SUDDEN DEATH";
            case RoundOver over -> "Round over";
        };
    }

    /** The big message in the middle of the board, or null when there is none. */
    private static String bannerFor(GamePhase phase) {
        return switch (phase) {
            case Countdown countdown ->
                    Integer.toString((countdown.ticksLeft() + GameConfig.TICKS_PER_SECOND - 1) / GameConfig.TICKS_PER_SECOND);
            case RoundOver over -> over.result().winner()
                    .map(winner -> "Player " + winner.id() + " wins!  Press R")
                    .orElse("Draw!  Press R");
            case Playing playing -> null;
            case SuddenDeath suddenDeath -> null;
        };
    }

    private static String clock(int ticks) {
        int seconds = Math.max(0, (ticks + GameConfig.TICKS_PER_SECOND - 1) / GameConfig.TICKS_PER_SECOND);
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    @Override
    public void dispose() {
        shapes.dispose();
        batch.dispose();
        font.dispose();
    }
}
