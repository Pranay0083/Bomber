package com.blastarena.core.control;

import com.blastarena.core.command.Command;

/**
 * Decides what one player does each tick. The keyboard and the bots both implement this,
 * and both see only a read-only {@link WorldView}.
 */
@FunctionalInterface
public interface Controller {

    /** Called once per running tick while this controller's player is alive. */
    Command nextCommand(WorldView view);
}
