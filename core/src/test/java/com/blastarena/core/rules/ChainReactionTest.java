package com.blastarena.core.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.blastarena.core.engine.GameWorld;
import com.blastarena.core.entity.Bomb;
import com.blastarena.core.entity.Fire;
import com.blastarena.core.entity.Player;
import com.blastarena.core.model.PlayerId;
import com.blastarena.core.model.Position;
import com.blastarena.core.testing.AsciiBoard;
import com.blastarena.core.testing.AsciiWorld;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ChainReactionTest {

    private static final PlayerId ONE = new PlayerId(1);

    private final ExplosionResolver resolver = new ExplosionResolver();

    @Test
    void aBombHitByABlastExplodesInTheSameTickAndSpreadsFurther() {
        GameWorld world = AsciiWorld.parse("1BBB..");
        List<Bomb> bombs = world.bombs();
        Bomb first = bombs.get(0);

        List<Explosion> chain = resolver.resolveChain(world, List.of(first));

        assertThat(chain).extracting(Explosion::bomb).containsExactly(bombs.get(0), bombs.get(1), bombs.get(2));
        Set<Position> fire = allFire(chain);
        assertThat(fire).contains(new Position(0, 0), new Position(4, 0)).doesNotContain(new Position(5, 0));
    }

    @Test
    void theChainStopsWhereABlastFallsShort() {
        GameWorld world = AsciiWorld.parse("1B..B");

        List<Explosion> chain = resolver.resolveChain(world, List.of(world.bombs().getFirst()));

        assertThat(chain).hasSize(1);
    }

    @Test
    void aCrateBetweenBombsBreaksTheChain() {
        GameWorld world = AsciiWorld.parse("1BxB.");
        Bomb first = world.bombs().getFirst();

        List<Explosion> chain = resolver.resolveChain(world, List.of(first));

        assertThat(chain).extracting(Explosion::bomb).containsExactly(first);
        assertThat(chain.getFirst().cratesHit()).containsExactly(new Position(2, 0));
    }

    @Test
    void bombsThatTriggerEachOtherEachExplodeOnce() {
        GameWorld world = AsciiWorld.parse("1BB.");

        List<Explosion> chain = resolver.resolveChain(world, world.bombs());

        assertThat(chain).hasSize(2);
    }

    @Test
    void aCrateHitByTwoBlastsInOneChainStopsBoth() {
        GameWorld world = AsciiWorld.parse("1Bx.");
        Player player = world.player(ONE);
        player.upgrade(stats -> stats.withRange(3).withBombCapacity(2));
        Bomb big = new BombPlacer().place(world, player).orElseThrow();

        List<Explosion> chain = resolver.resolveChain(world, List.of(big));

        assertThat(chain).hasSize(2);
        assertThat(chain).allSatisfy(explosion -> {
            assertThat(explosion.cratesHit()).containsExactly(new Position(2, 0));
            assertThat(explosion.fireTiles()).doesNotContain(new Position(3, 0));
        });
    }

    @Test
    void dueBombsAndBombsSittingInFireGoOffThisTick() {
        GameWorld world = AsciiWorld.parse("1B.B.B");
        Bomb due = world.bombs().get(0);
        Bomb inFire = world.bombs().get(1);
        Bomb waiting = world.bombs().get(2);
        while (!due.isDue()) {
            due.tick();
        }
        world.explode(waiting, new Fire(Set.of(waiting.position(), inFire.position()), 10));

        assertThat(resolver.bombsToExplode(world)).containsExactly(due, inFire);
    }

    @Test
    void applyingAChainDestroysCratesAndHandsBombsBack() {
        GameWorld world = AsciiWorld.parse("1BBx");

        List<Explosion> chain = resolver.resolveChain(world, List.of(world.bombs().getFirst()));
        apply(world, chain);

        assertThat(world.bombs()).isEmpty();
        assertThat(world.player(ONE).activeBombs()).isZero();
        assertThat(AsciiBoard.render(world.board())).containsExactly("....");
    }

    static void apply(GameWorld world, List<Explosion> chain) {
        for (Explosion explosion : chain) {
            world.explode(explosion.bomb(), new Fire(explosion.fireTiles(), world.config().fireTicks()));
        }
        chain.stream()
                .flatMap(explosion -> explosion.cratesHit().stream())
                .distinct()
                .forEach(world::destroyTile);
    }

    private static Set<Position> allFire(List<Explosion> chain) {
        return chain.stream()
                .flatMap(explosion -> explosion.fireTiles().stream())
                .collect(Collectors.toSet());
    }
}
