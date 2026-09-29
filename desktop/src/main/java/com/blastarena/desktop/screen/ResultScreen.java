package com.blastarena.desktop.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.blastarena.core.engine.Match;
import com.blastarena.core.level.MapSource;
import com.blastarena.core.model.PlayerId;
import com.blastarena.desktop.render.Palette;
import com.blastarena.desktop.ui.Button;
import com.blastarena.desktop.ui.ButtonBar;
import java.util.Map;

/** The end of a match: who won and the final scores. Enter plays the same map again; Esc goes back. */
public final class ResultScreen extends ScreenAdapter {

    private static final float WIDTH = MenuScreen.WIDTH;
    private static final float HEIGHT = MenuScreen.HEIGHT;
    private static final PlayerId HUMAN = new PlayerId(1);

    private final Navigator navigator;
    private final Match match;
    private final MapSource mapSource;
    private final MatchSettings settings;
    private final Runnable onExit;
    private final OrthographicCamera camera = new OrthographicCamera();
    private final Viewport viewport = new FitViewport(WIDTH, HEIGHT, camera);
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final SpriteBatch batch = new SpriteBatch();
    private final BitmapFont font = new BitmapFont();
    private final GlyphLayout glyphs = new GlyphLayout();
    private final ButtonBar buttons = new ButtonBar();

    public ResultScreen(Navigator navigator, Match match, MapSource mapSource, MatchSettings settings,
                        Runnable onExit) {
        this.navigator = navigator;
        this.match = match;
        this.mapSource = mapSource;
        this.settings = settings;
        this.onExit = onExit;
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
        ScreenUtils.clear(Palette.BACKGROUND);
        viewport.apply();
        shapes.setProjectionMatrix(camera.combined);
        batch.setProjectionMatrix(camera.combined);
        buttons.clear();
        buttons.add(Button.of("Enter  Play again", WIDTH / 2 - 250, 120, 240, 44, this::playAgain));
        buttons.add(Button.of("Esc  Back", WIDTH / 2 + 10, 120, 240, 44, onExit));

        PlayerId winner = match.winner().orElseThrow();
        float rowTop = 470;
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        int row = 0;
        for (Map.Entry<PlayerId, Integer> score : match.scores().entrySet()) {
            float y = rowTop - row * 54;
            shapes.setColor(score.getKey().equals(winner) ? Palette.BUTTON_SELECTED : Palette.BUTTON);
            shapes.rect(WIDTH / 2 - 250, y - 40, 500, 46);
            shapes.setColor(Palette.player(score.getKey()));
            shapes.circle(WIDTH / 2 - 215, y - 17, 13, 24);
            for (int pip = 0; pip < score.getValue(); pip++) {
                shapes.setColor(Palette.FIRE_INNER);
                shapes.circle(WIDTH / 2 + 110 + pip * 26, y - 17, 9, 18);
            }
            row++;
        }
        buttons.drawBoxes(shapes);
        shapes.end();

        batch.begin();
        font.getData().setScale(3f);
        font.setColor(Palette.FIRE_INNER);
        centred(winner.equals(HUMAN) ? "You win the match!" : "Player " + winner.id() + " wins the match", 640);
        font.getData().setScale(1.3f);
        font.setColor(Palette.TEXT_DIM);
        centred(match.roundsPlayed() + " rounds played"
                + (match.draws() > 0 ? ", " + match.draws() + (match.draws() == 1 ? " draw" : " draws") : "")
                + "   best of " + match.bestOf() + " against " + settings.difficultyLabel() + " bots", 560);
        row = 0;
        font.getData().setScale(1.5f);
        for (Map.Entry<PlayerId, Integer> score : match.scores().entrySet()) {
            float y = rowTop - row * 54;
            font.setColor(Palette.TEXT);
            String name = score.getKey().equals(HUMAN) ? "You" : "Player " + score.getKey().id();
            font.draw(batch, name, WIDTH / 2 - 185, y - 7);
            font.draw(batch, Integer.toString(score.getValue()), WIDTH / 2 + 60, y - 7);
            row++;
        }
        font.getData().setScale(1.3f);
        buttons.drawLabels(batch, font);
        batch.end();
    }

    private void centred(String text, float y) {
        glyphs.setText(font, text);
        font.draw(batch, glyphs, WIDTH / 2 - glyphs.width / 2, y);
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
    }
}
