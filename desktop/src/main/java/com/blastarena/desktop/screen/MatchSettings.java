package com.blastarena.desktop.screen;

import com.blastarena.core.bot.Difficulty;
import java.util.List;
import java.util.Objects;

/** What the player picked in the menu: how many bots, how good they are, and how long a match lasts. */
public record MatchSettings(int bots, Difficulty difficulty, int bestOf) {

    public static final List<Integer> MATCH_LENGTHS = List.of(1, 3, 5);
    public static final int MAX_BOTS = 3;
    // Declared after the constants above: the constructor checks against them, and statics run in order.
    public static final MatchSettings DEFAULT = new MatchSettings(3, Difficulty.MEDIUM, 3);

    public MatchSettings {
        Objects.requireNonNull(difficulty, "difficulty");
        if (bots < 1 || bots > MAX_BOTS) {
            throw new IllegalArgumentException("Bots must be from 1 to " + MAX_BOTS + ", not " + bots);
        }
        if (!MATCH_LENGTHS.contains(bestOf)) {
            throw new IllegalArgumentException("Match length must be one of " + MATCH_LENGTHS + ", not " + bestOf);
        }
    }

    public MatchSettings nextBotCount() {
        return new MatchSettings(bots % MAX_BOTS + 1, difficulty, bestOf);
    }

    public MatchSettings nextDifficulty() {
        Difficulty[] all = Difficulty.values();
        return new MatchSettings(bots, all[(difficulty.ordinal() + 1) % all.length], bestOf);
    }

    public MatchSettings nextMatchLength() {
        int next = MATCH_LENGTHS.get((MATCH_LENGTHS.indexOf(bestOf) + 1) % MATCH_LENGTHS.size());
        return new MatchSettings(bots, difficulty, next);
    }

    public MatchSettings withBestOf(int rounds) {
        return new MatchSettings(bots, difficulty, rounds);
    }

    public String difficultyLabel() {
        String name = difficulty.name();
        return name.charAt(0) + name.substring(1).toLowerCase();
    }
}
