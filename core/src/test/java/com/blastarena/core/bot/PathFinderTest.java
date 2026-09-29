package com.blastarena.core.bot;

import static org.assertj.core.api.Assertions.assertThat;

import com.blastarena.core.control.BombSnapshot;
import com.blastarena.core.control.WorldView;
import com.blastarena.core.model.PlayerId;
import com.blastarena.core.model.Position;
import com.blastarena.core.testing.BotWorlds;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PathFinderTest {

    private static final PlayerId ONE = new PlayerId(1);
    private static final PathFinder.Pace EVERY_TICK = new PathFinder.Pace(1, 1);

    private final PathFinder pathFinder = new PathFinder();

    @Test
    void findsTheShortestWayAroundWallsAndCrates() {
        WorldView view = BotWorlds.view(
                "1#...",
                ".#.x.",
                "....2");
        Position goal = new Position(2, 0);

        Optional<List<Position>> path = pathFinder.find(view, DangerMap.of(view), new Position(0, 0), EVERY_TICK,
                goal::equals, 20);

        assertThat(path).contains(List.of(
                new Position(0, 1), new Position(0, 2), new Position(1, 2), new Position(2, 2),
                new Position(2, 1), new Position(2, 0)));
    }

    @Test
    void bombsBlockThePath() {
        WorldView view = BotWorlds.view("1.B..");

        Optional<List<Position>> path = pathFinder.find(view, DangerMap.of(view), new Position(0, 0), EVERY_TICK,
                new Position(4, 0)::equals, 20);

        assertThat(path).isEmpty();
    }

    @Test
    void anAlreadyReachedGoalIsAnEmptyPath() {
        WorldView view = BotWorlds.view("1..");

        assertThat(pathFinder.find(view, DangerMap.of(view), new Position(0, 0), EVERY_TICK, position -> true, 5))
                .contains(List.of());
    }

    @Test
    void refusesToWalkThroughATileWhileItBurns() {
        WorldView view = BotWorlds.view(
                "1....",
                "###.#",
                "#...#");
        // Fire crosses (2, 0) and (3, 0) at tick 2, exactly when the walker would be there.
        DangerMap danger = DangerMap.of(view, List.of(new BombSnapshot(ONE, new Position(3, 1), 1, 2)));
        Position goal = new Position(4, 0);

        assertThat(pathFinder.find(view, danger, new Position(0, 0), EVERY_TICK, goal::equals, 20)).isEmpty();
        // Walking slower, the walker reaches (2, 0) at tick 5, after the fire has burned out.
        assertThat(pathFinder.find(view, danger, new Position(0, 0), new PathFinder.Pace(1, 5), goal::equals, 20))
                .isPresent();
    }

    @Test
    void theGoalMustBeSafeToStayOn() {
        WorldView view = BotWorlds.view("1...");
        DangerMap danger = DangerMap.of(view, List.of(new BombSnapshot(ONE, new Position(3, 0), 1, 30)));

        assertThat(pathFinder.find(view, danger, new Position(0, 0), EVERY_TICK, new Position(2, 0)::equals, 20))
                .isEmpty();
        assertThat(pathFinder.find(view, danger, new Position(0, 0), EVERY_TICK, new Position(1, 0)::equals, 20))
                .contains(List.of(new Position(1, 0)));
    }

    @Test
    void checksThatAPlannedPathIsStillSafe() {
        WorldView view = BotWorlds.view("1....");
        List<Position> path = List.of(new Position(1, 0), new Position(2, 0));
        DangerMap quiet = DangerMap.of(view);
        DangerMap blastOnTheWay = DangerMap.of(view, List.of(new BombSnapshot(ONE, new Position(3, 0), 1, 2)));

        assertThat(pathFinder.isSafe(view, quiet, new Position(0, 0), path, EVERY_TICK)).isTrue();
        assertThat(pathFinder.isSafe(view, blastOnTheWay, new Position(0, 0), path, EVERY_TICK)).isFalse();
        assertThat(pathFinder.isSafe(view, quiet, new Position(0, 0), List.of(new Position(2, 0)), EVERY_TICK))
                .as("steps must be adjacent").isFalse();
    }
}
