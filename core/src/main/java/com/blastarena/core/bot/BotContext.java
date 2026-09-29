package com.blastarena.core.bot;

import com.blastarena.core.control.PlayerSnapshot;
import com.blastarena.core.control.WorldView;
import com.blastarena.core.model.Position;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Predicate;

/** Everything a strategy may look at for one decision. Built fresh each tick. */
public record BotContext(
        WorldView view,
        PlayerSnapshot me,
        DangerMap danger,
        PathFinder pathFinder,
        EscapeChecker escapeChecker,
        Random random) {

    /** When my next step can land and how long each step takes. */
    public PathFinder.Pace pace() {
        return new PathFinder.Pace(Math.max(1, me.moveCooldown()), me.stats().moveDelayTicks());
    }

    public boolean inDanger() {
        return danger.isThreatened(me.position());
    }

    /** I have a bomb left and my tile has none yet. */
    public boolean canPlaceBomb() {
        return me.activeBombs() < me.stats().bombCapacity() && view.bombAt(me.position()).isEmpty();
    }

    public List<PlayerSnapshot> opponents() {
        return view.players().stream()
                .filter(PlayerSnapshot::alive)
                .filter(player -> !player.id().equals(me.id()))
                .toList();
    }

    public Optional<List<Position>> findPath(Predicate<Position> goal, int maxSteps) {
        return pathFinder.find(view, danger, me.position(), pace(), goal, maxSteps);
    }

    /** Me, as if I were standing on another tile and ready to move. */
    public PlayerSnapshot meAt(Position position) {
        return new PlayerSnapshot(me.id(), position, true, me.stats(), 0, me.activeBombs());
    }
}
