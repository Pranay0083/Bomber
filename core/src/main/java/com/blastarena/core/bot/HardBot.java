package com.blastarena.core.bot;

import com.blastarena.core.control.PlayerSnapshot;
import com.blastarena.core.model.Position;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Reacts every tick and hunts players. It bombs whenever its blast, counting chain reactions through other bombs,
 * would reach an opponent who is close by or who would have no way out; it prefers bomb spots that threaten a
 * nearby opponent over crates; it goes a long way for power-ups; and it only runs to safe tiles that are not
 * dead ends, so opponents find it hard to trap.
 */
public final class HardBot extends AbstractBotStrategy {

    private static final int CLOSE_RANGE = 4;
    /** Opponents further than this are left alone; Hard grows stronger on crates and power-ups first. */
    private static final int HUNT_STEPS = 8;

    @Override
    protected boolean shouldAttack(BotContext context, PlayerSnapshot opponent, DangerMap withMyBomb) {
        if (!withMyBomb.isThreatened(opponent.position())) {
            return false;
        }
        boolean close = opponent.position().manhattanDistanceTo(context.me().position()) <= CLOSE_RANGE;
        return close || isTrapped(context, opponent, withMyBomb);
    }

    /** Whether the opponent would have no safe tile to reach once this bomb is down. */
    private static boolean isTrapped(BotContext context, PlayerSnapshot opponent, DangerMap withMyBomb) {
        PathFinder.Pace pace = new PathFinder.Pace(
                Math.max(1, opponent.moveCooldown()), opponent.stats().moveDelayTicks());
        return context.pathFinder()
                .find(context.view(), withMyBomb, opponent.position(), pace,
                        position -> !withMyBomb.isThreatened(position), EscapeChecker.MAX_ESCAPE_STEPS)
                .isEmpty();
    }

    /** Somewhere no blast reaches, with a second way out, and not right next to an opponent who could bomb it. */
    @Override
    protected Predicate<Position> refuge(BotContext context, DangerMap danger) {
        return position -> {
            if (danger.isThreatened(position)) {
                return false;
            }
            long exits = position.neighbours().stream()
                    .filter(next -> PathFinder.isOpen(context.view(), next))
                    .count();
            boolean opponentNextToIt = context.opponents().stream()
                    .anyMatch(opponent -> opponent.position().manhattanDistanceTo(position) <= 1);
            return exits >= 2 && !opponentNextToIt;
        };
    }

    @Override
    protected int collectRange() {
        return 10;
    }

    @Override
    protected Optional<Plan> explore(BotContext context) {
        Optional<Plan> hunt = context.findPath(position -> threatensOpponentFrom(context, position), HUNT_STEPS)
                .filter(path -> !path.isEmpty())
                .map(PathPlan::new);
        return hunt.or(() -> super.explore(context));
    }

    private boolean threatensOpponentFrom(BotContext context, Position position) {
        if (context.view().bombAt(position).isPresent()) {
            return false;
        }
        boolean hitsOpponent = blastFrom(context.view(), position, context.me().stats().blastRange()).stream()
                .anyMatch(hit -> context.opponents().stream().anyMatch(opponent -> opponent.position().equals(hit)));
        return hitsOpponent && canEscape(context, context.meAt(position), position);
    }
}
