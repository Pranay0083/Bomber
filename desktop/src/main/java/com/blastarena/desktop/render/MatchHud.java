package com.blastarena.desktop.render;

import com.blastarena.core.model.PlayerId;
import java.util.Map;
import java.util.Optional;

/**
 * What the HUD shows about the match around the round.
 *
 * @param wins        round wins so far for each player
 * @param paused      whether the game is paused
 * @param matchWinner set once the match is decided
 */
public record MatchHud(int round, int bestOf, Map<PlayerId, Integer> wins, boolean paused,
                       Optional<PlayerId> matchWinner) {
}
