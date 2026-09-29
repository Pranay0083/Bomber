package com.blastarena.desktop.screen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class FixedStepClockTest {

    private final FixedStepClock clock = new FixedStepClock(0.05f);

    @Test
    void runsOneTickPerFiftyMillisecondsWhateverTheFrameRate() {
        int ticks = 0;
        for (int frame = 0; frame < 144; frame++) {
            ticks += clock.advance(1f / 144f);
        }

        assertThat(ticks).isBetween(19, 20);
    }

    @Test
    void carriesLeftoverTimeAndReportsItAsAlpha() {
        assertThat(clock.advance(0.03f)).isZero();
        assertThat(clock.alpha()).isCloseTo(0.6f, within(0.001f));

        assertThat(clock.advance(0.03f)).isEqualTo(1);
        assertThat(clock.alpha()).isCloseTo(0.2f, within(0.001f));
    }

    @Test
    void aLongFrameIsCappedSoTheGameDoesNotRaceAhead() {
        assertThat(clock.advance(3f)).isEqualTo(5);
    }

    @Test
    void resetDropsLeftoverTime() {
        clock.advance(0.04f);
        clock.reset();

        assertThat(clock.alpha()).isZero();
    }
}
