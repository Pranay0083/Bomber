package com.blastarena.core.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.blastarena.core.engine.GameWorld;
import com.blastarena.core.event.RoundEnded;
import com.blastarena.core.model.PlayerId;
import com.blastarena.core.testing.AsciiWorld;
import org.junit.jupiter.api.Test;

class WinConditionCheckerTest {

    private final WinConditionChecker checker = new WinConditionChecker();

    @Test
    void roundGoesOnWhileTwoOrMorePlayersLive() {
        GameWorld world = AsciiWorld.parse("1.2.3");
        world.player(new PlayerId(3)).kill();

        assertThat(checker.check(world)).isEmpty();
    }

    @Test
    void lastPlayerStandingWins() {
        GameWorld world = AsciiWorld.parse("1.2.3");
        world.player(new PlayerId(1)).kill();
        world.player(new PlayerId(3)).kill();

        assertThat(checker.check(world)).contains(RoundEnded.won(new PlayerId(2)));
    }

    @Test
    void everyoneDeadIsADraw() {
        GameWorld world = AsciiWorld.parse("1.2");
        world.players().forEach(world::killPlayer);

        assertThat(checker.check(world)).contains(RoundEnded.draw());
    }

    @Test
    void soloRoundEndsOnlyWhenThePlayerDies() {
        GameWorld world = AsciiWorld.parse("1..");

        assertThat(checker.check(world)).isEmpty();
        world.player(new PlayerId(1)).kill();
        assertThat(checker.check(world)).contains(RoundEnded.draw());
    }
}
