package com.blastarena.core.bot;

import com.blastarena.core.control.WorldView;
import com.blastarena.core.model.Position;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Breadth-first search over walkable tiles, aware of time. A walker makes its first step {@code firstMoveIn} ticks
 * from now and one more every {@code stepDelay} ticks; a path is only accepted if the walker is never on a tile
 * while it burns, and can stay on the final tile for good.
 */
public final class PathFinder {

    /** How a walker moves: when its first step lands and how many ticks each further step takes. */
    public record Pace(int firstMoveIn, int stepDelay) {

        public Pace {
            if (firstMoveIn < 1 || stepDelay < 1) {
                throw new IllegalArgumentException("Pace must be positive, was " + firstMoveIn + ", " + stepDelay);
            }
        }

        /** Tick (from now) at which the walker arrives on the {@code step}-th tile of a path, counting from 1. */
        int arrivalOf(int step) {
            return firstMoveIn + (step - 1) * stepDelay;
        }
    }

    /**
     * Shortest safe path from {@code start} to the nearest tile matching {@code goal}, not counting {@code start}.
     * Returns an empty list if {@code start} already matches and is safe to stay on,
     * and nothing if no safe path of at most {@code maxSteps} steps exists.
     */
    public Optional<List<Position>> find(WorldView view, DangerMap danger, Position start, Pace pace,
                                         Predicate<Position> goal, int maxSteps) {
        if (danger.isDeadlyBetween(start, 1, pace.firstMoveIn() - 1)) {
            return Optional.empty();
        }
        if (goal.test(start) && !danger.isDeadlyFrom(start, 1)) {
            return Optional.of(List.of());
        }
        Map<Position, Position> cameFrom = new HashMap<>();
        Map<Position, Integer> depth = new HashMap<>();
        Deque<Position> queue = new ArrayDeque<>();
        depth.put(start, 0);
        queue.add(start);

        while (!queue.isEmpty()) {
            Position current = queue.removeFirst();
            int step = depth.get(current) + 1;
            if (step > maxSteps) {
                continue;
            }
            int arrival = pace.arrivalOf(step);
            for (Position next : current.neighbours()) {
                if (depth.containsKey(next) || !isOpen(view, next)) {
                    continue;
                }
                if (goal.test(next) && !danger.isDeadlyFrom(next, arrival)) {
                    cameFrom.put(next, current);
                    return Optional.of(trace(cameFrom, start, next));
                }
                if (danger.isDeadlyBetween(next, arrival, arrival + pace.stepDelay() - 1)) {
                    continue;
                }
                depth.put(next, step);
                cameFrom.put(next, current);
                queue.addLast(next);
            }
        }
        return Optional.empty();
    }

    /** Whether walking this path from {@code start} at this pace is still safe, ending on a tile safe to stay on. */
    public boolean isSafe(WorldView view, DangerMap danger, Position start, List<Position> steps, Pace pace) {
        if (danger.isDeadlyBetween(start, 1, pace.firstMoveIn() - 1)) {
            return false;
        }
        Position previous = start;
        for (int i = 0; i < steps.size(); i++) {
            Position next = steps.get(i);
            if (next.manhattanDistanceTo(previous) != 1 || !isOpen(view, next)) {
                return false;
            }
            int arrival = pace.arrivalOf(i + 1);
            boolean last = i == steps.size() - 1;
            boolean deadly = last
                    ? danger.isDeadlyFrom(next, arrival)
                    : danger.isDeadlyBetween(next, arrival, arrival + pace.stepDelay() - 1);
            if (deadly) {
                return false;
            }
            previous = next;
        }
        return !steps.isEmpty() || !danger.isDeadlyFrom(start, 1);
    }

    /** On the board, walkable, and without a bomb. */
    public static boolean isOpen(WorldView view, Position position) {
        return view.isInside(position)
                && view.tileAt(position).isWalkable()
                && view.bombAt(position).isEmpty();
    }

    private static List<Position> trace(Map<Position, Position> cameFrom, Position start, Position end) {
        List<Position> path = new ArrayList<>();
        for (Position at = end; !at.equals(start); at = cameFrom.get(at)) {
            path.add(at);
        }
        Collections.reverse(path);
        return path;
    }
}
