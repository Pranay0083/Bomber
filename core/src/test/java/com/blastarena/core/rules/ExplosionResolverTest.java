package com.blastarena.core.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.blastarena.core.engine.GameWorld;
import com.blastarena.core.entity.Bomb;
import com.blastarena.core.entity.Player;
import com.blastarena.core.model.PlayerId;
import com.blastarena.core.model.Position;
import com.blastarena.core.testing.AsciiWorld;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ExplosionResolverTest {

    private static final PlayerId ONE = new PlayerId(1);

    private final ExplosionResolver resolver = new ExplosionResolver();

    /** Player 1 drops a bomb of the given range where they stand; {@code *} in the expected row marks fire. */
    @ParameterizedTest(name = "{0} range {1} -> {2}")
    @CsvSource({
        "..1..,      1, .***.",
        "..1..,      2, *****",
        "..1..,      9, *****",
        "#.1.#,      3, #***#",
        "..1#.,      3, ***#.",
        "#x1..,      3, #****",
        "xx1.x,      3, x****",
        "x.1.x.,     5, *****.",
        "..1xx,      2, ****x",
    })
    void blastRangeWallsAndCrates(String row, int range, String expected) {
        GameWorld world = AsciiWorld.parse(row);
        Bomb bomb = placeBomb(world, range);

        Explosion explosion = resolver.resolve(world, bomb);

        assertThat(render(row, explosion.fireTiles())).isEqualTo(expected);
    }

    @Test
    void spreadsVerticallyToo() {
        GameWorld world = AsciiWorld.parse(
                "x.#",
                "...",
                "#1.",
                "...",
                "...");
        Bomb bomb = placeBomb(world, 2);

        Explosion explosion = resolver.resolve(world, bomb);

        assertThat(explosion.fireTiles()).containsExactlyInAnyOrder(
                new Position(1, 2),
                new Position(1, 1), new Position(1, 0),
                new Position(1, 3), new Position(1, 4),
                new Position(2, 2));
    }

    @Test
    void reportsEveryCrateHitButNotCratesBehindThem() {
        GameWorld world = AsciiWorld.parse(
                "..x..",
                "..x..",
                "xx1.x",
                ".....");
        Bomb bomb = placeBomb(world, 3);

        Explosion explosion = resolver.resolve(world, bomb);

        assertThat(explosion.cratesHit())
                .containsExactlyInAnyOrder(new Position(2, 1), new Position(1, 2), new Position(4, 2));
    }

    @Test
    void bombsInThePathAreTriggeredAndDoNotStopTheBlast() {
        GameWorld world = AsciiWorld.parse("1.B..");
        Bomb other = world.bombs().getFirst();
        Bomb bomb = placeBomb(world, 4);

        Explosion explosion = resolver.resolve(world, bomb);

        assertThat(explosion.triggered()).containsExactly(other);
        assertThat(render("1.B..", explosion.fireTiles())).isEqualTo("*****");
    }

    @Test
    void aBombDoesNotTriggerItself() {
        GameWorld world = AsciiWorld.parse(".1.");
        Bomb bomb = placeBomb(world, 1);

        assertThat(resolver.resolve(world, bomb).triggered()).isEmpty();
    }

    @Test
    void resolvingDoesNotChangeTheWorld() {
        GameWorld world = AsciiWorld.parse("x1x");
        Bomb bomb = placeBomb(world, 1);

        resolver.resolve(world, bomb);

        assertThat(world.bombs()).containsExactly(bomb);
        assertThat(world.fires()).isEmpty();
        assertThat(world.board().tileAt(new Position(0, 0)).isWalkable()).isFalse();
    }

    private static Bomb placeBomb(GameWorld world, int range) {
        Player player = world.player(ONE);
        player.upgrade(stats -> stats.withRange(range).withBombCapacity(stats.bombCapacity() + 1));
        return new BombPlacer().place(world, player).orElseThrow();
    }

    private static String render(String row, Set<Position> fire) {
        StringBuilder rendered = new StringBuilder(row);
        for (Position position : fire) {
            rendered.setCharAt(position.x(), '*');
        }
        return rendered.toString();
    }
}
