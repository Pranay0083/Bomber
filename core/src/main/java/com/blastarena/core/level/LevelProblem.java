package com.blastarena.core.level;

import com.blastarena.core.model.Position;
import java.util.Objects;
import java.util.Optional;

/** Something that stops a level from being played, and the tile to highlight if there is one. */
public record LevelProblem(String message, Optional<Position> at) {

    public LevelProblem {
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(at, "at");
    }

    public static LevelProblem of(String message) {
        return new LevelProblem(message, Optional.empty());
    }

    public static LevelProblem at(Position position, String message) {
        return new LevelProblem(message, Optional.of(position));
    }
}
