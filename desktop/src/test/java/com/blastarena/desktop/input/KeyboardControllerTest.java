package com.blastarena.desktop.input;

import static org.assertj.core.api.Assertions.assertThat;

import com.badlogic.gdx.Input.Keys;
import com.blastarena.core.command.IdleCommand;
import com.blastarena.core.command.MoveCommand;
import com.blastarena.core.command.PlaceBombCommand;
import com.blastarena.core.model.Direction;
import com.blastarena.core.model.PlayerId;
import org.junit.jupiter.api.Test;

class KeyboardControllerTest {

    private static final PlayerId ONE = new PlayerId(1);

    private final KeyboardController keyboard = new KeyboardController(ONE);

    @Test
    void idleWhenNothingIsPressed() {
        assertThat(keyboard.nextCommand(null)).isEqualTo(new IdleCommand(ONE));
    }

    @Test
    void keepsMovingWhileAnArrowIsHeld() {
        keyboard.keyDown(Keys.RIGHT);

        assertThat(keyboard.nextCommand(null)).isEqualTo(new MoveCommand(ONE, Direction.RIGHT));
        assertThat(keyboard.nextCommand(null)).isEqualTo(new MoveCommand(ONE, Direction.RIGHT));

        keyboard.keyUp(Keys.RIGHT);
        assertThat(keyboard.nextCommand(null)).isEqualTo(new IdleCommand(ONE));
    }

    @Test
    void theLastPressedDirectionWinsAndReleasingItFallsBackToTheOther() {
        keyboard.keyDown(Keys.UP);
        keyboard.keyDown(Keys.A);

        assertThat(keyboard.nextCommand(null)).isEqualTo(new MoveCommand(ONE, Direction.LEFT));

        keyboard.keyUp(Keys.A);
        assertThat(keyboard.nextCommand(null)).isEqualTo(new MoveCommand(ONE, Direction.UP));
    }

    @Test
    void spaceDropsOneBombPerPressThenMovementResumes() {
        keyboard.keyDown(Keys.DOWN);
        keyboard.keyDown(Keys.SPACE);

        assertThat(keyboard.nextCommand(null)).isEqualTo(new PlaceBombCommand(ONE));
        assertThat(keyboard.nextCommand(null)).isEqualTo(new MoveCommand(ONE, Direction.DOWN));
    }

    @Test
    void ignoresOtherKeys() {
        assertThat(keyboard.keyDown(Keys.Q)).isFalse();
        assertThat(keyboard.nextCommand(null)).isEqualTo(new IdleCommand(ONE));
    }
}
