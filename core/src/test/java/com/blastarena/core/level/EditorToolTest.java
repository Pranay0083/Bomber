package com.blastarena.core.level;

import static org.assertj.core.api.Assertions.assertThat;

import com.blastarena.core.model.Position;
import java.util.List;
import org.junit.jupiter.api.Test;

class EditorToolTest {

    private final EditableLevel level = EditableLevel.blank("Tools", 9, 7);

    @Test
    void brushCoversTheCellUnderTheCursor() {
        assertThat(new BrushTool().cells(level, new Position(1, 1), new Position(3, 3)))
                .containsExactly(new Position(3, 3));
        assertThat(new BrushTool().cells(level, new Position(1, 1), new Position(20, 3))).isEmpty();
    }

    @Test
    void rectangleCoversEverythingBetweenTheCornersInAnyDirection() {
        List<Position> cells = new RectangleTool().cells(level, new Position(3, 3), new Position(1, 2));

        assertThat(cells).containsExactly(
                new Position(1, 2), new Position(2, 2), new Position(3, 2),
                new Position(1, 3), new Position(2, 3), new Position(3, 3));
    }

    @Test
    void rectangleIsClippedToTheLevel() {
        assertThat(new RectangleTool().cells(level, new Position(-5, -5), new Position(0, 0)))
                .containsExactly(new Position(0, 0));
    }

    @Test
    void fillCoversTheConnectedAreaOfTheSameKind() {
        new PaintCommand(List.of(new Position(3, 1), new Position(3, 2), new Position(3, 3), new Position(3, 4),
                new Position(3, 5)), new Paint.OfCell(Cell.CRATE)).execute(level);

        List<Position> cells = new FillTool().cells(level, new Position(1, 3), new Position(1, 3));

        // The crate column cuts the level in two, and the spawns in the corners are a different kind too.
        assertThat(cells).containsExactly(
                new Position(1, 2), new Position(1, 3), new Position(2, 3), new Position(1, 4));
    }

    @Test
    void mirrorRepeatsAnyToolInAllFourQuarters() {
        EditorTool mirrored = new MirrorTool(new BrushTool());

        assertThat(mirrored.cells(level, new Position(2, 1), new Position(2, 1))).containsExactly(
                new Position(2, 1), new Position(6, 1), new Position(2, 5), new Position(6, 5));
        assertThat(mirrored.name()).isEqualTo("Brush (mirrored)");
    }

    @Test
    void mirrorOnTheCentreLinesDoesNotRepeatCells() {
        assertThat(new MirrorTool(new BrushTool()).cells(level, new Position(4, 3), new Position(4, 3)))
                .containsExactly(new Position(4, 3));
    }

    @Test
    void mirroringARectangleKeepsTheLevelSymmetric() {
        List<Position> cells = new MirrorTool(new RectangleTool()).cells(level, new Position(1, 3), new Position(2, 3));
        new PaintCommand(cells, new Paint.OfCell(Cell.CRATE_ZONE)).execute(level);

        List<String> rows = level.snapshot().rows();
        for (String row : rows) {
            assertThat(row).isEqualTo(new StringBuilder(row).reverse().toString());
        }
        assertThat(rows).isEqualTo(rows.reversed());
    }
}
