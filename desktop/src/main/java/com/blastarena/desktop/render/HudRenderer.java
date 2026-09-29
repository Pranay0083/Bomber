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

/**
 * The strip above the board (stats, round and clock, players with their round wins) and the big messages:
 * the countdown, the pause menu, and the result of each round and of the match.
 */
public final class HudRenderer implements Disposable {

    /** A big message across the middle of the board, with a smaller line of instructions under it. */
    private record Banner(String title, String subtitle) {
    }

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

    public void draw(WorldView view, MatchHud match, Matrix4 projection) {
        shapes.setProjectionMatrix(projection);
        batch.setProjectionMatrix(projection);
        float top = layout.totalHeight();
        float stripBottom = layout.boardHeight();
        float middle = layout.boardHeight() / 2;
        Banner banner = bannerFor(view.phase(), match);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(Palette.HUD);
        shapes.rect(0, stripBottom, layout.boardWidth(), layout.hudHeight());
        drawPlayers(view, match, stripBottom);
        if (banner != null) {
            shapes.setColor(Palette.OVERLAY);
            shapes.rect(0, middle - 70, layout.boardWidth(), 140);
        }
        shapes.end();

        batch.begin();
        font.getData().setScale(1.2f);
        view.player(humanPlayer).ifPresent(player -> {
            font.setColor(player.alive() ? Palette.TEXT : Palette.TEXT_DIM);
            font.draw(batch, statsLine(player), 12, top - 17);
        });
        font.setColor(Palette.TEXT);
        String status = "Round " + match.round() + " of " + match.bestOf() + "   " + clockFor(view.phase());
        glyphs.setText(font, status);
        font.draw(batch, glyphs, layout.boardWidth() * 0.55f - glyphs.width / 2, top - 17);

        if (banner != null) {
            font.getData().setScale(2.6f);
            glyphs.setText(font, banner.title());
            font.draw(batch, glyphs, layout.boardWidth() / 2 - glyphs.width / 2, middle + 40);
            font.getData().setScale(1.3f);
            font.setColor(Palette.TEXT_DIM);
            glyphs.setText(font, banner.subtitle());
            font.draw(batch, glyphs, layout.boardWidth() / 2 - glyphs.width / 2, middle - 24);
        }
        batch.end();
    }

    /** A circle per player, dimmed once they are out, with a pip for each round they have won. */
    private void drawPlayers(WorldView view, MatchHud match, float stripBottom) {
        float centreY = stripBottom + layout.hudHeight() / 2;
        float x = layout.boardWidth() - 26;
        for (int i = view.players().size() - 1; i >= 0; i--) {
            PlayerSnapshot player = view.players().get(i);
            shapes.setColor(player.alive() ? Palette.player(player.id()) : Palette.WALL_DARK);
            shapes.circle(x, centreY + 5, 9, 20);
            int wins = match.wins().getOrDefault(player.id(), 0);
            for (int pip = 0; pip < wins; pip++) {
                shapes.setColor(Palette.FIRE_INNER);
                shapes.circle(x - 8 + pip * 8, centreY - 12, 3, 10);
            }
            x -= 36;
        }
    }

    private static String statsLine(PlayerSnapshot player) {
        return "You   bombs " + player.stats().bombCapacity()
                + "   range " + player.stats().blastRange()
                + "   speed " + player.stats().speedLevel();
    }

    private static String clockFor(GamePhase phase) {
        return switch (phase) {
            case Countdown countdown -> "";
            case Playing playing -> clock(playing.roundLengthTicks() - playing.elapsedTicks());
            case SuddenDeath suddenDeath -> "SUDDEN DEATH";
            case RoundOver over -> "";
        };
    }

    private Banner bannerFor(GamePhase phase, MatchHud match) {
        if (match.paused()) {
            return new Banner("Paused", "P resume     R restart round     Q quit to menu");
        }
        return switch (phase) {
            case Countdown countdown -> new Banner(
                    "Round " + match.round() + "   "
                            + (countdown.ticksLeft() + GameConfig.TICKS_PER_SECOND - 1) / GameConfig.TICKS_PER_SECOND,
                    "Arrows move   Space drops a bomb   P pauses");
            case RoundOver over -> {
                String title = over.result().winner().map(this::roundWinText).orElse("Draw!");
                String next = match.matchWinner()
                        .map(winner -> matchWinText(winner) + "   Enter for results")
                        .orElse("Enter for the next round");
                yield new Banner(title, next);
            }
            case Playing playing -> null;
            case SuddenDeath suddenDeath -> null;
        };
    }

    private String roundWinText(PlayerId winner) {
        return winner.equals(humanPlayer) ? "You win the round!" : "Player " + winner.id() + " wins the round";
    }

    private String matchWinText(PlayerId winner) {
        return winner.equals(humanPlayer) ? "You take the match!" : "Player " + winner.id() + " takes the match.";
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
