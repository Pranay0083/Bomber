package com.blastarena.core.bot;

import static org.assertj.core.api.Assertions.assertThat;

import com.blastarena.core.control.PlayerSnapshot;
import com.blastarena.core.control.WorldView;
import com.blastarena.core.model.PlayerId;
import com.blastarena.core.model.Position;
import com.blastarena.core.testing.BotWorlds;
import org.junit.jupiter.api.Test;

class EscapeCheckerTest {

    private static final PlayerId ONE = new PlayerId(1);

    private final EscapeChecker escapeChecker = new EscapeChecker(new PathFinder());

    private boolean canEscape(String... rows) {
        WorldView view = BotWorlds.view(rows);
        PlayerSnapshot me = view.player(ONE).orElseThrow();
        return escapeChecker.canEscapeAfterPlacing(view, me, me.position());
    }

    @Test
    void escapesByTurningACornerOutOfTheBlastLine() {
        assertThat(canEscape(
                "1.#",
                "#x#")).isFalse();
        assertThat(canEscape(
                "1.#",
                "#.#")).isTrue();
    }

    @Test
    void aDeadEndHasNoEscape() {
        assertThat(canEscape("#1.#")).isFalse();
    }

    @Test
    void aStraightCorridorLongerThanTheRangeIsAnEscape() {
        assertThat(canEscape("1..")).isTrue();
    }

    @Test
    void theFuseMustBeLongEnoughToWalkOut() {
        // Ten ticks of fuse at five ticks a step: two steps, the second landing on tick 7.
        assertThat(canEscape("1...")).isTrue();
        assertThat(canEscape(
                "1.....",
                "######")).isTrue();
    }

    @Test
    void aBiggerBlastNeedsALongerEscape() {
        WorldView view = BotWorlds.view("1...");
        PlayerSnapshot me = view.player(ONE).orElseThrow();
        PlayerSnapshot bigRange = new PlayerSnapshot(me.id(), me.position(), true,
                me.stats().withRange(3), me.moveCooldown(), me.activeBombs());

        assertThat(escapeChecker.canEscapeAfterPlacing(view, me, me.position())).isTrue();
        assertThat(escapeChecker.canEscapeAfterPlacing(view, bigRange, me.position())).isFalse();
    }

    @Test
    void theEscapeRouteLeadsToATileTheBombCannotReach() {
        WorldView view = BotWorlds.view(
                "1..",
                ".#.");
        PlayerSnapshot me = view.player(ONE).orElseThrow();
        DangerMap withBomb = escapeChecker.dangerWithBombAt(view, me, me.position());

        var route = escapeChecker.escapeRoute(view, me, me.position(), withBomb).orElseThrow();

        assertThat(withBomb.isThreatened(route.getLast())).isFalse();
        assertThat(route.getLast()).isIn(new Position(2, 0), new Position(0, 1));
    }
}
