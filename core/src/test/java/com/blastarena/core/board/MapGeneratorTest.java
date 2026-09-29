package com.blastarena.core.board;

import static org.assertj.core.api.Assertions.assertThat;

import com.blastarena.core.model.GameConfig;
import com.blastarena.core.model.Position;
import com.blastarena.core.testing.AsciiBoard;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class MapGeneratorTest {

    private final MapGenerator generator = new MapGenerator();

    private Board generate(GameConfig config) {
        return generator.generate(config, new Random(config.seed()));
    }

    @ParameterizedTest
    @CsvSource({"13, 11", "9, 7", "21, 17", "15, 9"})
    void wallsSitOnTheBorderAndOnEveryEvenCell(int width, int height) {
        Board board = generate(GameConfig.builder().width(width).height(height).seed(7L).build());

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                boolean border = x == 0 || y == 0 || x == width - 1 || y == height - 1;
                boolean expectSolid = border || (x % 2 == 0 && y % 2 == 0);
                Tile tile = board.tileAt(new Position(x, y));
                assertThat(tile instanceof SolidWall)
                        .as("solid wall at (%d, %d)", x, y)
                        .isEqualTo(expectSolid);
            }
        }
    }

    @Test
    void spawnsAreTheFourCornersWithOpponentsOppositeEachOther() {
        Board board = generate(GameConfig.builder().build());

        assertThat(board.spawns()).containsExactly(
                new Position(1, 1), new Position(11, 9), new Position(11, 1), new Position(1, 9));
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, 1L, 2L, 3L, 42L, 1234L})
    void spawnTileAndItsTwoNeighboursAreAlwaysClear(long seed) {
        Board board = generate(GameConfig.builder().seed(seed).crateDensity(1.0).build());

        for (Position spawn : board.spawns()) {
            assertThat(board.tileAt(spawn)).isInstanceOf(Floor.class);
            long walkableNeighbours = spawn.neighbours().stream()
                    .filter(board::isInside)
                    .filter(neighbour -> board.tileAt(neighbour).isWalkable())
                    .count();
            assertThat(walkableNeighbours).as("walkable neighbours of %s", spawn).isEqualTo(2);
        }
    }

    @Test
    void sameSeedGivesTheSameMap() {
        GameConfig config = GameConfig.builder().seed(99L).build();

        assertThat(AsciiBoard.render(generate(config))).isEqualTo(AsciiBoard.render(generate(config)));
    }

    @Test
    void differentSeedsGiveDifferentMaps() {
        Board first = generate(GameConfig.builder().seed(1L).build());
        Board second = generate(GameConfig.builder().seed(2L).build());

        assertThat(AsciiBoard.render(first)).isNotEqualTo(AsciiBoard.render(second));
    }

    @Test
    void cratesFillAboutSeventyPercentOfTheFreeCells() {
        int crates = 0;
        int freeCells = 0;
        for (long seed = 0; seed < 200; seed++) {
            Board board = generate(GameConfig.builder().seed(seed).build());
            for (String row : AsciiBoard.render(board)) {
                crates += (int) row.chars().filter(c -> c == 'x').count();
                freeCells += (int) row.chars().filter(c -> c != '#').count();
            }
        }
        // 12 cells per board are kept clear around the spawns, so the overall share sits a little below 70%.
        double share = (double) crates / freeCells;
        assertThat(share).isBetween(0.55, 0.70);
    }

    @Test
    void zeroDensityGivesNoCrates() {
        Board board = generate(GameConfig.builder().crateDensity(0.0).build());

        assertThat(String.join("", AsciiBoard.render(board))).doesNotContain("x");
    }

    @Test
    void fullDensityFillsEveryCellOutsideTheSpawnZones() {
        Board board = generate(GameConfig.builder().width(9).height(7).crateDensity(1.0).build());

        assertThat(AsciiBoard.render(board)).containsExactly(
                "#########",
                "#..xxx..#",
                "#.#x#x#.#",
                "#xxxxxxx#",
                "#.#x#x#.#",
                "#..xxx..#",
                "#########");
    }
}
