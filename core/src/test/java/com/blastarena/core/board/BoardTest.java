package com.blastarena.core.board;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.blastarena.core.model.Position;
import com.blastarena.core.testing.AsciiBoard;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class BoardTest {

    @Test
    void newBoardIsAllFloor() {
        Board board = new Board(3, 2, List.of());

        assertThat(AsciiBoard.render(board)).containsExactly("...", "...");
    }

    @Test
    void setTileReplacesOnlyThatCell() {
        Board board = new Board(3, 3, List.of());

        board.setTile(new Position(1, 2), new Crate());

        assertThat(board.tileAt(new Position(1, 2))).isEqualTo(new Crate());
        assertThat(AsciiBoard.render(board)).containsExactly("...", "...", ".x.");
    }

    @ParameterizedTest
    @CsvSource({"0, 0, true", "4, 2, true", "-1, 0, false", "5, 0, false", "0, 3, false", "2, -1, false"})
    void isInside(int x, int y, boolean expected) {
        Board board = new Board(5, 3, List.of());

        assertThat(board.isInside(new Position(x, y))).isEqualTo(expected);
    }

    @Test
    void readingOrWritingOutsideTheBoardFails() {
        Board board = new Board(5, 3, List.of());

        assertThatThrownBy(() -> board.tileAt(new Position(5, 0))).isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> board.setTile(new Position(0, -1), new Floor()))
                .isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    void spawnsMustBeOnTheBoard() {
        assertThatThrownBy(() -> new Board(3, 3, List.of(new Position(3, 1))))
                .isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    void rejectsEmptyDimensions() {
        assertThatIllegalArgumentException().isThrownBy(() -> new Board(0, 3, List.of()));
    }

    @Test
    void spawnListCannotBeChangedFromOutside() {
        Board board = new Board(3, 3, List.of(new Position(1, 1)));

        assertThatThrownBy(() -> board.spawns().add(new Position(0, 0)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void asciiBoardRoundTripsAndOrdersSpawnsByPlayer() {
        Board board = AsciiBoard.parse(
                "#####",
                "#2.x#",
                "#.#1#",
                "#####");

        assertThat(AsciiBoard.render(board)).containsExactly("#####", "#..x#", "#.#.#", "#####");
        assertThat(board.spawns()).containsExactly(new Position(3, 2), new Position(1, 1));
    }
}
