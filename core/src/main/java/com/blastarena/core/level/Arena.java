package com.blastarena.core.level;

import com.blastarena.core.board.Board;
import com.blastarena.core.model.Position;
import com.blastarena.core.powerup.PowerUpType;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** A board ready to play on, plus any power-ups lying on it from the start. */
public record Arena(Board board, Map<Position, PowerUpType> powerUps) {

    public Arena {
        Objects.requireNonNull(board, "board");
        powerUps = Collections.unmodifiableMap(new LinkedHashMap<>(powerUps));
    }

    public static Arena of(Board board) {
        return new Arena(board, Map.of());
    }
}
