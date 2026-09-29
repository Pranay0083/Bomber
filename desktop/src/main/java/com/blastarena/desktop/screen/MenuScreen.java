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
import com.blastarena.core.board.Board;
import com.blastarena.core.board.MapGenerator;
import com.blastarena.core.level.CustomLevelSource;
import com.blastarena.core.level.EditableLevel;
import com.blastarena.core.level.LevelData;
import com.blastarena.core.level.LevelFormatException;
import com.blastarena.core.level.LevelRepository;
import com.blastarena.core.level.LevelValidator;
import com.blastarena.core.level.RandomMapSource;
import com.blastarena.core.model.GameConfig;
import com.blastarena.desktop.render.MiniMap;
import com.blastarena.desktop.render.Palette;
import com.blastarena.desktop.render.Sprite;
import com.blastarena.desktop.render.SpriteSheet;
import com.blastarena.desktop.ui.Button;
import com.blastarena.desktop.ui.ButtonBar;
import com.blastarena.desktop.ui.Ui;
import com.blastarena.desktop.ui.UiFont;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
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

    private static final float MARGIN = 40;
    private static final float LIST_LEFT = MARGIN;
    private static final float LIST_WIDTH = 452;
    private static final float LIST_BOTTOM = 118;
    private static final float LIST_TOP = 450;
    private static final float ROW_HEIGHT = 56;
    private static final float PREVIEW_LEFT = LIST_LEFT + LIST_WIDTH + 16;
    private static final float PREVIEW_WIDTH = WIDTH - MARGIN - PREVIEW_LEFT;

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
    private final BitmapFont font = UiFont.create();
    private final SpriteSheet sheet = new SpriteSheet();
    private final ButtonBar buttons = new ButtonBar();
    private final List<Entry> entries = new ArrayList<>();
    private final Board sampleArena;
    private int selected;
    private boolean confirmingDelete;
    private String message = "";
    private float clock;

    public MenuScreen(Navigator navigator, LevelRepository repository) {
        this.navigator = navigator;
        this.repository = repository;
        GameConfig sample = GameConfig.builder().seed(new Random().nextLong()).build();
        this.sampleArena = new MapGenerator().generate(sample, new Random(sample.seed()));
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
                                + level.spawns().size() + " spawns" + (playable ? "" : "   needs fixes");
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
            message = "Press Delete again to delete " + level.get().name();
            return;
        }
        confirmingDelete = false;
        try {
            repository.delete(level.get().name());
            message = "Deleted " + level.get().name();
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

    private void change(MatchSettings settings) {
        navigator.changeSettings(settings);
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
                    case Keys.B -> change(navigator.settings().nextBotCount());
                    case Keys.D -> change(navigator.settings().nextDifficulty());
                    case Keys.M -> change(navigator.settings().nextMatchLength());
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
                int row = rowAt(at.x, at.y);
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

    private int visibleRows() {
        return (int) ((LIST_TOP - LIST_BOTTOM - 44 - 30) / ROW_HEIGHT);
    }

    private int firstVisibleRow() {
        return Math.max(0, Math.min(selected - visibleRows() / 2, entries.size() - visibleRows()));
    }

    private float rowTop(int row) {
        return LIST_TOP - 44 - (row - firstVisibleRow()) * ROW_HEIGHT;
    }

    private int rowAt(float x, float y) {
        if (x < LIST_LEFT || x > LIST_LEFT + LIST_WIDTH || y > LIST_TOP - 44) {
            return -1;
        }
        int offset = (int) Math.floor((LIST_TOP - 44 - y) / ROW_HEIGHT);
        return offset >= visibleRows() ? -1 : firstVisibleRow() + offset;
    }

    private void layOutButtons() {
        buttons.clear();
        MatchSettings settings = navigator.settings();
        float chipWidth = (WIDTH - MARGIN * 2 - 32) / 3;
        float chipY = 492;
        buttons.add(Button.of(Integer.toString(settings.bots()), MARGIN, chipY, chipWidth, 50,
                () -> change(navigator.settings().nextBotCount())));
        buttons.add(Button.of(settings.difficultyLabel(), MARGIN + chipWidth + 16, chipY, chipWidth, 50,
                () -> change(navigator.settings().nextDifficulty())));
        buttons.add(Button.of("Best of " + settings.bestOf(), MARGIN + (chipWidth + 16) * 2, chipY, chipWidth, 50,
                () -> change(navigator.settings().nextMatchLength())));

        float width = (WIDTH - MARGIN * 2 - 4 * 12) / 5;
        float y = 32;
        boolean saved = current().level().isPresent();
        buttons.add(Button.of("Enter  Play", MARGIN, y, width, 44, this::playSelected).selected(true));
        buttons.add(Button.of("E  Edit", MARGIN + (width + 12), y, width, 44, this::editSelected).enabled(saved));
        buttons.add(Button.of("N  New level", MARGIN + (width + 12) * 2, y, width, 44, this::newLevel));
        buttons.add(Button.of("Del  Delete", MARGIN + (width + 12) * 3, y, width, 44, this::deleteSelected)
                .enabled(saved));
        buttons.add(Button.of("Esc  Quit", MARGIN + (width + 12) * 4, y, width, 44, () -> Gdx.app.exit()));
    }

    @Override
    public void render(float delta) {
        clock += delta;
        ScreenUtils.clear(Palette.BACKGROUND);
        viewport.apply();
        shapes.setProjectionMatrix(camera.combined);
        batch.setProjectionMatrix(camera.combined);
        layOutButtons();

        batch.begin();
        MiniMap.draw(batch, sheet, sampleArena, -60, -60, WIDTH + 120, HEIGHT + 120);
        batch.end();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(Palette.DIM_OVERLAY);
        shapes.rect(0, 0, WIDTH, HEIGHT);
        Ui.panel(shapes, LIST_LEFT, LIST_BOTTOM, LIST_WIDTH, LIST_TOP - LIST_BOTTOM, Palette.PANEL, Palette.PANEL_EDGE);
        Ui.panel(shapes, PREVIEW_LEFT, LIST_BOTTOM, PREVIEW_WIDTH, LIST_TOP - LIST_BOTTOM, Palette.PANEL,
                Palette.PANEL_EDGE);
        int first = firstVisibleRow();
        int last = Math.min(entries.size(), first + visibleRows());
        for (int row = first; row < last; row++) {
            if (row == selected) {
                Ui.panel(shapes, LIST_LEFT + 10, rowTop(row) - ROW_HEIGHT + 4, LIST_WIDTH - 20, ROW_HEIGHT - 4,
                        Palette.BUTTON_SELECTED, Palette.BUTTON_SELECTED_EDGE);
            }
        }
        buttons.drawBoxes(shapes);
        shapes.end();

        batch.begin();
        drawTitle();
        drawChipLabels();
        Ui.text(batch, font, "Arenas", UiFont.SMALL, Palette.TEXT_DIM, LIST_LEFT + 18, LIST_TOP - 22, Ui.Align.LEFT);
        for (int row = first; row < last; row++) {
            Entry entry = entries.get(row);
            float top = rowTop(row);
            Ui.text(batch, font, entry.label(), UiFont.NORMAL, Palette.TEXT, LIST_LEFT + 24, top - 19, Ui.Align.LEFT);
            Ui.text(batch, font, entry.detail(), UiFont.SMALL, entry.playable() ? Palette.TEXT_DIM : Palette.PROBLEM,
                    LIST_LEFT + 24, top - 41, Ui.Align.LEFT);
        }
        Ui.text(batch, font, "Saved in ~/.blastarena/levels", UiFont.SMALL, Color.GRAY,
                LIST_LEFT + LIST_WIDTH / 2, LIST_BOTTOM + 20, Ui.Align.CENTRE);
        drawPreview();
        Ui.text(batch, font, message, UiFont.SMALL, Palette.ACCENT, WIDTH / 2, 97, Ui.Align.CENTRE);
        buttons.drawLabels(batch, font, UiFont.SMALL);
        batch.end();
    }

    private void drawTitle() {
        float centreY = HEIGHT - 92;
        float width = Ui.width(font, "BLAST ARENA", UiFont.TITLE);
        float bombSize = 64;
        float left = (WIDTH - width - bombSize - 20) / 2;
        Ui.shadowText(batch, font, "Blast Arena", UiFont.TITLE, Palette.ACCENT, left, centreY, Ui.Align.LEFT);
        float bob = 4 * (float) Math.sin(clock * 3);
        Sprite bomb = ((int) (clock * 6)) % 2 == 0 ? Sprite.BOMB_0 : Sprite.BOMB_1;
        batch.draw(sheet.get(bomb), left + width + 20, centreY - bombSize / 2 + bob, bombSize, bombSize);
        Ui.text(batch, font, "Last one standing wins", UiFont.SMALL, Palette.TEXT_DIM, WIDTH / 2, centreY - 58,
                Ui.Align.CENTRE);
    }

    private void drawChipLabels() {
        float chipWidth = (WIDTH - MARGIN * 2 - 32) / 3;
        String[] labels = {"B  Bots", "D  Difficulty", "M  Match"};
        for (int i = 0; i < labels.length; i++) {
            Ui.text(batch, font, labels[i], UiFont.SMALL, Palette.TEXT_DIM, MARGIN + (chipWidth + 16) * i + 4, 556,
                    Ui.Align.LEFT);
        }
    }

    private void drawPreview() {
        Entry entry = current();
        float boxLeft = PREVIEW_LEFT + 16;
        float boxWidth = PREVIEW_WIDTH - 32;
        Ui.text(batch, font, "Preview", UiFont.SMALL, Palette.TEXT_DIM, PREVIEW_LEFT + 18, LIST_TOP - 22,
                Ui.Align.LEFT);
        float mapBottom = LIST_BOTTOM + 60;
        float mapHeight = LIST_TOP - 44 - mapBottom;
        if (entry.level().isPresent()) {
            MiniMap.draw(batch, sheet, entry.level().get(), boxLeft, mapBottom, boxWidth, mapHeight);
        } else {
            MiniMap.draw(batch, sheet, sampleArena, boxLeft, mapBottom, boxWidth, mapHeight);
        }
        String note = entry.level().isEmpty() ? "New crates each round"
                : entry.playable() ? "Ready to play" : "Fix it in the editor";
        Ui.text(batch, font, note, UiFont.SMALL, entry.playable() ? Palette.TEXT_DIM : Palette.PROBLEM,
                PREVIEW_LEFT + PREVIEW_WIDTH / 2, LIST_BOTTOM + 34, Ui.Align.CENTRE);
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
