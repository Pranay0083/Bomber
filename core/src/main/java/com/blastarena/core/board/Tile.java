package com.blastarena.core.board;

/** What occupies a grid cell. Sealed so that switches over tile kinds are checked for completeness. */
public sealed interface Tile permits Floor, SolidWall, Crate {

    boolean isWalkable();

    /** Whether a blast stops when it reaches this tile. */
    boolean blocksBlast();
}
