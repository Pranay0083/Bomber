package com.blastarena.core.engine;

import static org.assertj.core.api.Assertions.assertThat;

import com.blastarena.core.event.RoundEnded;
import com.blastarena.core.model.GameConfig;
import com.blastarena.core.model.PlayerId;
import com.blastarena.core.rules.WinConditionChecker;
import com.blastarena.core.testing.AsciiWorld;
import org.junit.jupiter.api.Test;

class GamePhaseTest {

    private final GameWorld world = AsciiWorld.parse("1.2");
    private final PhaseContext context = new PhaseContext(world, new WinConditionChecker());

    @Test
    void startsWithACountdownUnlessItIsZero() {
        assertThat(GamePhase.initial(GameConfig.builder().countdownTicks(3).build())).isEqualTo(new Countdown(3, 2400));
        assertThat(GamePhase.initial(GameConfig.builder().countdownTicks(0).build())).isEqualTo(new Playing(0, 2400));
    }

    @Test
    void countdownRunsDownIntoPlay() {
        GamePhase phase = new Countdown(2, 100);

        phase = phase.next(context);
        assertThat(phase).isEqualTo(new Countdown(1, 100));
        assertThat(phase.isRunning()).isFalse();
        phase = phase.next(context);
        assertThat(phase).isEqualTo(new Playing(0, 100));
        assertThat(phase.isRunning()).isTrue();
    }

    @Test
    void playingTurnsIntoSuddenDeathAfterTheRoundLength() {
        GamePhase phase = new Playing(0, 3);

        phase = phase.next(context).next(context);
        assertThat(phase).isEqualTo(new Playing(2, 3));
        phase = phase.next(context);
        assertThat(phase).isEqualTo(new SuddenDeath(0));
        assertThat(phase.isRunning()).isTrue();
    }

    @Test
    void aResultEndsTheRoundDuringPlayOrSuddenDeath() {
        world.player(new PlayerId(2)).kill();

        assertThat(new Playing(5, 100).next(context)).isEqualTo(new RoundOver(RoundEnded.won(new PlayerId(1))));
        assertThat(new SuddenDeath(5).next(context)).isEqualTo(new RoundOver(RoundEnded.won(new PlayerId(1))));
    }

    @Test
    void roundOverStaysOver() {
        RoundOver over = new RoundOver(RoundEnded.draw());

        assertThat(over.next(context)).isSameAs(over);
        assertThat(over.isRunning()).isFalse();
    }
}
