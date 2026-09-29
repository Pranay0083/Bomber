package com.blastarena.desktop.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.blastarena.core.board.MapGenerator;
import com.blastarena.core.level.CustomLevelSource;
import com.blastarena.core.level.EditableLevel;
import com.blastarena.core.level.LevelData;
import com.blastarena.core.level.LevelFormatException;
import com.blastarena.core.level.LevelRepository;
import com.blastarena.core.level.LevelValidator;
import com.blastarena.core.level.RandomMapSource;
import com.blastarena.desktop.render.Palette;
import com.blastarena.desktop.ui.Button;
import com.blastarena.desktop.ui.ButtonBar;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main menu: choose the bots and match length, then play a random arena or a saved level,
 * or edit, create and delete levels. Arrow keys choose, Enter plays, E edits, Delete deletes (press twice),
 * N starts a new level, B, D and M change the bots, their difficulty and the match length, Esc quits.
 */
public final class MenuScreen extends ScreenAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(MenuScreen.class);
    public static final float WIDTH = 832;
    public static final float HEIGHT = 752;
    private static final float ROW_HEIGHT = 34;
    private static final int VISIBLE_ROWS = 10;
    private static final float LIST_TOP = 470;

    /** One line of the list. Saved levels have their data; the random arena does not. */
    private record Entry(String label, String detail, Optional<LevelData> level, boolean playable) {
    }

    private final Navigator navigator;
    private final LevelRepository repository;
    private final LevelValidator validator = LevelValidator.standard();
    private final OrthographicCamera camera = new OrthographicCamera();
    private final Viewport viewport = new FitViewport(WIDTH, HEIGHT, camera);
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final SpriteBatch batch = new SpriteBatch();
    private final BitmapFont font = new BitmapFont();
    private final ButtonBar buttons = new ButtonBar();
    private final List<Entry> entries = new ArrayList<>();
    private int selected;
    private boolean confirmingDelete;
    private String message = "";

    public MenuScreen(Navigator navigator, LevelRepository repository) {
        this.navigator = navigator;
        this.repository = repository;
        reload();
    }

    private void reload() {
        entries.clear();
        entries.add(new Entry("Random arena", "A new 13 x 11 map every round", Optional.empty(), true));
        try {
            for (String name : repository.list()) {
                try {
                    repository.load(name).ifPresent(level -> {
                        boolean playable = validator.isPlayable(level);
                        String detail = level.width() + " x " + level.height() + "   "
                                + level.spawns().size() + " spawns" + (playable ? "" : "   needs fixes before play");
                        entries.add(new Entry(level.name(), detail, Optional.of(level), playable));
                    });
                } catch (LevelFormatException e) {
                    LOG.warn("Could not read level {}: {}", name, e.getMessage());
                }
            }
        } catch (IOException e) {
            message = "Could not read saved levels: " + e.getMessage();
        }
        selected = Math.min(selected, entries.size() - 1);
    }

    private Entry current() {
        return entries.get(selected);
    }

    private void playSelected() {
        Entry entry = current();
        if (entry.level().isEmpty()) {
            navigator.play(new RandomMapSource(new MapGenerator()), navigator.settings(), navigator::showMenu);
        } else if (entry.playable()) {
            navigator.play(new CustomLevelSource(entry.level().get()), navigator.settings(), navigator::showMenu);
        } else {
            editSelected();
        }
    }

    private void editSelected() {
        current().level().ifPresent(level -> navigator.edit(EditableLevel.from(level), level.name()));
    }

    private void newLevel() {
        navigator.edit(EditableLevel.blank("Untitled", 13, 11), null);
    }

    private void deleteSelected() {
        Optional<LevelData> level = current().level();
        if (level.isEmpty()) {
            return;
        }
        if (!confirmingDelete) {
            confirmingDelete = true;
            message = "Press Delete again to delete \"" + level.get().name() + "\"";
            return;
        }
        confirmingDelete = false;
        try {
            repository.delete(level.get().name());
            message = "Deleted \"" + level.get().name() + "\"";
        } catch (IOException e) {
            message = "Could not delete: " + e.getMessage();
        }
        reload();
    }

    private void move(int by) {
        confirmingDelete = false;
        message = "";
        selected = Math.floorMod(selected + by, entries.size());
    }

    @Override
    public void show() {
        reload();
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                switch (keycode) {
                    case Keys.UP -> move(-1);
                    case Keys.DOWN -> move(1);
                    case Keys.ENTER, Keys.SPACE -> playSelected();
                    case Keys.E -> editSelected();
                    case Keys.N -> newLevel();
                    case Keys.B -> navigator.changeSettings(navigator.settings().nextBotCount());
                    case Keys.D -> navigator.changeSettings(navigator.settings().nextDifficulty());
                    case Keys.M -> navigator.changeSettings(navigator.settings().nextMatchLength());
                    case Keys.FORWARD_DEL, Keys.DEL -> deleteSelected();
                    case Keys.ESCAPE -> Gdx.app.exit();
                    default -> {
                        return false;
                    }
                }
                return true;
            }

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                Vector2 at = viewport.unproject(new Vector2(screenX, screenY));
                if (buttons.click(at.x, at.y)) {
                    return true;
                }
                int row = rowAt(at.y);
                if (row >= 0 && row < entries.size()) {
                    if (row == selected) {
                        playSelected();
                    } else {
                        confirmingDelete = false;
                        selected = row;
                    }
                }
                return true;
            }
        });
    }

    private int firstVisibleRow() {
        return Math.max(0, Math.min(selected - VISIBLE_ROWS / 2, entries.size() - VISIBLE_ROWS));
    }

    private float rowTop(int row) {
        return LIST_TOP - (row - firstVisibleRow()) * ROW_HEIGHT;
    }

    private int rowAt(float y) {
        int offset = (int) Math.floor((LIST_TOP - y) / ROW_HEIGHT);
        return y > LIST_TOP || offset >= VISIBLE_ROWS ? -1 : firstVisibleRow() + offset;
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(Palette.BACKGROUND);
        viewport.apply();
        shapes.setProjectionMatrix(camera.combined);
        batch.setProjectionMatrix(camera.combined);

        buttons.clear();
        float y = 60;
        buttons.add(Button.of("Play", 80, y, 120, 36, this::playSelected));
        buttons.add(Button.of("Edit", 216, y, 120, 36, this::editSelected).enabled(current().level().isPresent()));
        buttons.add(Button.of("New level", 352, y, 140, 36, this::newLevel));
        buttons.add(Button.of("Delete", 508, y, 120, 36, this::deleteSelected).enabled(current().level().isPresent()));
        buttons.add(Button.of("Quit", 644, y, 108, 36, () -> Gdx.app.exit()));
        MatchSettings settings = navigator.settings();
        float settingsY = HEIGHT - 240;
        buttons.add(Button.of("B  Bots: " + settings.bots(), 80, settingsY, 200, 36,
                () -> navigator.changeSettings(navigator.settings().nextBotCount())));
        buttons.add(Button.of("D  Bots are " + settings.difficultyLabel(), 296, settingsY, 240, 36,
                () -> navigator.changeSettings(navigator.settings().nextDifficulty())));
        buttons.add(Button.of("M  Best of " + settings.bestOf(), 552, settingsY, 200, 36,
                () -> navigator.changeSettings(navigator.settings().nextMatchLength())));

        int first = firstVisibleRow();
        int last = Math.min(entries.size(), first + VISIBLE_ROWS);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (int row = first; row < last; row++) {
            shapes.setColor(row == selected ? Palette.BUTTON_SELECTED : Palette.BUTTON);
            shapes.rect(80, rowTop(row) - ROW_HEIGHT + 4, WIDTH - 160, ROW_HEIGHT - 6);
        }
        buttons.drawBoxes(shapes);
        shapes.end();

        batch.begin();
        font.getData().setScale(3.2f);
        font.setColor(Palette.FIRE_INNER);
        font.draw(batch, "BLAST ARENA", 80, HEIGHT - 60);
        font.getData().setScale(1.3f);
        font.setColor(Palette.TEXT_DIM);
        font.draw(batch, "Arrows choose   Enter play   E edit   N new level   Delete remove   Esc quit",
                80, HEIGHT - 130);
        font.draw(batch, "Levels are saved in ~/.blastarena/levels/", 80, HEIGHT - 156);
        font.setColor(Palette.TEXT);
        font.draw(batch, "Match", 80, HEIGHT - 190);
        for (int row = first; row < last; row++) {
            Entry entry = entries.get(row);
            float top = rowTop(row);
            font.getData().setScale(1.4f);
            font.setColor(Palette.TEXT);
            font.draw(batch, entry.label(), 96, top - 9);
            font.getData().setScale(1.1f);
            font.setColor(entry.playable() ? Palette.TEXT_DIM : Palette.PROBLEM);
            font.draw(batch, entry.detail(), 380, top - 11);
        }
        font.getData().setScale(1.3f);
        font.setColor(Palette.TEXT);
        font.draw(batch, message, 80, 128);
        buttons.drawLabels(batch, font);
        batch.end();
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
