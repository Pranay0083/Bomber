package com.blastarena.core.level;

import com.blastarena.core.model.Position;
import java.util.List;

/** Paints the cell under the cursor; dragging paints each cell passed over. */
public final class BrushTool implements EditorTool {

    @Override
    public List<Position> cells(EditableLevel level, Position start, Position end) {
        return level.isInside(end) ? List.of(end) : List.of();
    }

    @Override
    public String name() {
        return "Brush";
    }
}
