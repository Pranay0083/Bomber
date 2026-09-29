package com.blastarena.core.engine;

import com.blastarena.core.event.RoundEnded;
import com.blastarena.core.model.PlayerId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A best-of-N series of rounds. The first player to win a majority of N rounds wins the match;
 * a drawn round counts as played but scores for nobody.
 */
public final class Match {

    private final int bestOf;
    private final Map<PlayerId, Integer> wins = new LinkedHashMap<>();
    private int roundsPlayed;
    private int draws;

    public Match(int bestOf, List<PlayerId> players) {
        if (bestOf < 1 || bestOf % 2 == 0) {
            throw new IllegalArgumentException("A match is best of an odd number of rounds, not " + bestOf);
        }
        if (players.size() < 2) {
            throw new IllegalArgumentException("A match needs at least two players, not " + players.size());
        }
        this.bestOf = bestOf;
        players.stream().sorted().forEach(player -> wins.put(player, 0));
    }

    public int bestOf() {
        return bestOf;
    }

    /** Round wins needed to take the match. */
    public int winsNeeded() {
        return bestOf / 2 + 1;
    }

    public int roundsPlayed() {
        return roundsPlayed;
    }

    /** The number of the round being played, or the last round once the match is over. */
    public int currentRound() {
        return isOver() ? roundsPlayed : roundsPlayed + 1;
    }

    public int draws() {
        return draws;
    }

    public int wins(PlayerId player) {
        Integer count = wins.get(player);
        if (count == null) {
            throw new IllegalArgumentException("Player " + player.id() + " is not in this match");
        }
        return count;
    }

    /** Round wins for every player, in player-id order. */
    public Map<PlayerId, Integer> scores() {
        return Collections.unmodifiableMap(wins);
    }

    public void recordRound(RoundEnded result) {
        Objects.requireNonNull(result, "result");
        if (isOver()) {
            throw new IllegalStateException("The match is already over");
        }
        result.winner().ifPresent(this::wins);
        roundsPlayed++;
        result.winner().ifPresentOrElse(winner -> wins.merge(winner, 1, Integer::sum), () -> draws++);
    }

    public boolean isOver() {
        return winner().isPresent();
    }

    public Optional<PlayerId> winner() {
        return wins.entrySet().stream()
                .filter(entry -> entry.getValue() >= winsNeeded())
                .map(Map.Entry::getKey)
                .findFirst();
    }
}
