package com.blastarena.core.board;

/** A tile that a blast can break. */
public interface Destructible {

    /** Returns the tile left behind once this one is destroyed. */
    Tile destroy();
}
