package com.blastarena.core.control;

import com.blastarena.core.command.Command;
import com.blastarena.core.command.IdleCommand;
import com.blastarena.core.model.PlayerId;

/** A controller that never does anything. Useful as a stand-in opponent. */
public final class IdleController implements Controller {

    private final IdleCommand idle;

    public IdleController(PlayerId player) {
        this.idle = new IdleCommand(player);
    }

    @Override
    public Command nextCommand(WorldView view) {
        return idle;
    }
}
