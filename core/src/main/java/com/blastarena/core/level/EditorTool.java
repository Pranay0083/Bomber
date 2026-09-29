package com.blastarena.core.level;

import com.blastarena.core.model.Position;
import java.util.List;

/**
 * Decides which cells an edit covers (the Strategy pattern): switching tools swaps this, not the painting code.
 * {@code start} is where the mouse went down and {@code end} where it is now; the editor paints the result
 * as one undoable step.
 */
public interface EditorTool {

    List<Position> cells(EditableLevel level, Position start, Position end);

    String name();
}
