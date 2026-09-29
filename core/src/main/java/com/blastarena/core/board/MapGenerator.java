package com.blastarena.core.board;

import com.blastarena.core.model.GameConfig;
import com.blastarena.core.model.Position;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Builds the classic arena: solid walls on the border and on every cell where both x and y are even,
 * crates scattered over the rest, and the four spawn corners kept clear.
 */
public final class MapGenerator {

    /**
     * Creates a new board. Cells are filled in row order, one random draw per free cell,
     * so the same config and the same seeded {@link Random} always give the same board.
     */
    public Board generate(GameConfig config, Random random) {
        int width = config.width();
        int height = config.height();
        List<Position> spawns = spawnCorners(width, height);
        Board board = new Board(width, height, spawns);
        Set<Position> keepClear = clearZone(board, spawns);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Position position = new Position(x, y);
                if (isSolid(position, width, height)) {
                    board.setTile(position, new SolidWall());
                } else if (!keepClear.contains(position) && random.nextDouble() < config.crateDensity()) {
                    board.setTile(position, new Crate());
                }
            }
        }
        return board;
    }

    /** Spawn order puts players 1 and 2 in opposite corners, so a two-player round starts as far apart as possible. */
    private static List<Position> spawnCorners(int width, int height) {
        int right = width - 2;
        int bottom = height - 2;
        return List.of(
                new Position(1, 1),
                new Position(right, bottom),
                new Position(right, 1),
                new Position(1, bottom));
    }

    private static boolean isSolid(Position position, int width, int height) {
        int x = position.x();
        int y = position.y();
        boolean border = x == 0 || y == 0 || x == width - 1 || y == height - 1;
        return border || (x % 2 == 0 && y % 2 == 0);
    }

    /** Each spawn plus its open neighbours; in a corner those are exactly two tiles. */
    private static Set<Position> clearZone(Board board, List<Position> spawns) {
        Set<Position> zone = new HashSet<>();
        for (Position spawn : spawns) {
            zone.add(spawn);
            for (Position neighbour : spawn.neighbours()) {
                if (!isSolid(neighbour, board.width(), board.height())) {
                    zone.add(neighbour);
                }
            }
        }
        return zone;
    }
}
