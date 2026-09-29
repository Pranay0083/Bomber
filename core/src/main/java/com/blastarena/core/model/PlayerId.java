package com.blastarena.core.model;

/** Identifies a player. Ids start at 1, matching the digits used in ASCII test boards. */
public record PlayerId(int id) implements Comparable<PlayerId> {

    public PlayerId {
        if (id < 1) {
            throw new IllegalArgumentException("Player id must be at least 1, was " + id);
        }
    }

    @Override
    public int compareTo(PlayerId other) {
        return Integer.compare(id, other.id);
    }
}
