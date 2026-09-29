package com.blastarena.core.command;

import com.blastarena.core.event.BombPlaced;
import com.blastarena.core.model.PlayerId;

/** Drop a bomb on the current tile. Ignored if the player has no bomb left or one is already there. */
public record PlaceBombCommand(PlayerId player) implements Command {

    @Override
    public CommandStage stage() {
        return CommandStage.PLACE_BOMB;
    }

    @Override
    public void execute(CommandContext context) {
        context.bombPlacer()
                .place(context.world(), context.world().player(player))
                .ifPresent(bomb -> context.events().accept(new BombPlaced(player, bomb.position())));
    }
}
