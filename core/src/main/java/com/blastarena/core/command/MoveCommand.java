package com.blastarena.core.command;

import com.blastarena.core.engine.GameWorld;
import com.blastarena.core.entity.Player;
import com.blastarena.core.event.PlayerMoved;
import com.blastarena.core.model.Direction;
import com.blastarena.core.model.PlayerId;
import com.blastarena.core.model.Position;

/** Step one tile. Ignored while the player is cooling down or the tile is blocked. */
public record MoveCommand(PlayerId player, Direction direction) implements Command {

    @Override
    public CommandStage stage() {
        return CommandStage.MOVE;
    }

    @Override
    public void execute(CommandContext context) {
        GameWorld world = context.world();
        Player mover = world.player(player);
        if (!context.movementValidator().canMove(world, mover, direction)) {
            return;
        }
        Position from = mover.position();
        Position to = from.step(direction);
        world.movePlayer(mover, to);
        context.events().accept(new PlayerMoved(player, from, to));
    }
}
