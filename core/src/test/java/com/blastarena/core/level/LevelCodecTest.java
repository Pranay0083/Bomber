package com.blastarena.core.level;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.blastarena.core.powerup.PowerUpType;
import org.junit.jupiter.api.Test;

class LevelCodecTest {

    private final LevelCodec codec = new LevelCodec();

    @Test
    void aLevelSurvivesASaveAndLoadRoundTrip() throws LevelFormatException {
        LevelData level = Levels.small();

        assertThat(codec.decode(codec.encode(level))).isEqualTo(level);
    }

    @Test
    void readsTheDocumentedFileFormat() throws LevelFormatException {
        String json = """
                {
                  "version": 1,
                  "name": "Four Rooms",
                  "width": 9,
                  "height": 7,
                  "rows": [
                    "#########",
                    "#S..x..S#",
                    "#.#x#x#.#",
                    "#x.....x#",
                    "#.#x#x#.#",
                    "#S..x..S#",
                    "#########"
                  ],
                  "powerUps": [{ "x": 4, "y": 3, "type": "BLAST_RANGE" }],
                  "crateDensity": 0.0
                }
                """;

        LevelData level = codec.decode(json);

        assertThat(level.name()).isEqualTo("Four Rooms");
        assertThat(level.spawns()).hasSize(4);
        assertThat(level.powerUps()).containsExactly(new PlacedPowerUp(4, 3, PowerUpType.BLAST_RANGE));
    }

    @Test
    void missingPowerUpsMeansNoneAndUnknownFieldsAreIgnored() throws LevelFormatException {
        String json = """
                {"version": 1, "name": "Tiny", "width": 1, "height": 1, "rows": ["S"],
                 "crateDensity": 0.0, "author": "someone"}
                """;

        assertThat(codec.decode(json).powerUps()).isEmpty();
    }

    @Test
    void refusesNewerVersions() {
        String json = codec.encode(Levels.small()).replace("\"version\" : 1", "\"version\" : 2");

        assertThatThrownBy(() -> codec.decode(json))
                .isInstanceOf(LevelFormatException.class)
                .hasMessageContaining("version 2");
    }

    @Test
    void refusesRowsThatDoNotMatchTheSize() {
        String json = """
                {"version": 1, "name": "Broken", "width": 3, "height": 1, "rows": ["S."], "crateDensity": 0.0}
                """;

        assertThatThrownBy(() -> codec.decode(json))
                .isInstanceOf(LevelFormatException.class)
                .hasMessageContaining("length");
    }

    @Test
    void refusesSomethingThatIsNotALevel() {
        assertThatThrownBy(() -> codec.decode("not json")).isInstanceOf(LevelFormatException.class);
    }
}
