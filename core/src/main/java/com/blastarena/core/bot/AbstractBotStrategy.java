package com.blastarena.core.bot;

import com.blastarena.core.board.Crate;
import com.blastarena.core.board.SolidWall;
import com.blastarena.core.board.Tile;
import com.blastarena.core.control.PlayerSnapshot;
import com.blastarena.core.control.WorldView;
import com.blastarena.core.model.Direction;
import com.blastarena.core.model.Position;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * The decision order every bot follows (the Template Method pattern): flee, attack, collect, explore, wait.
 * Difficulties override the individual steps, never the order.
 *
 * <p>Two rules hold for every step: a bomb is only placed when {@link EscapeChecker} finds a way out,
 * and paths come from {@link PathFinder}, which never steps onto a tile while it burns.
 */
public abstract class AbstractBotStrategy implements BotStrategy {

    protected static final int SEARCH_STEPS = 40;

    @Override
    public final Plan plan(BotContext context) {
        if (context.inDanger()) {
            Optional<Plan> escape = flee(context);
            if (escape.isPresent()) {
                return escape.get();
            }
        }
        return attack(context)
                .or(() -> collect(context))
                .or(() -> explore(context))
                .orElseGet(WaitPlan::new);
    }

    /**
     * Standing where fire is coming: take the shortest safe path to a {@link #refuge} tile,
     * or failing that to any tile no blast will reach.
     */
    protected Optional<Plan> flee(BotContext context) {
        DangerMap danger = context.danger();
        return context.findPath(refuge(context, danger), EscapeChecker.MAX_ESCAPE_STEPS)
                .or(() -> context.findPath(position -> !danger.isThreatened(position), EscapeChecker.MAX_ESCAPE_STEPS))
                .filter(path -> !path.isEmpty())
                .map(PathPlan::new);
    }

    /**
     * Which tiles count as a safe place to run to, given this danger. It must rule out every threatened tile.
     * By default that is all it rules out; stricter bots also avoid dead ends and opponents.
     */
    protected Predicate<Position> refuge(BotContext context, DangerMap danger) {
        return position -> !danger.isThreatened(position);
    }

    /** Whether there is a way out after dropping a bomb at {@code at}, to a {@link #refuge} tile. */
    protected boolean canEscape(BotContext context, PlayerSnapshot bomber, Position at) {
        DangerMap withBomb = context.escapeChecker().dangerWithBombAt(context.view(), bomber, at);
        return context.escapeChecker()
                .escapeRoute(context.view(), bomber, at, withBomb, refuge(context, withBomb))
                .isPresent();
    }

    /** Bomb now if it would catch an opponent this strategy wants to go after, and there is a way out. */
    protected Optional<Plan> attack(BotContext context) {
        if (!context.canPlaceBomb() || context.opponents().isEmpty()) {
            return Optional.empty();
        }
        PlayerSnapshot me = context.me();
        DangerMap withBomb = context.escapeChecker().dangerWithBombAt(context.view(), me, me.position());
        boolean worthIt = context.opponents().stream().anyMatch(opponent -> shouldAttack(context, opponent, withBomb));
        if (worthIt && context.escapeChecker()
                .escapeRoute(context.view(), me, me.position(), withBomb, refuge(context, withBomb)).isPresent()) {
            return Optional.of(new PlaceBombPlan());
        }
        return Optional.empty();
    }

    /** Whether a bomb dropped here now is worth it against this opponent. By default: it would reach them. */
    protected boolean shouldAttack(BotContext context, PlayerSnapshot opponent, DangerMap withMyBomb) {
        return withMyBomb.isThreatened(opponent.position());
    }

    /** Pick up a power-up if one is close by along a safe path. */
    protected Optional<Plan> collect(BotContext context) {
        int range = collectRange();
        if (range <= 0) {
            return Optional.empty();
        }
        return context.findPath(position -> context.view().powerUpAt(position).isPresent(), range)
                .filter(path -> !path.isEmpty())
                .map(PathPlan::new);
    }

    /** How many steps a bot will walk for a power-up. */
    protected int collectRange() {
        return 6;
    }

    /** Bomb a target from here if possible, else walk to the nearest tile where a bomb would hit one. */
    protected Optional<Plan> explore(BotContext context) {
        PlayerSnapshot me = context.me();
        if (context.canPlaceBomb() && wouldHitTarget(context, me.position()) && canEscape(context, me, me.position())) {
            return Optional.of(new PlaceBombPlan());
        }
        Optional<Plan> towardsTarget = context.findPath(position -> isGoodBombSpot(context, position), SEARCH_STEPS)
                .filter(path -> !path.isEmpty())
                .map(PathPlan::new);
        return towardsTarget.or(() -> approachOpponent(context));
    }

    /** Nothing left to blow up nearby: head for the closest opponent. */
    protected Optional<Plan> approachOpponent(BotContext context) {
        List<Position> opponents = context.opponents().stream().map(PlayerSnapshot::position).toList();
        return context.findPath(opponents::contains, SEARCH_STEPS)
                .filter(path -> !path.isEmpty())
                .map(PathPlan::new);
    }

    /** Whether a bomb dropped at {@code position} would hit something worth hitting. By default: a crate. */
    protected boolean wouldHitTarget(BotContext context, Position position) {
        for (Position hit : blastFrom(context.view(), position, context.me().stats().blastRange())) {
            if (context.view().tileAt(hit) instanceof Crate && !context.danger().isThreatened(hit)) {
                return true;
            }
        }
        return false;
    }

    protected boolean isGoodBombSpot(BotContext context, Position position) {
        return context.view().bombAt(position).isEmpty()
                && wouldHitTarget(context, position)
                && canEscape(context, context.meAt(position), position);
    }

    /** Tiles a bomb at {@code origin} would burn, ignoring other bombs: stops at walls, and on the first crate. */
    protected static List<Position> blastFrom(WorldView view, Position origin, int range) {
        List<Position> tiles = new ArrayList<>();
        tiles.add(origin);
        for (Direction direction : Direction.values()) {
            Position position = origin;
            for (int step = 1; step <= range; step++) {
                position = position.step(direction);
                if (!view.isInside(position)) {
                    break;
                }
                Tile tile = view.tileAt(position);
                if (tile instanceof SolidWall) {
                    break;
                }
                tiles.add(position);
                if (tile instanceof Crate) {
                    break;
                }
            }
        }
        return tiles;
    }

    /** A single random step onto a neighbouring tile that is safe to stay on. */
    protected static Optional<Plan> wander(BotContext context) {
        List<Position> options = context.me().position().neighbours().stream()
                .filter(position -> PathFinder.isOpen(context.view(), position))
                .filter(position -> !context.danger().isThreatened(position))
                .toList();
        if (options.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new PathPlan(List.of(options.get(context.random().nextInt(options.size())))));
    }
}
