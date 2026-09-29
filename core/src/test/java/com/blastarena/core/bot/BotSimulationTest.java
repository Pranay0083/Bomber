package com.blastarena.core.bot;

import static org.assertj.core.api.Assertions.assertThat;

import com.blastarena.core.model.PlayerId;
import com.blastarena.core.testing.BotSimulation;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Bots playing each other headless over many seeds. The quick checks run in every build;
 * the 500-round soak runs with {@code ./gradlew :core:soakTest} and in CI.
 */
class BotSimulationTest {

    private static final int ROUND_TICKS = 2400;

    private static final List<List<Difficulty>> LINEUPS = List.of(
            List.of(Difficulty.MEDIUM, Difficulty.MEDIUM, Difficulty.MEDIUM, Difficulty.MEDIUM),
            List.of(Difficulty.HARD, Difficulty.HARD, Difficulty.HARD, Difficulty.HARD),
            List.of(Difficulty.HARD, Difficulty.MEDIUM, Difficulty.HARD, Difficulty.MEDIUM),
            List.of(Difficulty.MEDIUM, Difficulty.HARD, Difficulty.EASY, Difficulty.MEDIUM),
            List.of(Difficulty.EASY, Difficulty.HARD, Difficulty.MEDIUM, Difficulty.EASY));

    @Test
    void mediumBotsSurviveASoloRoundMoreThanNinetyPercentOfTheTime() {
        int rounds = 100;
        long survived = LongStream.range(0, rounds)
                .mapToObj(seed -> BotSimulation.play(seed, List.of(Difficulty.MEDIUM), ROUND_TICKS))
                .filter(result -> !result.survivors().isEmpty())
                .count();

        assertThat((double) survived / rounds).isGreaterThan(0.9);
    }

    @Test
    void mediumAndHardBotsDoNotKillThemselvesInQuickRounds() {
        assertThat(selfKillsOfMediumAndHard(40)).isEmpty();
    }

    @Test
    @Tag("soak")
    void fiveHundredRoundsWithoutExceptionsOrSelfKills() {
        assertThat(selfKillsOfMediumAndHard(500)).isEmpty();
    }

    /** Plays the lineups in turn and lists every self-kill by a Medium or Hard bot as "seed/player/difficulty". */
    private static List<String> selfKillsOfMediumAndHard(int rounds) {
        List<String> selfKills = new ArrayList<>();
        for (long seed = 0; seed < rounds; seed++) {
            List<Difficulty> lineup = LINEUPS.get((int) (seed % LINEUPS.size()));
            BotSimulation.Result result = BotSimulation.play(seed, lineup, ROUND_TICKS);
            for (PlayerId victim : result.selfKills()) {
                Difficulty difficulty = lineup.get(victim.id() - 1);
                if (difficulty != Difficulty.EASY) {
                    selfKills.add(seed + "/" + victim.id() + "/" + difficulty);
                }
            }
        }
        return selfKills;
    }
}
