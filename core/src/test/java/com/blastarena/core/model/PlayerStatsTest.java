package com.blastarena.core.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class PlayerStatsTest {

    @Test
    void defaultsMatchTheSpec() {
        assertThat(PlayerStats.DEFAULT).isEqualTo(new PlayerStats(1, 1, 0));
    }

    @Test
    void withMethodsReturnChangedCopiesAndLeaveTheOriginalAlone() {
        PlayerStats base = PlayerStats.DEFAULT;

        assertThat(base.withBombCapacity(3)).isEqualTo(new PlayerStats(3, 1, 0));
        assertThat(base.withRange(4)).isEqualTo(new PlayerStats(1, 4, 0));
        assertThat(base.withSpeedLevel(2)).isEqualTo(new PlayerStats(1, 1, 2));
        assertThat(base).isEqualTo(new PlayerStats(1, 1, 0));
    }

    @Test
    void rejectsInvalidValues() {
        assertThatIllegalArgumentException().isThrownBy(() -> new PlayerStats(0, 1, 0));
        assertThatIllegalArgumentException().isThrownBy(() -> new PlayerStats(1, 0, 0));
        assertThatIllegalArgumentException().isThrownBy(() -> new PlayerStats(1, 1, -1));
    }
}
