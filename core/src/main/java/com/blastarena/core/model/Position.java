package com.blastarena.core.model;

import java.util.List;

/** A cell on the grid. Positions are plain coordinates; whether they are on the board is the board's concern. */
public record Position(int x, int y) {

    public Position step(Direction direction) {
        return new Position(x + direction.dx(), y + direction.dy());
    }

    /** The four orthogonal neighbours, in {@link Direction} order. */
    public List<Position> neighbours() {
        return List.of(step(Direction.UP), step(Direction.DOWN), step(Direction.LEFT), step(Direction.RIGHT));
    }

    public int manhattanDistanceTo(Position other) {
        return Math.abs(x - other.x) + Math.abs(y - other.y);
    }
}
