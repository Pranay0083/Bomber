package com.blastarena.core.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.blastarena.core.model.PlayerId;
import com.blastarena.core.model.Position;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BombTest {

    private static final PlayerId ONE = new PlayerId(1);
    private static final PlayerId TWO = new PlayerId(2);

    @Test
    void fuseCountsDownToDueAndStopsAtZero() {
        Bomb bomb = new Bomb(ONE, new Position(1, 1), 1, 2, Set.of());

        bomb.tick();
        assertThat(bomb.isDue()).isFalse();
        bomb.tick();
        assertThat(bomb.isDue()).isTrue();
        bomb.tick();
        assertThat(bomb.remainingFuse()).isZero();
    }

    @Test
    void onlyPlayersStandingOnItMayOccupyItUntilTheyLeave() {
        Bomb bomb = new Bomb(ONE, new Position(1, 1), 1, 50, Set.of(ONE));

        assertThat(bomb.mayBeOccupiedBy(ONE)).isTrue();
        assertThat(bomb.mayBeOccupiedBy(TWO)).isFalse();

        bomb.playerLeft(ONE);
        assertThat(bomb.mayBeOccupiedBy(ONE)).isFalse();
    }

    @Test
    void rejectsInvalidRangeAndFuse() {
        assertThatIllegalArgumentException().isThrownBy(() -> new Bomb(ONE, new Position(1, 1), 0, 50, Set.of()));
        assertThatIllegalArgumentException().isThrownBy(() -> new Bomb(ONE, new Position(1, 1), 1, 0, Set.of()));
    }
}
