package com.blastarena.core.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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

    @ParameterizedTest
    @CsvSource({"0, 5", "1, 4", "2, 3", "3, 2", "4, 1", "9, 1"})
    void eachSpeedLevelShortensTheMoveDelayDownToOneTick(int speedLevel, int expectedDelay) {
        assertThat(PlayerStats.DEFAULT.withSpeedLevel(speedLevel).moveDelayTicks()).isEqualTo(expectedDelay);
    }

    @Test
    void rejectsInvalidValues() {
        assertThatIllegalArgumentException().isThrownBy(() -> new PlayerStats(0, 1, 0));
        assertThatIllegalArgumentException().isThrownBy(() -> new PlayerStats(1, 0, 0));
        assertThatIllegalArgumentException().isThrownBy(() -> new PlayerStats(1, 1, -1));
    }
}
