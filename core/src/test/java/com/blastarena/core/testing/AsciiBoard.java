package com.blastarena.core.testing;

import com.blastarena.core.board.Board;
import com.blastarena.core.board.Crate;
import com.blastarena.core.board.Floor;
import com.blastarena.core.board.SolidWall;
import com.blastarena.core.board.Tile;
import com.blastarena.core.model.Position;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiConsumer;

/**
 * Builds boards from pictures so tests read like the board they describe.
 * {@code #} solid wall, {@code x} crate, {@code .} floor, {@code 1}-{@code 4} floor with that player's spawn.
 */
public final class AsciiBoard {

    private AsciiBoard() {
    }

    public static Board parse(String... rows) {
        if (rows.length == 0) {
            throw new IllegalArgumentException("Board needs at least one row");
        }
        int width = rows[0].length();
        for (int y = 0; y < rows.length; y++) {
            if (rows[y].length() != width) {
                throw new IllegalArgumentException(
                        "Row " + y + " has length " + rows[y].length() + ", expected " + width);
            }
        }

        Map<Integer, Position> spawnsByPlayer = new TreeMap<>();
        forEachCell(rows, (position, symbol) -> {
            if (Character.isDigit(symbol)) {
                spawnsByPlayer.put(symbol - '0', position);
            }
        });

        Board board = new Board(width, rows.length, List.copyOf(spawnsByPlayer.values()));
        forEachCell(rows, (position, symbol) -> {
            if (!Character.isDigit(symbol)) {
                board.setTile(position, tileFor(symbol, position));
            }
        });
        return board;
    }

    private static void forEachCell(String[] rows, BiConsumer<Position, Character> action) {
        for (int y = 0; y < rows.length; y++) {
            for (int x = 0; x < rows[y].length(); x++) {
                action.accept(new Position(x, y), rows[y].charAt(x));
            }
        }
    }

    /** Renders tiles only, one string per row; spawns show as floor. */
    public static List<String> render(Board board) {
        List<String> rows = new ArrayList<>(board.height());
        for (int y = 0; y < board.height(); y++) {
            StringBuilder row = new StringBuilder(board.width());
            for (int x = 0; x < board.width(); x++) {
                row.append(symbolFor(board.tileAt(new Position(x, y))));
            }
            rows.add(row.toString());
        }
        return rows;
    }

    private static Tile tileFor(char symbol, Position position) {
        return switch (symbol) {
            case '#' -> new SolidWall();
            case 'x' -> new Crate();
            case '.' -> new Floor();
            default -> throw new IllegalArgumentException("Unknown symbol '" + symbol + "' at " + position);
        };
    }

    private static char symbolFor(Tile tile) {
        return switch (tile) {
            case SolidWall wall -> '#';
            case Crate crate -> 'x';
            case Floor floor -> '.';
        };
    }
}
