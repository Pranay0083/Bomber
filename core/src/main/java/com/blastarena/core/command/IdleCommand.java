package com.blastarena.core.command;

import com.blastarena.core.model.PlayerId;

/** Do nothing this tick. */
public record IdleCommand(PlayerId player) implements Command {

    @Override
    public CommandStage stage() {
        return CommandStage.NONE;
    }

    @Override
    public void execute(CommandContext context) {
        // Standing still is a valid choice.
    }
}
