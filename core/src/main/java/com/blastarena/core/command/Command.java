package com.blastarena.core.command;

import com.blastarena.core.model.PlayerId;

/**
 * One action a player wants to take this tick. Keyboards and bots produce the same commands,
 * so the engine cannot tell them apart. A command that the rules do not allow does nothing.
 */
public interface Command {

    PlayerId player();

    CommandStage stage();

    void execute(CommandContext context);
}
