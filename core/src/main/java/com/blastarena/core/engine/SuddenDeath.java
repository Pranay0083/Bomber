package com.blastarena.core.engine;

import com.blastarena.core.board.SolidWall;
import com.blastarena.core.entity.Player;
import com.blastarena.core.event.PlayerDied;
import com.blastarena.core.event.WallDropped;
import com.blastarena.core.model.Position;
import java.util.List;
import java.util.Optional;

/**
 * Play past the round length. Every few ticks a solid wall drops on the next tile of the {@link WallSpiral},
 * crushing anyone standing there, until one player is left.
 */
public record SuddenDeath(int elapsedTicks) implements GamePhase {

    @Override
    public boolean isRunning() {
        return true;
    }

    @Override
    public GamePhase next(PhaseContext context) {
        GameWorld world = context.world();
        if (elapsedTicks % world.config().suddenDeathIntervalTicks() == 0) {
            nextWall(world).ifPresent(position -> {
                context.events().accept(new WallDropped(position));
                for (Player crushed : world.dropWall(position)) {
                    context.events().accept(new PlayerDied(crushed.id(), position));
                }
            });
        }
        return context.winConditionChecker().check(world)
                .<GamePhase>map(RoundOver::new)
                .orElseGet(() -> new SuddenDeath(elapsedTicks + 1));
    }

    /** The first tile of the spiral that is not a wall yet. */
    public static Optional<Position> nextWall(GameWorld world) {
        return remainingWalls(world).stream().findFirst();
    }

    /** Tiles still to be walled, in the order they will fall. */
    public static List<Position> remainingWalls(GameWorld world) {
        return WallSpiral.order(world.board().width(), world.board().height()).stream()
                .filter(position -> !(world.board().tileAt(position) instanceof SolidWall))
                .toList();
    }

    /**
     * Ticks from now until the next wall lands: that wall, and every {@code interval} ticks another one,
     * lands at the end of that tick.
     */
    public int ticksUntilNextWall(int interval) {
        return Math.floorMod(-elapsedTicks, interval) + 1;
    }
}
