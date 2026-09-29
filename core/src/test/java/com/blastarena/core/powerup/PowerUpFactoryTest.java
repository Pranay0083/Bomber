package com.blastarena.core.powerup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.Test;

class PowerUpFactoryTest {

    @Test
    void dropsAboutThirtyPercentOfTheTimeWithEveryKindEquallyLikely() {
        PowerUpFactory factory = new PowerUpFactory(new Random(5L), 0.3);
        Map<PowerUpType, Integer> counts = new EnumMap<>(PowerUpType.class);
        int rolls = 30_000;

        for (int i = 0; i < rolls; i++) {
            factory.rollDrop().ifPresent(powerUp -> counts.merge(powerUp.type(), 1, Integer::sum));
        }

        int drops = counts.values().stream().mapToInt(Integer::intValue).sum();
        assertThat((double) drops / rolls).isBetween(0.28, 0.32);
        assertThat(counts).containsOnlyKeys(PowerUpType.values());
        counts.values().forEach(count -> assertThat((double) count / drops).isBetween(0.30, 0.37));
    }

    @Test
    void sameSeedRollsTheSameDrops() {
        assertThat(roll(new PowerUpFactory(new Random(9L), 0.3), 200))
                .isEqualTo(roll(new PowerUpFactory(new Random(9L), 0.3), 200));
    }

    @Test
    void zeroChanceNeverDropsAndFullChanceAlwaysDrops() {
        assertThat(roll(new PowerUpFactory(new Random(1L), 0.0), 100)).containsOnly(Optional.empty());
        assertThat(roll(new PowerUpFactory(new Random(1L), 1.0), 100)).doesNotContain(Optional.empty());
    }

    @Test
    void canBeLimitedToSomeKinds() {
        PowerUpFactory factory = new PowerUpFactory(new Random(1L), 1.0, List.of(PowerUpType.SPEED));

        assertThat(roll(factory, 50)).containsOnly(Optional.of(PowerUpType.SPEED));
    }

    @Test
    void rejectsInvalidSettings() {
        assertThatIllegalArgumentException().isThrownBy(() -> new PowerUpFactory(new Random(), 1.5));
        assertThatIllegalArgumentException().isThrownBy(() -> new PowerUpFactory(new Random(), 0.3, List.of()));
    }

    private static List<Optional<PowerUpType>> roll(PowerUpFactory factory, int times) {
        List<Optional<PowerUpType>> results = new ArrayList<>();
        for (int i = 0; i < times; i++) {
            results.add(factory.rollDrop().map(PowerUp::type));
        }
        return results;
    }
}
