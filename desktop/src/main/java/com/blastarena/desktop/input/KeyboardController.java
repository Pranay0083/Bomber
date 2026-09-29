package com.blastarena.desktop.input;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
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
 * Turns key presses into commands for one player. Arrow keys or WASD move while held;
 * if several are held, the one pressed last wins. Space drops a bomb once per press.
 */
public final class KeyboardController extends InputAdapter implements Controller {

    private final PlayerId player;
    private final Deque<Direction> heldDirections = new ArrayDeque<>();
    private boolean bombRequested;

    public KeyboardController(PlayerId player) {
        this.player = player;
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Keys.SPACE) {
            bombRequested = true;
            return true;
        }
        Direction direction = directionFor(keycode);
        if (direction == null) {
            return false;
        }
        heldDirections.remove(direction);
        heldDirections.addLast(direction);
        return true;
    }

    @Override
    public boolean keyUp(int keycode) {
        Direction direction = directionFor(keycode);
        if (direction == null) {
            return keycode == Keys.SPACE;
        }
        heldDirections.remove(direction);
        return true;
    }

    /** A bomb press takes priority for one tick; otherwise keep walking the latest held direction. */
    @Override
    public Command nextCommand(WorldView view) {
        if (bombRequested) {
            bombRequested = false;
            return new PlaceBombCommand(player);
        }
        Direction direction = heldDirections.peekLast();
        return direction == null ? new IdleCommand(player) : new MoveCommand(player, direction);
    }

    private static Direction directionFor(int keycode) {
        return switch (keycode) {
            case Keys.UP, Keys.W -> Direction.UP;
            case Keys.DOWN, Keys.S -> Direction.DOWN;
            case Keys.LEFT, Keys.A -> Direction.LEFT;
            case Keys.RIGHT, Keys.D -> Direction.RIGHT;
            default -> null;
        };
    }
}
