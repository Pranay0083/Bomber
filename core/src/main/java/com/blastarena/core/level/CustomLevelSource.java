package com.blastarena.core.level;

import com.blastarena.core.board.Board;
import com.blastarena.core.board.Crate;
import com.blastarena.core.board.SolidWall;
import com.blastarena.core.model.GameConfig;
import com.blastarena.core.model.Position;
import com.blastarena.core.powerup.PowerUpType;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

/**
 * Plays a saved level (the Adapter pattern): turns {@link LevelData} into the board the engine already expects.
 * Crate zones are filled cell by cell in reading order from the config's seed, so a round replays exactly.
 */
public final class CustomLevelSource implements MapSource {

    private final LevelData level;

    public CustomLevelSource(LevelData level) {
        this.level = Objects.requireNonNull(level, "level");
    }

    @Override
    public Arena createArena(GameConfig config) {
        Random random = new Random(config.seed());
        Board board = new Board(level.width(), level.height(), level.spawns());
        for (int y = 0; y < level.height(); y++) {
            for (int x = 0; x < level.width(); x++) {
                Position position = new Position(x, y);
                switch (level.cellAt(position)) {
                    case WALL -> board.setTile(position, new SolidWall());
                    case CRATE -> board.setTile(position, new Crate());
                    case CRATE_ZONE -> {
                        if (random.nextDouble() < level.crateDensity()) {
                            board.setTile(position, new Crate());
                        }
                    }
                    case FLOOR, SPAWN -> {
                        // The board starts as floor.
                    }
                }
            }
        }
        Map<Position, PowerUpType> powerUps = new LinkedHashMap<>();
        for (PlacedPowerUp powerUp : level.powerUps()) {
            if (board.tileAt(powerUp.position()).isWalkable()) {
                powerUps.put(powerUp.position(), powerUp.type());
            }
        }
        return new Arena(board, powerUps);
    }
}
