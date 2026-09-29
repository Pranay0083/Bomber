package com.blastarena.core.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import com.blastarena.core.model.PlayerId;
import com.blastarena.core.model.PlayerStats;
import com.blastarena.core.model.Position;
import org.junit.jupiter.api.Test;

class PlayerTest {

    private final Player player = new Player(new PlayerId(1), new Position(1, 1));

    @Test
    void startsAliveAtItsSpawnWithDefaultStats() {
        assertThat(player.isAlive()).isTrue();
        assertThat(player.position()).isEqualTo(new Position(1, 1));
        assertThat(player.stats()).isEqualTo(PlayerStats.DEFAULT);
        assertThat(player.activeBombs()).isZero();
        assertThat(player.canMove()).isTrue();
    }

    @Test
    void movingStartsACooldownThatTicksDown() {
        player.moveTo(new Position(2, 1), 2);

        assertThat(player.position()).isEqualTo(new Position(2, 1));
        assertThat(player.canMove()).isFalse();
        player.tickCooldown();
        assertThat(player.canMove()).isFalse();
        player.tickCooldown();
        assertThat(player.canMove()).isTrue();
        player.tickCooldown();
        assertThat(player.moveCooldown()).isZero();
    }

    @Test
    void cannotMoveDuringCooldown() {
        player.moveTo(new Position(2, 1), 3);

        assertThatIllegalStateException().isThrownBy(() -> player.moveTo(new Position(3, 1), 3));
    }

    @Test
    void bombCountIsLimitedByCapacity() {
        player.bombPlaced();

        assertThat(player.hasBombAvailable()).isFalse();
        assertThatIllegalStateException().isThrownBy(player::bombPlaced);

        player.bombExploded();
        assertThat(player.hasBombAvailable()).isTrue();
    }

    @Test
    void upgradingCapacityAllowsMoreBombs() {
        player.upgrade(stats -> stats.withBombCapacity(2));
        player.bombPlaced();

        assertThat(player.hasBombAvailable()).isTrue();
    }

    @Test
    void cannotExplodeMoreBombsThanWerePlaced() {
        assertThatIllegalStateException().isThrownBy(player::bombExploded);
    }

    @Test
    void deadPlayersCannotMoveOrPlaceBombsButTheirBombsStillExplode() {
        player.bombPlaced();
        player.kill();

        assertThat(player.isAlive()).isFalse();
        assertThat(player.canMove()).isFalse();
        assertThat(player.hasBombAvailable()).isFalse();
        assertThatIllegalStateException().isThrownBy(() -> player.moveTo(new Position(2, 1), 0));

        player.bombExploded();
        assertThat(player.activeBombs()).isZero();
    }
}
