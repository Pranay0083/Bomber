package com.blastarena.core.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import com.blastarena.core.event.RoundEnded;
import com.blastarena.core.model.PlayerId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MatchTest {

    private static final PlayerId ONE = new PlayerId(1);
    private static final PlayerId TWO = new PlayerId(2);
    private static final PlayerId THREE = new PlayerId(3);

    private final Match match = new Match(3, List.of(TWO, ONE, THREE));

    @ParameterizedTest
    @CsvSource({"1, 1", "3, 2", "5, 3"})
    void winsNeededIsAMajority(int bestOf, int needed) {
        assertThat(new Match(bestOf, List.of(ONE, TWO)).winsNeeded()).isEqualTo(needed);
    }

    @Test
    void startsAtRoundOneWithNoScores() {
        assertThat(match.currentRound()).isEqualTo(1);
        assertThat(match.scores()).containsExactly(
                Map.entry(ONE, 0), Map.entry(TWO, 0), Map.entry(THREE, 0));
        assertThat(match.isOver()).isFalse();
    }

    @Test
    void theFirstToAMajorityWins() {
        match.recordRound(RoundEnded.won(TWO));
        match.recordRound(RoundEnded.won(ONE));
        assertThat(match.isOver()).isFalse();
        assertThat(match.currentRound()).isEqualTo(3);

        match.recordRound(RoundEnded.won(TWO));

        assertThat(match.isOver()).isTrue();
        assertThat(match.winner()).contains(TWO);
        assertThat(match.wins(TWO)).isEqualTo(2);
        assertThat(match.currentRound()).isEqualTo(3);
    }

    @Test
    void drawsArePlayedButScoreForNobody() {
        match.recordRound(RoundEnded.draw());
        match.recordRound(RoundEnded.won(THREE));
        match.recordRound(RoundEnded.draw());

        assertThat(match.roundsPlayed()).isEqualTo(3);
        assertThat(match.draws()).isEqualTo(2);
        assertThat(match.isOver()).isFalse();
        assertThat(match.currentRound()).isEqualTo(4);
    }

    @Test
    void noRoundsAfterTheMatchIsOver() {
        match.recordRound(RoundEnded.won(ONE));
        match.recordRound(RoundEnded.won(ONE));

        assertThatIllegalStateException().isThrownBy(() -> match.recordRound(RoundEnded.won(TWO)));
    }

    @Test
    void rejectsEvenLengthsTooFewPlayersAndStrangers() {
        assertThatIllegalArgumentException().isThrownBy(() -> new Match(2, List.of(ONE, TWO)));
        assertThatIllegalArgumentException().isThrownBy(() -> new Match(3, List.of(ONE)));
        assertThatIllegalArgumentException().isThrownBy(() -> match.recordRound(RoundEnded.won(new PlayerId(4))));
    }
}
