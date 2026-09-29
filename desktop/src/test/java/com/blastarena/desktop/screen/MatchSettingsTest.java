package com.blastarena.desktop.screen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.blastarena.core.bot.Difficulty;
import org.junit.jupiter.api.Test;

class MatchSettingsTest {

    @Test
    void eachSettingCyclesThroughItsChoices() {
        MatchSettings settings = MatchSettings.DEFAULT;

        assertThat(settings.nextBotCount().bots()).isEqualTo(1);
        assertThat(settings.nextBotCount().nextBotCount().bots()).isEqualTo(2);
        assertThat(settings.nextDifficulty().difficulty()).isEqualTo(Difficulty.HARD);
        assertThat(settings.nextDifficulty().nextDifficulty().difficulty()).isEqualTo(Difficulty.EASY);
        assertThat(settings.nextMatchLength().bestOf()).isEqualTo(5);
        assertThat(settings.nextMatchLength().nextMatchLength().bestOf()).isEqualTo(1);
    }

    @Test
    void labelsReadNicely() {
        assertThat(MatchSettings.DEFAULT.difficultyLabel()).isEqualTo("Medium");
    }

    @Test
    void rejectsSettingsTheGameCannotPlay() {
        assertThatIllegalArgumentException().isThrownBy(() -> new MatchSettings(0, Difficulty.EASY, 3));
        assertThatIllegalArgumentException().isThrownBy(() -> new MatchSettings(4, Difficulty.EASY, 3));
        assertThatIllegalArgumentException().isThrownBy(() -> new MatchSettings(2, Difficulty.EASY, 2));
    }
}
