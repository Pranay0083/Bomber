package com.blastarena.core.rules;

import com.blastarena.core.entity.Bomb;
import com.blastarena.core.model.Position;
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

    public Explosion {
        fireTiles = Set.copyOf(fireTiles);
        cratesHit = Set.copyOf(cratesHit);
        triggered = List.copyOf(triggered);
    }
}
