package com.blastarena.core.board;

/** An indestructible wall. Blocks both movement and blasts. */
public record SolidWall() implements Tile {

    @Override
    public boolean isWalkable() {
        return false;
    }

    @Override
    public boolean blocksBlast() {
        return true;
    }
}
