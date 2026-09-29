package com.blastarena.core.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class GameConfigTest {

    @Test
    void defaultsMatchTheSpec() {
        GameConfig config = GameConfig.builder().build();

        assertThat(config.width()).isEqualTo(13);
        assertThat(config.height()).isEqualTo(11);
        assertThat(config.fuseTicks()).isEqualTo(50);
        assertThat(config.fireTicks()).isEqualTo(10);
        assertThat(config.dropChance()).isEqualTo(0.3);
        assertThat(config.crateDensity()).isEqualTo(0.7);
        assertThat(config.roundLengthTicks()).isEqualTo(2400);
    }

    @Test
    void builderOverridesOnlyWhatIsSet() {
        GameConfig config = GameConfig.builder().width(15).seed(42L).build();

        assertThat(config.width()).isEqualTo(15);
        assertThat(config.seed()).isEqualTo(42L);
        assertThat(config.height()).isEqualTo(11);
    }

    @ParameterizedTest
    @ValueSource(ints = {7, 12, 23})
    void rejectsWidthsThatAreEvenOrOutOfRange(int width) {
        assertThatIllegalArgumentException().isThrownBy(() -> GameConfig.builder().width(width).build());
    }

    @ParameterizedTest
    @ValueSource(ints = {5, 10, 19})
    void rejectsHeightsThatAreEvenOrOutOfRange(int height) {
        assertThatIllegalArgumentException().isThrownBy(() -> GameConfig.builder().height(height).build());
    }

    @ParameterizedTest
    @ValueSource(doubles = {-0.1, 1.1, Double.NaN})
    void rejectsFractionsOutsideZeroToOne(double value) {
        assertThatIllegalArgumentException().isThrownBy(() -> GameConfig.builder().dropChance(value).build());
        assertThatIllegalArgumentException().isThrownBy(() -> GameConfig.builder().crateDensity(value).build());
    }

    @Test
    void rejectsNonPositiveTickCounts() {
        assertThatIllegalArgumentException().isThrownBy(() -> GameConfig.builder().fuseTicks(0).build());
        assertThatIllegalArgumentException().isThrownBy(() -> GameConfig.builder().fireTicks(0).build());
        assertThatIllegalArgumentException().isThrownBy(() -> GameConfig.builder().roundLengthTicks(0).build());
    }
}
