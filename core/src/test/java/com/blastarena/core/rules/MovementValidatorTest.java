package com.blastarena.core.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.blastarena.core.engine.GameWorld;
import com.blastarena.core.entity.Bomb;
import com.blastarena.core.entity.Player;
import com.blastarena.core.model.Direction;
import com.blastarena.core.model.PlayerId;
import com.blastarena.core.model.Position;
import com.blastarena.core.testing.AsciiWorld;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MovementValidatorTest {

    private static final PlayerId ONE = new PlayerId(1);
    private static final PlayerId TWO = new PlayerId(2);

    private final MovementValidator validator = new MovementValidator();

    @ParameterizedTest(name = "{0} is {1}")
    @CsvSource({"UP, blocked by a wall", "DOWN, blocked by a crate", "LEFT, open floor", "RIGHT, blocked by a bomb"})
    void onlyFloorWithoutABombCanBeEntered(Direction direction, String expectation) {
        GameWorld world = AsciiWorld.parse(
                "###",
                ".1B",
                ".x.");
        Player player = world.player(ONE);

        assertThat(validator.canMove(world, player, direction)).isEqualTo(direction == Direction.LEFT);
    }

    @Test
    void cannotLeaveTheBoard() {
        GameWorld world = AsciiWorld.parse("1.");

        assertThat(validator.canMove(world, world.player(ONE), Direction.LEFT)).isFalse();
        assertThat(validator.canMove(world, world.player(ONE), Direction.UP)).isFalse();
    }

    @Test
    void playersMayShareATile() {
        GameWorld world = AsciiWorld.parse("12");

        assertThat(validator.canMove(world, world.player(ONE), Direction.RIGHT)).isTrue();
    }

    @Test
    void cannotMoveWhileCoolingDownOrDead() {
        GameWorld world = AsciiWorld.parse("1.2.");
        Player one = world.player(ONE);
        world.movePlayer(one, new Position(1, 0));
        Player two = world.player(TWO);
        two.kill();

        assertThat(validator.canMove(world, one, Direction.RIGHT)).isFalse();
        assertThat(validator.canMove(world, two, Direction.RIGHT)).isFalse();
    }

    @Test
    void canWalkOffABombJustPlacedButNotBackOnto() {
        GameWorld world = AsciiWorld.parse("1..");
        Player player = world.player(ONE);
        world.addBomb(new Bomb(ONE, new Position(0, 0), 1, 50, Set.of(ONE)));

        assertThat(validator.canMove(world, player, Direction.RIGHT)).isTrue();
        world.movePlayer(player, new Position(1, 0));
        coolDown(player);

        assertThat(validator.canMove(world, player, Direction.LEFT)).isFalse();
    }

    @Test
    void anotherPlayerOnTheTileWhenTheBombLandsMayAlsoWalkOff() {
        GameWorld world = AsciiWorld.parse(".1.");
        Player one = world.player(ONE);
        Player two = new Player(TWO, new Position(1, 0));
        world = new GameWorld(world.config(), world.board(), List.of(one, two));
        world.addBomb(new Bomb(ONE, new Position(1, 0), 1, 50, Set.of(ONE, TWO)));

        assertThat(validator.canMove(world, two, Direction.LEFT)).isTrue();
        assertThat(validator.canMove(world, one, Direction.RIGHT)).isTrue();
    }

    @Test
    void aBombBlocksPlayersWhoWereNotOnIt() {
        GameWorld world = AsciiWorld.parse("1.2");
        world.addBomb(new Bomb(ONE, new Position(1, 0), 1, 50, Set.of()));

        assertThat(validator.canMove(world, world.player(TWO), Direction.LEFT)).isFalse();
        assertThat(validator.canMove(world, world.player(ONE), Direction.RIGHT)).isFalse();
    }

    private static void coolDown(Player player) {
        while (!player.canMove()) {
            player.tickCooldown();
        }
    }
}
