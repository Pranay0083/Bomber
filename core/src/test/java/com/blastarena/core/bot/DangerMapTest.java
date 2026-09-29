package com.blastarena.core.bot;

import static org.assertj.core.api.Assertions.assertThat;

import com.blastarena.core.control.BombSnapshot;
import com.blastarena.core.control.WorldView;
import com.blastarena.core.model.PlayerId;
import com.blastarena.core.model.Position;
import com.blastarena.core.testing.BotWorlds;
import java.util.List;
import org.junit.jupiter.api.Test;

class DangerMapTest {

    private static final PlayerId ONE = new PlayerId(1);

    /** Renders ticks until fire: '.' never, a digit for up to 9 ticks, '+' for later, '*' for burning now. */
    private static String picture(WorldView view, DangerMap danger) {
        StringBuilder text = new StringBuilder();
        for (int y = 0; y < view.height(); y++) {
            for (int x = 0; x < view.width(); x++) {
                Position position = new Position(x, y);
                int ticks = danger.ticksUntilFire(position);
                char symbol = danger.isBurning(position) ? '*'
                        : ticks == DangerMap.NEVER ? '.'
                        : ticks <= 9 ? Character.forDigit(ticks, 10) : '+';
                text.append(symbol);
            }
            text.append('\n');
        }
        return text.toString();
    }

    @Test
    void nothingIsThreatenedWithoutBombs() {
        WorldView view = BotWorlds.view("1..", "...");

        DangerMap danger = DangerMap.of(view);

        assertThat(picture(view, danger)).isEqualTo("...\n...\n");
        assertThat(danger.isThreatened(new Position(1, 1))).isFalse();
    }

    @Test
    void aBombThreatensItsCrossUntilWallsAndCrates() {
        WorldView view = BotWorlds.view(
                "1......",
                "...#...",
                "..x....",
                ".......");
        DangerMap danger = DangerMap.of(view,
                List.of(new BombSnapshot(ONE, new Position(3, 2), 3, 5)));

        // Range 3 from (3, 2): stopped by the wall above, burns the crate on the left and stops there.
        assertThat(picture(view, danger)).isEqualTo("""
                .......
                .......
                ..55555
                ...5...
                """);
    }

    @Test
    void aBlastReachingAnotherBombMakesItGoOffAtTheSameTime() {
        WorldView view = BotWorlds.view(
                "1......",
                ".......");
        DangerMap danger = DangerMap.of(view, List.of(
                new BombSnapshot(ONE, new Position(2, 1), 1, 3),
                new BombSnapshot(ONE, new Position(3, 1), 1, 9),
                new BombSnapshot(ONE, new Position(4, 1), 1, 9)));

        assertThat(picture(view, danger)).isEqualTo("""
                ..333..
                .33333.
                """);
    }

    @Test
    void aCrateBrokenByAnEarlierBlastNoLongerStopsALaterOne() {
        WorldView view = BotWorlds.view("1.x...");
        DangerMap danger = DangerMap.of(view, List.of(
                new BombSnapshot(ONE, new Position(1, 0), 1, 2),
                new BombSnapshot(ONE, new Position(3, 0), 3, 8)));

        // The crate breaks at tick 2, so the range-3 blast at tick 8 carries on to the left edge.
        assertThat(picture(view, danger)).isEqualTo("222888\n");
    }

    @Test
    void crateHitByTwoBombsInTheSameTickStopsBoth() {
        WorldView view = BotWorlds.view("1.x.....");
        DangerMap danger = DangerMap.of(view, List.of(
                new BombSnapshot(ONE, new Position(1, 0), 1, 4),
                new BombSnapshot(ONE, new Position(3, 0), 1, 4),
                new BombSnapshot(ONE, new Position(0, 0), 5, 9)));

        // The range-5 bomb is set off at tick 4 by its neighbour and stops at the crate like the others.
        assertThat(picture(view, danger)).isEqualTo("44444...\n");
    }

    @Test
    void deadlyOnlyWhileTheFireBurns() {
        WorldView view = BotWorlds.view("1....");
        DangerMap danger = DangerMap.of(view, List.of(new BombSnapshot(ONE, new Position(2, 0), 1, 5)));
        Position hit = new Position(3, 0);

        assertThat(danger.isDeadlyAt(hit, 4)).isFalse();
        assertThat(danger.isDeadlyAt(hit, 5)).isTrue();
        assertThat(danger.isDeadlyAt(hit, 7)).isTrue();
        assertThat(danger.isDeadlyAt(hit, 8)).isFalse();
        assertThat(danger.isDeadlyBetween(hit, 1, 4)).isFalse();
        assertThat(danger.isDeadlyBetween(hit, 1, 5)).isTrue();
        assertThat(danger.isDeadlyFrom(hit, 8)).isFalse();
        assertThat(danger.isDeadlyFrom(hit, 2)).isTrue();
    }

    @Test
    void tilesOnFireNowAreDeadlyAndBombsInFireGoOffNextTick() {
        var engine = BotWorlds.engine("1.B.B...");
        for (int i = 0; i < 10; i++) {
            engine.tick();
        }
        WorldView view = engine.view();
        DangerMap danger = DangerMap.of(view,
                List.of(new BombSnapshot(ONE, new Position(3, 0), 1, 9)));

        assertThat(danger.isBurning(new Position(2, 0))).isTrue();
        assertThat(danger.isDeadlyAt(new Position(2, 0), 1)).isTrue();
        assertThat(danger.ticksUntilFire(new Position(4, 0))).isEqualTo(1);
    }

    @Test
    void fireAlreadyBurningIsOnlyDeadlyUntilItBurnsOut() {
        var engine = BotWorlds.engine("1.B...");
        for (int i = 0; i < 10; i++) {
            engine.tick();
        }
        // The bomb went off on tick 10 with 3 ticks of fire: it is still there at the end of ticks 11 and 12.
        WorldView view = engine.view();
        DangerMap danger = DangerMap.of(view);
        Position burning = new Position(3, 0);

        assertThat(view.fireTicksLeft(burning)).isEqualTo(3);
        assertThat(danger.isDeadlyAt(burning, 2)).isTrue();
        assertThat(danger.isDeadlyAt(burning, 3)).isFalse();
        assertThat(danger.isDeadlyFrom(burning, 3)).isFalse();

        engine.tick();
        engine.tick();
        // One tick left: still drawn as burning, but gone before the next kill check.
        assertThat(engine.view().fireTicksLeft(burning)).isEqualTo(1);
        assertThat(DangerMap.of(engine.view()).isBurning(burning)).isTrue();
        assertThat(DangerMap.of(engine.view()).isDeadlyAt(burning, 1)).isFalse();
        engine.tick();
        assertThat(engine.view().burningTiles()).isEmpty();
    }
}
