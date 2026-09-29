package com.blastarena.core.level;

import java.util.function.UnaryOperator;

/** A whole-level change such as resizing, renaming or a new crate density, undone by restoring the level before it. */
public final class LevelChangeCommand implements EditorCommand {

    private final UnaryOperator<LevelData> change;
    private LevelData before;

    public LevelChangeCommand(UnaryOperator<LevelData> change) {
        this.change = change;
    }

    public static LevelChangeCommand resize(int width, int height) {
        return new LevelChangeCommand(level -> EditableLevel.resized(level, width, height));
    }

    public static LevelChangeCommand crateDensity(double density) {
        return new LevelChangeCommand(level -> new LevelData(level.version(), level.name(), level.width(),
                level.height(), level.rows(), level.powerUps(), density));
    }

    public static LevelChangeCommand rename(String name) {
        return new LevelChangeCommand(level -> level.withName(name));
    }

    @Override
    public boolean execute(EditableLevel level) {
        before = level.snapshot();
        LevelData after = change.apply(before);
        level.restore(after);
        return !after.equals(before);
    }

    @Override
    public void undo(EditableLevel level) {
        level.restore(before);
    }
}
