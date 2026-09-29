package com.blastarena.core.control;

import com.blastarena.core.board.Tile;
import com.blastarena.core.engine.GamePhase;
import com.blastarena.core.model.GameConfig;
import com.blastarena.core.model.PlayerId;
import com.blastarena.core.model.Position;
import com.blastarena.core.powerup.PowerUpType;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A read-only look at the world, handed to controllers and the renderer.
 * Everything it returns is immutable, so bots can look but not touch.
 */
public interface WorldView {

    GameConfig config();

    /** Ticks run so far in this round. */
    long tick();

    GamePhase phase();

    int width();

    int height();

    boolean isInside(Position position);

    Tile tileAt(Position position);

    /** All players, alive or dead, in player-id order. */
    List<PlayerSnapshot> players();

    Optional<PlayerSnapshot> player(PlayerId id);

    /** Bombs in the order they were placed. */
    List<BombSnapshot> bombs();

    Optional<BombSnapshot> bombAt(Position position);

    Optional<PowerUpType> powerUpAt(Position position);

    /** Power-ups on the floor, in the order they appeared. */
    Map<Position, PowerUpType> powerUps();

    boolean isBurning(Position position);

    Set<Position> burningTiles();

    /**
     * How long fire on this tile has left, or 0 if it is not burning. A player on a tile with {@code n} ticks left
     * dies if still there at the end of any of the next {@code n - 1} ticks.
     */
    int fireTicksLeft(Position position);
}
