package com.blastarena.core.level;

import com.blastarena.core.model.Position;
import java.util.ArrayList;
import java.util.List;

/** Fills the rectangle between where the drag started and where it is now. */
public final class RectangleTool implements EditorTool {

    @Override
    public List<Position> cells(EditableLevel level, Position start, Position end) {
        List<Position> cells = new ArrayList<>();
        int left = Math.max(0, Math.min(start.x(), end.x()));
        int right = Math.min(level.width() - 1, Math.max(start.x(), end.x()));
        int top = Math.max(0, Math.min(start.y(), end.y()));
        int bottom = Math.min(level.height() - 1, Math.max(start.y(), end.y()));
        for (int y = top; y <= bottom; y++) {
            for (int x = left; x <= right; x++) {
                cells.add(new Position(x, y));
            }
        }
        return cells;
    }

    @Override
    public String name() {
        return "Rectangle";
    }
}
