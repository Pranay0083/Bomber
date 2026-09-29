package com.blastarena.core.bot;

import static org.assertj.core.api.Assertions.assertThat;

import com.blastarena.core.command.Command;
import com.blastarena.core.command.IdleCommand;
import com.blastarena.core.command.MoveCommand;
import com.blastarena.core.command.PlaceBombCommand;
import com.blastarena.core.control.WorldView;
import com.blastarena.core.model.Direction;
import com.blastarena.core.model.PlayerId;
import com.blastarena.core.testing.BotWorlds;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class BotBehaviourTest {

    private static final PlayerId ONE = new PlayerId(1);

    private static Command firstCommand(Difficulty difficulty, String... rows) {
        WorldView view = BotWorlds.view(rows);
        return BotController.of(difficulty, ONE, 1L).nextCommand(view);
    }

    @ParameterizedTest
    @EnumSource(value = Difficulty.class, names = {"MEDIUM", "HARD"})
    void fleesOutOfABlastLine(Difficulty difficulty) {
        Command command = firstCommand(difficulty,
                "#####",
                "#1B.#",
                "#.###",
                "#...#",
                "#####");

        assertThat(command).isEqualTo(new MoveCommand(ONE, Direction.DOWN));
    }

    @ParameterizedTest
    @EnumSource(value = Difficulty.class, names = {"MEDIUM", "HARD"})
    void bombsACrateWhenThereIsAWayOut(Difficulty difficulty) {
        Command command = firstCommand(difficulty,
                "#####",
                "#1x.#",
                "#.###",
                "#...#",
                "#####");

        assertThat(command).isEqualTo(new PlaceBombCommand(ONE));
    }

    @ParameterizedTest
    @EnumSource(Difficulty.class)
    void neverBombsWithoutAWayOut(Difficulty difficulty) {
        Command command = firstCommand(difficulty,
                "#####",
                "#1x.#",
                "#####");

        assertThat(command).isNotEqualTo(new PlaceBombCommand(ONE));
    }

    @ParameterizedTest
    @EnumSource(value = Difficulty.class, names = {"MEDIUM", "HARD"})
    void walksToANearbyPowerUp(Difficulty difficulty) {
        Command command = firstCommand(difficulty,
                "#######",
                "#1..r.#",
                "#######");

        assertThat(command).isEqualTo(new MoveCommand(ONE, Direction.RIGHT));
    }

    @ParameterizedTest
    @EnumSource(value = Difficulty.class, names = {"MEDIUM", "HARD"})
    void attacksAnOpponentInReachWhenItCanGetAway(Difficulty difficulty) {
        Command command = firstCommand(difficulty,
                "#######",
                "#.12..#",
                "#.#####",
                "#.....#",
                "#######");

        assertThat(command).isEqualTo(new PlaceBombCommand(ONE));
    }

    @Test
    void withNoCratesLeftBotsGoLookingForOpponents() {
        String[] rows = {
            "#########",
            "#1......#",
            "#.#.#.#.#",
            "#......2#",
            "#########",
        };

        assertThat(firstCommand(Difficulty.HARD, rows)).isInstanceOf(MoveCommand.class);
        assertThat(firstCommand(Difficulty.MEDIUM, rows)).isInstanceOf(MoveCommand.class);
    }

    @ParameterizedTest
    @EnumSource(Difficulty.class)
    void staysPutWhenThereIsNothingToDo(Difficulty difficulty) {
        Command command = firstCommand(difficulty,
                "###",
                "#1#",
                "###");

        assertThat(command).isEqualTo(new IdleCommand(ONE));
    }

    @Test
    void theBotOnlyEverSeesTheReadOnlyView() {
        assertThat(BotController.class.getDeclaredFields())
                .noneMatch(field -> field.getType().getSimpleName().equals("GameWorld"));
    }
}
