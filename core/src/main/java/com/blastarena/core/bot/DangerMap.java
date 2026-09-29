package com.blastarena.core.bot;

import com.blastarena.core.board.Crate;
import com.blastarena.core.board.SolidWall;
import com.blastarena.core.board.Tile;
import com.blastarena.core.control.BombSnapshot;
import com.blastarena.core.control.WorldView;
import com.blastarena.core.model.Direction;
import com.blastarena.core.model.Position;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * For every tile, how many ticks until fire reaches it, counting pending bombs and the chain reactions between them.
 *
 * <p>Times are ticks from now: a tile with {@code ticksUntilFire == 3} burns at the end of the third tick from now,
 * and keeps burning for the fire duration. A player standing there at the end of any of those ticks dies.
 * A bomb's time is its remaining fuse, cut short if another blast reaches it first.
 * A crate broken by an earlier blast no longer stops a later one.
 */
public final class DangerMap {

    public static final int NEVER = Integer.MAX_VALUE;

    private final int width;
    private final int height;
    private final int fireTicks;
    private final int[] fireIn;
    private final boolean[] burning;
    /** For fire burning now: the last tick (from now) at whose end it is still there; 0 if it goes out next tick. */
    private final int[] burningUntil;

    private DangerMap(int width, int height, int fireTicks) {
        this.width = width;
        this.height = height;
        this.fireTicks = fireTicks;
        this.fireIn = new int[width * height];
        this.burning = new boolean[width * height];
        this.burningUntil = new int[width * height];
        Arrays.fill(fireIn, NEVER);
    }

    public static DangerMap of(WorldView view) {
        return of(view, List.of());
    }

    /** The danger if these extra bombs were on the board too; used to try out a bomb before placing it. */
    public static DangerMap of(WorldView view, List<BombSnapshot> extraBombs) {
        DangerMap map = new DangerMap(view.width(), view.height(), view.config().fireTicks());
        for (Position position : view.burningTiles()) {
            // Fire with n ticks left is still there at the end of the next n - 1 ticks.
            map.burning[map.index(position)] = true;
            map.burningUntil[map.index(position)] = Math.max(0, view.fireTicksLeft(position) - 1);
        }
        List<BombSnapshot> bombs = new ArrayList<>(view.bombs());
        bombs.addAll(extraBombs);
        map.simulate(view, bombs);
        return map;
    }

    /** Explodes bombs in time order, so each blast knows which crates are already gone and which bombs it sets off. */
    private void simulate(WorldView view, List<BombSnapshot> bombs) {
        Map<Position, Integer> bombAt = new HashMap<>();
        int[] explodeIn = new int[bombs.size()];
        for (int i = 0; i < bombs.size(); i++) {
            BombSnapshot bomb = bombs.get(i);
            bombAt.put(bomb.position(), i);
            explodeIn[i] = isBurning(bomb.position()) ? 1 : Math.max(1, bomb.remainingFuse());
        }
        int[] crateGoneAt = new int[width * height];
        Arrays.fill(crateGoneAt, NEVER);
        boolean[] done = new boolean[bombs.size()];
        PriorityQueue<int[]> queue = new PriorityQueue<>((a, b) -> Integer.compare(a[0], b[0]));
        for (int i = 0; i < bombs.size(); i++) {
            queue.add(new int[] {explodeIn[i], i});
        }

        while (!queue.isEmpty()) {
            int[] next = queue.poll();
            int time = next[0];
            int bombIndex = next[1];
            if (done[bombIndex] || time != explodeIn[bombIndex]) {
                continue;
            }
            done[bombIndex] = true;
            BombSnapshot bomb = bombs.get(bombIndex);
            markFire(bomb.position(), time);
            for (Direction direction : Direction.values()) {
                Position position = bomb.position();
                for (int step = 1; step <= bomb.range(); step++) {
                    position = position.step(direction);
                    if (!view.isInside(position)) {
                        break;
                    }
                    Tile tile = view.tileAt(position);
                    if (tile instanceof SolidWall) {
                        break;
                    }
                    if (tile instanceof Crate && crateGoneAt[index(position)] >= time) {
                        markFire(position, time);
                        crateGoneAt[index(position)] = Math.min(crateGoneAt[index(position)], time);
                        break;
                    }
                    markFire(position, time);
                    Integer caught = bombAt.get(position);
                    if (caught != null && !done[caught] && explodeIn[caught] > time) {
                        explodeIn[caught] = time;
                        queue.add(new int[] {time, caught});
                    }
                }
            }
        }
    }

    private void markFire(Position position, int time) {
        int i = index(position);
        fireIn[i] = Math.min(fireIn[i], time);
    }

    private int index(Position position) {
        return position.y() * width + position.x();
    }

    public int fireTicks() {
        return fireTicks;
    }

    public boolean isBurning(Position position) {
        return burning[index(position)];
    }

    /** Ticks until a pending blast reaches this tile, or {@link #NEVER}. Fire already burning is not counted. */
    public int ticksUntilFire(Position position) {
        return fireIn[index(position)];
    }

    /** Burning now or about to be. */
    public boolean isThreatened(Position position) {
        return isBurning(position) || ticksUntilFire(position) != NEVER;
    }

    /** Whether a player on this tile at the end of the tick {@code tick} ticks from now would die. */
    public boolean isDeadlyAt(Position position, int tick) {
        return isDeadlyBetween(position, tick, tick);
    }

    /** Whether being on this tile at the end of any tick from {@code first} to {@code last} (inclusive) is deadly. */
    public boolean isDeadlyBetween(Position position, int first, int last) {
        if (last < first) {
            return false;
        }
        if (first <= burningUntil[index(position)]) {
            return true;
        }
        int start = ticksUntilFire(position);
        if (start == NEVER) {
            return false;
        }
        long end = (long) start + fireTicks - 1;
        return first <= end && last >= start;
    }

    /** Whether staying on this tile forever, starting at {@code tick}, is ever deadly. */
    public boolean isDeadlyFrom(Position position, int tick) {
        return isDeadlyBetween(position, tick, Integer.MAX_VALUE - 1);
    }

    @Override
    public String toString() {
        return "DangerMap[" + width + "x" + height + "]";
    }
}
