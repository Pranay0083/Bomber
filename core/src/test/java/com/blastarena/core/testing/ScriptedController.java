package com.blastarena.core.testing;

import com.blastarena.core.command.Command;
import com.blastarena.core.command.IdleCommand;
import com.blastarena.core.command.MoveCommand;
import com.blastarena.core.command.PlaceBombCommand;
import com.blastarena.core.control.Controller;
import com.blastarena.core.control.WorldView;
import com.blastarena.core.model.Direction;
import com.blastarena.core.model.PlayerId;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Plays a fixed list of commands, one per tick, then stands still.
 * Moves sent during a cooldown are ignored by the engine, so scripts repeat a move to wait for it.
 */
public final class ScriptedController implements Controller {

    private final PlayerId player;
    private final Deque<Command> script = new ArrayDeque<>();

    public ScriptedController(PlayerId player) {
        this.player = player;
    }

    public ScriptedController move(Direction direction, int ticks) {
        for (int i = 0; i < ticks; i++) {
            script.addLast(new MoveCommand(player, direction));
        }
        return this;
    }

    public ScriptedController bomb() {
        script.addLast(new PlaceBombCommand(player));
        return this;
    }

    public ScriptedController idle(int ticks) {
        for (int i = 0; i < ticks; i++) {
            script.addLast(new IdleCommand(player));
        }
        return this;
    }

    @Override
    public Command nextCommand(WorldView view) {
        return script.isEmpty() ? new IdleCommand(player) : script.removeFirst();
    }
}
