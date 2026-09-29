package com.blastarena.desktop.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
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
import com.blastarena.core.model.Direction;
import com.blastarena.core.model.GameConfig;
import com.blastarena.core.model.PlayerId;
import com.blastarena.desktop.ui.Ui;
import com.blastarena.desktop.ui.UiFont;

/**
 * The strip above the board (your stats, the round and clock, every player with their round wins) and the
 * messages across the middle: the countdown, the pause menu, and the result of each round and of the match.
 */
public final class HudRenderer implements Disposable {

    /** A message across the middle of the board, with a line of instructions under it. */
    private record Banner(String title, Color colour, String subtitle) {
    }

    private static final int CLOCK_WARNING_SECONDS = 10;

    private final ShapeRenderer shapes = new ShapeRenderer();
    private final SpriteBatch batch = new SpriteBatch();
    private final BitmapFont font = UiFont.create();
    private final SpriteSheet sheet = new SpriteSheet();
    private final Layout layout;
    private final PlayerId humanPlayer;
    private float clock;

    public HudRenderer(Layout layout, PlayerId humanPlayer) {
        this.layout = layout;
        this.humanPlayer = humanPlayer;
    }

    public void draw(WorldView view, MatchHud match, float delta, Matrix4 projection) {
        clock += delta;
        shapes.setProjectionMatrix(projection);
        batch.setProjectionMatrix(projection);
        float stripBottom = layout.boardHeight();
        float stripMiddle = stripBottom + layout.hudHeight() / 2;
        float middle = layout.boardHeight() / 2;
        Banner banner = bannerFor(view.phase(), match);
        boolean countdown = view.phase() instanceof Countdown && !match.paused();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(Palette.HUD);
        shapes.rect(0, stripBottom, layout.boardWidth(), layout.hudHeight());
        shapes.setColor(Palette.PANEL_EDGE);
        shapes.rect(0, stripBottom, layout.boardWidth(), 2);
        drawWinPips(view, match, stripMiddle);
        if (banner != null) {
            shapes.setColor(Palette.OVERLAY);
            shapes.rect(0, 0, layout.boardWidth(), layout.boardHeight());
            float width = Math.min(layout.boardWidth() - 40, 640);
            Ui.panel(shapes, (layout.boardWidth() - width) / 2, middle - (countdown ? 110 : 80), width,
                    countdown ? 220 : 160, Palette.PANEL, Palette.PANEL_EDGE);
        }
        shapes.end();

        batch.begin();
        drawStats(view, stripMiddle);
        drawClock(view, match, stripMiddle);
        drawPortraits(view, stripMiddle);
        if (banner != null) {
            if (countdown) {
                Countdown phase = (Countdown) view.phase();
                int seconds = (phase.ticksLeft() + GameConfig.TICKS_PER_SECOND - 1) / GameConfig.TICKS_PER_SECOND;
                Ui.shadowText(batch, font, "Round " + match.round(), UiFont.NORMAL, Palette.TEXT,
                        layout.boardWidth() / 2, middle + 70, Ui.Align.CENTRE);
                Ui.shadowText(batch, font, Integer.toString(seconds), UiFont.TITLE, Palette.ACCENT,
                        layout.boardWidth() / 2, middle + 2, Ui.Align.CENTRE);
                Ui.text(batch, font, banner.subtitle(), UiFont.SMALL, Palette.TEXT_DIM,
                        layout.boardWidth() / 2, middle - 78, Ui.Align.CENTRE);
            } else {
                Ui.shadowText(batch, font, banner.title(), UiFont.LARGE, banner.colour(),
                        layout.boardWidth() / 2, middle + 22, Ui.Align.CENTRE);
                Ui.text(batch, font, banner.subtitle(), UiFont.SMALL, Palette.TEXT_DIM,
                        layout.boardWidth() / 2, middle - 38, Ui.Align.CENTRE);
            }
        }
        batch.setColor(Color.WHITE);
        batch.end();
    }

    /** Your portrait, then an icon and a number for each stat. */
    private void drawStats(WorldView view, float middle) {
        view.player(humanPlayer).ifPresent(player -> {
            float size = 34;
            batch.setColor(player.alive() ? Palette.player(player.id()) : Palette.WALL_DARK);
            batch.draw(sheet.get(Sprite.player(Direction.DOWN, 0)), 10, middle - size / 2, size, size);
            batch.setColor(Color.WHITE);
            float x = 54;
            x = stat(Sprite.POWER_UP_BOMB, player.stats().bombCapacity(), x, middle, player.alive());
            x = stat(Sprite.POWER_UP_RANGE, player.stats().blastRange(), x, middle, player.alive());
            stat(Sprite.POWER_UP_SPEED, player.stats().speedLevel(), x, middle, player.alive());
        });
    }

    private float stat(Sprite icon, int value, float x, float middle, boolean alive) {
        float size = 28;
        batch.setColor(alive ? Color.WHITE : Color.GRAY);
        batch.draw(sheet.get(icon), x, middle - size / 2, size, size);
        batch.setColor(Color.WHITE);
        float width = Ui.text(batch, font, Integer.toString(value), UiFont.SMALL, alive ? Palette.TEXT : Palette.TEXT_DIM,
                x + size + 4, middle, Ui.Align.LEFT);
        return x + size + 4 + width + 14;
    }

    private void drawClock(WorldView view, MatchHud match, float middle) {
        float centre = layout.boardWidth() * 0.5f;
        Ui.text(batch, font, "Round " + match.round() + " of " + match.bestOf(), UiFont.SMALL, Palette.TEXT_DIM,
                centre, middle + 10, Ui.Align.CENTRE);
        GamePhase phase = view.phase();
        if (phase instanceof SuddenDeath) {
            float pulse = 0.6f + 0.4f * (float) Math.abs(Math.sin(clock * 5));
            Ui.text(batch, font, "Sudden death", UiFont.SMALL, new Color(1f, 0.3f, 0.3f, pulse),
                    centre, middle - 9, Ui.Align.CENTRE);
        } else if (phase instanceof Playing playing) {
            int ticks = playing.roundLengthTicks() - playing.elapsedTicks();
            int seconds = Math.max(0, (ticks + GameConfig.TICKS_PER_SECOND - 1) / GameConfig.TICKS_PER_SECOND);
            Color colour = seconds <= CLOCK_WARNING_SECONDS ? Palette.PROBLEM : Palette.TEXT;
            Ui.text(batch, font, String.format("%d:%02d", seconds / 60, seconds % 60), UiFont.SMALL, colour,
                    centre, middle - 9, Ui.Align.CENTRE);
        }
    }

    /** Every player's portrait on the right, greyed out once they are knocked out. */
    private void drawPortraits(WorldView view, float middle) {
        float size = 30;
        float x = layout.boardWidth() - 12 - size;
        for (int i = view.players().size() - 1; i >= 0; i--) {
            PlayerSnapshot player = view.players().get(i);
            batch.setColor(player.alive() ? Palette.player(player.id()) : new Color(0.3f, 0.32f, 0.36f, 0.8f));
            batch.draw(sheet.get(Sprite.player(Direction.DOWN, 0)), x, middle - size / 2 + 5, size, size);
            x -= size + 14;
        }
        batch.setColor(Color.WHITE);
    }

    private void drawWinPips(WorldView view, MatchHud match, float middle) {
        float size = 30;
        float x = layout.boardWidth() - 12 - size;
        for (int i = view.players().size() - 1; i >= 0; i--) {
            PlayerSnapshot player = view.players().get(i);
            int wins = match.wins().getOrDefault(player.id(), 0);
            float first = x + size / 2 - (wins - 1) * 4f;
            for (int pip = 0; pip < wins; pip++) {
                shapes.setColor(Palette.ACCENT);
                shapes.rect(first + pip * 8 - 3, middle - 19, 6, 6);
            }
            x -= size + 14;
        }
    }

    private Banner bannerFor(GamePhase phase, MatchHud match) {
        if (match.paused()) {
            return new Banner("Paused", Palette.TEXT,
                    "P resume   R restart   Q menu   M sound " + (match.muted() ? "on" : "off"));
        }
        return switch (phase) {
            case Countdown countdown -> new Banner("", Palette.TEXT, "Arrows move  Space bomb  P pause  M mute");
            case RoundOver over -> {
                String title = over.result().winner().map(this::roundWinText).orElse("Draw!");
                Color colour = over.result().winner().map(Palette::player).orElse(Palette.TEXT);
                String next = match.matchWinner()
                        .map(winner -> matchWinText(winner) + "  Enter for results")
                        .orElse("Enter for the next round");
                yield new Banner(title, colour, next);
            }
            case Playing playing -> null;
            case SuddenDeath suddenDeath -> null;
        };
    }

    private String roundWinText(PlayerId winner) {
        return winner.equals(humanPlayer) ? "You win the round!" : "Player " + winner.id() + " wins";
    }

    private String matchWinText(PlayerId winner) {
        return winner.equals(humanPlayer) ? "You take the match!" : "Player " + winner.id() + " takes the match.";
    }

    @Override
    public void dispose() {
        shapes.dispose();
        batch.dispose();
        font.dispose();
        sheet.dispose();
    }
}
