package com.blastarena.core.board;

import com.blastarena.core.model.Position;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** The tile grid plus the spawn points. Starts as all floor; owned and changed only by the game world. */
public final class Board {

    private final int width;
    private final int height;
    private final Tile[][] tiles;
    private final List<Position> spawns;

    /**
     * @param spawns spawn points in player order: the first is player 1's, and so on
     */
    public Board(int width, int height, List<Position> spawns) {
        if (width < 1 || height < 1) {
            throw new IllegalArgumentException("Board must be at least 1x1, was " + width + "x" + height);
        }
        this.width = width;
        this.height = height;
        this.tiles = new Tile[height][width];
        Floor floor = new Floor();
        for (Tile[] row : tiles) {
            Arrays.fill(row, floor);
        }
        this.spawns = List.copyOf(spawns);
        for (Position spawn : this.spawns) {
            requireInside(spawn);
        }
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public List<Position> spawns() {
        return spawns;
    }

    public boolean isInside(Position position) {
        return position.x() >= 0 && position.x() < width && position.y() >= 0 && position.y() < height;
    }

    public Tile tileAt(Position position) {
        requireInside(position);
        return tiles[position.y()][position.x()];
    }

    public void setTile(Position position, Tile tile) {
        requireInside(position);
        tiles[position.y()][position.x()] = Objects.requireNonNull(tile, "tile");
    }

    private void requireInside(Position position) {
        if (!isInside(position)) {
            throw new IndexOutOfBoundsException(position + " is outside the " + width + "x" + height + " board");
        }
    }
}
