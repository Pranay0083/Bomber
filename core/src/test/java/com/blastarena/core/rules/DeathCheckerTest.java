package com.blastarena.core.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.blastarena.core.engine.GameWorld;
import com.blastarena.core.entity.Player;
import com.blastarena.core.model.PlayerId;
import com.blastarena.core.testing.AsciiWorld;
import java.util.List;
import org.junit.jupiter.api.Test;

class DeathCheckerTest {

    private final ExplosionResolver resolver = new ExplosionResolver();
    private final DeathChecker deathChecker = new DeathChecker();

    @Test
    void playersInFireDieIncludingTheBombsOwner() {
        GameWorld world = AsciiWorld.parse("1B2.3");

        ChainReactionTest.apply(world, resolver.resolveChain(world, world.bombs()));

        assertThat(deathChecker.playersInFire(world)).extracting(player -> player.id().id()).containsExactly(1, 2);
    }

    @Test
    void wallsAndCratesShieldPlayers() {
        GameWorld world = AsciiWorld.parse(
                "1#.",
                "B.2",
                "x..",
                "3..");

        ChainReactionTest.apply(world, resolver.resolveChain(world, world.bombs()));

        assertThat(deathChecker.playersInFire(world)).extracting(Player::id).containsExactly(new PlayerId(1));
    }

    @Test
    void aChainReactionCatchesPlayersFarFromTheFirstBomb() {
        GameWorld world = AsciiWorld.parse("1BBB2.3");

        ChainReactionTest.apply(world, resolver.resolveChain(world, List.of(world.bombs().getFirst())));

        assertThat(deathChecker.playersInFire(world)).extracting(player -> player.id().id()).containsExactly(1, 2);
    }

    @Test
    void alreadyDeadPlayersAreNotReportedAgain() {
        GameWorld world = AsciiWorld.parse("1B2");
        world.player(new PlayerId(2)).kill();

        ChainReactionTest.apply(world, resolver.resolveChain(world, world.bombs()));

        assertThat(deathChecker.playersInFire(world)).extracting(player -> player.id().id()).containsExactly(1);
    }

    @Test
    void nobodyDiesWithoutFire() {
        GameWorld world = AsciiWorld.parse("1B2");

        assertThat(deathChecker.playersInFire(world)).isEmpty();
    }
}
