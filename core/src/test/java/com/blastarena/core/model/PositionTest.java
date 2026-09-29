package com.blastarena.core.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PositionTest {

    @ParameterizedTest
    @CsvSource({"UP, 3, 2", "DOWN, 3, 4", "LEFT, 2, 3", "RIGHT, 4, 3"})
    void stepMovesOneTile(Direction direction, int expectedX, int expectedY) {
        assertThat(new Position(3, 3).step(direction)).isEqualTo(new Position(expectedX, expectedY));
    }

    @Test
    void neighboursAreTheFourOrthogonalTiles() {
        assertThat(new Position(1, 1).neighbours())
                .containsExactly(new Position(1, 0), new Position(1, 2), new Position(0, 1), new Position(2, 1));
    }

    @Test
    void manhattanDistance() {
        assertThat(new Position(1, 1).manhattanDistanceTo(new Position(4, 5))).isEqualTo(7);
    }

    @Test
    void equalByValue() {
        assertThat(new Position(2, 5)).isEqualTo(new Position(2, 5)).hasSameHashCodeAs(new Position(2, 5));
    }
}
