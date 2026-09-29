package com.blastarena.core.level;

import com.blastarena.core.board.MapGenerator;
import com.blastarena.core.model.GameConfig;
import java.util.Random;

/** The classic generated arena, sized and seeded by the config. */
public final class RandomMapSource implements MapSource {

    private final MapGenerator generator;

    public RandomMapSource(MapGenerator generator) {
        this.generator = generator;
    }

    @Override
    public Arena createArena(GameConfig config) {
        return Arena.of(generator.generate(config, new Random(config.seed())));
    }
}
