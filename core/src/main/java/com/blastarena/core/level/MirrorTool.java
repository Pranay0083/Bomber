package com.blastarena.core.level;

import com.blastarena.core.model.Position;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Wraps any tool and repeats its cells mirrored left-right and top-bottom (the Decorator pattern),
 * so the level stays symmetric and fair for four players.
 */
public final class MirrorTool implements EditorTool {

    private final EditorTool inner;

    public MirrorTool(EditorTool inner) {
        this.inner = inner;
    }

    @Override
    public List<Position> cells(EditableLevel level, Position start, Position end) {
        Set<Position> cells = new LinkedHashSet<>();
        for (Position position : inner.cells(level, start, end)) {
            int mirroredX = level.width() - 1 - position.x();
            int mirroredY = level.height() - 1 - position.y();
            cells.add(position);
            cells.add(new Position(mirroredX, position.y()));
            cells.add(new Position(position.x(), mirroredY));
            cells.add(new Position(mirroredX, mirroredY));
        }
        return new ArrayList<>(cells);
    }

    @Override
    public String name() {
        return inner.name() + " (mirrored)";
    }
}
