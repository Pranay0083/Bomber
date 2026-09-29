package com.blastarena.core.level;

import static org.assertj.core.api.Assertions.assertThat;

import com.blastarena.core.model.Position;
import com.blastarena.core.powerup.PowerUpType;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CommandHistoryTest {

    private static final Position MIDDLE = new Position(3, 3);

    private EditableLevel level;
    private CommandHistory history;
    private LevelData original;

    @BeforeEach
    void setUp() {
        level = EditableLevel.blank("Test", 9, 7);
        history = new CommandHistory(level);
        original = level.snapshot();
    }

    private void paint(Paint paint, Position... positions) {
        history.execute(new PaintCommand(List.of(positions), paint));
    }

    @Test
    void aBlankLevelHasABorderPillarsAndFourCornerSpawnsAndIsPlayable() {
        assertThat(original.rows()).containsExactly(
                "#########",
                "#S.....S#",
                "#.#.#.#.#",
                "#.......#",
                "#.#.#.#.#",
                "#S.....S#",
                "#########");
        assertThat(LevelValidator.standard().isPlayable(original)).isTrue();
    }

    @Test
    void undoRevertsAnEditAndRedoReappliesIt() {
        paint(new Paint.OfCell(Cell.CRATE), MIDDLE, new Position(4, 3));
        LevelData painted = level.snapshot();

        assertThat(history.undo()).isTrue();
        assertThat(level.snapshot()).isEqualTo(original);
        assertThat(history.redo()).isTrue();
        assertThat(level.snapshot()).isEqualTo(painted);
    }

    @Test
    void undoIsUnlimitedAndGoesBackInOrder() {
        for (int i = 0; i < 50; i++) {
            paint(new Paint.OfCell(i % 2 == 0 ? Cell.CRATE : Cell.CRATE_ZONE), MIDDLE);
        }

        int undone = 0;
        while (history.undo()) {
            undone++;
        }

        assertThat(undone).isEqualTo(50);
        assertThat(level.snapshot()).isEqualTo(original);
        assertThat(history.canUndo()).isFalse();
    }

    @Test
    void aNewEditClearsTheRedoStack() {
        paint(new Paint.OfCell(Cell.CRATE), MIDDLE);
        history.undo();
        paint(new Paint.OfCell(Cell.WALL), new Position(1, 3));

        assertThat(history.canRedo()).isFalse();
        assertThat(history.redo()).isFalse();
    }

    @Test
    void editsThatChangeNothingAreNotRecorded() {
        paint(new Paint.OfCell(Cell.FLOOR), MIDDLE);

        assertThat(history.canUndo()).isFalse();
    }

    @Test
    void powerUpsOnlyGoOnFloorOrCrateZonesAndPaintingOverOneClearsIt() {
        paint(new Paint.OfPowerUp(PowerUpType.SPEED), MIDDLE, new Position(2, 2));
        assertThat(level.powerUpAt(MIDDLE)).contains(PowerUpType.SPEED);
        assertThat(level.powerUpAt(new Position(2, 2))).isEmpty();

        paint(new Paint.OfCell(Cell.WALL), MIDDLE);
        assertThat(level.powerUpAt(MIDDLE)).isEmpty();

        history.undo();
        assertThat(level.powerUpAt(MIDDLE)).contains(PowerUpType.SPEED);
        assertThat(level.cellAt(MIDDLE)).isEqualTo(Cell.FLOOR);
    }

    @Test
    void resizingKeepsWhatFitsAndCanBeUndone() {
        paint(new Paint.OfCell(Cell.CRATE), MIDDLE);

        history.execute(LevelChangeCommand.resize(11, 9));

        assertThat(level.width()).isEqualTo(11);
        assertThat(level.cellAt(MIDDLE)).isEqualTo(Cell.CRATE);
        assertThat(LevelValidator.standard().validate(level.snapshot()))
                .extracting(LevelProblem::message)
                .noneMatch(message -> message.contains("border"));
        history.undo();
        assertThat(level.width()).isEqualTo(9);
        assertThat(level.cellAt(MIDDLE)).isEqualTo(Cell.CRATE);
    }

    @Test
    void renameAndDensityAreUndoableToo() {
        history.execute(LevelChangeCommand.rename("Arena"));
        history.execute(LevelChangeCommand.crateDensity(0.9));

        history.undo();
        assertThat(level.crateDensity()).isEqualTo(0.5);
        history.undo();
        assertThat(level.name()).isEqualTo("Test");
    }
}
