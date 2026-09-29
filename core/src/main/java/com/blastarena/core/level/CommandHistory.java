package com.blastarena.core.level;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/** Undo and redo stacks for one level. Unlimited; a new edit clears the redo stack. */
public final class CommandHistory {

    private final EditableLevel level;
    private final Deque<EditorCommand> undo = new ArrayDeque<>();
    private final Deque<EditorCommand> redo = new ArrayDeque<>();

    public CommandHistory(EditableLevel level) {
        this.level = Objects.requireNonNull(level, "level");
    }

    /** Runs the command. Edits that change nothing are not recorded, so undo never seems to do nothing. */
    public boolean execute(EditorCommand command) {
        boolean changed = command.execute(level);
        if (changed) {
            undo.push(command);
            redo.clear();
        }
        return changed;
    }

    public boolean undo() {
        if (undo.isEmpty()) {
            return false;
        }
        EditorCommand command = undo.pop();
        command.undo(level);
        redo.push(command);
        return true;
    }

    public boolean redo() {
        if (redo.isEmpty()) {
            return false;
        }
        EditorCommand command = redo.pop();
        command.execute(level);
        undo.push(command);
        return true;
    }

    public boolean canUndo() {
        return !undo.isEmpty();
    }

    public boolean canRedo() {
        return !redo.isEmpty();
    }
}
