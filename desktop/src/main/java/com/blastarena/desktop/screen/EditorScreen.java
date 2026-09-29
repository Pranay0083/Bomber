package com.blastarena.desktop.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.utils.Align;
import com.blastarena.desktop.ui.UiFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.blastarena.core.level.BrushTool;
import com.blastarena.core.level.Cell;
import com.blastarena.core.level.CommandHistory;
import com.blastarena.core.level.CustomLevelSource;
import com.blastarena.core.level.EditableLevel;
import com.blastarena.core.level.EditorTool;
import com.blastarena.core.level.FillTool;
import com.blastarena.core.level.LevelChangeCommand;
import com.blastarena.core.level.LevelData;
import com.blastarena.core.level.LevelProblem;
import com.blastarena.core.level.LevelRepository;
import com.blastarena.core.level.LevelValidator;
import com.blastarena.core.level.MirrorTool;
import com.blastarena.core.level.Paint;
import com.blastarena.core.level.PaintCommand;
import com.blastarena.core.level.RectangleTool;
import com.blastarena.core.model.GameConfig;
import com.blastarena.core.model.Position;
import com.blastarena.core.powerup.PowerUpType;
import com.blastarena.desktop.render.Palette;
import com.blastarena.desktop.render.Sprite;
import com.blastarena.desktop.render.SpriteSheet;
import com.blastarena.desktop.render.TileArt;
import com.blastarena.desktop.ui.Button;
import com.blastarena.desktop.ui.ButtonBar;
import com.blastarena.desktop.ui.Ui;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Paint a level, check it, save it and try it out. Left-click paints, right-click erases.
 * Every edit goes through {@link CommandHistory}, so undo and redo cover everything.
 */
public final class EditorScreen extends ScreenAdapter {

    private static final float WIDTH = MenuScreen.WIDTH;
    private static final float HEIGHT = MenuScreen.HEIGHT;
    private static final float PANEL_WIDTH = 300;
    private static final float MARGIN = 16;
    private static final float BUTTON_HEIGHT = 26;
    private static final float GAP = 6;

    /** One entry of the paint palette. */
    private record PaintChoice(String label, int key, Paint paint) {
    }

    private static final List<PaintChoice> PAINTS = List.of(
            new PaintChoice("1 Floor", Keys.NUM_1, new Paint.OfCell(Cell.FLOOR)),
            new PaintChoice("2 Wall", Keys.NUM_2, new Paint.OfCell(Cell.WALL)),
            new PaintChoice("3 Crate", Keys.NUM_3, new Paint.OfCell(Cell.CRATE)),
            new PaintChoice("4 Spawn", Keys.NUM_4, new Paint.OfCell(Cell.SPAWN)),
            new PaintChoice("5 Zone", Keys.NUM_5, new Paint.OfCell(Cell.CRATE_ZONE)),
            new PaintChoice("6 Bomb+", Keys.NUM_6, new Paint.OfPowerUp(PowerUpType.EXTRA_BOMB)),
            new PaintChoice("7 Range+", Keys.NUM_7, new Paint.OfPowerUp(PowerUpType.BLAST_RANGE)),
            new PaintChoice("8 Speed+", Keys.NUM_8, new Paint.OfPowerUp(PowerUpType.SPEED)));

    private final Navigator navigator;
    private final LevelRepository repository;
    private final EditableLevel level;
    private final CommandHistory history;
    private final LevelValidator validator = LevelValidator.standard();
    private final OrthographicCamera camera = new OrthographicCamera();
    private final Viewport viewport = new FitViewport(WIDTH, HEIGHT, camera);
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final SpriteBatch batch = new SpriteBatch();
    private final BitmapFont font = UiFont.create();
    private final SpriteSheet sheet = new SpriteSheet();
    private final ButtonBar buttons = new ButtonBar();
    private final GlyphLayout wrapped = new GlyphLayout();

    private List<LevelProblem> problems = List.of();
    private PaintChoice paint = PAINTS.get(1);
    private EditorTool baseTool = new BrushTool();
    private boolean mirror;
    private String savedName;
    private LevelData savedAs;
    private String message = "";
    private StringBuilder renaming;

    private Position dragStart;
    private Position dragEnd;
    private boolean erasing;
    private final Set<Position> brushed = new LinkedHashSet<>();
    private Position hover;

    public EditorScreen(Navigator navigator, LevelRepository repository, EditableLevel level, String savedName) {
        this.navigator = navigator;
        this.repository = repository;
        this.level = level;
        this.history = new CommandHistory(level);
        this.savedName = savedName;
        this.savedAs = savedName == null ? null : level.snapshot();
        refresh();
    }

    // ---- editing ----------------------------------------------------------------------------------------------

    private EditorTool tool() {
        return mirror ? new MirrorTool(baseTool) : baseTool;
    }

    private boolean isBrush() {
        return baseTool instanceof BrushTool;
    }

    private Paint eraser() {
        return paint.paint() instanceof Paint.OfCell ? new Paint.OfCell(Cell.FLOOR) : new Paint.RemovePowerUp();
    }

    private List<Position> pendingCells() {
        if (dragStart == null) {
            return List.of();
        }
        return isBrush() ? new ArrayList<>(brushed) : tool().cells(level, dragStart, dragEnd);
    }

    private void commitDrag() {
        List<Position> cells = pendingCells();
        if (!cells.isEmpty()) {
            history.execute(new PaintCommand(cells, erasing ? eraser() : paint.paint()));
            refresh();
        }
        dragStart = null;
        dragEnd = null;
        brushed.clear();
    }

    private void change(LevelChangeCommand command) {
        history.execute(command);
        refresh();
    }

    private void resizeLevel(int deltaWidth, int deltaHeight) {
        int width = level.width() + deltaWidth;
        int height = level.height() + deltaHeight;
        if (width < GameConfig.MIN_WIDTH || width > GameConfig.MAX_WIDTH
                || height < GameConfig.MIN_HEIGHT || height > GameConfig.MAX_HEIGHT) {
            message = "Sizes run from 9 x 7 to 21 x 17";
            return;
        }
        change(LevelChangeCommand.resize(width, height));
    }

    private void density(double delta) {
        double density = Math.round(Math.clamp(level.crateDensity() + delta, 0.0, 1.0) * 10) / 10.0;
        change(LevelChangeCommand.crateDensity(density));
    }

    private void undo() {
        message = history.undo() ? "Undone" : "Nothing to undo";
        refresh();
    }

    private void redo() {
        message = history.redo() ? "Redone" : "Nothing to redo";
        refresh();
    }

    private void refresh() {
        problems = validator.validate(level.snapshot());
    }

    private boolean hasUnsavedChanges() {
        return savedAs == null || !savedAs.equals(level.snapshot());
    }

    private void save() {
        LevelData snapshot = level.snapshot();
        try {
            repository.save(snapshot);
            if (savedName != null && !repository.sameFile(savedName, snapshot.name())) {
                repository.delete(savedName);
            }
            savedName = snapshot.name();
            savedAs = snapshot;
            message = problems.isEmpty() ? "Saved \"" + snapshot.name() + "\""
                    : "Saved, but it needs fixes before it can be played";
        } catch (IOException e) {
            message = "Could not save: " + e.getMessage();
        }
    }

    private void testPlay() {
        if (!problems.isEmpty()) {
            message = "Fix the problems listed before playing";
            return;
        }
        // A test play is a single round with the bots chosen in the menu.
        navigator.play(new CustomLevelSource(level.snapshot()), navigator.settings().withBestOf(1),
                () -> navigator.resume(this));
    }

    private void startRenaming() {
        renaming = new StringBuilder(level.name());
        message = "Type a name, Enter to keep it, Esc to cancel";
    }

    private void finishRenaming(boolean keep) {
        String name = renaming.toString().trim();
        renaming = null;
        message = "";
        if (keep && !name.isEmpty()) {
            change(LevelChangeCommand.rename(name));
        }
    }

    private void back() {
        if (hasUnsavedChanges() && !"Unsaved changes: press Esc again to leave without saving".equals(message)) {
            message = "Unsaved changes: press Esc again to leave without saving";
            return;
        }
        navigator.showMenu();
    }

    // ---- board geometry ---------------------------------------------------------------------------------------

    private float tileSize() {
        float areaWidth = WIDTH - PANEL_WIDTH - MARGIN * 2;
        float areaHeight = HEIGHT - MARGIN * 2;
        return (float) Math.floor(Math.min(areaWidth / level.width(), areaHeight / level.height()));
    }

    private float boardLeft() {
        return MARGIN + (WIDTH - PANEL_WIDTH - MARGIN * 2 - tileSize() * level.width()) / 2;
    }

    private float boardBottom() {
        return MARGIN + (HEIGHT - MARGIN * 2 - tileSize() * level.height()) / 2;
    }

    private float cellLeft(int x) {
        return boardLeft() + x * tileSize();
    }

    private float cellBottom(int y) {
        return boardBottom() + (level.height() - 1 - y) * tileSize();
    }

    private Optional<Position> cellAt(float worldX, float worldY) {
        int x = (int) Math.floor((worldX - boardLeft()) / tileSize());
        int rowFromBottom = (int) Math.floor((worldY - boardBottom()) / tileSize());
        Position position = new Position(x, level.height() - 1 - rowFromBottom);
        return level.isInside(position) ? Optional.of(position) : Optional.empty();
    }

    // ---- input ------------------------------------------------------------------------------------------------

    @Override
    public void show() {
        refresh();
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                Vector2 at = viewport.unproject(new Vector2(screenX, screenY));
                if (buttons.click(at.x, at.y)) {
                    return true;
                }
                cellAt(at.x, at.y).ifPresent(cell -> {
                    dragStart = cell;
                    dragEnd = cell;
                    erasing = button == Input.Buttons.RIGHT;
                    brushed.clear();
                    brushed.addAll(tool().cells(level, cell, cell));
                });
                return true;
            }

            @Override
            public boolean touchDragged(int screenX, int screenY, int pointer) {
                if (dragStart != null) {
                    Vector2 at = viewport.unproject(new Vector2(screenX, screenY));
                    cellAt(at.x, at.y).ifPresent(cell -> {
                        dragEnd = cell;
                        if (isBrush()) {
                            brushed.addAll(tool().cells(level, dragStart, cell));
                        }
                    });
                    hover = dragEnd;
                }
                return true;
            }

            @Override
            public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                if (dragStart != null) {
                    commitDrag();
                }
                return true;
            }

            @Override
            public boolean mouseMoved(int screenX, int screenY) {
                Vector2 at = viewport.unproject(new Vector2(screenX, screenY));
                hover = cellAt(at.x, at.y).orElse(null);
                return false;
            }

            @Override
            public boolean keyTyped(char character) {
                if (renaming != null && character >= ' ' && character != 127 && renaming.length() < 32) {
                    renaming.append(character);
                    return true;
                }
                return false;
            }

            @Override
            public boolean keyDown(int keycode) {
                return renaming != null ? renamingKey(keycode) : shortcut(keycode);
            }
        });
    }

    private boolean renamingKey(int keycode) {
        switch (keycode) {
            case Keys.ENTER -> finishRenaming(true);
            case Keys.ESCAPE -> finishRenaming(false);
            case Keys.BACKSPACE -> {
                if (!renaming.isEmpty()) {
                    renaming.setLength(renaming.length() - 1);
                }
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    private static boolean commandHeld() {
        return Gdx.input.isKeyPressed(Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Keys.CONTROL_RIGHT)
                || Gdx.input.isKeyPressed(Keys.SYM);
    }

    private boolean shortcut(int keycode) {
        boolean command = commandHeld();
        boolean shift = Gdx.input.isKeyPressed(Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Keys.SHIFT_RIGHT);
        if (command && keycode == Keys.Z) {
            if (shift) {
                redo();
            } else {
                undo();
            }
            return true;
        }
        if (command && keycode == Keys.Y) {
            redo();
            return true;
        }
        if (command && keycode == Keys.S) {
            save();
            return true;
        }
        for (PaintChoice choice : PAINTS) {
            if (choice.key() == keycode) {
                paint = choice;
                return true;
            }
        }
        switch (keycode) {
            case Keys.B -> baseTool = new BrushTool();
            case Keys.R -> baseTool = new RectangleTool();
            case Keys.F -> baseTool = new FillTool();
            case Keys.M -> mirror = !mirror;
            case Keys.N -> startRenaming();
            case Keys.T -> testPlay();
            case Keys.LEFT_BRACKET -> resizeLevel(-2, 0);
            case Keys.RIGHT_BRACKET -> resizeLevel(2, 0);
            case Keys.COMMA -> resizeLevel(0, -2);
            case Keys.PERIOD -> resizeLevel(0, 2);
            case Keys.MINUS -> density(-0.1);
            case Keys.EQUALS -> density(0.1);
            case Keys.ESCAPE -> back();
            default -> {
                return false;
            }
        }
        return true;
    }

    // ---- drawing ----------------------------------------------------------------------------------------------

    /** Section headings and where they go, filled in while laying out the buttons. */
    private final List<Object[]> headings = new ArrayList<>();
    private float problemsTop;

    private void heading(String text, float left, float y) {
        headings.add(new Object[] {text, left, y});
    }

    private void layOutButtons(float left, float top) {
        buttons.clear();
        headings.clear();
        float inner = PANEL_WIDTH - MARGIN * 2;
        float half = (inner - GAP) / 2;
        float third = (inner - GAP * 2) / 3;
        float quarter = (inner - GAP * 3) / 4;
        float y = top;

        heading("Tools", left, y);
        y -= 18 + BUTTON_HEIGHT;
        buttons.add(Button.of("B Brush", left, y, third, BUTTON_HEIGHT, () -> baseTool = new BrushTool())
                .selected(baseTool instanceof BrushTool));
        buttons.add(Button.of("R Rect", left + third + GAP, y, third, BUTTON_HEIGHT, () -> baseTool = new RectangleTool())
                .selected(baseTool instanceof RectangleTool));
        buttons.add(Button.of("F Fill", left + (third + GAP) * 2, y, third, BUTTON_HEIGHT, () -> baseTool = new FillTool())
                .selected(baseTool instanceof FillTool));
        y -= BUTTON_HEIGHT + GAP;
        buttons.add(Button.of("M Mirror " + (mirror ? "on" : "off"), left, y, inner, BUTTON_HEIGHT,
                () -> mirror = !mirror).selected(mirror));

        y -= 14;
        heading("Paint", left, y);
        y -= 18;
        for (int i = 0; i < PAINTS.size(); i++) {
            PaintChoice choice = PAINTS.get(i);
            if (i % 2 == 0) {
                y -= BUTTON_HEIGHT + (i == 0 ? 0 : GAP);
            }
            float x = left + (i % 2) * (half + GAP);
            buttons.add(Button.of(choice.label(), x, y, half, BUTTON_HEIGHT, () -> paint = choice)
                    .selected(choice == paint));
        }

        y -= 14;
        heading("Size " + level.width() + " x " + level.height(), left, y);
        y -= 18 + BUTTON_HEIGHT;
        buttons.add(Button.of("[ W-", left, y, quarter, BUTTON_HEIGHT, () -> resizeLevel(-2, 0)));
        buttons.add(Button.of("W+ ]", left + (quarter + GAP), y, quarter, BUTTON_HEIGHT, () -> resizeLevel(2, 0)));
        buttons.add(Button.of(", H-", left + (quarter + GAP) * 2, y, quarter, BUTTON_HEIGHT, () -> resizeLevel(0, -2)));
        buttons.add(Button.of("H+ .", left + (quarter + GAP) * 3, y, quarter, BUTTON_HEIGHT, () -> resizeLevel(0, 2)));

        y -= 14;
        heading("Crate zones " + Math.round(level.crateDensity() * 100) + "%", left, y);
        y -= 18 + BUTTON_HEIGHT;
        buttons.add(Button.of("- Fewer", left, y, half, BUTTON_HEIGHT, () -> density(-0.1)));
        buttons.add(Button.of("= More", left + half + GAP, y, half, BUTTON_HEIGHT, () -> density(0.1)));

        y -= 14;
        heading("Level", left, y);
        y -= 18 + BUTTON_HEIGHT;
        buttons.add(Button.of("Undo", left, y, half, BUTTON_HEIGHT, this::undo).enabled(history.canUndo()));
        buttons.add(Button.of("Redo", left + half + GAP, y, half, BUTTON_HEIGHT, this::redo).enabled(history.canRedo()));
        y -= BUTTON_HEIGHT + GAP;
        buttons.add(Button.of("N Rename", left, y, half, BUTTON_HEIGHT, this::startRenaming));
        buttons.add(Button.of("Save", left + half + GAP, y, half, BUTTON_HEIGHT, this::save));
        y -= BUTTON_HEIGHT + GAP;
        buttons.add(Button.of("T Play", left, y, half, BUTTON_HEIGHT, this::testPlay)
                .enabled(problems.isEmpty()).selected(problems.isEmpty()));
        buttons.add(Button.of("Esc Menu", left + half + GAP, y, half, BUTTON_HEIGHT, this::back));
        problemsTop = y - 22;
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(Palette.BACKGROUND);
        viewport.apply();
        shapes.setProjectionMatrix(camera.combined);
        batch.setProjectionMatrix(camera.combined);
        float panelLeft = WIDTH - PANEL_WIDTH;
        layOutButtons(panelLeft + MARGIN, HEIGHT - 84);

        batch.begin();
        drawCells();
        batch.end();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        drawPreview();
        shapes.setColor(Palette.HUD);
        shapes.rect(panelLeft, 0, PANEL_WIDTH, HEIGHT);
        buttons.drawBoxes(shapes);
        shapes.end();

        shapes.begin(ShapeRenderer.ShapeType.Line);
        drawOutlines();
        shapes.end();

        batch.begin();
        drawPanelText(panelLeft + MARGIN);
        buttons.drawLabels(batch, font);
        batch.end();
    }

    private void drawCells() {
        float size = tileSize();
        for (int y = 0; y < level.height(); y++) {
            for (int x = 0; x < level.width(); x++) {
                Position position = new Position(x, y);
                float left = cellLeft(x);
                float bottom = cellBottom(y);
                switch (level.cellAt(position)) {
                    case FLOOR -> TileArt.floor(batch, sheet, left, bottom, size, x, y);
                    case WALL -> TileArt.wall(batch, sheet, left, bottom, size);
                    case CRATE -> TileArt.crate(batch, sheet, left, bottom, size);
                    case SPAWN -> {
                        TileArt.floor(batch, sheet, left, bottom, size, x, y);
                        TileArt.draw(batch, sheet, Sprite.SPAWN_MARKER, left, bottom, size);
                    }
                    case CRATE_ZONE -> {
                        TileArt.floor(batch, sheet, left, bottom, size, x, y);
                        TileArt.draw(batch, sheet, Sprite.CRATE_ZONE, left, bottom, size);
                    }
                }
                level.powerUpAt(position).ifPresent(type -> TileArt.powerUp(batch, sheet, left, bottom, size, type));
            }
        }
    }

    private void drawPreview() {
        float size = tileSize();
        shapes.setColor(Palette.PREVIEW);
        for (Position cell : pendingCells()) {
            shapes.rect(cellLeft(cell.x()), cellBottom(cell.y()), size, size);
        }
    }

    private void drawOutlines() {
        float size = tileSize();
        shapes.setColor(Palette.PROBLEM);
        for (LevelProblem problem : problems) {
            problem.at().ifPresent(cell -> {
                shapes.rect(cellLeft(cell.x()) + 1, cellBottom(cell.y()) + 1, size - 2, size - 2);
                shapes.rect(cellLeft(cell.x()) + 3, cellBottom(cell.y()) + 3, size - 6, size - 6);
            });
        }
        if (hover != null && dragStart == null) {
            shapes.setColor(Palette.TEXT);
            for (Position cell : tool().cells(level, hover, hover)) {
                if (!(baseTool instanceof FillTool)) {
                    shapes.rect(cellLeft(cell.x()), cellBottom(cell.y()), size, size);
                }
            }
        }
    }

    private void drawPanelText(float left) {
        float inner = PANEL_WIDTH - MARGIN * 2;
        String name = renaming != null ? renaming + "_" : level.name() + (hasUnsavedChanges() ? " *" : "");
        Ui.text(batch, font, name, UiFont.NORMAL, renaming != null ? Palette.ACCENT : Palette.TEXT,
                left, HEIGHT - 30, Ui.Align.LEFT);
        Ui.text(batch, font, "Right-click erases", UiFont.SMALL, Palette.TEXT_DIM, left, HEIGHT - 58, Ui.Align.LEFT);
        for (Object[] heading : headings) {
            Ui.text(batch, font, (String) heading[0], UiFont.SMALL, Palette.ACCENT, (float) heading[1],
                    (float) heading[2] - 8, Ui.Align.LEFT);
        }

        float y = problemsTop;
        if (problems.isEmpty()) {
            Ui.text(batch, font, "Ready to play", UiFont.SMALL, Palette.GOOD, left, y, Ui.Align.LEFT);
        } else {
            Ui.text(batch, font, problems.size() + (problems.size() == 1 ? " problem" : " problems"), UiFont.SMALL,
                    Palette.PROBLEM, left, y, Ui.Align.LEFT);
            y -= 12;
            font.getData().setScale(UiFont.SMALL);
            font.setColor(Palette.PROBLEM);
            for (LevelProblem problem : problems.stream().limit(3).toList()) {
                wrapped.setText(font, problem.message(), Palette.PROBLEM, inner, Align.left, true);
                font.draw(batch, wrapped, left, y);
                y -= wrapped.height + 10;
            }
        }
        if (!message.isEmpty()) {
            font.getData().setScale(UiFont.SMALL);
            wrapped.setText(font, message, Palette.TEXT, inner, Align.left, true);
            font.draw(batch, wrapped, left, 16 + wrapped.height);
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
