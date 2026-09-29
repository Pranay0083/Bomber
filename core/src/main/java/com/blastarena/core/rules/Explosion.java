package com.blastarena.core.rules;

import com.blastarena.core.entity.Bomb;
import com.blastarena.core.model.Position;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * What one bomb's blast reaches.
 *
 * @param bomb      the bomb that exploded
 * @param fireTiles every tile the blast burns, including the bomb's own tile and any crate tiles it hit
 * @param cratesHit crates (or other destructible tiles) the blast reached
 * @param triggered other bombs caught in the blast, in the order they were found
 */
public record Explosion(Bomb bomb, Set<Position> fireTiles, Set<Position> cratesHit, List<Bomb> triggered) {

    /** Sets keep the order the blast reached each tile, so anything built from them is deterministic. */
    public Explosion {
        fireTiles = Collections.unmodifiableSet(new LinkedHashSet<>(fireTiles));
        cratesHit = Collections.unmodifiableSet(new LinkedHashSet<>(cratesHit));
        triggered = List.copyOf(triggered);
    }
}
