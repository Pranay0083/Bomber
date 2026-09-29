package com.blastarena.core.level;

/** What a level cell holds, with the letter it is saved as. */
public enum Cell {
    FLOOR('.'),
    WALL('#'),
    CRATE('x'),
    SPAWN('S'),
    /** Floor that may get a crate when the level is played, at the level's crate density. */
    CRATE_ZONE('?');

    private final char symbol;

    Cell(char symbol) {
        this.symbol = symbol;
    }

    public char symbol() {
        return symbol;
    }

    /** Whether a player could stand here once crate zones and crates are cleared. */
    public boolean isOpenWithoutCrates() {
        return this != WALL;
    }

    /** Whether a player can stand here at the start, counting every crate zone as a crate. */
    public boolean isOpenAtStart() {
        return this == FLOOR || this == SPAWN;
    }

    public static Cell fromSymbol(char symbol) {
        for (Cell cell : values()) {
            if (cell.symbol == symbol) {
                return cell;
            }
        }
        throw new IllegalArgumentException("Unknown level symbol '" + symbol + "'");
    }
}
