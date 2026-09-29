package com.blastarena.core.rules;

import com.blastarena.core.board.Board;
import com.blastarena.core.board.Destructible;
import com.blastarena.core.board.Tile;
import com.blastarena.core.engine.GameWorld;
import com.blastarena.core.entity.Bomb;
import com.blastarena.core.model.Direction;
import com.blastarena.core.model.Position;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Works out what a blast reaches, without changing the world.
 * A blast spreads from the bomb in all four directions up to its range. It stops before a solid wall,
 * and stops on the first destructible tile it hits after burning it. Other bombs do not stop it;
 * they are triggered.
 */
public final class ExplosionResolver {

    public Explosion resolve(GameWorld world, Bomb bomb) {
        Board board = world.board();
        Set<Position> fire = new LinkedHashSet<>();
        Set<Position> cratesHit = new LinkedHashSet<>();
        List<Bomb> triggered = new ArrayList<>();

        fire.add(bomb.position());
        for (Direction direction : Direction.values()) {
            Position position = bomb.position();
            for (int step = 1; step <= bomb.range(); step++) {
                position = position.step(direction);
                if (!board.isInside(position)) {
                    break;
                }
                Tile tile = board.tileAt(position);
                if (tile.blocksBlast()) {
                    if (tile instanceof Destructible) {
                        fire.add(position);
                        cratesHit.add(position);
                    }
                    break;
                }
                fire.add(position);
                Optional<Bomb> caught = world.bombAt(position);
                caught.ifPresent(triggered::add);
            }
        }
        return new Explosion(bomb, fire, cratesHit, triggered);
    }
}
