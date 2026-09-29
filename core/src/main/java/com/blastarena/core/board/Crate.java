package com.blastarena.core.board;

/**
 * A breakable block. It blocks movement and stops a blast, but the blast destroys it.
 * Whether a power-up drops is decided separately by the power-up factory.
 */
public record Crate() implements Tile, Destructible {

    @Override
    public boolean isWalkable() {
        return false;
    }

    @Override
    public boolean blocksBlast() {
        return true;
    }

    @Override
    public Tile destroy() {
        return new Floor();
    }
}
