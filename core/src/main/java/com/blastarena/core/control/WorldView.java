package com.blastarena.core.control;

import com.blastarena.core.board.Tile;
import com.blastarena.core.engine.GamePhase;
import com.blastarena.core.model.GameConfig;
import com.blastarena.core.model.PlayerId;
import com.blastarena.core.model.Position;
import java.util.List;
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

    boolean isBurning(Position position);

    Set<Position> burningTiles();
}
