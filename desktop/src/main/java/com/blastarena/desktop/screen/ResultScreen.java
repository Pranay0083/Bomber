package com.blastarena.desktop.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.blastarena.core.engine.Match;
import com.blastarena.core.level.MapSource;
import com.blastarena.core.model.Direction;
import com.blastarena.core.model.PlayerId;
import com.blastarena.core.powerup.PowerUpType;
import com.blastarena.desktop.render.Palette;
import com.blastarena.desktop.render.Sprite;
import com.blastarena.desktop.render.SpriteSheet;
import com.blastarena.desktop.ui.Button;
import com.blastarena.desktop.ui.ButtonBar;
import com.blastarena.desktop.ui.Ui;
import com.blastarena.desktop.ui.UiFont;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * The end of a match: the winner hopping under falling confetti, and everyone's final score.
 * Enter plays the same map again; Esc goes back.
 */
public final class ResultScreen extends ScreenAdapter {

    private static final float WIDTH = MenuScreen.WIDTH;
    private static final float HEIGHT = MenuScreen.HEIGHT;
    private static final PlayerId HUMAN = new PlayerId(1);
    private static final int CONFETTI = 70;

    /** One scrap of confetti: where it started, how fast it falls and sways, and its colour. */
    private record Scrap(float x, float y, float fall, float sway, float phase, Color colour) {
    }

    private final Navigator navigator;
    private final Match match;
    private final MapSource mapSource;
    private final MatchSettings settings;
    private final Runnable onExit;
    private final OrthographicCamera camera = new OrthographicCamera();
    private final Viewport viewport = new FitViewport(WIDTH, HEIGHT, camera);
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final SpriteBatch batch = new SpriteBatch();
    private final BitmapFont font = UiFont.create();
    private final SpriteSheet sheet = new SpriteSheet();
    private final ButtonBar buttons = new ButtonBar();
    private final List<Scrap> confetti = new ArrayList<>();
    private float clock;

    public ResultScreen(Navigator navigator, Match match, MapSource mapSource, MatchSettings settings,
                        Runnable onExit) {
        this.navigator = navigator;
        this.match = match;
        this.mapSource = mapSource;
        this.settings = settings;
        this.onExit = onExit;
        Random random = new Random();
        Color[] colours = {Palette.ACCENT, Palette.FIRE_OUTER, Palette.powerUp(PowerUpType.SPEED),
            Palette.powerUp(PowerUpType.EXTRA_BOMB), Color.WHITE};
        for (int i = 0; i < CONFETTI; i++) {
            confetti.add(new Scrap(random.nextFloat() * WIDTH, random.nextFloat() * HEIGHT,
                    40 + random.nextFloat() * 70, 10 + random.nextFloat() * 20, random.nextFloat() * 6.28f,
                    colours[random.nextInt(colours.length)]));
        }
    }

    private void playAgain() {
        navigator.play(mapSource, settings, onExit);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                if (keycode == Keys.ENTER || keycode == Keys.SPACE) {
                    playAgain();
                    return true;
                }
                if (keycode == Keys.ESCAPE) {
                    onExit.run();
                    return true;
                }
                return false;
            }

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                Vector2 at = viewport.unproject(new Vector2(screenX, screenY));
                return buttons.click(at.x, at.y);
            }
        });
    }

    @Override
    public void render(float delta) {
        clock += delta;
        ScreenUtils.clear(Palette.BACKGROUND);
        viewport.apply();
        shapes.setProjectionMatrix(camera.combined);
        batch.setProjectionMatrix(camera.combined);
        buttons.clear();
        buttons.add(Button.of("Enter  Play again", WIDTH / 2 - 250, 60, 240, 48, this::playAgain).selected(true));
        buttons.add(Button.of("Esc  Back", WIDTH / 2 + 10, 60, 240, 48, onExit));

        PlayerId winner = match.winner().orElseThrow();
        float rowTop = 380;
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        drawConfetti();
        Ui.panel(shapes, WIDTH / 2 - 270, rowTop - match.scores().size() * 58 - 20, 540,
                match.scores().size() * 58 + 30, Palette.PANEL, Palette.PANEL_EDGE);
        int row = 0;
        for (Map.Entry<PlayerId, Integer> score : match.scores().entrySet()) {
            float y = rowTop - row * 58;
            if (score.getKey().equals(winner)) {
                Ui.panel(shapes, WIDTH / 2 - 256, y - 50, 512, 50, Palette.BUTTON_SELECTED, Palette.BUTTON_SELECTED_EDGE);
            }
            for (int pip = 0; pip < match.winsNeeded(); pip++) {
                shapes.setColor(pip < score.getValue() ? Palette.ACCENT : Palette.BUTTON_EDGE);
                shapes.rect(WIDTH / 2 + 110 + pip * 30, y - 33, 16, 16);
            }
            row++;
        }
        buttons.drawBoxes(shapes);
        shapes.end();

        batch.begin();
        float hop = Math.abs((float) Math.sin(clock * 4)) * 18;
        batch.setColor(Palette.player(winner));
        batch.draw(sheet.get(Sprite.player(Direction.DOWN, hop > 9 ? 1 : 0)), WIDTH / 2 - 56, 560 + hop, 112, 112);
        batch.setColor(Color.WHITE);
        Ui.shadowText(batch, font, winner.equals(HUMAN) ? "You win the match!" : "Player " + winner.id() + " wins!",
                UiFont.LARGE, Palette.ACCENT, WIDTH / 2, 510, Ui.Align.CENTRE);
        Ui.text(batch, font, match.roundsPlayed() + (match.roundsPlayed() == 1 ? " round" : " rounds")
                        + (match.draws() > 0 ? ", " + match.draws() + (match.draws() == 1 ? " draw" : " draws") : "")
                        + "   Best of " + match.bestOf() + "   " + settings.difficultyLabel() + " bots",
                UiFont.SMALL, Palette.TEXT_DIM, WIDTH / 2, 460, Ui.Align.CENTRE);
        row = 0;
        for (Map.Entry<PlayerId, Integer> score : match.scores().entrySet()) {
            float y = rowTop - row * 58;
            batch.setColor(Palette.player(score.getKey()));
            batch.draw(sheet.get(Sprite.player(Direction.DOWN, 0)), WIDTH / 2 - 240, y - 46, 42, 42);
            batch.setColor(Color.WHITE);
            String name = score.getKey().equals(HUMAN) ? "You" : "Player " + score.getKey().id();
            Ui.text(batch, font, name, UiFont.NORMAL, Palette.TEXT, WIDTH / 2 - 184, y - 25, Ui.Align.LEFT);
            row++;
        }
        buttons.drawLabels(batch, font);
        batch.end();
    }

    private void drawConfetti() {
        for (Scrap scrap : confetti) {
            float y = Math.floorMod((int) (scrap.y() - clock * scrap.fall()), (int) HEIGHT + 20) - 10;
            float x = scrap.x() + (float) Math.sin(clock * 2 + scrap.phase()) * scrap.sway();
            shapes.setColor(scrap.colour());
            shapes.rect(x, y, 6, 10);
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        shapes.dispose();
        batch.dispose();
        font.dispose();
        sheet.dispose();
    }
}
