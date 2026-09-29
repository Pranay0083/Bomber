package com.blastarena.core.board;

/** Open ground that players walk on and blasts pass through. */
public record Floor() implements Tile {

    @Override
    public boolean isWalkable() {
        return true;
    }

    @Override
    public boolean blocksBlast() {
        return false;
    }
}
