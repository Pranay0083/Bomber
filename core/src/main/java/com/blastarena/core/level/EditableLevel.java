package com.blastarena.core.level;

import com.blastarena.core.model.Position;
import com.blastarena.core.powerup.PowerUpType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** The level the editor paints on. Mutable; {@link #snapshot()} gives an immutable {@link LevelData} to save or play. */
public final class EditableLevel {

    private String name;
    private int width;
    private int height;
    private Cell[][] cells;
    private final Map<Position, PowerUpType> powerUps = new LinkedHashMap<>();
    private double crateDensity;

    private EditableLevel(LevelData level) {
        restore(level);
    }

    /** A new level: solid border, the classic pillars, and a spawn in each corner. */
    public static EditableLevel blank(String name, int width, int height) {
        List<String> rows = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            StringBuilder row = new StringBuilder();
            for (int x = 0; x < width; x++) {
                row.append(defaultCell(x, y, width, height).symbol());
            }
            rows.add(row.toString());
        }
        return new EditableLevel(new LevelData(LevelData.CURRENT_VERSION, name, width, height, rows, List.of(), 0.5));
    }

    public static EditableLevel from(LevelData level) {
        return new EditableLevel(level);
    }

    /** What a new level has at this cell: wall on the border and pillars, spawns in the corners, floor elsewhere. */
    static Cell defaultCell(int x, int y, int width, int height) {
        boolean border = x == 0 || y == 0 || x == width - 1 || y == height - 1;
        if (border || (x % 2 == 0 && y % 2 == 0)) {
            return Cell.WALL;
        }
        boolean cornerX = x == 1 || x == width - 2;
        boolean cornerY = y == 1 || y == height - 2;
        return cornerX && cornerY ? Cell.SPAWN : Cell.FLOOR;
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = Objects.requireNonNull(name, "name");
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public double crateDensity() {
        return crateDensity;
    }

    public void setCrateDensity(double crateDensity) {
        if (!(crateDensity >= 0.0 && crateDensity <= 1.0)) {
            throw new IllegalArgumentException("Crate density must be between 0 and 1, was " + crateDensity);
        }
        this.crateDensity = crateDensity;
    }

    public boolean isInside(Position position) {
        return position.x() >= 0 && position.x() < width && position.y() >= 0 && position.y() < height;
    }

    public Cell cellAt(Position position) {
        return cells[position.y()][position.x()];
    }

    public void setCell(Position position, Cell cell) {
        cells[position.y()][position.x()] = Objects.requireNonNull(cell, "cell");
    }

    public Optional<PowerUpType> powerUpAt(Position position) {
        return Optional.ofNullable(powerUps.get(position));
    }

    /** Puts a power-up on a cell, or removes it with {@code null}. */
    public void setPowerUp(Position position, PowerUpType type) {
        if (type == null) {
            powerUps.remove(position);
        } else {
            powerUps.put(position, type);
        }
    }

    public LevelData snapshot() {
        List<String> rows = new ArrayList<>(height);
        for (Cell[] row : cells) {
            StringBuilder text = new StringBuilder(width);
            for (Cell cell : row) {
                text.append(cell.symbol());
            }
            rows.add(text.toString());
        }
        List<PlacedPowerUp> placed = new ArrayList<>();
        powerUps.forEach((position, type) -> placed.add(new PlacedPowerUp(position, type)));
        return new LevelData(LevelData.CURRENT_VERSION, name, width, height, rows, placed, crateDensity);
    }

    /** Replaces everything with the given level; used for whole-level changes such as resizing. */
    public void restore(LevelData level) {
        this.name = level.name();
        this.width = level.width();
        this.height = level.height();
        this.crateDensity = level.crateDensity();
        this.cells = new Cell[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                cells[y][x] = level.cellAt(new Position(x, y));
            }
        }
        powerUps.clear();
        level.powerUps().forEach(powerUp -> powerUps.put(powerUp.position(), powerUp.type()));
    }

    /**
     * The same level at a new size: cells that still fit are kept, the new border is made solid,
     * and new cells get what a blank level would have there.
     */
    public static LevelData resized(LevelData level, int newWidth, int newHeight) {
        List<String> rows = new ArrayList<>(newHeight);
        for (int y = 0; y < newHeight; y++) {
            StringBuilder row = new StringBuilder(newWidth);
            for (int x = 0; x < newWidth; x++) {
                boolean border = x == 0 || y == 0 || x == newWidth - 1 || y == newHeight - 1;
                boolean kept = x < level.width() - 1 && y < level.height() - 1;
                Cell cell = border ? Cell.WALL
                        : kept ? level.cellAt(new Position(x, y))
                        : defaultCell(x, y, newWidth, newHeight);
                row.append(cell.symbol());
            }
            rows.add(row.toString());
        }
        List<PlacedPowerUp> powerUps = level.powerUps().stream()
                .filter(powerUp -> powerUp.x() < newWidth - 1 && powerUp.y() < newHeight - 1)
                .toList();
        return new LevelData(level.version(), level.name(), newWidth, newHeight, rows, powerUps, level.crateDensity());
    }
}
