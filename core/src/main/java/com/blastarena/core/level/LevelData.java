package com.blastarena.core.level;

import com.blastarena.core.model.Position;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A saved level. Rows use the same letters as the test boards: {@code #} wall, {@code x} crate, {@code .} floor,
 * {@code S} spawn, plus {@code ?} for a crate zone that is filled at {@code crateDensity} when the level is played.
 * The constructor only checks that the data hangs together; whether the level is playable is
 * {@link LevelValidator}'s job, so the editor can hold unfinished levels.
 */
public record LevelData(
        int version,
        String name,
        int width,
        int height,
        List<String> rows,
        List<PlacedPowerUp> powerUps,
        double crateDensity) {

    public static final int CURRENT_VERSION = 1;

    public LevelData {
        Objects.requireNonNull(name, "name");
        rows = List.copyOf(Objects.requireNonNull(rows, "rows"));
        powerUps = powerUps == null ? List.of() : List.copyOf(powerUps);
        if (width < 1 || height < 1) {
            throw new IllegalArgumentException("Level must be at least 1x1, was " + width + "x" + height);
        }
        if (rows.size() != height) {
            throw new IllegalArgumentException("Expected " + height + " rows, got " + rows.size());
        }
        for (int y = 0; y < height; y++) {
            String row = rows.get(y);
            if (row.length() != width) {
                throw new IllegalArgumentException("Row " + y + " has length " + row.length() + ", expected " + width);
            }
            for (int x = 0; x < width; x++) {
                Cell.fromSymbol(row.charAt(x));
            }
        }
        for (PlacedPowerUp powerUp : powerUps) {
            if (powerUp.x() < 0 || powerUp.x() >= width || powerUp.y() < 0 || powerUp.y() >= height) {
                throw new IllegalArgumentException("Power-up at " + powerUp.position() + " is outside the level");
            }
        }
        if (!(crateDensity >= 0.0 && crateDensity <= 1.0)) {
            throw new IllegalArgumentException("Crate density must be between 0 and 1, was " + crateDensity);
        }
    }

    public boolean isInside(Position position) {
        return position.x() >= 0 && position.x() < width && position.y() >= 0 && position.y() < height;
    }

    public Cell cellAt(Position position) {
        return Cell.fromSymbol(rows.get(position.y()).charAt(position.x()));
    }

    /** Spawn cells in reading order: the first is player 1's. */
    public List<Position> spawns() {
        List<Position> spawns = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (rows.get(y).charAt(x) == Cell.SPAWN.symbol()) {
                    spawns.add(new Position(x, y));
                }
            }
        }
        return spawns;
    }

    public LevelData withName(String newName) {
        return new LevelData(version, newName, width, height, rows, powerUps, crateDensity);
    }
}
