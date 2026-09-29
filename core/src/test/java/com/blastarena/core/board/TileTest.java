package com.blastarena.core.board;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TileTest {

    static Stream<Arguments> tileRules() {
        return Stream.of(
                Arguments.of(new Floor(), true, false),
                Arguments.of(new SolidWall(), false, true),
                Arguments.of(new Crate(), false, true));
    }

    @ParameterizedTest
    @MethodSource("tileRules")
    void walkabilityAndBlastBlocking(Tile tile, boolean walkable, boolean blocksBlast) {
        assertThat(tile.isWalkable()).isEqualTo(walkable);
        assertThat(tile.blocksBlast()).isEqualTo(blocksBlast);
    }

    @Test
    void destroyedCrateLeavesFloor() {
        assertThat(new Crate().destroy()).isEqualTo(new Floor());
    }

    @Test
    void onlyCratesAreDestructible() {
        assertThat(new Crate()).isInstanceOf(Destructible.class);
        assertThat(new SolidWall()).isNotInstanceOf(Destructible.class);
        assertThat(new Floor()).isNotInstanceOf(Destructible.class);
    }
}
