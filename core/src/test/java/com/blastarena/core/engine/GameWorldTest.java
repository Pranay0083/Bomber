package com.blastarena.core.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import com.blastarena.core.board.Board;
import com.blastarena.core.board.Floor;
import com.blastarena.core.board.MapGenerator;
import com.blastarena.core.entity.Bomb;
import com.blastarena.core.entity.Fire;
import com.blastarena.core.entity.Player;
import com.blastarena.core.entity.PowerUpDrop;
import com.blastarena.core.model.GameConfig;
import com.blastarena.core.model.PlayerId;
import com.blastarena.core.model.Position;
import com.blastarena.core.powerup.SpeedPowerUp;
import com.blastarena.core.testing.AsciiWorld;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GameWorldTest {

    private static final PlayerId ONE = new PlayerId(1);

    @Test
    void placesPlayersOnSpawnsInIdOrder() {
        GameConfig config = GameConfig.builder().build();
        Board board = new MapGenerator().generate(config, new Random(1L));

        GameWorld world = GameWorld.withPlayersOnSpawns(config, board, 3);

        assertThat(world.players()).extracting(Player::id)
                .containsExactly(new PlayerId(1), new PlayerId(2), new PlayerId(3));
        assertThat(world.players()).extracting(Player::position)
                .containsExactlyElementsOf(board.spawns().subList(0, 3));
    }

    @Test
    void rejectsMorePlayersThanSpawns() {
        GameConfig config = GameConfig.builder().build();
        Board board = new MapGenerator().generate(config, new Random(1L));

        assertThatIllegalArgumentException().isThrownBy(() -> GameWorld.withPlayersOnSpawns(config, board, 5));
    }

    @Test
    void playersAreKeptInIdOrderWhateverOrderTheyArriveIn() {
        GameWorld world = AsciiWorld.parse("2.1");

        assertThat(world.players()).extracting(player -> player.id().id()).containsExactly(1, 2);
    }

    @Test
    void addingABombCountsAgainstItsOwner() {
        GameWorld world = AsciiWorld.parse("1..");
        Bomb bomb = new Bomb(ONE, new Position(0, 0), 1, 50, Set.of(ONE));

        world.addBomb(bomb);

        assertThat(world.bombAt(new Position(0, 0))).contains(bomb);
        assertThat(world.player(ONE).activeBombs()).isEqualTo(1);
    }

    @Test
    void onlyOneBombPerTile() {
        GameWorld world = AsciiWorld.parse("1..");
        world.player(ONE).upgrade(stats -> stats.withBombCapacity(2));
        world.addBomb(new Bomb(ONE, new Position(1, 0), 1, 50, Set.of()));

        assertThatIllegalStateException()
                .isThrownBy(() -> world.addBomb(new Bomb(ONE, new Position(1, 0), 1, 50, Set.of())));
    }

    @Test
    void explodingABombReturnsItToItsOwnerAndLightsFire() {
        GameWorld world = AsciiWorld.parse("1B.");
        Bomb bomb = world.bombs().getFirst();

        world.explode(bomb, new Fire(Set.of(new Position(1, 0)), 10));

        assertThat(world.bombs()).isEmpty();
        assertThat(world.player(ONE).activeBombs()).isZero();
        assertThat(world.isBurning(new Position(1, 0))).isTrue();
        assertThat(world.isBurning(new Position(2, 0))).isFalse();
    }

    @Test
    void expiredFireIsRemoved() {
        GameWorld world = AsciiWorld.parse("1B.");
        world.explode(world.bombs().getFirst(), new Fire(Set.of(new Position(1, 0)), 2));

        world.tickFires(world.fires());
        assertThat(world.fires()).hasSize(1);
        world.tickFires(world.fires());
        assertThat(world.fires()).isEmpty();
    }

    @Test
    void destroyingACrateLeavesFloorButWallsCannotBeDestroyed() {
        GameWorld world = AsciiWorld.parse("1x#");

        world.destroyTile(new Position(1, 0));

        assertThat(world.board().tileAt(new Position(1, 0))).isEqualTo(new Floor());
        assertThatIllegalStateException().isThrownBy(() -> world.destroyTile(new Position(2, 0)));
    }

    @Test
    void movingUsesTheSpeedBasedDelay() {
        GameWorld world = AsciiWorld.parse("1..");
        Player player = world.player(ONE);
        player.upgrade(stats -> stats.withSpeedLevel(2));

        world.movePlayer(player, new Position(1, 0));

        assertThat(player.position()).isEqualTo(new Position(1, 0));
        assertThat(player.moveCooldown()).isEqualTo(3);
    }

    @Test
    void powerUpsLieOnlyOnFreeWalkableTiles() {
        GameWorld world = AsciiWorld.parse("1.x");

        world.addDrop(new PowerUpDrop(new Position(1, 0), new SpeedPowerUp()));

        assertThat(world.dropAt(new Position(1, 0))).isPresent();
        assertThatIllegalStateException()
                .isThrownBy(() -> world.addDrop(new PowerUpDrop(new Position(1, 0), new SpeedPowerUp())));
        assertThatIllegalStateException()
                .isThrownBy(() -> world.addDrop(new PowerUpDrop(new Position(2, 0), new SpeedPowerUp())));
    }

    @Test
    void collectingAppliesThePowerUpAndTakesItOffTheFloor() {
        GameWorld world = AsciiWorld.parse("1r.");
        Player player = world.player(ONE);
        PowerUpDrop drop = world.dropAt(new Position(1, 0)).orElseThrow();

        world.collect(player, drop);

        assertThat(player.stats().blastRange()).isEqualTo(2);
        assertThat(world.drops()).isEmpty();
    }
}
