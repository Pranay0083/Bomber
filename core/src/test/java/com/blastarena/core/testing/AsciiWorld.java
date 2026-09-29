package com.blastarena.core.testing;

import com.blastarena.core.board.Board;
import com.blastarena.core.engine.GameWorld;
import com.blastarena.core.entity.Bomb;
import com.blastarena.core.entity.Player;
import com.blastarena.core.entity.PowerUpDrop;
import com.blastarena.core.model.GameConfig;
import com.blastarena.core.model.PlayerId;
import com.blastarena.core.model.Position;
import com.blastarena.core.powerup.PowerUpType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds a whole world from a picture. On top of the {@link AsciiBoard} symbols,
 * {@code 1}-{@code 4} place that player on the tile, {@code B} places a range-1 bomb owned by player 1,
 * and {@code b}, {@code r}, {@code s} place an extra-bomb, range or speed power-up on the floor.
 * The board can be any size; the config only supplies timings.
 */
public final class AsciiWorld {

    private static final Map<Character, PowerUpType> POWER_UPS =
            Map.of('b', PowerUpType.EXTRA_BOMB, 'r', PowerUpType.BLAST_RANGE, 's', PowerUpType.SPEED);

    private AsciiWorld() {
    }

    public static GameWorld parse(String... rows) {
        return parse(GameConfig.builder().build(), rows);
    }

    public static GameWorld parse(GameConfig config, String... rows) {
        List<Position> bombPositions = new ArrayList<>();
        List<PowerUpDrop> drops = new ArrayList<>();
        List<Player> players = new ArrayList<>();
        String[] tileRows = new String[rows.length];
        for (int y = 0; y < rows.length; y++) {
            StringBuilder tiles = new StringBuilder(rows[y]);
            for (int x = 0; x < rows[y].length(); x++) {
                char symbol = rows[y].charAt(x);
                if (symbol == 'B') {
                    bombPositions.add(new Position(x, y));
                    tiles.setCharAt(x, '.');
                } else if (POWER_UPS.containsKey(symbol)) {
                    drops.add(new PowerUpDrop(new Position(x, y), POWER_UPS.get(symbol).create()));
                    tiles.setCharAt(x, '.');
                } else if (Character.isDigit(symbol)) {
                    players.add(new Player(new PlayerId(symbol - '0'), new Position(x, y)));
                }
            }
            tileRows[y] = tiles.toString();
        }

        Board board = AsciiBoard.parse(tileRows);
        GameWorld world = new GameWorld(config, board, players);
        drops.forEach(world::addDrop);
        if (!bombPositions.isEmpty()) {
            PlayerId owner = new PlayerId(1);
            world.player(owner).upgrade(stats -> stats.withBombCapacity(bombPositions.size()));
            for (Position position : bombPositions) {
                world.addBomb(new Bomb(owner, position, 1, config.fuseTicks(), Set.of()));
            }
        }
        return world;
    }
}
