package com.blastarena.core.level;

import static org.assertj.core.api.Assertions.assertThat;

import com.blastarena.core.model.Position;
import com.blastarena.core.powerup.PowerUpType;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class LevelValidatorTest {

    private final LevelValidator validator = LevelValidator.standard();

    private static LevelData level(String... rows) {
        return new LevelData(1, "Test", rows[0].length(), rows.length, List.of(rows), List.of(), 0.0);
    }

    private static final String[] GOOD = {
        "#########",
        "#S.....S#",
        "#.#.#.#.#",
        "#...x...#",
        "#.#.#.#.#",
        "#S.....S#",
        "#########",
    };

    @Test
    void aWellFormedLevelHasNoProblems() {
        assertThat(validator.validate(level(GOOD))).isEmpty();
        assertThat(validator.isPlayable(Levels.small())).isTrue();
    }

    @Test
    void sizesMustBeOddAndWithinBounds() {
        LevelData tooSmall = level(
                "#######",
                "#S...S#",
                "#######");

        assertThat(new SizeRule().check(tooSmall)).extracting(LevelProblem::message)
                .anyMatch(message -> message.startsWith("Width"))
                .anyMatch(message -> message.startsWith("Height"));
    }

    @Test
    void theBorderMustBeSolidAndTheGapIsHighlighted() {
        String[] rows = GOOD.clone();
        rows[3] = "....x...#";

        assertThat(validator.validate(level(rows)))
                .containsExactly(LevelProblem.at(new Position(0, 3), "The border must be solid wall"));
    }

    @Test
    void needsTwoToFourSpawns() {
        String[] one = GOOD.clone();
        one[1] = "#S......#";
        one[5] = "#.......#";
        String[] five = GOOD.clone();
        five[3] = "#...S...#";

        assertThat(new SpawnCountRule().check(level(one))).hasSize(1);
        assertThat(new SpawnCountRule().check(level(five))).hasSize(1);
        assertThat(new SpawnCountRule().check(level(GOOD))).isEmpty();
    }

    @Test
    void aSpawnBoxedInByCratesOrZonesIsFlagged() {
        String[] rows = GOOD.clone();
        rows[1] = "#Sx...?S#";

        List<LevelProblem> problems = new SpawnExitsRule().check(level(rows));

        assertThat(problems).extracting(LevelProblem::at)
                .containsExactly(Optional.of(new Position(1, 1)), Optional.of(new Position(7, 1)));
    }

    @Test
    void spawnsWalledOffFromEachOtherAreFlaggedButCratesDoNotCount() {
        LevelData walledOff = level(
                "#########",
                "#S..#..S#",
                "#.#.#.#.#",
                "#...#...#",
                "#########");
        LevelData crateWall = level(
                "#########",
                "#S..x..S#",
                "#.#.?.#.#",
                "#...x...#",
                "#########");

        assertThat(new SpawnsConnectedRule().check(walledOff))
                .containsExactly(LevelProblem.at(new Position(7, 1), "Spawn at 7,1 is walled off from the first spawn"));
        assertThat(new SpawnsConnectedRule().check(crateWall)).isEmpty();
    }

    @Test
    void powerUpsMustLieOnFloorOrInACrateZoneButNotOnWallsOrSpawns() {
        LevelData level = new LevelData(1, "P", 9, 7, List.of(GOOD), List.of(
                new PlacedPowerUp(2, 2, PowerUpType.SPEED),
                new PlacedPowerUp(1, 1, PowerUpType.SPEED),
                new PlacedPowerUp(2, 1, PowerUpType.SPEED)), 0.0);

        assertThat(validator.validate(level)).containsExactly(
                LevelProblem.at(new Position(2, 2), "A power-up must lie on floor or in a crate zone"),
                LevelProblem.at(new Position(1, 1), "A power-up must lie on floor or in a crate zone"));
    }

    @Test
    void theValidatorRunsWhateverRulesItIsGiven() {
        LevelRule noName = level -> level.name().isBlank() ? List.of(LevelProblem.of("Give the level a name")) : List.of();
        LevelValidator custom = new LevelValidator(List.of(noName));

        assertThat(custom.validate(level(GOOD).withName(" "))).containsExactly(LevelProblem.of("Give the level a name"));
    }
}
