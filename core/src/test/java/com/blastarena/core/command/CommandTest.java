package com.blastarena.core.command;

import static org.assertj.core.api.Assertions.assertThat;

import com.blastarena.core.engine.GameWorld;
import com.blastarena.core.event.BombPlaced;
import com.blastarena.core.event.GameEvent;
import com.blastarena.core.event.PlayerMoved;
import com.blastarena.core.model.Direction;
import com.blastarena.core.model.PlayerId;
import com.blastarena.core.model.Position;
import com.blastarena.core.rules.BombPlacer;
import com.blastarena.core.rules.MovementValidator;
import com.blastarena.core.testing.AsciiWorld;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CommandTest {

    private static final PlayerId ONE = new PlayerId(1);

    private final List<GameEvent> events = new ArrayList<>();

    private CommandContext contextFor(GameWorld world) {
        return new CommandContext(world, new MovementValidator(), new BombPlacer(), events::add);
    }

    @Test
    void moveStepsOneTileAndReportsIt() {
        GameWorld world = AsciiWorld.parse("1..");

        new MoveCommand(ONE, Direction.RIGHT).execute(contextFor(world));

        assertThat(world.player(ONE).position()).isEqualTo(new Position(1, 0));
        assertThat(events).containsExactly(new PlayerMoved(ONE, new Position(0, 0), new Position(1, 0)));
    }

    @Test
    void blockedMoveDoesNothing() {
        GameWorld world = AsciiWorld.parse("1#.");

        new MoveCommand(ONE, Direction.RIGHT).execute(contextFor(world));

        assertThat(world.player(ONE).position()).isEqualTo(new Position(0, 0));
        assertThat(events).isEmpty();
    }

    @Test
    void placeBombDropsABombAndReportsIt() {
        GameWorld world = AsciiWorld.parse(".1.");

        new PlaceBombCommand(ONE).execute(contextFor(world));

        assertThat(world.bombAt(new Position(1, 0))).isPresent();
        assertThat(events).containsExactly(new BombPlaced(ONE, new Position(1, 0)));
    }

    @Test
    void placeBombWithoutCapacityDoesNothing() {
        GameWorld world = AsciiWorld.parse("1B.");

        new PlaceBombCommand(ONE).execute(contextFor(world));

        assertThat(world.bombs()).hasSize(1);
        assertThat(events).isEmpty();
    }

    @Test
    void idleDoesNothing() {
        GameWorld world = AsciiWorld.parse("1..");

        new IdleCommand(ONE).execute(contextFor(world));

        assertThat(world.player(ONE).position()).isEqualTo(new Position(0, 0));
        assertThat(events).isEmpty();
    }

    @Test
    void movesRunBeforeBombPlacements() {
        assertThat(new MoveCommand(ONE, Direction.UP).stage()).isLessThan(new PlaceBombCommand(ONE).stage());
    }
}
