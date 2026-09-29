package com.blastarena.core.bot;

import com.blastarena.core.command.Command;
import com.blastarena.core.command.IdleCommand;
import com.blastarena.core.command.MoveCommand;
import com.blastarena.core.control.PlayerSnapshot;
import com.blastarena.core.model.Direction;
import com.blastarena.core.model.Position;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

/** Walk a path one tile at a time. Moves sent during a cooldown are simply repeated until they land. */
public final class PathPlan implements Plan {

    private final Deque<Position> remaining;

    public PathPlan(List<Position> steps) {
        this.remaining = new ArrayDeque<>(steps);
    }

    public List<Position> remaining() {
        return List.copyOf(remaining);
    }

    @Override
    public Command next(PlayerSnapshot me) {
        dropReached(me);
        Position target = remaining.peekFirst();
        if (target == null) {
            return new IdleCommand(me.id());
        }
        return directionTo(me.position(), target)
                .<Command>map(direction -> new MoveCommand(me.id(), direction))
                .orElseGet(() -> new IdleCommand(me.id()));
    }

    /** Done when the path is walked, or when the bot is no longer next to its next step. */
    @Override
    public boolean isFinished(PlayerSnapshot me) {
        dropReached(me);
        Position target = remaining.peekFirst();
        return target == null || target.manhattanDistanceTo(me.position()) != 1;
    }

    @Override
    public boolean isStillSafe(BotContext context) {
        dropReached(context.me());
        return context.pathFinder().isSafe(
                context.view(), context.danger(), context.me().position(), remaining(), context.pace());
    }

    private void dropReached(PlayerSnapshot me) {
        while (!remaining.isEmpty() && remaining.peekFirst().equals(me.position())) {
            remaining.removeFirst();
        }
    }

    private static Optional<Direction> directionTo(Position from, Position to) {
        for (Direction direction : Direction.values()) {
            if (from.step(direction).equals(to)) {
                return Optional.of(direction);
            }
        }
        return Optional.empty();
    }
}
