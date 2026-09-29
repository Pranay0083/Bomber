package com.blastarena.core.bot;

import com.blastarena.core.control.BombSnapshot;
import com.blastarena.core.control.PlayerSnapshot;
import com.blastarena.core.control.WorldView;
import com.blastarena.core.model.Position;
import java.util.List;
import java.util.Optional;

/**
 * Tries out a bomb before it is placed: pretends it is on the board and checks that a safe tile can still be
 * reached before it goes off. This is the golden rule: never place a bomb without a verified way out.
 */
public final class EscapeChecker {

    /** Longest escape the bots look for; far more than any fuse allows at normal speed. */
    public static final int MAX_ESCAPE_STEPS = 20;

    private final PathFinder pathFinder;

    public EscapeChecker(PathFinder pathFinder) {
        this.pathFinder = pathFinder;
    }

    /** The danger if {@code player} dropped a bomb at {@code at} on the next tick. */
    public DangerMap dangerWithBombAt(WorldView view, PlayerSnapshot player, Position at) {
        // Placed next tick, the fuse is counted down that same tick, so it goes off fuseTicks ticks from now.
        BombSnapshot pretend = new BombSnapshot(player.id(), at, player.stats().blastRange(), view.config().fuseTicks());
        return DangerMap.of(view, List.of(pretend));
    }

    /**
     * A safe way out after dropping a bomb at {@code at}, for a player who is standing there.
     * The tick spent placing the bomb delays the first step by one.
     */
    public Optional<List<Position>> escapeRoute(WorldView view, PlayerSnapshot player, Position at, DangerMap withBomb) {
        PathFinder.Pace pace = new PathFinder.Pace(
                Math.max(2, player.moveCooldown()), player.stats().moveDelayTicks());
        return pathFinder.find(view, withBomb, at, pace, position -> !withBomb.isThreatened(position), MAX_ESCAPE_STEPS);
    }

    public boolean canEscapeAfterPlacing(WorldView view, PlayerSnapshot player, Position at) {
        return escapeRoute(view, player, at, dangerWithBombAt(view, player, at)).isPresent();
    }
}
