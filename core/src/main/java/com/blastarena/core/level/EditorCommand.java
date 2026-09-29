package com.blastarena.core.level;

/** One reversible edit (the Command pattern, with undo). */
public interface EditorCommand {

    /** Makes the edit and returns whether anything actually changed. */
    boolean execute(EditableLevel level);

    /** Reverses the last {@link #execute}. */
    void undo(EditableLevel level);
}
