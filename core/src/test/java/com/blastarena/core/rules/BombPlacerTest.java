package com.blastarena.core.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.blastarena.core.engine.GameWorld;
import com.blastarena.core.entity.Bomb;
import com.blastarena.core.entity.Player;
import com.blastarena.core.model.GameConfig;
import com.blastarena.core.model.PlayerId;
import com.blastarena.core.model.Position;
import com.blastarena.core.testing.AsciiWorld;
import java.util.List;
import org.junit.jupiter.api.Test;

class BombPlacerTest {

    private static final PlayerId ONE = new PlayerId(1);
    private static final PlayerId TWO = new PlayerId(2);

    private final BombPlacer placer = new BombPlacer();

    @Test
    void placesABombWithThePlayersRangeAndTheConfiguredFuse() {
        GameWorld world = AsciiWorld.parse(GameConfig.builder().fuseTicks(40).build(), "1..");
        Player player = world.player(ONE);
        player.upgrade(stats -> stats.withRange(3));

        Bomb bomb = placer.place(world, player).orElseThrow();

        assertThat(bomb.owner()).isEqualTo(ONE);
        assertThat(bomb.position()).isEqualTo(new Position(0, 0));
        assertThat(bomb.range()).isEqualTo(3);
        assertThat(bomb.remainingFuse()).isEqualTo(40);
        assertThat(world.bombs()).containsExactly(bomb);
        assertThat(bomb.mayBeOccupiedBy(ONE)).isTrue();
    }

    @Test
    void defaultCapacityAllowsOneBombAtATime() {
        GameWorld world = AsciiWorld.parse("1..");
        Player player = world.player(ONE);
        placer.place(world, player);
        world.movePlayer(player, new Position(1, 0));

        assertThat(placer.canPlace(world, player)).isFalse();
        assertThat(placer.place(world, player)).isEmpty();
        assertThat(world.bombs()).hasSize(1);
    }

    @Test
    void extraCapacityAllowsMoreBombs() {
        GameWorld world = AsciiWorld.parse("1..");
        Player player = world.player(ONE);
        player.upgrade(stats -> stats.withBombCapacity(2));
        placer.place(world, player);
        world.movePlayer(player, new Position(1, 0));

        assertThat(placer.place(world, player)).isPresent();
        assertThat(world.bombs()).hasSize(2);
    }

    @Test
    void onlyOneBombPerTile() {
        GameWorld world = AsciiWorld.parse("1..");
        Player player = world.player(ONE);
        player.upgrade(stats -> stats.withBombCapacity(2));
        placer.place(world, player);

        assertThat(placer.place(world, player)).isEmpty();
    }

    @Test
    void deadPlayersCannotPlaceBombs() {
        GameWorld world = AsciiWorld.parse("1..");
        world.player(ONE).kill();

        assertThat(placer.place(world, world.player(ONE))).isEmpty();
    }

    @Test
    void everyoneOnTheTileMayWalkOffButNotPlayersElsewhere() {
        GameWorld base = AsciiWorld.parse("1.3");
        Player one = base.player(ONE);
        Player two = new Player(TWO, new Position(0, 0));
        GameWorld world = new GameWorld(base.config(), base.board(), List.of(one, two, base.player(new PlayerId(3))));

        Bomb bomb = placer.place(world, one).orElseThrow();

        assertThat(bomb.mayBeOccupiedBy(ONE)).isTrue();
        assertThat(bomb.mayBeOccupiedBy(TWO)).isTrue();
        assertThat(bomb.mayBeOccupiedBy(new PlayerId(3))).isFalse();
    }
}
