package com.blastarena.core.testing;

import com.blastarena.core.command.Command;
import com.blastarena.core.command.IdleCommand;
import com.blastarena.core.command.MoveCommand;
import com.blastarena.core.command.PlaceBombCommand;
import com.blastarena.core.control.Controller;
import com.blastarena.core.control.WorldView;
import com.blastarena.core.model.Direction;
import com.blastarena.core.model.PlayerId;
import java.util.Random;

/** Picks seeded random commands: mostly moves, sometimes a bomb. For soak and determinism tests. */
public final class RandomController implements Controller {

    private final PlayerId player;
    private final Random random;

    public RandomController(PlayerId player, long seed) {
        this.player = player;
        this.random = new Random(seed);
    }

    @Override
    public Command nextCommand(WorldView view) {
        int roll = random.nextInt(20);
        if (roll < 16) {
            return new MoveCommand(player, Direction.values()[roll % 4]);
        }
        return roll == 16 ? new PlaceBombCommand(player) : new IdleCommand(player);
    }
}
