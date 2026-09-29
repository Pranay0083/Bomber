package com.blastarena.core.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class DirectionTest {

    @Test
    void upPointsTowardsRowZero() {
        assertThat(Direction.UP.dy()).isEqualTo(-1);
        assertThat(Direction.UP.dx()).isZero();
    }

    @ParameterizedTest
    @EnumSource(Direction.class)
    void oppositeCancelsOut(Direction direction) {
        Direction opposite = direction.opposite();
        assertThat(direction.dx() + opposite.dx()).isZero();
        assertThat(direction.dy() + opposite.dy()).isZero();
        assertThat(opposite.opposite()).isEqualTo(direction);
    }
}
