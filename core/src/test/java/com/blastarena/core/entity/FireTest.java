package com.blastarena.core.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.blastarena.core.model.Position;
import java.util.Set;
import org.junit.jupiter.api.Test;

class FireTest {

    @Test
    void burnsOnItsTilesUntilItExpires() {
        Fire fire = new Fire(Set.of(new Position(1, 1), new Position(2, 1)), 2);

        assertThat(fire.covers(new Position(2, 1))).isTrue();
        assertThat(fire.covers(new Position(3, 1))).isFalse();
        fire.tick();
        assertThat(fire.isExpired()).isFalse();
        fire.tick();
        assertThat(fire.isExpired()).isTrue();
    }

    @Test
    void rejectsEmptyOrZeroLengthFire() {
        assertThatIllegalArgumentException().isThrownBy(() -> new Fire(Set.of(), 10));
        assertThatIllegalArgumentException().isThrownBy(() -> new Fire(Set.of(new Position(1, 1)), 0));
    }
}
