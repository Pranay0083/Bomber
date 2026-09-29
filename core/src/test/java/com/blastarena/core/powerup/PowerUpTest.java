package com.blastarena.core.powerup;

import static org.assertj.core.api.Assertions.assertThat;

import com.blastarena.core.entity.Player;
import com.blastarena.core.model.PlayerId;
import com.blastarena.core.model.PlayerStats;
import com.blastarena.core.model.Position;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

class PowerUpTest {

    static Stream<Arguments> powerUps() {
        return Stream.of(
                Arguments.of(new ExtraBombPowerUp(), (ToIntFunction<PlayerStats>) PlayerStats::bombCapacity, 1, 8),
                Arguments.of(new BlastRangePowerUp(), (ToIntFunction<PlayerStats>) PlayerStats::blastRange, 1, 8),
                Arguments.of(new SpeedPowerUp(), (ToIntFunction<PlayerStats>) PlayerStats::speedLevel, 0, 4));
    }

    @ParameterizedTest
    @MethodSource("powerUps")
    void eachPickupRaisesItsStatByOneUpToTheCap(PowerUp powerUp, ToIntFunction<PlayerStats> stat, int start, int cap) {
        Player player = new Player(new PlayerId(1), new Position(1, 1));
        assertThat(stat.applyAsInt(player.stats())).isEqualTo(start);

        powerUp.apply(player);
        assertThat(stat.applyAsInt(player.stats())).isEqualTo(start + 1);

        for (int i = 0; i < 20; i++) {
            powerUp.apply(player);
        }
        assertThat(stat.applyAsInt(player.stats())).isEqualTo(cap);
    }

    @ParameterizedTest
    @MethodSource("powerUps")
    void aPowerUpOnlyChangesItsOwnStat(PowerUp powerUp, ToIntFunction<PlayerStats> stat, int start, int cap) {
        Player player = new Player(new PlayerId(1), new Position(1, 1));

        powerUp.apply(player);

        int changed = 0;
        PlayerStats after = player.stats();
        changed += after.bombCapacity() != 1 ? 1 : 0;
        changed += after.blastRange() != 1 ? 1 : 0;
        changed += after.speedLevel() != 0 ? 1 : 0;
        assertThat(changed).isEqualTo(1);
    }

    @ParameterizedTest
    @EnumSource(PowerUpType.class)
    void eachTypeCreatesAPowerUpOfThatType(PowerUpType type) {
        assertThat(type.create().type()).isEqualTo(type);
    }
}
