package com.blastarena.core.bot;

import com.blastarena.core.command.Command;
import com.blastarena.core.command.IdleCommand;
import com.blastarena.core.control.Controller;
import com.blastarena.core.control.PlayerSnapshot;
import com.blastarena.core.control.WorldView;
import com.blastarena.core.model.PlayerId;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

/**
 * A computer player. It sees only the {@link WorldView} and sends the same commands as the keyboard.
 * It thinks again when its plan runs out, every {@code reactionDelayTicks} ticks, or straight away when the
 * next step of its plan has become unsafe.
 */
public final class BotController implements Controller {

    private final PlayerId player;
    private final BotStrategy strategy;
    private final int reactionDelayTicks;
    private final Random random;
    private final PathFinder pathFinder = new PathFinder();
    private final EscapeChecker escapeChecker = new EscapeChecker(pathFinder);
    private Plan plan;
    private long plannedAt;

    public BotController(PlayerId player, BotStrategy strategy, int reactionDelayTicks, long seed) {
        if (reactionDelayTicks < 1) {
            throw new IllegalArgumentException("Reaction delay must be at least 1 tick, was " + reactionDelayTicks);
        }
        this.player = Objects.requireNonNull(player, "player");
        this.strategy = Objects.requireNonNull(strategy, "strategy");
        this.reactionDelayTicks = reactionDelayTicks;
        this.random = new Random(seed);
    }

    public static BotController of(Difficulty difficulty, PlayerId player, long seed) {
        return new BotController(player, difficulty.newStrategy(), difficulty.reactionDelayTicks(), seed);
    }

    @Override
    public Command nextCommand(WorldView view) {
        Optional<PlayerSnapshot> found = view.player(player);
        if (found.isEmpty() || !found.get().alive()) {
            return new IdleCommand(player);
        }
        PlayerSnapshot me = found.get();
        BotContext context = new BotContext(view, me, DangerMap.of(view), pathFinder, escapeChecker, random);

        boolean rethink = plan == null
                || plan.isFinished(me)
                || view.tick() - plannedAt >= reactionDelayTicks
                || !plan.isStillSafe(context);
        if (rethink) {
            plan = strategy.plan(context);
            plannedAt = view.tick();
        }
        return plan.next(me);
    }
}
