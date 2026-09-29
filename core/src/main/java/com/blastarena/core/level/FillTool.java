package com.blastarena.core.level;

import com.blastarena.core.bot.PathFinder;
import com.blastarena.core.model.Position;
import java.util.List;

/** Flood-fills the connected area of cells of the same kind as the one clicked. */
public final class FillTool implements EditorTool {

    @Override
    public List<Position> cells(EditableLevel level, Position start, Position end) {
        if (!level.isInside(start)) {
            return List.of();
        }
        Cell target = level.cellAt(start);
        return PathFinder.reachable(start, position -> level.isInside(position) && level.cellAt(position) == target)
                .stream()
                .sorted((a, b) -> a.y() != b.y() ? Integer.compare(a.y(), b.y()) : Integer.compare(a.x(), b.x()))
                .toList();
    }

    @Override
    public String name() {
        return "Fill";
    }
}
